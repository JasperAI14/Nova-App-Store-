package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // Upload Fields State
    var appName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Productivity") }
    var isGame by remember { mutableStateOf(false) }
    var version by remember { mutableStateOf("1.0.0") }
    var developerName by remember { mutableStateOf(userSession?.displayName ?: "DevCorp") }
    var sizeMb by remember { mutableStateOf("12.5") }
    var apkFileName by remember { mutableStateOf("build_dist.apk") }
    var logoUrl by remember { mutableStateOf("") }
    var screenshotsCsv by remember { mutableStateOf("") }

    var isUploading by remember { mutableStateOf(false) }

    var appToEdit by remember { mutableStateOf<AppEntity?>(null) }
    var appToDelete by remember { mutableStateOf<AppEntity?>(null) }

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

                // --- UPLOAD FORM SECTION ---
                Text("Publish New App Bundle", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // App Name
                        DeveloperField(label = "Application Name", value = appName, onValueChange = { appName = it }, placeholder = "e.g., Nova Notes Pro", testTag = "dev_app_name_input")
                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        DeveloperField(label = "Detailed Description", value = description, onValueChange = { description = it }, placeholder = "Explain what your app does...", testTag = "dev_description_input")
                        Spacer(modifier = Modifier.height(12.dp))

                        // Category & Game Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                                DeveloperField(label = "Version Code", value = version, onValueChange = { version = it }, placeholder = "1.0.0")
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DeveloperField(label = "Size (MB)", value = sizeMb, onValueChange = { sizeMb = it }, placeholder = "12.5")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // Logo URL & APK bundle Name
                        DeveloperField(label = "Icon Drawable/URL", value = logoUrl, onValueChange = { logoUrl = it }, placeholder = "https://images.unsplash.com/... (optional)")
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
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        DeveloperField(label = "APK Package File Name (Required)", value = apkFileName, onValueChange = { apkFileName = it }, placeholder = "my_app_payload.apk")
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
                                if (appName.isBlank() || description.isBlank()) {
                                    Toast.makeText(context, "Please complete App Name and Description.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!apkFileName.endsWith(".apk")) {
                                    Toast.makeText(context, "Error: App bundle must be a valid APK package! Please specify an .apk file.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                isUploading = true
                                viewModel.publishDeveloperApp(
                                    name = appName,
                                    description = description,
                                    category = category,
                                    isGame = isGame,
                                    version = version,
                                    developer = developerName,
                                    sizeMb = sizeMb.toFloatOrNull() ?: 10.5f,
                                    apkFileName = apkFileName,
                                    logoUrl = logoUrl,
                                    screenshotsCsv = screenshotsCsv
                                ) { success, msg ->
                                    isUploading = false
                                    if (success) {
                                        appName = ""
                                        description = ""
                                        logoUrl = ""
                                        screenshotsCsv = ""
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
                            enabled = !isUploading && appName.isNotBlank() && description.isNotBlank()
                        ) {
                            if (isUploading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("VirusTotal Scan Active...", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload & Analyze APK Bundle", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // VirusTotal scanning live console output
                if (viewModel.scanningState == "Scanning") {
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

        // --- EDIT APP DIALOG OVERLAY ---
        val appEditing = appToEdit
        if (appEditing != null) {
            var editName by remember(appEditing) { mutableStateOf(appEditing.name) }
            var editDescription by remember(appEditing) { mutableStateOf(appEditing.description) }
            var editCategory by remember(appEditing) { mutableStateOf(appEditing.category) }
            var editIsGame by remember(appEditing) { mutableStateOf(appEditing.isGame) }
            var editVersion by remember(appEditing) { mutableStateOf(appEditing.version) }
            var editSize by remember(appEditing) { mutableStateOf(appEditing.sizeMb.toString()) }
            var editLogoUrl by remember(appEditing) { mutableStateOf(appEditing.logoUrl) }
            var editScreenshots by remember(appEditing) { mutableStateOf(appEditing.screenshotsCsv) }
            var editApkName by remember(appEditing) { mutableStateOf(appEditing.apkFileName) }

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
                        Text(
                            text = "Edit Application Details",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        DeveloperField(label = "Application Name", value = editName, onValueChange = { editName = it }, placeholder = "e.g. FitTrack", testTag = "edit_app_name")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Description", value = editDescription, onValueChange = { editDescription = it }, placeholder = "e.g. Healthy life daily planner", testTag = "edit_app_desc")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Category", value = editCategory, onValueChange = { editCategory = it }, placeholder = "e.g. Productivity, Finance", testTag = "edit_app_category")
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F0C20), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Is Game?", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = editIsGame,
                                onCheckedChange = { editIsGame = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00F5D4),
                                    checkedTrackColor = Color(0xFF00F5D4).copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.testTag("edit_app_is_game")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Version Code", value = editVersion, onValueChange = { editVersion = it }, placeholder = "1.0.0", testTag = "edit_app_version")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Size (MB)", value = editSize, onValueChange = { editSize = it }, placeholder = "15.0", testTag = "edit_app_size")
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "Launcher Logo URL (Optional)", value = editLogoUrl, onValueChange = { editLogoUrl = it }, placeholder = "Image web address", testTag = "edit_logo_url")
                        Spacer(modifier = Modifier.height(10.dp))

                        // --- APP PREVIEW EDIT SECTION ---
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
                                    value = editScreenshots,
                                    onValueChange = { editScreenshots = it },
                                    placeholder = "e.g., https://domain.com/video.mp4, https://domain.com/screen1.png",
                                    testTag = "edit_screenshots"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        DeveloperField(label = "APK Filename", value = editApkName, onValueChange = { editApkName = it }, placeholder = "app.apk", testTag = "edit_apk_name")
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
                                        name = editName,
                                        description = editDescription,
                                        category = editCategory,
                                        isGame = editIsGame,
                                        version = editVersion,
                                        sizeMb = editSize.toFloatOrNull() ?: 1.0f,
                                        logoUrl = editLogoUrl,
                                        screenshotsCsv = editScreenshots,
                                        apkFileName = editApkName
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
        Row(
            modifier = Modifier.padding(12.dp),
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
                Text("Pkg: ${app.apkFileName}", color = Color(0xFFB0AEC6), fontSize = 10.sp)
            }
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(app.status.uppercase(), color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Icon
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E2A4F))
                        .clickable { onEditClick() }
                        .padding(6.dp)
                        .testTag("edit_app_button_${app.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit App",
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(14.dp)
                    )
                }
                // Delete Icon
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E2A4F))
                        .clickable { onDeleteClick() }
                        .padding(6.dp)
                        .testTag("delete_app_button_${app.id}")
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
