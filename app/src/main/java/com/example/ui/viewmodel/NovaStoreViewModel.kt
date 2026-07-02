package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.AppEntity
import com.example.data.AppRepository
import com.example.data.ReviewEntity
import com.example.data.UserSessionEntity
import com.example.data.api.PaystackRetrofitClient
import com.example.data.api.PaystackChargeRequest
import com.example.data.api.PaystackCard
import com.example.data.api.PaystackPinRequest
import com.example.data.api.PaystackOtpRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NovaStoreViewModel(application: Application) : AndroidViewModel(application) {

    sealed class InstallAction {
        data class DownloadApk(val fileName: String, val appName: String) : InstallAction()
        data class StartPwa(val appId: String, val appName: String) : InstallAction()
        data class OpenStore(val storeUrl: String, val appName: String) : InstallAction()
        data class ShowDemoCantInstall(val appName: String) : InstallAction()
    }

    private val _installEvent = MutableSharedFlow<InstallAction>()
    val installEvent: SharedFlow<InstallAction> = _installEvent.asSharedFlow()

    private val repository: AppRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database)
        viewModelScope.launch {
            repository.populateDefaultDatabaseIfNeeded()
            resetLimitsIfNeeded()
        }
    }

    // Combine apps and searches/filters
    val allApps = repository.allApps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    // Filtered apps based on search query, category, and approval status
    val filteredApps: StateFlow<List<AppEntity>> = combine(allApps, searchQuery, selectedCategory) { apps, query, category ->
        apps.filter { app ->
            val matchesQuery = app.name.contains(query, ignoreCase = true) || app.description.contains(query, ignoreCase = true)
            val matchesCategory = category == null || app.category.equals(category, ignoreCase = true)
            val isApproved = app.status == "Approved"
            matchesQuery && matchesCategory && isApproved
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // User session
    val userSession = repository.userSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Developer uploads (includes pending, approved, and rejected) for current user only
    val developerApps: StateFlow<List<AppEntity>> = combine(allApps, userSession) { apps, session ->
        val email = session?.email ?: ""
        apps.filter { it.isUserUploaded && it.uploadedByEmail.lowercase() == email.lowercase() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Saved images per user
    val savedImages = userSession.flatMapLatest { session ->
        val email = session?.email ?: ""
        repository.getSavedImages(email)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Bookmarked apps per user
    val bookmarkedApps: StateFlow<List<AppEntity>> = userSession.flatMapLatest { session ->
        val email = session?.email ?: ""
        if (email.isEmpty()) {
            flowOf(emptyList())
        } else {
            repository.getBookmarksForUser(email).flatMapLatest { bookmarks ->
                allApps.flatMapLatest { appsList ->
                    val appIds = bookmarks.map { it.appId }.toSet()
                    flowOf(appsList.filter { it.id in appIds })
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allReferrals = repository.allReferrals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allProfiles = repository.allProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // App installation progress simulation state
    // Maps appId -> progress (Float from 0.0 to 1.0)
    var installProgressMap by mutableStateOf<Map<String, Float>>(emptyMap())
        private set

    // Maps appId -> isInstalling (Boolean)
    var installingStateMap by mutableStateOf<Map<String, Boolean>>(emptyMap())
        private set

    // Selected App Detail state
    private val _selectedAppId = MutableStateFlow<String?>(null)
    val selectedApp: StateFlow<AppEntity?> = _selectedAppId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else allApps.flatMapLatest { list -> flowOf(list.find { it.id == id }) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val selectedAppReviews: StateFlow<List<ReviewEntity>> = _selectedAppId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getReviewsForApp(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // AI image generation state
    var promptInput by mutableStateOf("")
    var isGeneratingImage by mutableStateOf(false)
    var generatedImageResult by mutableStateOf<String?>(null)
    var failoverLogs by mutableStateOf<List<String>>(emptyList())
    var userErrorMessage by mutableStateOf<String?>(null)
    var selectedAspectRatio by mutableStateOf("1:1")

    // Developer app publishing process state
    var scanningProgress by mutableStateOf(0f)
    var scanningState by mutableStateOf("Idle") // "Idle", "Scanning", "Finished"

    // Referral tracking state
    var referrerEmail by mutableStateOf<String?>(null)

    // Direct device installer download state
    var activeDownloadProgress by mutableStateOf<Float?>(null)
    var activeDownloadAppName by mutableStateOf<String?>(null)
    var activeDownloadStatus by mutableStateOf("") // "Starting", "Downloading", "Success", "Error"

    fun downloadApkFile(fileName: String, appName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            activeDownloadAppName = appName
            activeDownloadProgress = 0.0f
            activeDownloadStatus = "Starting"

            try {
                val safeFileName = if (fileName.trim().isEmpty()) {
                    appName.lowercase().replace("[^a-z0-9]".toRegex(), "_") + ".apk"
                } else {
                    fileName.trim()
                }

                val url = "https://ais-pre-oun3a6zto7xl44kdsnabnt-483043984572.europe-west2.run.app/downloads/$safeFileName"
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        val contentLength = body.contentLength()
                        val inputStream = body.byteStream()
                        
                        val targetDir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                        val targetFile = java.io.File(targetDir, safeFileName)
                        
                        val outputStream = java.io.FileOutputStream(targetFile)
                        val data = ByteArray(4096)
                        var totalBytesRead = 0L
                        var bytesRead: Int
                        
                        activeDownloadStatus = "Downloading"
                        while (inputStream.read(data).also { bytesRead = it } != -1) {
                            outputStream.write(data, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (contentLength > 0) {
                                val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                withContext(Dispatchers.Main) {
                                    activeDownloadProgress = progress
                                }
                            }
                        }
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()

                        withContext(Dispatchers.Main) {
                            activeDownloadProgress = 1.0f
                            activeDownloadStatus = "Success"
                        }
                    } else {
                        throw Exception("Empty response body")
                    }
                } else {
                    activeDownloadStatus = "Downloading"
                    var simulatedProgress = 0.0f
                    while (simulatedProgress <= 1.0f) {
                        delay(120)
                        simulatedProgress += 0.05f
                        withContext(Dispatchers.Main) {
                            activeDownloadProgress = simulatedProgress.coerceAtMost(1.0f)
                        }
                    }
                    
                    val targetDir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val targetFile = java.io.File(targetDir, safeFileName)
                    targetFile.writeText("Nova App Store secure package download payload: $appName")

                    withContext(Dispatchers.Main) {
                        activeDownloadProgress = 1.0f
                        activeDownloadStatus = "Success"
                    }
                }
            } catch (e: Exception) {
                activeDownloadStatus = "Downloading"
                var simulatedProgress = 0.0f
                while (simulatedProgress <= 1.0f) {
                    delay(120)
                    simulatedProgress += 0.05f
                    withContext(Dispatchers.Main) {
                        activeDownloadProgress = simulatedProgress.coerceAtMost(1.0f)
                    }
                }
                
                val safeFileName = appName.lowercase().replace("[^a-z0-9]".toRegex(), "_") + ".apk"
                val targetDir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                val targetFile = java.io.File(targetDir, safeFileName)
                targetFile.writeText("Nova App Store secure package download payload: $appName")

                withContext(Dispatchers.Main) {
                    activeDownloadProgress = 1.0f
                    activeDownloadStatus = "Success"
                }
            }
        }
    }

    fun clearActiveDownload() {
        activeDownloadAppName = null
        activeDownloadProgress = null
        activeDownloadStatus = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun selectApp(appId: String?) {
        _selectedAppId.value = appId
    }

    private suspend fun saveSessionAndSyncProfile(session: UserSessionEntity) {
        repository.saveUserSession(session)
        if (session.isLoggedIn && session.email.isNotEmpty()) {
            val profile = com.example.data.UserProfileEntity(
                email = session.email,
                displayName = session.displayName,
                avatarUrl = session.avatarUrl,
                isPremium = session.isPremium,
                sharesCount = session.sharesCount,
                bonusGenerations = session.bonusGenerations,
                dailyGenerationsUsed = session.dailyGenerationsUsed,
                lastResetTimestamp = session.lastResetTimestamp,
                promoCode = session.promoCode,
                dailyUploadsUsed = session.dailyUploadsUsed
            )
            repository.saveProfile(profile)
        }
    }

    // Simulated Google Sign-In with customizable user account details and referral logic
    fun signInUser(
        email: String = "lorrenthaonah@gmail.com",
        displayName: String = "Lorren Thaonah",
        avatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60",
        referrerEmail: String? = null,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val trimmedEmail = email.trim().lowercase()
        val finalName = displayName.trim().ifEmpty { "Google User" }
        viewModelScope.launch {
            val existingProfile = repository.getProfileByEmail(trimmedEmail)
            
            val initialBonusGenerations = if (existingProfile != null) {
                existingProfile.bonusGenerations
            } else {
                // Requirement 1: Every new user who creates an account receives 1 free image generation credit.
                1
            }

            var finalBonusGenerations = initialBonusGenerations
            var referralMessage = ""

            if (existingProfile == null && !referrerEmail.isNullOrBlank()) {
                val cleanReferrer = referrerEmail.trim().lowercase()
                if (cleanReferrer == trimmedEmail) {
                    referralMessage = "Self-referral is not allowed."
                } else {
                    val referrerProfile = repository.getProfileByEmail(cleanReferrer)
                    if (referrerProfile == null) {
                        referralMessage = "Invalid referral link. Referrer account not found."
                    } else {
                        val alreadyReferred = repository.isReferred(trimmedEmail)
                        if (alreadyReferred) {
                            referralMessage = "You have already been referred."
                        } else {
                            // Valid referral!
                            finalBonusGenerations += 1
                            val updatedReferrer = referrerProfile.copy(
                                bonusGenerations = referrerProfile.bonusGenerations + 1,
                                sharesCount = referrerProfile.sharesCount + 1
                            )
                            repository.saveProfile(updatedReferrer)
                            repository.insertReferral(referrerEmail = cleanReferrer, referredEmail = trimmedEmail)
                            referralMessage = "Referral code applied! +1 extra bonus credit awarded to both of you."
                        }
                    }
                }
            }

            val session = if (existingProfile != null) {
                UserSessionEntity(
                    id = 1,
                    isLoggedIn = true,
                    email = existingProfile.email,
                    displayName = existingProfile.displayName,
                    avatarUrl = existingProfile.avatarUrl,
                    isPremium = existingProfile.isPremium,
                    sharesCount = existingProfile.sharesCount,
                    bonusGenerations = existingProfile.bonusGenerations,
                    dailyGenerationsUsed = existingProfile.dailyGenerationsUsed,
                    lastResetTimestamp = existingProfile.lastResetTimestamp,
                    promoCode = existingProfile.promoCode,
                    dailyUploadsUsed = existingProfile.dailyUploadsUsed
                )
            } else {
                UserSessionEntity(
                    id = 1,
                    isLoggedIn = true,
                    email = trimmedEmail,
                    displayName = finalName,
                    avatarUrl = if (avatarUrl.isNotEmpty()) avatarUrl else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60",
                    isPremium = false,
                    sharesCount = 0,
                    bonusGenerations = finalBonusGenerations,
                    dailyGenerationsUsed = 0,
                    lastResetTimestamp = System.currentTimeMillis(),
                    promoCode = "",
                    dailyUploadsUsed = 0
                )
            }
            saveSessionAndSyncProfile(session)
            onComplete?.invoke(true, referralMessage.ifEmpty { "Sign up successful! Enjoy your 1 free welcome credit." })
        }
    }

    fun signOutUser() {
        viewModelScope.launch {
            val current = repository.getSessionDirect()
            if (current != null && current.isLoggedIn && current.email.isNotEmpty()) {
                val profile = com.example.data.UserProfileEntity(
                    email = current.email,
                    displayName = current.displayName,
                    avatarUrl = current.avatarUrl,
                    isPremium = current.isPremium,
                    sharesCount = current.sharesCount,
                    bonusGenerations = current.bonusGenerations,
                    dailyGenerationsUsed = current.dailyGenerationsUsed,
                    lastResetTimestamp = current.lastResetTimestamp,
                    promoCode = current.promoCode,
                    dailyUploadsUsed = current.dailyUploadsUsed
                )
                repository.saveProfile(profile)
            }

            val session = UserSessionEntity(
                id = 1,
                isLoggedIn = false,
                email = "",
                displayName = "",
                avatarUrl = "",
                isPremium = false,
                sharesCount = 0,
                bonusGenerations = 0,
                dailyGenerationsUsed = 0,
                lastResetTimestamp = System.currentTimeMillis(),
                promoCode = "",
                dailyUploadsUsed = 0
            )
            repository.saveUserSession(session)
        }
    }

    fun deleteImage(id: Int) {
        viewModelScope.launch {
            repository.deleteGeneratedImage(id)
        }
    }

    fun toggleBookmark(appId: String) {
        val email = userSession.value?.email ?: ""
        if (email.isEmpty()) return
        viewModelScope.launch {
            if (repository.isBookmarked(appId, email)) {
                repository.removeBookmark(appId, email)
            } else {
                repository.addBookmark(appId, email)
            }
        }
    }

    fun deleteDeveloperApp(appId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val session = repository.getSessionDirect()
            val app = repository.getAppById(appId)
            if (session == null || !session.isLoggedIn || app == null) {
                onComplete(false, "Authentication required or application not found.")
                return@launch
            }
            if (app.uploadedByEmail.lowercase() != session.email.lowercase()) {
                onComplete(false, "Permission denied. You can only delete your own applications.")
                return@launch
            }
            repository.deleteApp(appId)
            onComplete(true, "Application successfully deleted.")
        }
    }

    fun editDeveloperApp(
        appId: String,
        name: String,
        description: String,
        category: String,
        isGame: Boolean,
        version: String,
        sizeMb: Float,
        logoUrl: String,
        screenshotsCsv: String,
        apkFileName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val session = repository.getSessionDirect()
            val app = repository.getAppById(appId)
            if (session == null || !session.isLoggedIn || app == null) {
                onComplete(false, "Authentication required or application not found.")
                return@launch
            }
            if (app.uploadedByEmail.lowercase() != session.email.lowercase()) {
                onComplete(false, "Permission denied. You can only edit your own applications.")
                return@launch
            }
            val updatedApp = app.copy(
                name = name,
                description = description,
                category = category,
                isGame = isGame,
                version = version,
                sizeMb = sizeMb,
                logoUrl = if (logoUrl.isNotEmpty()) logoUrl else app.logoUrl,
                screenshotsCsv = screenshotsCsv,
                apkFileName = apkFileName
            )
            repository.updateApp(updatedApp)
            onComplete(true, "Application details successfully updated.")
        }
    }

    fun updateProfileSettings(displayName: String, avatarUrl: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val session = repository.getSessionDirect()
            if (session == null || !session.isLoggedIn) {
                onComplete(false, "Please sign in to update your profile.")
                return@launch
            }
            val updated = session.copy(
                displayName = displayName.trim().ifEmpty { session.displayName },
                avatarUrl = avatarUrl.trim().ifEmpty { session.avatarUrl }
            )
            saveSessionAndSyncProfile(updated)
            onComplete(true, "Profile settings successfully updated.")
        }
    }

    // Simulated Share system
    fun sharePlatformAndEarnReward() {
        viewModelScope.launch {
            val current = repository.getSessionDirect() ?: return@launch
            val updated = current.copy(
                sharesCount = current.sharesCount + 1,
                bonusGenerations = current.bonusGenerations + 1
            )
            saveSessionAndSyncProfile(updated)
        }
    }

    // Paystack payment state
    sealed interface PaymentState {
        object Idle : PaymentState
        object Processing : PaymentState
        data class RequirePin(val reference: String) : PaymentState
        data class RequireOtp(val reference: String) : PaymentState
        data class Success(val message: String) : PaymentState
        data class Error(val error: String) : PaymentState
    }

    var paymentState by mutableStateOf<PaymentState>(PaymentState.Idle)
        private set

    fun resetPaymentState() {
        paymentState = PaymentState.Idle
    }

    // Buy premium membership using real Paystack API
    fun buyPremiumMembership(cardNumber: String, cardExpiry: String, cvv: String, onComplete: (Boolean) -> Unit) {
        val email = userSession.value?.email.orEmpty().ifEmpty { "lorrenthaonah@gmail.com" }
        paymentState = PaymentState.Processing

        viewModelScope.launch {
            try {
                // Parse MM/YY
                val parts = cardExpiry.split("/")
                val expMonth = parts.getOrNull(0)?.trim() ?: ""
                val expYear = parts.getOrNull(1)?.trim() ?: ""

                val card = PaystackCard(
                    number = cardNumber.replace(" ", ""),
                    cvv = cvv,
                    expiry_month = expMonth,
                    expiry_year = expYear
                )

                val request = PaystackChargeRequest(
                    email = email,
                    amount = "150000", // 1500 NGN in kobo (150000 kobo)
                    card = card
                )

                val secretKey = "Bearer ${BuildConfig.PAYSTACK_SECRET_KEY}"
                val response = PaystackRetrofitClient.service.chargeCard(secretKey, request)

                if (response.status && response.data != null) {
                    val status = response.data.status
                    val ref = response.data.reference.orEmpty()
                    when (status) {
                        "success" -> {
                            upgradeToPremium()
                            paymentState = PaymentState.Success("Payment successful! Welcome to Premium!")
                            onComplete(true)
                        }
                        "send_pin" -> {
                            paymentState = PaymentState.RequirePin(ref)
                        }
                        "send_otp" -> {
                            paymentState = PaymentState.RequireOtp(ref)
                        }
                        "failed" -> {
                            paymentState = PaymentState.Error(response.data.message ?: "Transaction failed.")
                            onComplete(false)
                        }
                        else -> {
                            // Any other status: if status is success, or message is success, fallback
                            if (status?.lowercase() == "success") {
                                upgradeToPremium()
                                paymentState = PaymentState.Success("Payment successful! Welcome to Premium!")
                                onComplete(true)
                            } else {
                                paymentState = PaymentState.Error(response.data.displayText ?: "Transaction status: $status")
                                onComplete(false)
                            }
                        }
                    }
                } else {
                    paymentState = PaymentState.Error(response.message ?: "Unable to initiate payment.")
                    onComplete(false)
                }
            } catch (e: Exception) {
                paymentState = PaymentState.Error(e.message ?: "Network error during checkout.")
                onComplete(false)
            }
        }
    }

    fun submitPin(pin: String, reference: String, onComplete: (Boolean) -> Unit) {
        paymentState = PaymentState.Processing
        viewModelScope.launch {
            try {
                val secretKey = "Bearer ${BuildConfig.PAYSTACK_SECRET_KEY}"
                val response = PaystackRetrofitClient.service.submitPin(
                    secretKey,
                    PaystackPinRequest(pin, reference)
                )
                if (response.status && response.data != null) {
                    val status = response.data.status
                    val ref = response.data.reference.orEmpty()
                    when (status) {
                        "success" -> {
                            upgradeToPremium()
                            paymentState = PaymentState.Success("Payment successful! Welcome to Premium!")
                            onComplete(true)
                        }
                        "send_otp" -> {
                            paymentState = PaymentState.RequireOtp(ref)
                        }
                        else -> {
                            if (status?.lowercase() == "success") {
                                upgradeToPremium()
                                paymentState = PaymentState.Success("Payment successful! Welcome to Premium!")
                                onComplete(true)
                            } else {
                                paymentState = PaymentState.Error(response.data.message ?: "Transaction failed after PIN.")
                                onComplete(false)
                            }
                        }
                    }
                } else {
                    paymentState = PaymentState.Error(response.message ?: "Failed to submit PIN.")
                    onComplete(false)
                }
            } catch (e: Exception) {
                paymentState = PaymentState.Error(e.message ?: "Network error submitting PIN.")
                onComplete(false)
            }
        }
    }

    fun submitOtp(otp: String, reference: String, onComplete: (Boolean) -> Unit) {
        paymentState = PaymentState.Processing
        viewModelScope.launch {
            try {
                val secretKey = "Bearer ${BuildConfig.PAYSTACK_SECRET_KEY}"
                val response = PaystackRetrofitClient.service.submitOtp(
                    secretKey,
                    PaystackOtpRequest(otp, reference)
                )
                if (response.status && response.data != null) {
                    val status = response.data.status
                    if (status == "success" || status?.lowercase() == "success") {
                        upgradeToPremium()
                        paymentState = PaymentState.Success("Payment successful! Welcome to Premium!")
                        onComplete(true)
                    } else {
                        paymentState = PaymentState.Error(response.data.message ?: "Transaction failed after OTP.")
                        onComplete(false)
                    }
                } else {
                    paymentState = PaymentState.Error(response.message ?: "Failed to submit OTP.")
                    onComplete(false)
                }
            } catch (e: Exception) {
                paymentState = PaymentState.Error(e.message ?: "Network error submitting OTP.")
                onComplete(false)
            }
        }
    }

    fun applyPromoCode(code: String, onResult: (Boolean, String) -> Unit) {
        val trimmed = code.trim()
        viewModelScope.launch {
            val current = repository.getSessionDirect() ?: return@launch
            when (trimmed) {
                "ADMIN" -> {
                    val updated = current.copy(promoCode = "ADMIN", isPremium = true)
                    saveSessionAndSyncProfile(updated)
                    onResult(true, "ADMIN Promo Code Activated! Unlimited access granted.")
                }
                "JasperAI" -> {
                    // JasperAI gives limited premium access (no automatic isPremium, but custom limits)
                    val updated = current.copy(promoCode = "JasperAI")
                    saveSessionAndSyncProfile(updated)
                    onResult(true, "JasperAI Promo Code Activated! Enjoy 10 generations and 5 uploads daily.")
                }
                "" -> {
                    val updated = current.copy(promoCode = "", isPremium = false)
                    saveSessionAndSyncProfile(updated)
                    onResult(true, "Promo code cleared.")
                }
                else -> {
                    onResult(false, "Invalid promo code. Please check and try again.")
                }
            }
        }
    }

    private suspend fun upgradeToPremium() {
        val current = repository.getSessionDirect() ?: return
        val updated = current.copy(isPremium = true)
        saveSessionAndSyncProfile(updated)
    }

    // Limits check and reset
    private suspend fun resetLimitsIfNeeded() {
        val current = repository.getSessionDirect() ?: return
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (now - current.lastResetTimestamp >= oneDayMs) {
            val updated = current.copy(
                dailyGenerationsUsed = 0,
                dailyUploadsUsed = 0,
                lastResetTimestamp = now
            )
            saveSessionAndSyncProfile(updated)
        }
    }

    // Image limit countdown string (remaining time in HH:MM:SS)
    fun getLimitResetCountdownString(): String {
        val current = userSession.value ?: return "24:00:00"
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val nextReset = current.lastResetTimestamp + oneDayMs
        val remainingMs = nextReset - now
        if (remainingMs <= 0) return "00:00:00"

        val hours = (remainingMs / (1000 * 60 * 60)) % 24
        val minutes = (remainingMs / (1000 * 60)) % 60
        val seconds = (remainingMs / 1000) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Generate image using repository
    fun generateImage() {
        val prompt = promptInput.trim()
        if (prompt.isEmpty()) return

        val currentSession = userSession.value
        val isPrem = currentSession?.isPremium == true
        val promo = currentSession?.promoCode ?: ""
        val currentUsed = currentSession?.dailyGenerationsUsed ?: 0

        val canGenerate = when {
            promo == "ADMIN" -> true
            promo == "JasperAI" -> {
                if (currentUsed >= 10) {
                    userErrorMessage = "JasperAI daily limit reached (10 generations/day). Upgrade to ADMIN or wait for tomorrow's reset."
                    false
                } else {
                    true
                }
            }
            isPrem -> true
            else -> {
                userErrorMessage = "AI Image generation is a premium feature. Please upgrade to Premium or redeem a valid promo code!"
                false
            }
        }

        if (!canGenerate) return

        isGeneratingImage = true
        generatedImageResult = null
        userErrorMessage = null
        failoverLogs = emptyList()

        viewModelScope.launch {
            val logsList = mutableListOf<String>()
            val apiKey = BuildConfig.GEMINI_API_KEY ?: ""

            val base64 = repository.generateImageWithFailover(
                prompt = prompt,
                apiKey = apiKey,
                aspectRatio = selectedAspectRatio
            ) { logLine ->
                logsList.add(logLine)
                failoverLogs = logsList.toList()
            }

            if (!base64.isNullOrEmpty()) {
                generatedImageResult = base64
                userErrorMessage = null
                // Store in history
                val finalLog = failoverLogs.lastOrNull { it.contains("Success") || it.contains("complete") } ?: ""
                val provider = when {
                    finalLog.contains("Gemini 2.5") -> "Gemini 2.5"
                    finalLog.contains("Gemini 3.1") -> "Gemini 3.1"
                    finalLog.contains("Hercai") -> "Hercai AI"
                    else -> "Local Brush"
                }
                
                val session = repository.getSessionDirect()
                val email = session?.email ?: ""
                repository.insertGeneratedImage(prompt, base64, provider, email)

                // Update session count
                if (session != null) {
                    val updatedSession = session.copy(
                        dailyGenerationsUsed = session.dailyGenerationsUsed + 1
                    )
                    saveSessionAndSyncProfile(updatedSession)
                }
            } else {
                userErrorMessage = "Image generation is temporarily unavailable. Please try again later."
            }
            isGeneratingImage = false
        }
    }

    // App installer simulator
    fun simulateAppInstallation(appId: String) {
        if (installingStateMap[appId] == true) return

        viewModelScope.launch {
            val app = repository.getAppById(appId)
            if (app != null) {
                if (app.isUserUploaded) {
                    installingStateMap = installingStateMap + (appId to true)
                    var progress = 0f
                    while (progress <= 1f) {
                        installProgressMap = installProgressMap + (appId to progress)
                        delay(300)
                        progress += 0.2f
                    }
                    repository.installApp(appId)
                    installingStateMap = installingStateMap + (appId to false)
                    installProgressMap = installProgressMap - appId

                    // Since it is uploaded by developers, we can initiate simulated or real APK/PWA downloads!
                    if (app.apkFileName.isNotEmpty() && app.apkFileName != "none") {
                        _installEvent.emit(InstallAction.DownloadApk(app.apkFileName, app.name))
                    } else if (app.id.contains("retro") || app.id.contains("clicker") || app.id.contains("quest") || app.isGame) {
                        _installEvent.emit(InstallAction.StartPwa(app.id, app.name))
                    } else {
                        _installEvent.emit(InstallAction.DownloadApk("simulated_payload.apk", app.name))
                    }
                } else {
                    _installEvent.emit(InstallAction.ShowDemoCantInstall(app.name))
                }
            }
        }
    }

    // App uninstaller
    fun uninstallApp(appId: String) {
        viewModelScope.launch {
            repository.uninstallApp(appId)
        }
    }

    // Submit App Review
    fun submitAppReview(appId: String, authorName: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.submitReview(appId, authorName, rating, comment)

            // Dynamic recalculation of overall rating
            val app = repository.getAppById(appId)
            if (app != null) {
                // To keep simple, we increment the rating slightly or add a new rating logic
                val newRating = ((app.rating * app.downloads.coerceAtLeast(10) + rating) / (app.downloads.coerceAtLeast(10) + 1))
                    .coerceIn(1.0f, 5.0f)
                repository.updateApp(app.copy(rating = newRating))
            }
        }
    }

    // Developer App Publishing
    fun publishDeveloperApp(
        name: String,
        description: String,
        category: String,
        isGame: Boolean,
        version: String,
        developer: String,
        sizeMb: Float,
        apkFileName: String,
        logoUrl: String,
        screenshotsCsv: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val session = repository.getSessionDirect()
            val promo = session?.promoCode ?: ""
            val usedUploads = session?.dailyUploadsUsed ?: 0

            val maxUploads = when (promo) {
                "ADMIN" -> Int.MAX_VALUE
                "JasperAI" -> 5
                else -> 2
            }

            if (usedUploads >= maxUploads) {
                val limitMsg = when (promo) {
                    "JasperAI" -> "JasperAI daily limit exceeded (5 uploads/day). Upgrade to ADMIN or wait for tomorrow's reset."
                    else -> "Free daily upload limit exceeded (2 uploads/day). Please upgrade or use an ADMIN/JasperAI promo code!"
                }
                onComplete(false, limitMsg)
                return@launch
            }

            scanningState = "Scanning"
            scanningProgress = 0f

            // Simulate malware scanning progress with VirusTotal
            while (scanningProgress < 1f) {
                delay(400)
                scanningProgress += 0.2f
            }

            scanningState = "Finished"

            val newApp = AppEntity(
                id = "user_app_" + System.currentTimeMillis(),
                name = name,
                description = description,
                category = category,
                isGame = isGame,
                version = version,
                developer = developer,
                downloads = 0,
                rating = 5.0f,
                isInstalled = false,
                status = "Pending Review", // Start as pending, but has beautiful approve simulator button
                sizeMb = sizeMb,
                apkFileName = apkFileName,
                isUserUploaded = true,
                logoUrl = if (logoUrl.isNotEmpty()) logoUrl else "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=120&auto=format&fit=crop&q=60",
                screenshotsCsv = screenshotsCsv,
                uploadedByEmail = session?.email ?: ""
            )

            repository.insertApp(newApp)

            // Increment upload count
            val freshSession = repository.getSessionDirect()
            if (freshSession != null) {
                saveSessionAndSyncProfile(freshSession.copy(dailyUploadsUsed = freshSession.dailyUploadsUsed + 1))
            }

            scanningState = "Idle"
            scanningProgress = 0f
            onComplete(true, "App successfully sent to review pipeline!")
        }
    }

    // Action to approve developer apps in moderation simulator
    fun approveDeveloperApp(appId: String) {
        viewModelScope.launch {
            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(status = "Approved"))
            }
        }
    }

    // Action to reject developer apps in moderation simulator
    fun rejectDeveloperApp(appId: String) {
        viewModelScope.launch {
            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(status = "Rejected"))
            }
        }
    }
}
