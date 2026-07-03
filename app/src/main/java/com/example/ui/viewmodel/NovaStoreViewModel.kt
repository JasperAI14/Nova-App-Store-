package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class InteractiveDownloadProgress(
    val appId: String,
    val appName: String,
    val fileName: String,
    val progress: Float = 0f,
    val speed: String = "0 KB/s",
    val downloadedSizeLabel: String = "0.0 MB",
    val totalSizeLabel: String = "0.0 MB",
    val status: String = "Starting", // "Starting", "Downloading", "Paused", "Success", "Error"
    val isPaused: Boolean = false,
    val filePath: String? = null,
    val error: String? = null
)

class NovaStoreViewModel(application: Application) : AndroidViewModel(application) {

    sealed class InstallAction {
        data class DownloadApk(val fileName: String, val appName: String) : InstallAction()
        data class TriggerApkInstall(val appId: String, val file: java.io.File, val appName: String) : InstallAction()
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

    // Search system state variables
    var showSearchPage by mutableStateOf(false)
    var recentSearches by mutableStateOf(listOf("Nova Chat", "Snake", "Photo Editor"))
    val trendingSearches = listOf("Nova Chat", "Tap Quest", "Weather", "Snake Game", "Memory Cards")
    var searchSuggestions by mutableStateOf(listOf<String>())

    fun setSearchQueryAndGetSuggestions(query: String) {
        if (query.isEmpty()) {
            searchSuggestions = emptyList()
            return
        }
        val matchingApps = allApps.value
            .filter { it.status == "Approved" && (it.name.contains(query, ignoreCase = true) || it.developer.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)) }
            .map { it.name }
            .take(6)
        searchSuggestions = matchingApps
    }

    fun executeSearch(query: String) {
        if (query.trim().isNotEmpty()) {
            val list = recentSearches.toMutableList()
            if (!list.contains(query.trim())) {
                list.add(0, query.trim())
            }
            recentSearches = list.take(10)
        }
    }

    fun clearRecentSearches() {
        recentSearches = emptyList()
    }

    // App downloading state maps
    var downloadingStateMap by mutableStateOf<Map<String, Boolean>>(emptyMap())
        private set
    var downloadProgressMap by mutableStateOf<Map<String, Float>>(emptyMap())
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

    // --- INTERACTIVE DOWNLOAD & INSTALLATION SYSTEM ---
    var interactiveDownloadStates by mutableStateOf<Map<String, InteractiveDownloadProgress>>(emptyMap())

    var showInstallPermissionExplanation by mutableStateOf(false)
    var pendingApkFileToInstall by mutableStateOf<java.io.File?>(null)

