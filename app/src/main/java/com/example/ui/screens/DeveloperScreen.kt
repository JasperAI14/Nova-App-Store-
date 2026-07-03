package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppEntity
import com.example.ui.viewmodel.NovaStoreViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun DeveloperScreen(
    viewModel: NovaStoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val userSession by viewModel.userSession.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val developerApps by viewModel.developerApps.collectAsState()

    val isLoggedIn = userSession?.isLoggedIn == true
    val pendingReviewApps = allApps.filter { it.status == "Pending Review" }

    // State to toggle upload form visibility when dashboard is empty
    var showUploadForm by remember { mutableStateOf(false) }

    // Upload Fields State
    var appName by remember { mutableStateOf("") }
    var shortDescription by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Productivity") }
    var isGame by remember { mutableStateOf(false) }
    var version by remember { mutableStateOf("1.0.0") }
    var developerName by remember { mutableStateOf(userSession?.displayName ?: "DevCorp") }
    var sizeMb by remember { mutableStateOf("12.5") }
    var apkFileName by remember { mutableStateOf("build_dist.apk") }
    var logoUrl by remember { mutableStateOf("") }
    var screenshotsCsv by remember { mutableStateOf("") }
    var supportWebsite by remember { mutableStateOf("") }
    var privacyPolicy by remember { mutableStateOf("") }
    var tagsCsv by remember { mutableStateOf("") }
    var keepOlderVersions by remember { mutableStateOf(false) }

    // Device File Pickers State
    var selectedApkUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedApkUri = uri
            val fileName = uri.path?.substringAfterLast('/') ?: "selected_app.apk"
            apkFileName = if (fileName.endsWith(".apk")) fileName else "$fileName.apk"
            Toast.makeText(context, "APK chosen: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedMediaUris = selectedMediaUris + uris
            val mediaUrls = uris.map { uri ->
                val name = uri.path?.substringAfterLast('/') ?: "media_file"
                "device://$name"
            }
            if (screenshotsCsv.isBlank()) {
                screenshotsCsv = mediaUrls.joinToString(", ")
            } else {
                screenshotsCsv = screenshotsCsv + ", " + mediaUrls.joinToString(", ")
            }
            Toast.makeText(context, "Added ${uris.size} media file(s)", Toast.LENGTH_SHORT).show()
        }
    }

    var isUploading by remember { mutableStateOf(false) }

    // Update Form States
    var updateApkName by remember { mutableStateOf("") }
    var updateVersionName by remember { mutableStateOf("") }
    var updateReleaseNotes by remember { mutableStateOf("") }
    var updateScreenshots by remember { mutableStateOf("") }
    var updateSelectedApkUri by remember { mutableStateOf<Uri?>(null) }
    var updateSelectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val updateApkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            updateSelectedApkUri = uri
            val fileName = uri.path?.substringAfterLast('/') ?: "updated_app.apk"
            updateApkName = if (fileName.endsWith(".apk")) fileName else "$fileName.apk"
            Toast.makeText(context, "APK chosen: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    val updateMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            updateSelectedMediaUris = updateSelectedMediaUris + uris
            val mediaUrls = uris.map { uri ->
                val name = uri.path?.substringAfterLast('/') ?: "media_file"
                "device://$name"
            }
            if (updateScreenshots.isBlank()) {
                updateScreenshots = mediaUrls.joinToString(", ")
            } else {
                updateScreenshots = updateScreenshots + ", " + mediaUrls.joinToString(", ")
            }
            Toast.makeText(context, "Added ${uris.size} media file(s)", Toast.LENGTH_SHORT).show()
        }
    }

    // Overlays State
    var appToEdit by remember { mutableStateOf<AppEntity?>(null) }
    var appToUpdate by remember { mutableStateOf<AppEntity?>(null) }
    var appToViewAnalytics by remember { mutableStateOf<AppEntity?>(null) }
    var appToDelete by remember { mutableStateOf<AppEntity?>(null) }

    LaunchedEffect(appToUpdate) {
        val app = appToUpdate
        if (app != null) {
            updateApkName = app.apkFileName
            updateVersionName = ""
            updateReleaseNotes = ""
            updateScreenshots = app.screenshotsCsv
            updateSelectedApkUri = null
            updateSelectedMediaUris = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D0B1C))
                .verticalScroll(scrollState)
        ) {
            // --- HEADER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131026))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AppRegistration,
                        contentDescription = null,
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Nova Creator Space",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Publish Bundles, Review Analytics, and Moderation Pipeline",
                            color = Color(0xFFB0AEC6),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (!isLoggedIn) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Developer Credentials Required", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Please authenticate your developer workspace inside the 'Profile' section using your Google Account to begin publishing APK packages.",
                        color = Color(0xFFB0AEC6),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {

                    // --- UPLOAD LIMIT STATUS BADGE ---
                    val promo = userSession?.promoCode ?: ""
                    val usedUploads = userSession?.dailyUploadsUsed ?: 0
                    val limitText = when (promo) {
                        "ADMIN" -> "ADMIN ACCESS — Unlimited uploads/day"
                        "JasperAI" -> "JASPERAI ACCESS — ${5 - usedUploads} of 5 uploads remaining today"
                        else -> "FREE ACCOUNT — ${2 - usedUploads} of 2 uploads remaining today"
                    }
                    val limitColor = when (promo) {
                        "ADMIN" -> Color(0xFFFFD100)
                        "JasperAI" -> Color(0xFF00FFFF)
                        else -> Color(0xFFB0AEC6)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("upload_limit_status_card"),
                        colors = CardDefaults.cardColors(containerColor = limitColor.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, limitColor.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = limitColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = limitText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // --- 1. FRIENDLY EMPTY-STATE DASHBOARD ---
                    if (developerApps.isEmpty() && !showUploadForm) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(Color(0xFF00F5D4).copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color(0xFF00F5D4),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Upload Your First App",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Publish your APK and share it with users around the world.",
                                    color = Color(0xFFB0AEC6),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { showUploadForm = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("empty_state_upload_button")
                                ) {
                                    Text("Upload App", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    } else {
                        // --- 2. FULL APP UPLOAD FORM FLOW ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Publish New App Bundle",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (developerApps.isNotEmpty()) {
                                TextButton(onClick = { showUploadForm = !showUploadForm }) {
                                    Text(
                                        text = if (showUploadForm) "Hide Form" else "Add New App",
                                        color = Color(0xFF00F5D4),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (showUploadForm || developerApps.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // App Name
                                    DeveloperField(label = "Application Name (Required)", value = appName, onValueChange = { appName = it }, placeholder = "e.g., Nova Notes Pro", testTag = "dev_app_name_input")
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Short Description
                                    DeveloperField(label = "Short Description (Required)", value = shortDescription, onValueChange = { shortDescription = it }, placeholder = "A brief catchphrase summary (max 80 chars)", testTag = "dev_short_desc_input")
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Description
                                    DeveloperField(label = "Detailed Description (Required)", value = description, onValueChange = { description = it }, placeholder = "Explain what your app does in detail...", testTag = "dev_description_input")
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Category & Game Toggle Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.0f)) {
                                            Text("Category Selector", color = Color(0xFFB0AEC6), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row {
                                                listOf("Productivity", "Tools", "Arcade", "RPG").forEach { cat ->
                                                    val isSel = category == cat
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(end = 4.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSel) Color(0xFF00F5D4) else Color(0xFF1F1B3E))
                                                            .clickable {
                                                                category = cat
                                                                if (cat == "Arcade" || cat == "RPG") isGame = true
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(cat, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Is this a game?", color = Color(0xFFB0AEC6), fontSize = 11.sp)
                                            Switch(
                                                checked = isGame,
                                                onCheckedChange = { isGame = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color(0xFF00F5D4),
                                                    checkedTrackColor = Color(0xFF7B2CBF)
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Version & Size
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            DeveloperField(label = "Version Name (e.g. 1.0.0)", value = version, onValueChange = { version = it }, placeholder = "1.0.0")
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            DeveloperField(label = "Size (MB)", value = sizeMb, onValueChange = { sizeMb = it }, placeholder = "12.5")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Logo URL & APK bundle Name
                                    DeveloperField(label = "Icon Drawable/URL (Optional)", value = logoUrl, onValueChange = { logoUrl = it }, placeholder = "e.g., https://domain.com/icon.png")
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Support Web & Privacy Policy & Tags
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            DeveloperField(label = "Support Website", value = supportWebsite, onValueChange = { supportWebsite = it }, placeholder = "https://nova.app/support")
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            DeveloperField(label = "Privacy Policy Link", value = privacyPolicy, onValueChange = { privacyPolicy = it }, placeholder = "https://nova.app/privacy")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    DeveloperField(label = "Tags / Keywords (Comma-Separated)", value = tagsCsv, onValueChange = { tagsCsv = it }, placeholder = "e.g., utilities, planning, secure")
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // --- VERSION MANAGEMENT OPTIONS ---
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0C20))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Keep Older Versions Available", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("Allow users to view, download, and roll back to previous app releases instead of only offering the latest build.", color = Color(0xFFB0AEC6), fontSize = 10.sp, lineHeight = 13.sp)
                                            }
                                            Switch(
                                                checked = keepOlderVersions,
                                                onCheckedChange = { keepOlderVersions = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color(0xFF00F5D4),
                                                    checkedTrackColor = Color(0xFF00F5D4).copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // --- APP PREVIEW UPLOAD SECTION ---
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0C20))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "App Preview",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Upload preview media that showcases your app. You can upload images, screenshots, demo videos, and screen recordings to help users understand your app before downloading.",
                                                color = Color(0xFFB0AEC6),
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Supported Images: PNG, JPG, JPEG, WEBP\nSupported Videos: MP4, WEBM, MOV",
                                                color = Color(0xFF00F5D4).copy(alpha = 0.8f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            DeveloperField(
                                                label = "Preview Media URL List (Optional, Comma-Separated)",
                                                value = screenshotsCsv,
                                                onValueChange = { screenshotsCsv = it },
                                                placeholder = "e.g., https://domain.com/video.mp4, https://domain.com/screen1.png",
                                                testTag = "dev_app_preview_input"
                                            )

                                             Spacer(modifier = Modifier.height(10.dp))
                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text(
                                                     text = "Select Preview Media from Device",
                                                     color = Color.White,
                                                     fontSize = 11.sp,
                                                     fontWeight = FontWeight.Bold
                                                 )
                                                 Button(
                                                     onClick = { mediaPickerLauncher.launch("*/*") },
                                                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                                     shape = RoundedCornerShape(8.dp),
                                                     contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                     modifier = Modifier.height(30.dp)
                                                 ) {
                                                     Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                                     Spacer(modifier = Modifier.width(4.dp))
                                                     Text("Browse Gallery", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                 }
                                             }

                                             if (selectedMediaUris.isNotEmpty()) {
                                                 Spacer(modifier = Modifier.height(8.dp))
                                                 Text(
                                                     text = "Selected Files (Swipe horizontally, tap X to remove):",
                                                     color = Color(0xFFB0AEC6),
                                                     fontSize = 10.sp
                                                 )
                                                 Spacer(modifier = Modifier.height(6.dp))
                                                 androidx.compose.foundation.lazy.LazyRow(
                                                     horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                     modifier = Modifier.fillMaxWidth()
                                                 ) {
                                                     items(selectedMediaUris) { uri ->
                                                         val fileName = uri.path?.substringAfterLast('/') ?: "media"
                                                         Card(
                                                             modifier = Modifier
                                                                 .width(130.dp)
                                                                 .height(60.dp),
                                                             colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1736)),
                                                             border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.2f))
                                                         ) {
                                                             Row(
                                                                 modifier = Modifier
                                                                     .fillMaxSize()
                                                                     .padding(6.dp),
                                                                 verticalAlignment = Alignment.CenterVertically,
                                                                 horizontalArrangement = Arrangement.SpaceBetween
                                                             ) {
                                                                 Column(modifier = Modifier.weight(1f)) {
                                                                     Text(
                                                                         text = fileName,
                                                                         color = Color.White,
                                                                         fontSize = 9.sp,
                                                                         fontWeight = FontWeight.Bold,
                                                                         maxLines = 1
                                                                     )
                                                                     Text(
                                                                         text = if (fileName.contains(Regex("(mp4|webm|mov|avi)", RegexOption.IGNORE_CASE))) "🎥 Video" else "🖼️ Image",
                                                                         color = Color(0xFF00F5D4),
                                                                         fontSize = 8.sp
                                                                     )
                                                                 }
                                                                 IconButton(
                                                                     onClick = {
                                                                         selectedMediaUris = selectedMediaUris - uri
                                                                         val mediaUrls = selectedMediaUris.map { u ->
                                                                             val name = u.path?.substringAfterLast('/') ?: "media_file"
                                                                             "device://$name"
                                                                         }
                                                                         screenshotsCsv = mediaUrls.joinToString(", ")
                                                                     },
                                                                     modifier = Modifier.size(18.dp)
                                                                 ) {
                                                                     Icon(
                                                                         imageVector = Icons.Default.Close,
                                                                         contentDescription = "Remove",
                                                                         tint = Color(0xFFD62246),
                                                                         modifier = Modifier.size(10.dp)
                                                                     )
                                                                 }
                                                             }
                                                         }
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                     Spacer(modifier = Modifier.height(12.dp))

                                     DeveloperField(label = "APK Package File Name (Required)", value = apkFileName, onValueChange = { apkFileName = it }, placeholder = "my_app_payload.apk")
                                     
                                     // Modern Device APK Picker Integration
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Card(
                                         modifier = Modifier.fillMaxWidth(),
                                         colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1736)),
                                         shape = RoundedCornerShape(10.dp),
                                         border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.3f))
                                     ) {
                                         Column(modifier = Modifier.padding(12.dp)) {
                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text(
                                                     text = "Select APK File from Storage",
                                                     color = Color.White,
                                                     fontSize = 12.sp,
                                                     fontWeight = FontWeight.Bold
                                                 )
                                                 Button(
                                                     onClick = { apkPickerLauncher.launch("*/*") },
                                                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                                     shape = RoundedCornerShape(8.dp),
                                                     contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                     modifier = Modifier.height(32.dp)
                                                 ) {
                                                     Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                                     Spacer(modifier = Modifier.width(4.dp))
                                                     Text("Browse Files", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                 }
                                             }
                                             Spacer(modifier = Modifier.height(8.dp))
                                             Text(
                                                 text = "🛡️ Privacy Protection: We use Android's modern system-level file picker, which secures your private data and does not require granting broad external storage permissions.",
                                                 color = Color(0xFFB0AEC6),
                                                 fontSize = 10.sp,
                                                 lineHeight = 13.sp
                                             )
                                             if (selectedApkUri != null) {
                                                 Spacer(modifier = Modifier.height(8.dp))
                                                 Row(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .background(Color(0xFF0D0B1C), RoundedCornerShape(6.dp))
                                                         .padding(8.dp),
                                                     verticalAlignment = Alignment.CenterVertically,
                                                     horizontalArrangement = Arrangement.SpaceBetween
                                                 ) {
                                                     Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                         Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(14.dp))
                                                         Spacer(modifier = Modifier.width(6.dp))
                                                         Text(
                                                             text = apkFileName,
                                                             color = Color.White,
                                                             fontSize = 11.sp,
                                                             fontWeight = FontWeight.Bold,
                                                             maxLines = 1
                                                         )
                                                     }
                                                     IconButton(
                                                         onClick = {
                                                             selectedApkUri = null
                                                             apkFileName = "build_dist.apk"
                                                         },
                                                         modifier = Modifier.size(20.dp)
                                                     ) {
                                                         Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFD62246), modifier = Modifier.size(12.dp))
                                                     }
                                                 }
                                             }
                                         }
                                     }

                                     Spacer(modifier = Modifier.height(8.dp))
                                     Text(
                                         text = "Requirements Note: APK package format is REQUIRED. Because this platform delivers offline-executable, direct device installer packages that bypass conventional stores, pure APK packages are the best option for instant on-device installation.",
                                         color = Color(0xFF00F5D4).copy(alpha = 0.9f),
                                         fontSize = 10.sp,
                                         fontWeight = FontWeight.Medium,
                                         lineHeight = 14.sp
                                     )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // SUBMIT ACTION WITH SCANNING TRIGGER
                                    Button(
                                        onClick = {
                                            if (appName.isBlank() || description.isBlank() || shortDescription.isBlank()) {
                                                Toast.makeText(context, "Please complete App Name, Short Description, and Full Description.", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            if (!apkFileName.endsWith(".apk")) {
                                                Toast.makeText(context, "Error: App bundle must be a valid APK package! Please specify an .apk file.", Toast.LENGTH_LONG).show()
                                                return@Button
                                            }
                                            isUploading = true
                                            viewModel.publishDeveloperApp(
                                                name = appName,
                                                shortDescription = shortDescription,
                                                description = description,
                                                category = category,
                                                isGame = isGame,
                                                version = version,
                                                developer = developerName,
                                                sizeMb = sizeMb.toFloatOrNull() ?: 12.5f,
                                                apkFileName = apkFileName,
                                                logoUrl = logoUrl,
                                                screenshotsCsv = screenshotsCsv,
                                                supportWebsite = supportWebsite,
                                                privacyPolicy = privacyPolicy,
                                                tagsCsv = tagsCsv,
                                                keepOlderVersions = keepOlderVersions
                                            ) { success, msg ->
                                                isUploading = false
                                                if (success) {
                                                    appName = ""
                                                    shortDescription = ""
                                                    description = ""
                                                    logoUrl = ""
                                                    screenshotsCsv = ""
                                                    supportWebsite = ""
                                                    privacyPolicy = ""
                                                    tagsCsv = ""
                                                    showUploadForm = false
                                                }
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("dev_publish_app_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                        shape = RoundedCornerShape(10.dp),
                                        enabled = !isUploading && appName.isNotBlank() && description.isNotBlank() && shortDescription.isNotBlank()
                                    ) {
                                        if (isUploading) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("${viewModel.scanningState}... Active", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Upload & Analyze APK Bundle", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // VirusTotal and extraction console outputs
                    if (viewModel.scanningState == "Analyzing") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFFB703).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF221A0F))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.IntegrationInstructions, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("APK MANIFEST METADATA EXTRACTION ACTIVE", color = Color(0xFFFFB703), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Parsing AndroidManifest.xml package parameters...", color = Color.White, fontSize = 11.sp)
                                Text("Extracting target Android Version codes & requested permissions...", color = Color.White, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = viewModel.scanningProgress,
                                        color = Color(0xFFFFB703),
                                        trackColor = Color(0xFF1B1736),
                                        modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("${(viewModel.scanningProgress * 100).toInt()}%", color = Color(0xFFFFB703), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (viewModel.scanningState == "Scanning") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF071F1B))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("VIRUSTOTAL MALWARE ANALYSIS ACTIVE", color = Color(0xFF00F5D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Uploading cryptographic hashes of $apkFileName to global directories...", color = Color.White, fontSize = 11.sp)
                                Text("Querying database across 72 threat indexes...", color = Color.White, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = viewModel.scanningProgress,
                                        color = Color(0xFF00F5D4),
                                        trackColor = Color(0xFF1B1736),
                                        modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("${(viewModel.scanningProgress * 100).toInt()}%", color = Color(0xFF00F5D4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // --- DEVELOPER UPLOADED HISTORY TRACKER ---
                    Text("Your Upload History", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (developerApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color(0xFF16132D), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No applications published yet.", color = Color(0xFF757193), fontSize = 12.sp)
                        }
                    } else {
                        developerApps.forEach { devApp ->
                            DeveloperAppTrackItem(
                                app = devApp,
                                onEditClick = { appToEdit = devApp },
                                onUpdateClick = { appToUpdate = devApp },
                                onAnalyticsClick = { appToViewAnalytics = devApp },
                                onDeleteClick = { appToDelete = devApp }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // --- COMPLIANCE MODERATION SIMULATOR PIPELINE ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SettingsSuggest, contentDescription = null, tint = Color(0xFFFF007F), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ecosystem Moderation Simulator", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simulates the admin review panel. Audit uploaded payloads and click 'Approve' to instantly list them under public category indices.",
                        color = Color(0xFFB0AEC6),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (pendingReviewApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color(0xFF16132D), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("All items processed. Queue is currently empty.", color = Color(0xFF757193), fontSize = 12.sp)
                        }
                    } else {
                        pendingReviewApps.forEach { pendingApp ->
                            ModerationPipelineItem(
                                app = pendingApp,
                                onApprove = { viewModel.approveDeveloperApp(pendingApp.id) },
                                onReject = { viewModel.rejectDeveloperApp(pendingApp.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // --- EDIT APP LISTING DIALOG OVERLAY (RESTRICTED TO LISTING DATA ONLY) ---
        val appEditing = appToEdit
        if (appEditing != null) {
            var editShortDescription by remember(appEditing) { mutableStateOf(appEditing.shortDescription) }
            var editDescription by remember(appEditing) { mutableStateOf(appEditing.description) }
            var editCategory by remember(appEditing) { mutableStateOf(appEditing.category) }
            var editIsGame by remember(appEditing) { mutableStateOf(appEditing.isGame) }
            var editTags by remember(appEditing) { mutableStateOf(appEditing.tagsCsv) }
            var editScreenshots by remember(appEditing) { mutableStateOf(appEditing.screenshotsCsv) }
            var editSupportUrl by remember(appEditing) { mutableStateOf(appEditing.supportWebsite) }
            var editPrivacyUrl by remember(appEditing) { mutableStateOf(appEditing.privacyPolicy) }
            var editAiAutoRepliesEnabled by remember(appEditing) { mutableStateOf(appEditing.aiAutoRepliesEnabled) }
            var editAiReviewTone by remember(appEditing) { mutableStateOf(appEditing.aiReviewTone) }
            var editAiTrainingExamplesCsv by remember(appEditing) { mutableStateOf(appEditing.aiTrainingExamplesCsv) }
            var editAiReviewBeforePosting by remember(appEditing) { mutableStateOf(appEditing.aiReviewBeforePosting) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = true) { /* prevent click through */ }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131026))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit Store Listing",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Safe listings policy in effect. You can edit storefront descriptions and preview media. Critical application-binary configurations can only be modified via the Update panel.",
                            color = Color(0xFFB0AEC6),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // LOCKED READ-ONLY BLOCKS
                        Text("Locked Metadata (Application Binary Bound)", color = Color(0xFFFF007F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0C20)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF007F), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Core application definitions are locked:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• App Name: ${appEditing.name}", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                                Text("• Package: ${appEditing.packageName}", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                                Text("• Target APK Payload: ${appEditing.apkFileName}", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                                Text("• Version Name: ${appEditing.version} (Code ${appEditing.versionCode})", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                            }
                        }

                        // EDITABLE LISTING FIELDS
                        DeveloperField(label = "Short Description", value = editShortDescription, onValueChange = { editShortDescription = it }, placeholder = "A brief summary", testTag = "edit_app_short_desc")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Detailed Description", value = editDescription, onValueChange = { editDescription = it }, placeholder = "Full description text", testTag = "edit_app_desc")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Category", value = editCategory, onValueChange = { editCategory = it }, placeholder = "e.g. Tools", testTag = "edit_app_category")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Tags / Keywords", value = editTags, onValueChange = { editTags = it }, placeholder = "tools, utility, speed", testTag = "edit_app_tags")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Support Website", value = editSupportUrl, onValueChange = { editSupportUrl = it }, placeholder = "https://nova.app/support", testTag = "edit_app_support")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Privacy Policy Link", value = editPrivacyUrl, onValueChange = { editPrivacyUrl = it }, placeholder = "https://nova.app/privacy", testTag = "edit_app_privacy")
                        Spacer(modifier = Modifier.height(12.dp))

                        // APP PREVIEW
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0C20))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "App Preview Media",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Upload preview media that showcases your app. Supported format images (PNG, JPG, JPEG, WEBP) and videos (MP4, WEBM, MOV) are handled in a single feed.",
                                    color = Color(0xFFB0AEC6),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                DeveloperField(
                                    label = "Preview URLs (Comma-Separated)",
                                    value = editScreenshots,
                                    onValueChange = { editScreenshots = it },
                                    placeholder = "e.g., https://domain.com/screen1.png, https://domain.com/video1.mp4",
                                    testTag = "edit_screenshots"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- AI AUTO-REPLIES & REVIEW ASSISTANT (Goal 8) ---
                        val isPremiumDev = userSession?.isPremium == true
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF00F5D4).copy(alpha = if (isPremiumDev) 0.3f else 0.1f), RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0C20))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isPremiumDev) Color(0xFF00F5D4) else Color(0xFFFF007F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI Review Auto-Replies",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (!isPremiumDev) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF007F).copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "PREMIUM",
                                                color = Color(0xFFFF007F),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enable Gemini to automatically propose or post helpful replies to review feedback. AI will mirror your custom tone and adapt based on your training examples.",
                                    color = Color(0xFFB0AEC6),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Enable Auto-Replies", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = editAiAutoRepliesEnabled && isPremiumDev,
                                        onCheckedChange = { if (isPremiumDev) editAiAutoRepliesEnabled = it },
                                        enabled = isPremiumDev,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00F5D4),
                                            checkedTrackColor = Color(0xFF00F5D4).copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                if (editAiAutoRepliesEnabled && isPremiumDev) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Tone Selection Row
                                    Text("AI Tone Preference", color = Color.White, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val tones = listOf("Friendly", "Professional", "Enthusiastic", "Empathetic")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        tones.forEach { tone ->
                                            val isSelected = editAiReviewTone == tone
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { editAiReviewTone = tone },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFF00F5D4).copy(alpha = 0.2f) else Color(0xFF1E1A3C)
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF00F5D4) else Color.Transparent
                                                )
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                                    Text(tone, color = if (isSelected) Color(0xFF00F5D4) else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    DeveloperField(
                                        label = "Developer Training Examples",
                                        value = editAiTrainingExamplesCsv,
                                        onValueChange = { editAiTrainingExamplesCsv = it },
                                        placeholder = "Example: Review: Bad app -> Reply: Sorry to hear that! We will fix it.",
                                        testTag = "edit_ai_training_examples"
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Review AI Reply Before Posting", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = editAiReviewBeforePosting,
                                            onCheckedChange = { editAiReviewBeforePosting = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF00F5D4),
                                                checkedTrackColor = Color(0xFF00F5D4).copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { appToEdit = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A4F)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    viewModel.editDeveloperApp(
                                        appId = appEditing.id,
                                        shortDescription = editShortDescription,
                                        description = editDescription,
                                        category = editCategory,
                                        isGame = editIsGame,
                                        tagsCsv = editTags,
                                        screenshotsCsv = editScreenshots,
                                        supportWebsite = editSupportUrl,
                                        privacyPolicy = editPrivacyUrl,
                                        aiAutoRepliesEnabled = editAiAutoRepliesEnabled,
                                        aiReviewTone = editAiReviewTone,
                                        aiTrainingExamplesCsv = editAiTrainingExamplesCsv,
                                        aiReviewBeforePosting = editAiReviewBeforePosting
                                    ) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            appToEdit = null
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                modifier = Modifier.weight(1f).testTag("save_edit_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- UPDATE APP VERSION DIALOG OVERLAY (FOR NEW APK BINARIES) ---
        val appUpdating = appToUpdate
        if (appUpdating != null) {
            var isScanningUpdate by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = true) { /* prevent click through */ }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .border(1.dp, Color(0xFFFFB703).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131026))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Deploy Version Update",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Release an updated app build. This initiates APK security scans and increments the software's versionCode automatically in our indexes.",
                            color = Color(0xFFB0AEC6),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        DeveloperField(label = "New APK File Name (Required)", value = updateApkName, onValueChange = { updateApkName = it }, placeholder = "e.g., build_v2.apk")
                        
                        // Device APK Picker for Update Dialog
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select APK from Storage", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { updateApkPickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Browse Files", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (updateSelectedApkUri != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0D0B1C), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(updateApkName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                IconButton(
                                    onClick = {
                                        updateSelectedApkUri = null
                                        updateApkName = appUpdating.apkFileName
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFD62246), modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "New Version Name (Required)", value = updateVersionName, onValueChange = { updateVersionName = it }, placeholder = "e.g., 1.1.0")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Release Notes", value = updateReleaseNotes, onValueChange = { updateReleaseNotes = it }, placeholder = "What is new in this release?")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Update App Previews CSV (Optional)", value = updateScreenshots, onValueChange = { updateScreenshots = it }, placeholder = "Comma-separated screenshots or video URLs")
                        
                        // Device Media Picker for Update Dialog
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select New Media from Device", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { updateMediaPickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Browse Gallery", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (updateSelectedMediaUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(updateSelectedMediaUris) { uri ->
                                    val fileName = uri.path?.substringAfterLast('/') ?: "media"
                                    Card(
                                        modifier = Modifier.width(110.dp).height(50.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1736))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(fileName, color = Color.White, fontSize = 8.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    updateSelectedMediaUris = updateSelectedMediaUris - uri
                                                    val mediaUrls = updateSelectedMediaUris.map { u ->
                                                        val name = u.path?.substringAfterLast('/') ?: "media"
                                                        "device://$name"
                                                    }
                                                    updateScreenshots = mediaUrls.joinToString(", ")
                                                },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFD62246), modifier = Modifier.size(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isScanningUpdate) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF221A0F)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${viewModel.scanningState}... Analyzing build payloads", color = Color(0xFFFFB703), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(progress = viewModel.scanningProgress, color = Color(0xFFFFB703), trackColor = Color(0xFF1B1736), modifier = Modifier.fillMaxWidth().height(4.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { appToUpdate = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A4F)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isScanningUpdate
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    if (updateApkName.isBlank() || updateVersionName.isBlank()) {
                                        Toast.makeText(context, "Please complete APK File Name and Version Name.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!updateApkName.endsWith(".apk")) {
                                        Toast.makeText(context, "Error: App bundle must be an APK file.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isScanningUpdate = true
                                    viewModel.updateDeveloperApp(
                                        appId = appUpdating.id,
                                        newApkFileName = updateApkName,
                                        newVersion = updateVersionName,
                                        releaseNotes = updateReleaseNotes,
                                        updatedScreenshotsCsv = updateScreenshots
                                    ) { success, msg ->
                                        isScanningUpdate = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            appToUpdate = null
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                                modifier = Modifier.weight(1f).testTag("deploy_update_button"),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isScanningUpdate
                            ) {
                                Text("Apply & Scan", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- DEVELOPER ANALYTICS MODAL OVERLAY ---
        val appAnalytics = appToViewAnalytics
        if (appAnalytics != null) {
            val simulatedActive = (appAnalytics.downloads * 0.84).toInt()
            val simulatedReviews = (appAnalytics.downloads * 0.12).toInt().coerceAtLeast(3)
            val formatAdoption = "94.2%"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = true) { /* prevent click through */ }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131026))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analytics Dashboard",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(appAnalytics.name, color = Color(0xFF00F5D4), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))

                        // METRICS GRID
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1B3E)), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Total Downloads", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(appAnalytics.downloads.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1B3E)), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Active Installs", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(simulatedActive.toString(), color = Color(0xFF00F5D4), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1B3E)), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Rating Average", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(String.format("%.1f", appAnalytics.rating), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1B3E)), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Review Count", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(simulatedReviews.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // VERSION & ADOPTION RATES
                        Text("Version Statistics", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("v${appAnalytics.version} (Latest)", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                LinearProgressIndicator(progress = 0.85f, color = Color(0xFF00F5D4), trackColor = Color(0xFF0F0C20), modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("85%", color = Color.White, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Older releases", color = Color(0xFFB0AEC6), fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                LinearProgressIndicator(progress = 0.15f, color = Color(0xFFFFB703), trackColor = Color(0xFF0F0C20), modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("15%", color = Color(0xFFB0AEC6), fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFF0F0C20), RoundedCornerShape(8.dp)).padding(10.dp)) {
                            Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update Adoption: $formatAdoption adoption of latest update within 7 days of rollout.", color = Color.White, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { appToViewAnalytics = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close Analytics", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- DELETE APP CONFIRMATION DIALOG OVERLAY ---
        val appDeleting = appToDelete
        if (appDeleting != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = true) { /* prevent click through */ }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131026))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Delete Application Bundle?",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Are you sure you want to permanently delete '${appDeleting.name}'? This action is irreversible and will remove all app files, analytics, and user metadata from the Nova App Store.",
                            color = Color(0xFFB0AEC6),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { appToDelete = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A4F)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    viewModel.deleteDeveloperApp(appDeleting.id) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            appToDelete = null
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                                modifier = Modifier.weight(1f).testTag("confirm_delete_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Delete Forever", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    testTag: String = ""
) {
    Column {
        Text(label, color = Color(0xFFB0AEC6), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF757193), fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F0C20),
                unfocusedContainerColor = Color(0xFF0F0C20),
                focusedBorderColor = Color(0xFF00F5D4),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}

@Composable
fun DeveloperAppTrackItem(
    app: AppEntity,
    onEditClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val statusColor = when (app.status) {
        "Approved" -> Color(0xFF00F5D4)
        "Rejected" -> Color(0xFFD62246)
        else -> Color(0xFFFFB703)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1B3E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = app.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Pkg: ${app.packageName} (v${app.version})", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(app.status.uppercase(), color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Edit Listing
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A4F)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(30.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Update APK
                Button(
                    onClick = onUpdateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A4F)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(30.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // View Analytics
                Button(
                    onClick = onAnalyticsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A4F)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(30.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Analytics", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Delete Icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E2A4F))
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete App",
                        tint = Color(0xFFFF4444),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModerationPipelineItem(
    app: AppEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, Color(0xFFFFB703).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1736))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = app.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Developer: ${app.developer} | Size: ${app.sizeMb}MB", color = Color(0xFFB0AEC6), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(app.description, color = Color(0xFFB0AEC6), fontSize = 11.sp, maxLines = 2)

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD62246)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp).testTag("moderator_reject_button_${app.id}")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp).testTag("moderator_approve_button_${app.id}")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve Bundle", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
