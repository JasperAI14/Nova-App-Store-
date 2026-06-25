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

    // Developer uploads (includes pending, approved, and rejected)
    val developerApps: StateFlow<List<AppEntity>> = allApps.flatMapLatest { apps ->
        flowOf(apps.filter { it.isUserUploaded })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Saved images
    val savedImages = repository.savedImages.stateIn(
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun selectApp(appId: String?) {
        _selectedAppId.value = appId
    }

    // Simulated Sign-In using user information
    fun signInUser() {
        viewModelScope.launch {
            val session = UserSessionEntity(
                id = 1,
                isLoggedIn = true,
                email = "lorrenthaonah@gmail.com",
                displayName = "Lorren Thaonah",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60",
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

    fun signOutUser() {
        viewModelScope.launch {
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

    // Simulated Share system
    fun sharePlatformAndEarnReward() {
        viewModelScope.launch {
            val current = repository.getSessionDirect() ?: return@launch
            val updated = current.copy(
                sharesCount = current.sharesCount + 1,
                bonusGenerations = current.bonusGenerations + 1
            )
            repository.saveUserSession(updated)
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
                    repository.saveUserSession(updated)
                    onResult(true, "ADMIN Promo Code Activated! Unlimited access granted.")
                }
                "JasperAI" -> {
                    // JasperAI gives limited premium access (no automatic isPremium, but custom limits)
                    val updated = current.copy(promoCode = "JasperAI")
                    repository.saveUserSession(updated)
                    onResult(true, "JasperAI Promo Code Activated! Enjoy 10 generations and 5 uploads daily.")
                }
                "" -> {
                    val updated = current.copy(promoCode = "", isPremium = false)
                    repository.saveUserSession(updated)
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
        repository.saveUserSession(updated)
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
            repository.saveUserSession(updated)
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
                    finalLog.contains("Pollinations") -> "Pollinations AI"
                    else -> "Local Brush"
                }
                repository.insertGeneratedImage(prompt, base64, provider)

                // Update session count
                val session = repository.getSessionDirect()
                if (session != null) {
                    val updatedSession = session.copy(
                        dailyGenerationsUsed = session.dailyGenerationsUsed + 1
                    )
                    repository.saveUserSession(updatedSession)
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

            // Dispatch installation success action based on metadata
            val app = repository.getAppById(appId)
            if (app != null) {
                if (app.apkFileName.isNotEmpty() && app.apkFileName != "none") {
                    _installEvent.emit(InstallAction.DownloadApk(app.apkFileName, app.name))
                } else if (app.id.contains("retro") || app.id.contains("clicker") || app.id.contains("quest") || app.isGame) {
                    _installEvent.emit(InstallAction.StartPwa(app.id, app.name))
                } else {
                    _installEvent.emit(InstallAction.OpenStore("https://play.google.com/store/apps/details?id=com.google.android.apps.maps", app.name))
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
                logoUrl = if (logoUrl.isNotEmpty()) logoUrl else "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=120&auto=format&fit=crop&q=60"
            )

            repository.insertApp(newApp)

            // Increment upload count
            val freshSession = repository.getSessionDirect()
            if (freshSession != null) {
                repository.saveUserSession(freshSession.copy(dailyUploadsUsed = freshSession.dailyUploadsUsed + 1))
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
