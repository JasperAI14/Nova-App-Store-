package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.widget.Toast
import android.widget.VideoView
import android.widget.MediaController
import android.net.Uri
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterHdr
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppEntity
import com.example.data.ReviewEntity
import com.example.ui.viewmodel.NovaStoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppStoreScreen(
    viewModel: NovaStoreViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()
    val appReviews by viewModel.selectedAppReviews.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val bookmarkedApps by viewModel.bookmarkedApps.collectAsState()
    val isBookmarked = selectedApp?.let { app -> bookmarkedApps.any { it.id == app.id } } ?: false

    var activeSimulatorAppId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B1C))
    ) {
        if (viewModel.showSearchPage) {
            StoreSearchOverlay(
                viewModel = viewModel,
                onDismiss = { viewModel.showSearchPage = false },
                onAppSelect = { viewModel.selectApp(it) }
            )
        } else if (activeSimulatorAppId != null) {
            val app = filteredApps.find { it.id == activeSimulatorAppId } ?: selectedApp
            if (app != null) {
                MiniAppSimulator(
                    app = app,
                    onClose = { activeSimulatorAppId = null }
                )
            }
        } else if (selectedApp != null) {
            AppDetailsView(
                app = selectedApp!!,
                reviews = appReviews,
                installProgress = viewModel.installProgressMap[selectedApp!!.id],
                isInstalling = viewModel.installingStateMap[selectedApp!!.id] ?: false,
                isLoggedIn = userSession?.isLoggedIn == true,
                isBookmarked = isBookmarked,
                onToggleBookmark = { viewModel.toggleBookmark(selectedApp!!.id) },
                onBack = { viewModel.selectApp(null) },
                onInstall = { viewModel.simulateAppInstallation(selectedApp!!.id) },
                onUninstall = { viewModel.uninstallApp(selectedApp!!.id) },
                onOpen = { activeSimulatorAppId = selectedApp!!.id },
                onSubmitReview = { rating, comment ->
                    val author = userSession?.displayName ?: "Anonymous User"
                    viewModel.submitAppReview(selectedApp!!.id, author, rating, comment)
                },
                viewModel = viewModel
            )
        } else {
            // Main browsing view
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar Header
                StoreHeader(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onOpenSearch = { viewModel.showSearchPage = true }
                )

                // Categories Row
                CategoriesBar(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.setSelectedCategory(it) }
                )

                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFB0AEC6),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No applications found", color = Color.White, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Try adjusting your query or category filter.", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                        }
                    }
                } else {
                    // Filter standard apps (not games)
                    val appsOnly = filteredApps.filter { !it.isGame }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Top Verified Applications",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(appsOnly) { app ->
                            AppStoreRowItem(
                                app = app,
                                installProgress = viewModel.installProgressMap[app.id],
                                isInstalling = viewModel.installingStateMap[app.id] ?: false,
                                onClick = { viewModel.selectApp(app.id) },
                                onInstallClick = { viewModel.simulateAppInstallation(app.id) },
                                onOpenClick = { activeSimulatorAppId = app.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenSearch: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131026))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Discover Apps",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // Dedicated Search Button
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier.testTag("store_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search apps, categories or developers",
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .background(Color(0xFF00F5D4).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "VT Verified",
                        color = Color(0xFF00F5D4),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Clicking the search box also launches the dedicated search overlay
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search productivity, tools, photography...", color = Color(0xFF757193)) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSearch() }
                .testTag("app_store_search_input"),
            enabled = false, // click-through enabled to trigger full-screen overlay
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF757193)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = Color(0xFF1B1736),
                disabledBorderColor = Color.Transparent,
                disabledTextColor = Color.White,
                focusedContainerColor = Color(0xFF1B1736),
                unfocusedContainerColor = Color(0xFF1B1736),
                focusedBorderColor = Color(0xFF7B2CBF),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun CategoriesBar(
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit
) {
    val categories = listOf("All", "Productivity", "Photography", "Health", "Tools")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0B1C))
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = (category == "All" && selectedCategory == null) || (category == selectedCategory)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFF7B2CBF) else Color(0xFF1B1736))
                    .clickable {
                        onSelectCategory(if (category == "All") null else category)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.White else Color(0xFFB0AEC6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AppStoreRowItem(
    app: AppEntity,
    installProgress: Float?,
    isInstalling: Boolean,
    onClick: () -> Unit,
    onInstallClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "row_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() }
            .testTag("app_item_${app.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131026).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(Color(0xFF7B2CBF).copy(alpha = 0.45f), Color(0xFF00F0FF).copy(alpha = 0.15f))
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo with gradient border
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color(0xFF070514), RoundedCornerShape(14.dp))
                    .border(
                        1.2.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00F0FF).copy(alpha = 0.5f), Color(0xFF7B2CBF).copy(alpha = 0.5f))
                        ),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                AsyncImage(
                    model = app.logoUrl,
                    contentDescription = app.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App Meta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00FFFF).copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = app.category.uppercase(),
                            color = Color(0xFF00FFFF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "v${app.version}",
                        color = Color(0xFF757193),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFFFD100).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD100),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", app.rating),
                            color = Color(0xFFFFD100),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "${app.downloads / 1000}k downloads",
                        color = Color(0xFFB0AEC6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Install/Open Button with premium visual feel
            Box(
                modifier = Modifier.width(72.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isInstalling) {
                    CircularProgressIndicator(
                        progress = installProgress ?: 0f,
                        color = Color(0xFF00F5D4),
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Button(
                        onClick = {
                            if (app.isInstalled) {
                                onOpenClick()
                            } else {
                                onInstallClick()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (app.isInstalled) Color(0xFF1B1736) else Color(0xFF7B2CBF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .border(
                                1.dp,
                                if (app.isInstalled) Color(0xFF7B2CBF).copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Text(
                            text = if (app.isInstalled) "Open" else "Get",
                            color = if (app.isInstalled) Color(0xFF00F5D4) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppDetailsView(
    app: AppEntity,
    reviews: List<ReviewEntity>,
    installProgress: Float?,
    isInstalling: Boolean,
    isLoggedIn: Boolean,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onBack: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onOpen: () -> Unit,
    onSubmitReview: (Int, String) -> Unit,
    viewModel: NovaStoreViewModel? = null
) {
    val scrollState = rememberScrollState()
    var showReviewDialog by remember { mutableStateOf(false) }
    var showFullScreenImageUri by remember { mutableStateOf<String?>(null) }
    
    val userSession = viewModel?.userSession?.collectAsState()?.value
    val isDeveloper = userSession?.isLoggedIn == true && app.isUserUploaded && app.uploadedByEmail.lowercase() == userSession.email.lowercase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B1C))
            .verticalScroll(scrollState)
    ) {
        val context = LocalContext.current
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Application Info",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isLoggedIn) {
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("bookmark_app_button")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Bookmark App",
                        tint = if (isBookmarked) Color(0xFFFFD700) else Color.White
                    )
                }
            }
            IconButton(
                onClick = {
                    try {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Check out ${app.name} on Nova Store! Download direct: https://ais-pre-oun3a6zto7xl44kdsnabnt-483043984572.europe-west2.run.app/?appId=${app.id}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to share app link.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("share_app_button")
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share App", tint = Color(0xFF00F5D4))
            }
        }

        // Header Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = app.logoUrl,
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFF8A2BE2), RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = app.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.developer,
                        color = Color(0xFF00FFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version: ${app.version} | Size: ${app.sizeMb} MB",
                        color = Color(0xFFB0AEC6),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Downloads / Ratings Meta Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16132D), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetaStatColumn(title = "Rating", value = String.format(Locale.US, "%.1f ★", app.rating))
                MetaStatColumn(title = "Downloads", value = "${app.downloads / 1000}k+")
                MetaStatColumn(title = "Malware Check", value = "Clean", textColor = Color(0xFF00F5D4))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Installer
            val isDownloading = viewModel?.downloadingStateMap?.get(app.id) ?: false
            val downloadProgress = viewModel?.downloadProgressMap?.get(app.id) ?: 0f
            val isInstallingNew = viewModel?.installingStateMap?.get(app.id) ?: false
            val installProgressNew = viewModel?.installProgressMap?.get(app.id) ?: 0f

            if (isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Downloading secure payload...", color = Color.White, fontSize = 12.sp)
                        Text("${(downloadProgress * 100).toInt()}%", color = Color(0xFF00F5D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = downloadProgress,
                        color = Color(0xFF00F5D4),
                        trackColor = Color(0xFF1B1736),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    )
                }
            } else if (isInstallingNew) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Verifying signatures & installing...", color = Color.White, fontSize = 12.sp)
                        Text("${(installProgressNew * 100).toInt()}%", color = Color(0xFF00F5D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = installProgressNew,
                        color = Color(0xFF00F5D4),
                        trackColor = Color(0xFF1B1736),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val hasUpdate = app.isInstalled && app.installedVersion.isNotEmpty() && app.installedVersion != app.version

                    Button(
                        onClick = {
                            if (hasUpdate) {
                                viewModel?.startAppUpdate(app.id)
                            } else if (app.isInstalled) {
                                onOpen()
                            } else if (app.isDownloaded) {
                                viewModel?.startAppInstall(app.id)
                            } else {
                                viewModel?.startAppDownload(app.id)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (app.isInstalled && !hasUpdate) Color(0xFF00F5D4) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .testTag("app_details_action_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUpdate) Color(0xFFFFB703) else if (app.isInstalled) Color(0xFF131026) else Color(0xFF00F5D4),
                            contentColor = if (app.isInstalled && !hasUpdate) Color(0xFF00F5D4) else Color.Black
                        )
                    ) {
                        val (icon, label) = when {
                            hasUpdate -> Icons.Default.Download to "Update to v${app.version}"
                            app.isInstalled -> Icons.Default.PlayArrow to "Launch App"
                            app.isDownloaded -> Icons.Default.CheckCircle to "Install Now"
                            else -> Icons.Default.Download to "Download APK"
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (app.isInstalled && !hasUpdate) Color(0xFF00F5D4) else Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (app.isInstalled && !hasUpdate) Color(0xFF00F5D4) else Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (app.isInstalled) {
                        Button(
                            onClick = onUninstall,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD62246))
                        ) {
                            Text("Uninstall", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- APP PREVIEW SECTION ---
            Text(
                text = "Preview",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Inline helper function for media categorization
            val isVideoUrl = { url: String ->
                val cleanUrl = url.split("?").first().lowercase()
                cleanUrl.endsWith(".mp4") || cleanUrl.endsWith(".webm") || cleanUrl.endsWith(".mov") || url.contains(".mp4") || url.contains(".webm") || url.contains(".mov")
            }

            val allMedia = if (app.screenshotsCsv.isNotEmpty()) {
                app.screenshotsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                when (app.category) {
                    "Productivity" -> listOf(
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                        "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500&auto=format&fit=crop&q=60"
                    )
                    "Photography" -> listOf(
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                        "https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1554080353-a576cf803bda?w=500&auto=format&fit=crop&q=60"
                    )
                    "Health" -> listOf(
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                        "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?w=500&auto=format&fit=crop&q=60"
                    )
                    "Tools" -> listOf(
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=500&auto=format&fit=crop&q=60"
                    )
                    else -> listOf( // Arcade, RPG, Puzzle
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                        "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=500&auto=format&fit=crop&q=60",
                        "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500&auto=format&fit=crop&q=60"
                    )
                }
            }

            val orderedMedia = remember(allMedia) {
                val videos = allMedia.filter { isVideoUrl(it) }
                val imgs = allMedia.filter { !isVideoUrl(it) }
                videos + imgs
            }

            var activeIndex by remember(orderedMedia) { mutableIntStateOf(0) }
            val activeUrl = orderedMedia.getOrNull(activeIndex) ?: ""
            val isActiveVideo = isVideoUrl(activeUrl)

            if (orderedMedia.isNotEmpty()) {
                // Large primary preview frame (Showcase)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .border(1.5.dp, Color(0xFF00F5D4).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .pointerInput(orderedMedia) {
                            var totalDragX = 0f
                            detectDragGestures(
                                onDragEnd = {
                                    if (totalDragX > 100f && activeIndex > 0) {
                                        activeIndex--
                                    } else if (totalDragX < -100f && activeIndex < orderedMedia.lastIndex) {
                                        activeIndex++
                                    }
                                    totalDragX = 0f
                                },
                                onDragCancel = {
                                    totalDragX = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                }
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isActiveVideo) {
                            // Native streamable video player
                            InlineVideoPlayer(
                                videoUrl = activeUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Clickable image with a full-screen expand indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showFullScreenImageUri = activeUrl }
                            ) {
                                AsyncImage(
                                    model = activeUrl,
                                    contentDescription = "Active Preview Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                // Full Screen instruction overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Expand to full screen",
                                            tint = Color(0xFF00F5D4),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Tap to Expand",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Left & Right Swipe Assist Icons for Desktop or click users
                        if (activeIndex > 0) {
                            IconButton(
                                onClick = { activeIndex-- },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Previous Preview",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (activeIndex < orderedMedia.lastIndex) {
                            IconButton(
                                onClick = { activeIndex++ },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Next Preview",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer(scaleX = -1f) // Flip horizontally
                                )
                            }
                        }

                        // Showcase format badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color(0xFF16132D), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isActiveVideo) "▶ VIDEO PREVIEW" else "SCREENSHOT",
                                color = if (isActiveVideo) Color(0xFF00F5D4) else Color(0xFF00FFFF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Swipeable Thumbnails Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(orderedMedia.size) { index ->
                        val mUrl = orderedMedia[index]
                        val isVideo = isVideoUrl(mUrl)
                        val isSelected = index == activeIndex

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .width(90.dp)
                                .height(60.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00F5D4) else Color(0xFF1B1736),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { activeIndex = index },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131026))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isVideo) {
                                    // Visual thumbnail layout for videos
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF0F0C20)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Video Thumbnail",
                                            tint = Color(0xFF00F5D4),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = mUrl,
                                        contentDescription = "Thumbnail Screenshot",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Format indicator overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isVideo) "Video" else "Image",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Fallback elegant empty text
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No App Previews uploaded.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description
            Text(
                text = "About this app",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.description,
                color = Color(0xFFB0AEC6),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Security Scanner detailed report
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2625))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "VirusTotal Clean Shield Certified",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = app.scanResult,
                            color = Color(0xFF00F5D4),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // APK Analysis Details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF7B2CBF).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131026))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("APK Analysis & Metadata", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• Package ID: ${app.packageName}", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                    Text("• Version Code: ${app.versionCode}", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                    Text("• Target SDK Version: ${app.targetSdk}", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                    Text("• Minimum Android Required: ${app.minSdk}", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Requested Permissions:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    val permsList = if (app.permissionsCsv.isNotEmpty()) app.permissionsCsv.split(",").map { it.trim() } else emptyList()
                    if (permsList.isEmpty()) {
                        Text("• No hazardous device permissions requested.", color = Color(0xFF00F5D4), fontSize = 11.sp)
                    } else {
                        permsList.forEach { perm ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(perm.substringAfterLast("."), color = Color(0xFFB0AEC6), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Version Management History Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Version History", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Current release Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("v${app.version} (Latest Release)", color = Color(0xFF00F5D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Active Bundle • Build ${app.versionCode}", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00F5D4).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ACTIVE", color = Color(0xFF00F5D4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Render historical versions if enabled
                    if (app.keepOlderVersions) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(6.dp))

                        val historyList = remember(app.historyVersionsJson) {
                            try {
                                val raw = app.historyVersionsJson
                                if (raw.isNotEmpty() && raw.startsWith("[")) {
                                    listOf(
                                        Triple("1.0.1", "Stable rollback release", "Build 1"),
                                        Triple("1.0.0", "Initial store submission release", "Build 0")
                                    )
                                } else {
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        if (historyList.isEmpty()) {
                            Text("No older historical builds available for download.", color = Color(0xFF757193), fontSize = 11.sp)
                        } else {
                            historyList.forEach { history ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("v${history.first}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("${history.second} • ${history.third}", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Initiating download for legacy version build ${history.first}...", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1736)),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rollback APK", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Developer has restricted store listings to the latest release only.", color = Color(0xFF757193), fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Developer Support Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131026))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Developer Support Info", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val supportUrl = if (app.supportWebsite.isNotEmpty()) app.supportWebsite else "https://novastore.example.com"
                    val privacyUrl = if (app.privacyPolicy.isNotEmpty()) app.privacyPolicy else "https://novastore.example.com/privacy"

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid Support URL.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1736)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF00FFFF), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Support Website", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid Privacy Policy URL.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1736)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Privacy Policy", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    if (app.tagsCsv.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tags:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            app.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .background(Color(0xFF1B1736), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(tag, color = Color(0xFF00F5D4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Reviews Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ratings & Reviews",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Report App button (for non-developers) (Goal 11)
                    if (isLoggedIn && !isDeveloper && !app.isReported) {
                        var showAppReportDialog by remember { mutableStateOf(false) }
                        var appReportReason by remember { mutableStateOf("") }
                        
                        Button(
                            onClick = { showAppReportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(32.dp).testTag("report_app_button")
                        ) {
                            Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = Color(0xFFFF007F), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Report App", color = Color(0xFFFF007F), fontSize = 11.sp)
                        }

                        if (showAppReportDialog) {
                            AlertDialog(
                                onDismissRequest = { showAppReportDialog = false },
                                title = { Text("Report App", color = Color.White) },
                                text = {
                                    Column {
                                        Text("Please describe why you are reporting \"${app.name}\":", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextField(
                                            value = appReportReason,
                                            onValueChange = { appReportReason = it },
                                            placeholder = { Text("Reason (e.g. copyright, malware)", color = Color(0xFF757193), fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (appReportReason.isNotEmpty()) {
                                                viewModel?.reportApp(app.id, appReportReason)
                                                showAppReportDialog = false
                                                Toast.makeText(context, "App successfully reported.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F))
                                    ) {
                                        Text("Report", color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAppReportDialog = false }) {
                                        Text("Cancel", color = Color.White)
                                    }
                                },
                                containerColor = Color(0xFF131026)
                            )
                        }
                    }

                    if (isLoggedIn && !isDeveloper) {
                        Button(
                            onClick = { showReviewDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1736)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(32.dp).testTag("write_review_button")
                        ) {
                            Icon(imageVector = Icons.Default.RateReview, contentDescription = null, tint = Color(0xFF00FFFF), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Write review", color = Color(0xFF00FFFF), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (reviews.isEmpty()) {
                Text(
                    text = "No reviews available yet. Be the first to express feedback!",
                    color = Color(0xFFB0AEC6),
                    fontSize = 12.sp
                )
            } else {
                reviews.forEach { review ->
                    ReviewRowItem(
                        review = review,
                        app = app,
                        userSession = userSession,
                        onReplySubmitted = { reviewId, replyText ->
                            viewModel?.submitDeveloperReply(reviewId, app.id, replyText)
                        },
                        onReportClicked = { reviewId, reason ->
                            viewModel?.reportAppReview(reviewId, app.id, reason)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showReviewDialog) {
        WriteReviewDialog(
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                onSubmitReview(rating, comment)
                showReviewDialog = false
            }
        )
    }

    if (showFullScreenImageUri != null) {
        val isVideoUrlLocal = { url: String ->
            val cleanUrl = url.split("?").first().lowercase()
            cleanUrl.endsWith(".mp4") || cleanUrl.endsWith(".webm") || cleanUrl.endsWith(".mov") || url.contains(".mp4") || url.contains(".webm") || url.contains(".mov")
        }

        val allMediaLocal = if (app.screenshotsCsv.isNotEmpty()) {
            app.screenshotsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            when (app.category) {
                "Productivity" -> listOf(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500&auto=format&fit=crop&q=60"
                )
                "Photography" -> listOf(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    "https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1554080353-a576cf803bda?w=500&auto=format&fit=crop&q=60"
                )
                "Health" -> listOf(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?w=500&auto=format&fit=crop&q=60"
                )
                "Tools" -> listOf(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=500&auto=format&fit=crop&q=60"
                )
                else -> listOf( // Arcade, RPG, Puzzle
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                    "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500&auto=format&fit=crop&q=60"
                )
            }
        }

        val orderedMediaLocal = remember(allMediaLocal) {
            val videos = allMediaLocal.filter { isVideoUrlLocal(it) }
            val imgs = allMediaLocal.filter { !isVideoUrlLocal(it) }
            videos + imgs
        }

        var fullScreenIndex by remember {
            mutableIntStateOf(orderedMediaLocal.indexOf(showFullScreenImageUri).coerceAtLeast(0))
        }
        val currentFullUrl = orderedMediaLocal.getOrNull(fullScreenIndex) ?: ""
        val isCurrentImageVideo = isVideoUrlLocal(currentFullUrl)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { showFullScreenImageUri = null }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .pointerInput(orderedMediaLocal) {
                        var totalDragX = 0f
                        detectDragGestures(
                            onDragEnd = {
                                if (totalDragX > 100f && fullScreenIndex > 0) {
                                    fullScreenIndex--
                                } else if (totalDragX < -100f && fullScreenIndex < orderedMediaLocal.lastIndex) {
                                    fullScreenIndex++
                                }
                                totalDragX = 0f
                            },
                            onDragCancel = {
                                totalDragX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                            }
                        )
                    }
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentImageVideo) {
                    InlineVideoPlayer(
                        videoUrl = currentFullUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                } else {
                    AsyncImage(
                        model = currentFullUrl,
                        contentDescription = "Full Screen Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                // Left / Right Navigation overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fullScreenIndex > 0) {
                        IconButton(
                            onClick = { fullScreenIndex-- },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Previous",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    if (fullScreenIndex < orderedMediaLocal.lastIndex) {
                        IconButton(
                            onClick = { fullScreenIndex++ },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.graphicsLayer(scaleX = -1f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }

            // Close Button
            IconButton(
                onClick = { showFullScreenImageUri = null },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close full screen",
                    tint = Color.White
                )
            }

            // Status indicator bottom bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Preview ${fullScreenIndex + 1} of ${orderedMediaLocal.size}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCurrentImageVideo) "Demo Video" else "App Screenshot",
                    color = Color(0xFF00F5D4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MetaStatColumn(title: String, value: String, textColor: Color = Color.White) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(6.dp)
    ) {
        Text(title, color = Color(0xFF757193), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReviewRowItem(
    review: ReviewEntity,
    app: AppEntity?,
    userSession: com.example.data.UserSessionEntity?,
    onReplySubmitted: (Int, String) -> Unit,
    onReportClicked: (Int, String) -> Unit
) {
    val date = Date(review.timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val context = LocalContext.current

    val isDeveloper = userSession?.isLoggedIn == true && app?.isUserUploaded == true && app.uploadedByEmail.lowercase() == userSession.email.lowercase()

    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var reportReason by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131026))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = review.authorName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (review.isReported) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF007F).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "REPORTED",
                                color = Color(0xFFFF007F),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = format.format(date),
                    color = Color(0xFF757193),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { starIndex ->
                    Icon(
                        imageVector = if (starIndex < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Report review button (for non-developers) (Goal 11)
                if (!isDeveloper && !review.isReported) {
                    TextButton(
                        onClick = { showReportDialog = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Flag, contentDescription = "Report Review", tint = Color(0xFFFF007F), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report", color = Color(0xFFFF007F), fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = review.comment,
                color = Color(0xFFB0AEC6),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Display Developer Reply (if exists) (Goal 11)
            val devReply = review.developerReply
            if (!devReply.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A3C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SupportAgent, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Developer Response", color = Color(0xFF00F5D4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = devReply,
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Display AI Proposed Reply (Goal 8: Review AI replies before posting)
            val aiProposed = review.aiProposedReply
            if (isDeveloper && !aiProposed.isNullOrEmpty() && devReply.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C38)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Suggested Response (Pending Review)", color = Color(0xFF00F5D4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiProposed,
                            color = Color(0xFFB0AEC6),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onReplySubmitted(review.id, aiProposed) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Approve & Post", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    replyText = aiProposed
                                    showReplyInput = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A4F)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Edit Reply", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // Developer Reply Input Trigger / Display (Goal 11)
            if (isDeveloper && devReply.isNullOrEmpty() && aiProposed.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (!showReplyInput) {
                    TextButton(
                        onClick = { showReplyInput = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Reply, contentDescription = "Reply", tint = Color(0xFF00F5D4), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reply to Review", color = Color(0xFF00F5D4), fontSize = 11.sp)
                    }
                }
            }

            if (showReplyInput) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Write your reply...", color = Color(0xFF757193), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1A3C),
                            unfocusedContainerColor = Color(0xFF1E1A3C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotEmpty()) {
                                onReplySubmitted(review.id, replyText)
                                showReplyInput = false
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF00F5D4))
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Review", color = Color.White) },
            text = {
                Column {
                    Text("Help us keep Nova App Store clean and professional. Specify the reason for reporting this review:", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("Reason (e.g., spam, offensive language)", color = Color(0xFF757193), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isNotEmpty()) {
                            onReportClicked(review.id, reportReason)
                            showReportDialog = false
                            Toast.makeText(context, "Review successfully reported.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F))
                ) {
                    Text("Report", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF131026)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF16132D),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF7B2CBF).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Write App Review", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(14.dp))

                Text("Rating", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(5) { index ->
                        val currentStar = index + 1
                        Icon(
                            imageVector = if (currentStar <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFB703),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { rating = currentStar }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Your comment", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("review_comment_input"),
                    placeholder = { Text("Write your review...", color = Color(0xFF757193), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0D0B1C),
                        unfocusedContainerColor = Color(0xFF0D0B1C),
                        focusedBorderColor = Color(0xFF7B2CBF),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (comment.isNotBlank()) onSubmit(rating, comment) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2CBF)),
                        enabled = comment.isNotBlank(),
                        modifier = Modifier.testTag("submit_review_dialog_button")
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

// Interactive Mini-App Simulator System
@Composable
fun MiniAppSimulator(
    app: AppEntity,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06040C))
    ) {
        // App Frame Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131026))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Simulator", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = app.logoUrl,
                contentDescription = app.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(app.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF00F5D4), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ACTIVE SIMULATOR RUNTIME", color = Color(0xFF00F5D4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Divider(color = Color(0xFF00FFFF).copy(alpha = 0.3f), thickness = 1.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (app.id) {
                "nova_chat_ai" -> ChatAiSimulator()
                "nova_editor" -> PhotoEditorSimulator()
                "nova_fit" -> FitnessSimulator()
                "nova_weather" -> WeatherSimulator()
                else -> GenericAppSimulator(app = app)
            }
        }
    }
}

@Composable
fun ChatAiSimulator() {
    val messages = remember {
        mutableStateListOf(
            "AI Bot" to "Hello! I am Nova Chat AI. How can I help you navigate the Nova App Store platform today?"
        )
    }
    var currentInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages.toList()) { (sender, text) ->
                val isUser = sender == "You"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) Color(0xFF7B2CBF) else Color(0xFF1B1736)
                        ),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 12.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(sender, color = if (isUser) Color(0xFF00FFFF) else Color(0xFFFF007F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentInput,
                onValueChange = { currentInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type messaging...", color = Color(0xFF757193)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7B2CBF),
                    unfocusedBorderColor = Color(0xFF1B1736),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (currentInput.isNotBlank()) {
                        messages.add("You" to currentInput)
                        val query = currentInput
                        currentInput = ""
                        // AI replies dynamically
                        messages.add("AI Bot" to "Thinking...")
                        val botIndex = messages.size - 1
                        // delayed response
                        // To trigger instantly in compose state, we replace after simple math
                        val response = when {
                            query.contains("app", ignoreCase = true) -> "Nova App Store offers standard applications, high-graphics games, and a full developer hosting SDK portal!"
                            query.contains("game", ignoreCase = true) -> "Our game page offers retro-interactive classic compilations like Snake and Tap RPG natively compiled inside the app!"
                            query.contains("premium", ignoreCase = true) -> "Premium accounts unlock unlimited Gemini model usage, high-definition canvas rendering, and instant PNG downloads!"
                            else -> "That sounds fascinating! As Nova Assistant, I can confirm our failover priority guarantees 100% uptime utilizing smart API routing!"
                        }
                        messages[botIndex] = "AI Bot" to response
                    }
                },
                modifier = Modifier
                    .background(Color(0xFF7B2CBF), CircleShape)
                    .size(46.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun PhotoEditorSimulator() {
    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Controlled Image
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF00FFFF), RoundedCornerShape(16.dp))
        ) {
            // Apply color filter matrix dynamically!
            val cm = ColorMatrix().apply {
                setToSaturation(saturation)
                // Matrix logic for brightness and contrast
                val scale = brightness
                val translate = (1f - contrast) * 128f
                // Apply manual adjustments
                val m = values
                m[0] = contrast * scale; m[12] = translate
                m[6] = contrast * scale; m[13] = translate
                m[12] = contrast * scale; m[14] = translate
            }

            Image(
                painter = painterResource(id = R.drawable.img_landing_hero),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(cm)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sliders
        Text("Visual Filters Curve Editor", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(14.dp))

        SliderItem(label = "Brightness", value = brightness, onValueChange = { brightness = it }, range = 0.5f..2.0f)
        SliderItem(label = "Contrast", value = contrast, onValueChange = { contrast = it }, range = 0.5f..2.0f)
        SliderItem(label = "Saturation", value = saturation, onValueChange = { saturation = it }, range = 0.0f..3.0f)
    }
}

@Composable
fun SliderItem(label: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFFB0AEC6), fontSize = 11.sp)
            Text(String.format(Locale.US, "%.2f", value), color = Color(0xFF00FFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00FFFF),
                activeTrackColor = Color(0xFF7B2CBF)
            )
        )
    }
}

@Composable
fun FitnessSimulator() {
    var isRunning by remember { mutableStateOf(false) }
    var steps by remember { mutableIntStateOf(5230) }
    var calories by remember { mutableIntStateOf(245) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Pedometer Counter", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(30.dp))

            // Step Circle Gauge
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .border(4.dp, Color(0xFF7B2CBF), CircleShape)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(36.dp))
                    Text("$steps", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("steps today", color = Color(0xFFB0AEC6), fontSize = 11.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetaStatColumn(title = "Distance", value = String.format(Locale.US, "%.2f km", steps * 0.00075f))
            MetaStatColumn(title = "Calories Burn", value = "$calories kcal", textColor = Color(0xFFFF007F))
            MetaStatColumn(title = "Active Timer", value = "00:42:15")
        }

        Button(
            onClick = {
                steps += (15..45).random()
                calories += (1..3).random()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Simulate Step Trigger (+30 steps)", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WeatherSimulator() {
    var selectedCity by remember { mutableStateOf("New York") }
    val weatherData = mapOf(
        "New York" to Triple("Rainy Conditions", "18°C", "Precise Radar alerts active for storm cells."),
        "Tokyo" to Triple("Sunny Clear", "26°C", "UV Index is high. Ideal day for outdoor fitness."),
        "London" to Triple("Foggy Mist", "14°C", "Low visibility levels reported at Heathrow."),
        "Lagos" to Triple("Humid Rain", "31°C", "Precipitation probability at 85% with heavy thunder.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Meteorological Vector Dashboard", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(20.dp))

            // Select City Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weatherData.keys.forEach { city ->
                    val isSel = city == selectedCity
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Color(0xFF7B2CBF) else Color(0xFF131026))
                            .clickable { selectedCity = city }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(city, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val data = weatherData[selectedCity]!!
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 20.dp)
        ) {
            Icon(
                imageVector = if (data.first.contains("Sunny")) Icons.Default.WbSunny else Icons.Default.BrightnessLow,
                contentDescription = null,
                tint = Color(0xFFFFB703),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(data.first, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(data.second, color = Color(0xFF00FFFF), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = data.third,
                color = Color(0xFFB0AEC6),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun GenericAppSimulator(app: AppEntity) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Simulated App Operational", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Launched successfully. All local assets loaded for ${app.name} (${app.version}).",
                color = Color(0xFFB0AEC6),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InlineVideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    var isPrepared by remember(videoUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    val mediaController = MediaController(ctx)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)

                    setVideoURI(Uri.parse(videoUrl))

                    setOnPreparedListener { mp ->
                        isPrepared = true
                        mp.isLooping = true
                        start()
                    }
                    setOnErrorListener { _, _, _ ->
                        true
                    }
                }
            },
            update = { view ->
                // Keep video synchronized with URL updates
                val currentUri = Uri.parse(videoUrl)
                // Simply set URI and play
                view.setVideoURI(currentUri)
                view.start()
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isPrepared) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00F5D4),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
fun StoreSearchOverlay(
    viewModel: NovaStoreViewModel,
    onDismiss: () -> Unit,
    onAppSelect: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchSuggestions = viewModel.searchSuggestions
    val recentSearches = viewModel.recentSearches
    val filteredApps by viewModel.filteredApps.collectAsState()
    val trendingSearches = listOf("Nova Chat", "Snake Game", "Weather", "Tools", "RPG")
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B1C))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search field Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        viewModel.setSearchQueryAndGetSuggestions(query)
                    },
                    placeholder = { Text("Search by name, category, dev...", color = Color(0xFF757193)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_overlay_input"),
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF757193)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQueryAndGetSuggestions("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1B1736),
                        unfocusedContainerColor = Color(0xFF1B1736),
                        focusedBorderColor = Color(0xFF00F5D4),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotEmpty()) {
                            viewModel.executeSearch(searchQuery)
                        }
                        keyboardController?.hide()
                    })
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (searchQuery.isEmpty()) {
                // Recent Searches
                if (recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.clearRecentSearches() }) {
                            Text("Clear", color = Color(0xFFFF4444), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    recentSearches.forEach { search ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.executeSearch(search)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF757193), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(search, color = Color.White, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Trending Searches
                Text("Trending Searches", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trendingSearches) { term ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1B1736))
                                .clickable {
                                    viewModel.executeSearch(term)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(term, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Suggestions
                if (searchSuggestions.isNotEmpty()) {
                    Text("Suggestions", color = Color(0xFFB0AEC6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131026)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            searchSuggestions.take(5).forEach { sug ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.executeSearch(sug)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(sug, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Search results
                Text("Search Results (${filteredApps.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                if (filteredApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No matching apps found.", color = Color(0xFFB0AEC6), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1B1736), RoundedCornerShape(10.dp))
                                    .clickable {
                                        onAppSelect(app.id)
                                        onDismiss()
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = app.logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(app.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Category: ${app.category} | Developer: ${app.developer}", color = Color(0xFFB0AEC6), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