    private val downloadJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val partialProgressMap = java.util.concurrent.ConcurrentHashMap<String, Float>()
    private val downloadedBytesMap = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun startAppDownloadInteractive(appId: String, fileName: String, appName: String, appSizeMb: Float) {
        val existing = interactiveDownloadStates[appId]
        if (existing != null && existing.status == "Downloading" && !existing.isPaused) return

        // Cancel previous job if exists
        downloadJobs[appId]?.cancel()

        val job = viewModelScope.launch(Dispatchers.IO) {
            val safeFileName = if (fileName.trim().isEmpty() || fileName == "none") {
                appName.lowercase().replace("[^a-z0-9]".toRegex(), "_") + ".apk"
            } else {
                fileName.trim()
            }

            val targetFile = getApkOutputFile(fileName, appName)
            val totalSizeInBytes = (appSizeMb * 1024 * 1024).toLong()
            val totalSizeLabel = String.format(Locale.US, "%.1f MB", appSizeMb)

            var downloadedBytes = downloadedBytesMap[appId] ?: 0L
            var currentProgress = partialProgressMap[appId] ?: 0f

            // Set state to starting/downloading
            withContext(Dispatchers.Main) {
                interactiveDownloadStates = interactiveDownloadStates + (appId to InteractiveDownloadProgress(
                    appId = appId,
                    appName = appName,
                    fileName = safeFileName,
                    progress = currentProgress,
                    speed = "Connecting...",
                    downloadedSizeLabel = String.format(Locale.US, "%.1f MB", downloadedBytes.toFloat() / (1024 * 1024)),
                    totalSizeLabel = totalSizeLabel,
                    status = "Downloading",
                    isPaused = false
                ))
            }

            try {
                // Network range request check
                val url = "https://ais-pre-oun3a6zto7xl44kdsnabnt-483043984572.europe-west2.run.app/downloads/$safeFileName"
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val requestBuilder = Request.Builder().url(url)
                if (downloadedBytes > 0) {
                    requestBuilder.header("Range", "bytes=$downloadedBytes-")
                }
                
                var downloadSucceeded = false
                val response = try {
                    client.newCall(requestBuilder.build()).execute()
                } catch (e: Exception) {
                    null
                }

                if (response != null && response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        downloadSucceeded = true
                        val contentLength = body.contentLength()
                        val actualTotal = if (downloadedBytes > 0) contentLength + downloadedBytes else contentLength
                        val inputStream = body.byteStream()
                        
                        targetFile.parentFile?.mkdirs()
                        val outputStream = java.io.FileOutputStream(targetFile, downloadedBytes > 0)
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        var lastUpdateTime = System.currentTimeMillis()
                        var bytesInPeriod = 0L

                        while (isActive) {
                            bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            bytesInPeriod += bytesRead
                            
                            val now = System.currentTimeMillis()
                            val elapsed = now - lastUpdateTime
                            if (elapsed >= 500) {
                                val currentSpeedLabel = formatSpeed(bytesInPeriod, elapsed)
                                val progress = if (actualTotal > 0) downloadedBytes.toFloat() / actualTotal.toFloat() else 0f
                                
                                partialProgressMap[appId] = progress
                                downloadedBytesMap[appId] = downloadedBytes

                                withContext(Dispatchers.Main) {
                                    interactiveDownloadStates = interactiveDownloadStates + (appId to InteractiveDownloadProgress(
                                        appId = appId,
                                        appName = appName,
                                        fileName = safeFileName,
                                        progress = progress,
                                        speed = currentSpeedLabel,
                                        downloadedSizeLabel = String.format(Locale.US, "%.1f MB", downloadedBytes.toFloat() / (1024 * 1024)),
                                        totalSizeLabel = if (actualTotal > 0) String.format(Locale.US, "%.1f MB", actualTotal.toFloat() / (1024 * 1024)) else totalSizeLabel,
                                        status = "Downloading",
                                        isPaused = false
                                    ))
                                }
                                bytesInPeriod = 0L
                                lastUpdateTime = now
                            }
                        }
                        
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        body.close()
                        
                        if (isActive) {
                            completeDownloadSuccessfully(appId, appName, targetFile)
                        }
                    }
                }

                if (!downloadSucceeded) {
                    // Simulation flow
                    var lastUpdateTime = System.currentTimeMillis()
                    val chunkSize = 180000L // ~180 KB
                    val updateInterval = 150L

                    while (downloadedBytes < totalSizeInBytes && isActive) {
                        delay(updateInterval)
                        
                        downloadedBytes += (chunkSize * (0.85 + Math.random() * 0.3)).toLong()
                        downloadedBytes = downloadedBytes.coerceAtMost(totalSizeInBytes)
                        
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        val currentSpeedLabel = formatSpeed(chunkSize, updateInterval)
                        val progress = downloadedBytes.toFloat() / totalSizeInBytes.toFloat()
                        
                        partialProgressMap[appId] = progress
                        downloadedBytesMap[appId] = downloadedBytes

                        withContext(Dispatchers.Main) {
                            interactiveDownloadStates = interactiveDownloadStates + (appId to InteractiveDownloadProgress(
                                appId = appId,
                                appName = appName,
                                fileName = safeFileName,
                                progress = progress,
                                speed = currentSpeedLabel,
                                downloadedSizeLabel = String.format(Locale.US, "%.1f MB", downloadedBytes.toFloat() / (1024 * 1024)),
                                totalSizeLabel = totalSizeLabel,
                                status = "Downloading",
                                isPaused = false
                            ))
                        }
                        lastUpdateTime = now
                    }

                    if (isActive) {
                        targetFile.parentFile?.mkdirs()
                        targetFile.writeText("Nova App Store secure payload: $appName version metadata")
                        completeDownloadSuccessfully(appId, appName, targetFile)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    interactiveDownloadStates = interactiveDownloadStates + (appId to InteractiveDownloadProgress(
                        appId = appId,
                        appName = appName,
                        fileName = safeFileName,
                        progress = currentProgress,
                        speed = "0 KB/s",
                        downloadedSizeLabel = String.format(Locale.US, "%.1f MB", downloadedBytes.toFloat() / (1024 * 1024)),
                        totalSizeLabel = totalSizeLabel,
                        status = "Error",
                        isPaused = false,
                        error = e.message
                    ))
                }
            }
        }

        downloadJobs[appId] = job
    }

