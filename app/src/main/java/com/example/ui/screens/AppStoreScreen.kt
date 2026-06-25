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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

    var activeSimulatorAppId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B1C))
    ) {
        if (activeSimulatorAppId != null) {
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
                onBack = { viewModel.selectApp(null) },
                onInstall = { viewModel.simulateAppInstallation(selectedApp!!.id) },
                onUninstall = { viewModel.uninstallApp(selectedApp!!.id) },
                onOpen = { activeSimulatorAppId = selectedApp!!.id },
                onSubmitReview = { rating, comment ->
                    val author = userSession?.displayName ?: "Anonymous User"
                    viewModel.submitAppReview(selectedApp!!.id, author, rating, comment)
                }
            )
        } else {
            // Main browsing view
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar Header
                StoreHeader(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) }
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
    onSearchChange: (String) -> Unit
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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search productivity, tools, photography...", color = Color(0xFF757193)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_store_search_input"),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF757193)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1B1736),
                unfocusedContainerColor = Color(0xFF1B1736),
                focusedBorderColor = Color(0xFF7B2CBF),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("app_item_${app.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo
            AsyncImage(
                model = app.logoUrl,
                contentDescription = app.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF8A2BE2).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            // App Meta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.category,
                    color = Color(0xFF00FFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", app.rating),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${app.downloads / 1000}k downloads",
                        color = Color(0xFFB0AEC6),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Install/Open Button
            Box(contentAlignment = Alignment.Center) {
                if (isInstalling) {
                    CircularProgressIndicator(
                        progress = installProgress ?: 0f,
                        color = Color(0xFF00F5D4),
                        modifier = Modifier.size(36.dp),
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
                            containerColor = if (app.isInstalled) Color(0xFF00F5D4) else Color(0xFF7B2CBF)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (app.isInstalled) "Open" else "Get",
                            color = if (app.isInstalled) Color.Black else Color.White,
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
    onBack: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onOpen: () -> Unit,
    onSubmitReview: (Int, String) -> Unit
) {
    val scrollState = rememberScrollState()
    var showReviewDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B1C))
            .verticalScroll(scrollState)
    ) {
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
            if (isInstalling) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Simulating Secure Download...", color = Color.White, fontSize = 12.sp)
                        Text("${((installProgress ?: 0f) * 100).toInt()}%", color = Color(0xFF00F5D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = installProgress ?: 0f,
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
                    Button(
                        onClick = {
                            if (app.isInstalled) onOpen() else onInstall()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (app.isInstalled) Color(0xFF00F5D4) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .testTag("app_details_action_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (app.isInstalled) Color(0xFF131026) else Color(0xFF00F5D4),
                            contentColor = if (app.isInstalled) Color(0xFF00F5D4) else Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (app.isInstalled) Icons.Default.PlayArrow else Icons.Default.Download,
                            contentDescription = null,
                            tint = if (app.isInstalled) Color(0xFF00F5D4) else Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (app.isInstalled) "Launch App" else "Install Now",
                            color = if (app.isInstalled) Color(0xFF00F5D4) else Color.Black,
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

            // --- APP SCREENSHOTS SECTION (NEW) ---
            Text(
                text = "Screenshots",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val screenshots = when (app.category) {
                "Productivity" -> listOf(
                    "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500&auto=format&fit=crop&q=60"
                )
                "Photography" -> listOf(
                    "https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1554080353-a576cf803bda?w=500&auto=format&fit=crop&q=60"
                )
                "Health" -> listOf(
                    "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?w=500&auto=format&fit=crop&q=60"
                )
                "Tools" -> listOf(
                    "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=500&auto=format&fit=crop&q=60"
                )
                else -> listOf( // Arcade, RPG, Puzzle
                    "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500&auto=format&fit=crop&q=60"
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(screenshots) { imgUrl ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(180.dp)
                            .height(300.dp)
                            .border(1.dp, Color(0xFF1B1736), RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131026))
                    ) {
                        AsyncImage(
                            model = imgUrl,
                            contentDescription = "App Screenshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
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
                if (isLoggedIn) {
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

            Spacer(modifier = Modifier.height(12.dp))

            if (reviews.isEmpty()) {
                Text(
                    text = "No reviews available yet. Be the first to express feedback!",
                    color = Color(0xFFB0AEC6),
                    fontSize = 12.sp
                )
            } else {
                reviews.forEach { review ->
                    ReviewRowItem(review = review)
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
fun ReviewRowItem(review: ReviewEntity) {
    val date = Date(review.timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.US)

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
                Text(
                    text = review.authorName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = format.format(date),
                    color = Color(0xFF757193),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row {
                repeat(5) { starIndex ->
                    Icon(
                        imageVector = if (starIndex < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = review.comment,
                color = Color(0xFFB0AEC6),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
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
            "AI Bot" to "Hello! I am Nova Chat AI. How can I help you navigate the Nova Mind platform today?"
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
                            query.contains("app", ignoreCase = true) -> "Nova Mind AI store offers standard applications, high-graphics games, and a full developer hosting SDK portal!"
                            query.contains("game", ignoreCase = true) -> "Our game page offers retro-interactive classic compilations like Snake and Tap RPG natively compiled inside the app!"
                            query.contains("premium", ignoreCase = true) -> "Premium accounts unlock unlimited Gemini model usage, high-definition canvas rendering, and instant PNG downloads!"
                            else -> "That sounds fascinating! As Nova Chat AI, I can confirm our failover priority guarantees 100% uptime utilizing smart API routing!"
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