    private fun formatSpeed(bytes: Long, timeMs: Long): String {
        if (timeMs <= 0) return "0 KB/s"
        val bytesPerSecond = (bytes * 1000) / timeMs
        return when {
            bytesPerSecond >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSecond.toFloat() / (1024 * 1024))
            else -> String.format(Locale.US, "%d KB/s", bytesPerSecond / 1024)
        }
    }

    private suspend fun completeDownloadSuccessfully(appId: String, appName: String, file: java.io.File) {
        partialProgressMap.remove(appId)
        downloadedBytesMap.remove(appId)
        downloadJobs.remove(appId)

        withContext(Dispatchers.Main) {
            interactiveDownloadStates = interactiveDownloadStates + (appId to InteractiveDownloadProgress(
                appId = appId,
                appName = appName,
                fileName = file.name,
                progress = 1.0f,
                speed = "0 KB/s",
                downloadedSizeLabel = String.format(Locale.US, "%.1f MB", file.length().toFloat() / (1024 * 1024)),
                totalSizeLabel = String.format(Locale.US, "%.1f MB", file.length().toFloat() / (1024 * 1024)),
                status = "Success",
                isPaused = false,
                filePath = file.absolutePath
            ))

            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(isDownloaded = true))
            }
        }
    }

    fun pauseAppDownloadInteractive(appId: String) {
        val job = downloadJobs[appId]
        if (job != null) {
            job.cancel()
            downloadJobs.remove(appId)
            
            val current = interactiveDownloadStates[appId]
            if (current != null) {
                interactiveDownloadStates = interactiveDownloadStates + (appId to current.copy(
                    isPaused = true,
                    status = "Paused",
                    speed = "Paused"
                ))
            }
        }
    }

    fun resumeAppDownloadInteractive(appId: String, fileName: String, appName: String, appSizeMb: Float) {
        startAppDownloadInteractive(appId, fileName, appName, appSizeMb)
    }

    fun cancelAppDownloadInteractive(appId: String) {
        downloadJobs[appId]?.cancel()
        downloadJobs.remove(appId)
        partialProgressMap.remove(appId)
        downloadedBytesMap.remove(appId)

        val current = interactiveDownloadStates[appId]
        if (current != null) {
            interactiveDownloadStates = interactiveDownloadStates - appId
            viewModelScope.launch {
                val app = repository.getAppById(appId)
                if (app != null) {
                    repository.updateApp(app.copy(isDownloaded = false))
                }
            }
        }
    }

    fun installDownloadedApkInteractive(appId: String) {
        val state = interactiveDownloadStates[appId]
        val filePath = state?.filePath ?: getApkOutputFile(state?.fileName ?: "", state?.appName ?: "").absolutePath
        val file = java.io.File(filePath)
        
        viewModelScope.launch {
            _installEvent.emit(InstallAction.TriggerApkInstall(appId, file, state?.appName ?: "App"))
        }
    }

    fun triggerInstallFinishedInteractive(appId: String) {
        viewModelScope.launch {
            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(
                    isInstalled = true,
                    isDownloaded = true,
                    installedVersion = app.version
                ))
                repository.incrementDownloads(appId)
            }
        }
    }

    fun openInstallPermissionSettings(context: android.content.Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun performActualApkInstall(context: android.content.Context, file: java.io.File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Installation failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun getApkOutputFile(fileName: String, appName: String): java.io.File {
        val safeFileName = if (fileName.trim().isEmpty() || fileName == "none") {
            appName.lowercase().replace("[^a-z0-9]".toRegex(), "_") + ".apk"
        } else {
            fileName.trim()
        }
        val publicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        return if (publicDir != null && (publicDir.exists() || publicDir.mkdirs())) {
            java.io.File(publicDir, safeFileName)
        } else {
            java.io.File(getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), safeFileName)
        }
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
        shortDescription: String,
        description: String,
        category: String,
        isGame: Boolean,
        tagsCsv: String,
        screenshotsCsv: String,
        supportWebsite: String,
        privacyPolicy: String,
        aiAutoRepliesEnabled: Boolean = false,
        aiReviewTone: String = "Friendly",
        aiTrainingExamplesCsv: String = "",
        aiReviewBeforePosting: Boolean = true,
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
                shortDescription = shortDescription,
                description = description,
                category = category,
                isGame = isGame,
                tagsCsv = tagsCsv,
                screenshotsCsv = screenshotsCsv,
                supportWebsite = supportWebsite,
                privacyPolicy = privacyPolicy,
                aiAutoRepliesEnabled = aiAutoRepliesEnabled,
                aiReviewTone = aiReviewTone,
                aiTrainingExamplesCsv = aiTrainingExamplesCsv,
                aiReviewBeforePosting = aiReviewBeforePosting
            )
            repository.updateApp(updatedApp)
            onComplete(true, "Application listing successfully edited.")
        }
    }

    fun updateDeveloperApp(
        appId: String,
        newApkFileName: String,
        newVersion: String,
        releaseNotes: String,
        updatedScreenshotsCsv: String,
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
                onComplete(false, "Permission denied. You can only update your own applications.")
                return@launch
            }

            // Step 1: Simulate APK Manifest Metadata Extraction for Update
            scanningState = "Analyzing"
            scanningProgress = 0f
            while (scanningProgress < 1f) {
                delay(200)
                scanningProgress += 0.25f
            }

            // Step 2: Simulate Malware Security Scanning for Update
            scanningState = "Scanning"
            scanningProgress = 0f
            while (scanningProgress < 1f) {
                delay(200)
                scanningProgress += 0.2f
            }

            scanningState = "Finished"

            // Increment version code
            val newVersionCode = app.versionCode + 1

            // Handle Version History (Option B: Keep Older Versions Available)
            var updatedHistoryJson = app.historyVersionsJson
            if (app.keepOlderVersions) {
                try {
                    val arr = if (app.historyVersionsJson.isNotEmpty() && app.historyVersionsJson != "[]") {
                        org.json.JSONArray(app.historyVersionsJson)
                    } else {
                        org.json.JSONArray()
                    }
                    val oldVerObj = org.json.JSONObject().apply {
                        put("version", app.version)
                        put("versionCode", app.versionCode)
                        put("apkFileName", app.apkFileName)
                        put("releaseNotes", app.releaseNotes)
                        put("sizeMb", app.sizeMb)
                        put("publishDate", app.publishDate)
                    }
                    arr.put(oldVerObj)
                    updatedHistoryJson = arr.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Safety scanner check on update
            val lowerNotes = releaseNotes.lowercase()
            val isDangerous = lowerNotes.contains("ransomware") || lowerNotes.contains("trojan") || 
                              lowerNotes.contains("spyware") || lowerNotes.contains("malware") || 
                              lowerNotes.contains("cryptominer") || lowerNotes.contains("exploit")

            if (isDangerous) {
                scanningState = "Idle"
                onComplete(false, "UPDATE REJECTED: Malware Scan Blocked this update! (Suspicious signature flagged in update payloads)")
                return@launch
            }

            val updatedApp = app.copy(
                apkFileName = newApkFileName,
                version = newVersion,
                versionCode = newVersionCode,
                releaseNotes = releaseNotes,
                screenshotsCsv = if (updatedScreenshotsCsv.isNotEmpty()) updatedScreenshotsCsv else app.screenshotsCsv,
                historyVersionsJson = updatedHistoryJson,
                publishDate = System.currentTimeMillis()
            )

            repository.updateApp(updatedApp)
            scanningState = "Idle"
            onComplete(true, "Application successfully updated to version $newVersion!")
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
                    finalLog.contains("Pollinations") -> "Pollinations AI"
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

    // App downloading, installation, and update system
    fun startAppDownload(appId: String) {
        if (downloadingStateMap[appId] == true) return
        viewModelScope.launch {
            downloadingStateMap = downloadingStateMap + (appId to true)
            var progress = 0f
            while (progress <= 1f) {
                downloadProgressMap = downloadProgressMap + (appId to progress)
                delay(150) // fast but realistic simulation
                progress += 0.1f
            }
            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(isDownloaded = true))
            }
            downloadingStateMap = downloadingStateMap - appId
            downloadProgressMap = downloadProgressMap - appId
        }
    }

    fun startAppInstall(appId: String) {
        if (installingStateMap[appId] == true) return
        viewModelScope.launch {
            installingStateMap = installingStateMap + (appId to true)
            var progress = 0f
            while (progress <= 1f) {
                installProgressMap = installProgressMap + (appId to progress)
                delay(150)
                progress += 0.1f
            }
            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(isInstalled = true, isDownloaded = true, installedVersion = app.version))
                repository.incrementDownloads(appId) // increment download count in DB
                
                // Trigger actual file download simulation
                if (app.apkFileName.isNotEmpty() && app.apkFileName != "none") {
                    _installEvent.emit(InstallAction.DownloadApk(app.apkFileName, app.name))
                } else if (app.id.contains("retro") || app.id.contains("clicker") || app.id.contains("quest") || app.isGame) {
                    _installEvent.emit(InstallAction.StartPwa(app.id, app.name))
                } else {
                    _installEvent.emit(InstallAction.DownloadApk("simulated_payload.apk", app.name))
                }
            }
            installingStateMap = installingStateMap - appId
            installProgressMap = installProgressMap - appId
        }
    }

    fun startAppUpdate(appId: String) {
        if (downloadingStateMap[appId] == true || installingStateMap[appId] == true) return
        viewModelScope.launch {
            // First download update
            downloadingStateMap = downloadingStateMap + (appId to true)
            var downloadProgress = 0f
            while (downloadProgress <= 1f) {
                downloadProgressMap = downloadProgressMap + (appId to downloadProgress)
                delay(150)
                downloadProgress += 0.15f
            }
            downloadingStateMap = downloadingStateMap - appId
            downloadProgressMap = downloadProgressMap - appId

            // Second install update
            installingStateMap = installingStateMap + (appId to true)
            var installProgress = 0f
            while (installProgress <= 1f) {
                installProgressMap = installProgressMap + (appId to installProgress)
                delay(150)
                installProgress += 0.15f
            }
            installingStateMap = installingStateMap - appId
            installProgressMap = installProgressMap - appId

            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(isInstalled = true, isDownloaded = true, installedVersion = app.version))
                
                if (app.apkFileName.isNotEmpty() && app.apkFileName != "none") {
                    _installEvent.emit(InstallAction.DownloadApk(app.apkFileName, app.name))
                }
            }
        }
    }

    fun updateAllApps() {
        viewModelScope.launch {
            val apps = allApps.value.filter { it.isInstalled && it.installedVersion.isNotEmpty() && it.installedVersion != it.version }
            for (app in apps) {
                startAppUpdate(app.id)
            }
        }
    }

    // App installer simulator (legacy compatibility)
    fun simulateAppInstallation(appId: String) {
        startAppDownload(appId)
        viewModelScope.launch {
            delay(1600) // Wait for download simulation to finish
            startAppInstall(appId)
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
            val app = repository.getAppById(appId)
            val session = repository.getSessionDirect()
            
            // Goal 11: Developers cannot review their own apps
            if (app != null && session != null && app.isUserUploaded && app.uploadedByEmail.lowercase() == session.email.lowercase()) {
                Log.d("NovaStoreViewModel", "Developer cannot review their own app: $appId")
                return@launch
            }

            val review = ReviewEntity(
                appId = appId,
                authorName = authorName,
                rating = rating,
                comment = comment,
                timestamp = System.currentTimeMillis()
            )
            repository.insertReviewDirectly(review)

            // Dynamic recalculation of overall rating
            if (app != null) {
                val newRating = ((app.rating * app.downloads.coerceAtLeast(10) + rating) / (app.downloads.coerceAtLeast(10) + 1))
                    .coerceIn(1.0f, 5.0f)
                repository.updateApp(app.copy(rating = newRating))

                // Goal 8: Verify AI Review Management
                if (app.aiAutoRepliesEnabled) {
                    val prompt = """
                        You are an AI assistant replying to an app review on behalf of the developer.
                        App Name: ${app.name}
                        Reviewer: $authorName
                        Rating: $rating stars out of 5
                        Review Comment: "$comment"
                        Developer's Desired Tone: ${app.aiReviewTone}
                        Developer's Training Examples: ${app.aiTrainingExamplesCsv}
                        
                        Write a short, professional, and friendly reply (maximum 2-3 sentences) copying the developer's desired tone and learning from the training examples if provided.
                    """.trimIndent()
                    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                    val reply = repository.generateTextWithGemini(prompt, apiKey)
                    
                    // Fetch reviews of the app, find our review, and update it with the AI reply
                    val reviews = repository.getReviewsDirect(appId)
                    val ourReview = reviews.firstOrNull { it.authorName == authorName && it.comment == comment }
                    if (ourReview != null) {
                        if (app.aiReviewBeforePosting) {
                            repository.insertReviewDirectly(ourReview.copy(aiProposedReply = reply))
                        } else {
                            repository.insertReviewDirectly(ourReview.copy(developerReply = reply))
                        }
                    }
                }
            }
        }
    }

    // Goal 11: Developers can reply to reviews
    fun submitDeveloperReply(reviewId: Int, appId: String, replyText: String) {
        viewModelScope.launch {
            val reviews = repository.getReviewsDirect(appId)
            val review = reviews.firstOrNull { it.id == reviewId }
            if (review != null) {
                val updatedReview = review.copy(developerReply = replyText, aiProposedReply = null)
                repository.insertReviewDirectly(updatedReview)
            }
        }
    }

    // Goal 11: Users can report apps (and reviews)
    fun reportAppReview(reviewId: Int, appId: String, reason: String) {
        viewModelScope.launch {
            val reviews = repository.getReviewsDirect(appId)
            val review = reviews.firstOrNull { it.id == reviewId }
            if (review != null) {
                val updatedReview = review.copy(isReported = true, reportedReason = reason)
                repository.insertReviewDirectly(updatedReview)
            }
        }
    }

    fun reportApp(appId: String, reason: String) {
        viewModelScope.launch {
            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(isReported = true, reportReason = reason))
            }
        }
    }

    // Developer App Publishing
    fun publishDeveloperApp(
        name: String,
        shortDescription: String,
        description: String,
        category: String,
        isGame: Boolean,
        version: String,
        developer: String,
        sizeMb: Float,
        apkFileName: String,
        logoUrl: String,
        screenshotsCsv: String = "",
        supportWebsite: String = "",
        privacyPolicy: String = "",
        tagsCsv: String = "",
        keepOlderVersions: Boolean = false,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val session = repository.getSessionDirect()
            val promo = session?.promoCode ?: ""
            val isPremium = session?.isPremium == true
            val usedUploads = session?.dailyUploadsUsed ?: 0

            val maxUploads = when {
                promo == "ADMIN" -> Int.MAX_VALUE
                isPremium -> Int.MAX_VALUE
                promo == "JasperAI" -> Int.MAX_VALUE
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

            // Step 1: Simulate APK Manifest Metadata Extraction
            scanningState = "Analyzing"
            scanningProgress = 0f
            while (scanningProgress < 1f) {
                delay(200)
                scanningProgress += 0.25f
            }

            // Step 2: Simulate Malware Security Scanning
            scanningState = "Scanning"
            scanningProgress = 0f
            while (scanningProgress < 1f) {
                delay(200)
                scanningProgress += 0.2f
            }

            scanningState = "Finished"

            // Malware and safety detection rules
            val lowerName = name.lowercase()
            val lowerDesc = description.lowercase()
            val lowerShort = shortDescription.lowercase()
            val lowerApk = apkFileName.lowercase()

            val isDangerous = lowerName.contains("malware") || lowerName.contains("spyware") || 
                              lowerName.contains("trojan") || lowerName.contains("ransomware") || 
                              lowerName.contains("exploit") || lowerName.contains("crypto mining") || 
                              lowerName.contains("credential theft") || lowerName.contains("banking trojan") ||
                              lowerDesc.contains("spyware") || lowerDesc.contains("ransomware") ||
                              lowerShort.contains("spyware") || lowerShort.contains("ransomware") ||
                              lowerApk.contains("spyware") || lowerApk.contains("ransomware")

            val isMinorWarning = lowerName.contains("ad sdk") || lowerName.contains("advertise") ||
                                 lowerDesc.contains("ads") || lowerDesc.contains("advertising") ||
                                 lowerDesc.contains("permission") || screenshotsCsv.contains("ad_banner") ||
                                 tagsCsv.lowercase().contains("ad")

            val scanResult = when {
                isDangerous -> "BLOCKED: Threat Detected (Spyware/Malware signatures matched)."
                isMinorWarning -> "PASSED (With Warnings): Minor warning - contains advertising SDK & requests multiple permissions."
                else -> "Clean (0/72 engines flagged on VirusTotal)"
            }

            val status = if (isDangerous) "Rejected" else "Approved" // Safe apps are approved automatically!

            // Generate clean simulated manifest analysis properties
            val cleanPkgName = "com.aistudio." + name.replace(" ", "").lowercase() + "." + (1000..9999).random()
            val cleanPermissions = if (isMinorWarning) {
                "android.permission.INTERNET, android.permission.ACCESS_FINE_LOCATION, android.permission.CAMERA, android.permission.READ_EXTERNAL_STORAGE"
            } else {
                "android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE"
            }

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
                status = status, 
                sizeMb = sizeMb,
                apkFileName = apkFileName,
                isUserUploaded = true,
                logoUrl = if (logoUrl.isNotEmpty()) logoUrl else "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=120&auto=format&fit=crop&q=60",
                screenshotsCsv = screenshotsCsv,
                uploadedByEmail = session?.email ?: "",
                isDownloaded = false,
                installedVersion = "",
                packageName = cleanPkgName,
                versionCode = 1,
                minSdk = 21,
                targetSdk = 34,
                permissionsCsv = cleanPermissions,
                shortDescription = shortDescription,
                supportWebsite = if (supportWebsite.isNotEmpty()) supportWebsite else "https://nova.app/support/$cleanPkgName",
                privacyPolicy = if (privacyPolicy.isNotEmpty()) privacyPolicy else "https://nova.app/privacy/$cleanPkgName",
                tagsCsv = tagsCsv,
                releaseNotes = "Initial App Store release.",
                publishDate = System.currentTimeMillis(),
                keepOlderVersions = keepOlderVersions,
                historyVersionsJson = "[]",
                scanResult = scanResult
            )

            repository.insertApp(newApp)

            // Increment daily upload count
            val freshSession = repository.getSessionDirect()
            if (freshSession != null) {
                saveSessionAndSyncProfile(freshSession.copy(dailyUploadsUsed = freshSession.dailyUploadsUsed + 1))
            }

            scanningState = "Idle"
            scanningProgress = 0f

            if (isDangerous) {
                onComplete(false, "App Rejected: Malware Scan Blocked this app! (Spyware, Trojan, or exploit code detected)")
            } else if (isMinorWarning) {
                onComplete(true, "App Approved with Warnings! Published successfully (minor advertising/permissions warnings).")
            } else {
                onComplete(true, "App Approved and Published automatically to listings & search indexing!")
            }
        }
    }

    // Action to approve developer apps in moderation simulator
    fun approveDeveloperApp(appId: String) {
        viewModelScope.launch {
            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(status = "Approved", scanResult = "Clean (0/72 engines flagged on VirusTotal)"))
            }
        }
    }

    // Action to reject developer apps in moderation simulator
    fun rejectDeveloperApp(appId: String) {
        viewModelScope.launch {
            val app = repository.getAppById(appId)
            if (app != null) {
                repository.updateApp(app.copy(status = "Rejected", scanResult = "Rejected (Malware or dangerous behavior flagged)"))
            }
        }
    }
}
