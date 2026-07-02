package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.viewmodel.NovaStoreViewModel
import java.util.Locale

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceHomeScreen(
    viewModel: NovaStoreViewModel,
    onNavigateToPremium: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLanding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val appReviews by viewModel.selectedAppReviews.collectAsState()
    val bookmarkedApps by viewModel.bookmarkedApps.collectAsState()
    val isBookmarked = selectedApp?.let { app -> bookmarkedApps.any { it.id == app.id } } ?: false

    var activeSimulatorAppId by remember { mutableStateOf<String?>(null) }

    // Navigation subtab indexes
    // 0: For You, 1: Apps, 2: Games, 3: AI Images, 4: Developer
    var selectedTopTab by remember { mutableIntStateOf(0) }
    val topTabs = listOf("For You", "Apps", "Games", "AI Images", "Developer")

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
                isBookmarked = isBookmarked,
                onToggleBookmark = { viewModel.toggleBookmark(selectedApp!!.id) },
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
            Column(modifier = Modifier.fillMaxSize()) {
                // --- TOP PLAY-STORE HEADER ---
                PlayStoreTopHeader(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    userAvatarUrl = userSession?.avatarUrl,
                    onProfileClick = onNavigateToProfile,
                    onInfoClick = onNavigateToLanding
                )

                // --- TOP NAVIGATION TABS ---
                ScrollableTabRow(
                    selectedTabIndex = selectedTopTab,
                    containerColor = Color(0xFF131026),
                    contentColor = Color.White,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        if (selectedTopTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTopTab]),
                                color = Color(0xFF00F5D4)
                            )
                        }
                    }
                ) {
                    topTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTopTab == index,
                            onClick = { selectedTopTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTopTab == index) Color.White else Color(0xFF757193)
                                )
                            },
                            modifier = Modifier.testTag("top_tab_$index")
                        )
                    }
                }

                // --- TAB CONTENT VIEWPORT ---
                Box(modifier = Modifier.weight(1f)) {
                    Crossfade(
                        targetState = selectedTopTab,
                        animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
                        label = "tab_fade"
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> ForYouTabContent(
                                viewModel = viewModel,
                                onAppSelect = { app -> viewModel.selectApp(app.id) }
                            )
                            1 -> AppStoreScreen(
                                viewModel = viewModel
                            )
                            2 -> GameStoreScreen(
                                viewModel = viewModel
                            )
                            3 -> AiImageScreen(
                                viewModel = viewModel,
                                onNavigateToPremium = onNavigateToPremium
                            )
                            4 -> DeveloperScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayStoreTopHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    userAvatarUrl: String?,
    onProfileClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131026))
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 10.dp)
    ) {
        // App Wordmark and Profile Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Futuristic logo icon
            Image(
                painter = painterResource(id = R.drawable.img_nova_logo_1782386944329),
                contentDescription = "Nova App Store Logo",
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFF00FFFF), CircleShape)
                    .clickable { onInfoClick() },
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.clickable { onInfoClick() }
            ) {
                Text(
                    text = "Nova App Store",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Application Ecosystem",
                    color = Color(0xFF00F5D4),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Info button
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_header_info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About Nova App Store",
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // User Profile Avatar / Icon
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, Color(0xFF7B2CBF).copy(alpha = 0.5f), CircleShape)
                    .testTag("top_header_profile_avatar")
            ) {
                if (!userAvatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = "User Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User Profile",
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar (Play Store Style)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search apps, retro games, AI generators...", color = Color(0xFF757193), fontSize = 13.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("play_store_search_input"),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF757193), modifier = Modifier.size(18.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF757193), modifier = Modifier.size(18.dp))
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
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
fun ForYouTabContent(
    viewModel: NovaStoreViewModel,
    onAppSelect: (AppEntity) -> Unit
) {
    val filteredApps by viewModel.filteredApps.collectAsState()
    val scrollState = rememberScrollState()

    // Separate apps and games
    val appsList = filteredApps.filter { !it.isGame }
    val gamesList = filteredApps.filter { it.isGame }

    // Grouping sections to match Play Store requirements
    val featuredApps = appsList
    val trendingApps = appsList.reversed()
    val newApps = appsList.shuffled(java.util.Random(101))
    val aiTools = appsList.filter { 
        it.id == "nova_chat_ai" || 
        it.id.contains("ai") || 
        it.name.contains("AI") || 
        it.description.contains("AI") || 
        it.category == "Photography" 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // --- FEATURED BANNER HERO ---
        FeaturedHeroSection(onAppSelect = onAppSelect)

        Spacer(modifier = Modifier.height(20.dp))

        // --- CATEGORIES SCROLL ROW ---
        SectionHeader(title = "Popular Categories")
        Spacer(modifier = Modifier.height(10.dp))
        CategoriesChipsRow(viewModel = viewModel)

        Spacer(modifier = Modifier.height(24.dp))

        // --- 1. FEATURED APPS ---
        SectionHeader(title = "Featured Apps")
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalAppCardsList(
            apps = featuredApps,
            viewModel = viewModel,
            onAppSelect = onAppSelect
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. TRENDING APPS ---
        SectionHeader(title = "Trending Apps")
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalAppCardsList(
            apps = trendingApps,
            viewModel = viewModel,
            onAppSelect = onAppSelect
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 3. NEW RELEASES ---
        SectionHeader(title = "New Releases")
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalAppCardsList(
            apps = newApps,
            viewModel = viewModel,
            onAppSelect = onAppSelect
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. AI TOOLS ---
        SectionHeader(title = "AI Tools")
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalAppCardsList(
            apps = aiTools,
            viewModel = viewModel,
            onAppSelect = onAppSelect
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 5. GAMES ---
        SectionHeader(title = "Games")
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalAppCardsList(
            apps = gamesList,
            viewModel = viewModel,
            onAppSelect = onAppSelect
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- VT SECURITY PLEDGE CARD ---
        MalwarePledgeCard()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF00F0FF), Color(0xFF7B2CBF))
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun FeaturedHeroSection(onAppSelect: (AppEntity) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val glowOffset by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val bgScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_scale"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                onAppSelect(
                    AppEntity(
                        id = "nova_chat_ai",
                        name = "Nova Chat Assistant",
                        description = "A conversational assistant that helps you with daily tasks, professional writing, and complex problem-solving.",
                        logoUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=120&auto=format&fit=crop&q=60",
                        category = "Productivity",
                        isGame = false,
                        version = "2.4.1",
                        developer = "Nova App Store Developer",
                        downloads = 142300,
                        rating = 4.8f,
                        isInstalled = false,
                        status = "Approved",
                        sizeMb = 24.5f,
                        apkFileName = "nova_chat_ai.apk"
                    )
                )
            },
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(Color(0xFF00FFFF).copy(alpha = 0.8f), Color(0xFF7B2CBF).copy(alpha = 0.8f))
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image with gentle breathing scaling animation
            Image(
                painter = painterResource(id = R.drawable.img_nova_banner_1782979675678),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = bgScale
                        scaleY = bgScale
                    },
                contentScale = ContentScale.Crop
            )

            // Linear gradient overlay + radial glowing orb offset
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF070514).copy(alpha = 0.95f),
                                Color(0xFF070514).copy(alpha = 0.65f),
                                Color(0xFF7B2CBF).copy(alpha = 0.15f)
                            )
                        )
                    )
            )

            // Animated light sweep / glowing particle overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00FFFF).copy(alpha = 0.18f), Color.Transparent),
                                radius = 250f
                            ),
                            center = androidx.compose.ui.geometry.Offset(glowOffset, size.height / 2f)
                        )
                    }
            )

            // Content inside hero
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF007F).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFF007F), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFF007F),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FEATURED AI HUB", color = Color(0xFFFF007F), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Nova Chat AI Workspace",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.25.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Unlock professional conversational intelligence with our signature offline-enabled LLM agent sandbox.",
                    color = Color(0xFFD1CFE7),
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00FFFF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Open Sandbox", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    
                    Text(
                        text = "v2.4 • Active Ecosystem",
                        color = Color(0xFF00FFCC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriesChipsRow(viewModel: NovaStoreViewModel) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories = listOf("All", "Productivity", "Photography", "Health", "Tools", "Arcade", "RPG", "Puzzle")

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = (category == "All" && selectedCategory == null) || (category == selectedCategory)
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (category == "All") {
                        viewModel.setSelectedCategory(null)
                    } else {
                        viewModel.setSelectedCategory(category)
                    }
                },
                label = {
                    Text(
                        text = category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00F5D4),
                    selectedLabelColor = Color.Black,
                    containerColor = Color(0xFF16132D),
                    labelColor = Color(0xFFB0AEC6)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color(0xFF261F4D),
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

@Composable
fun HorizontalAppCardsList(
    apps: List<AppEntity>,
    viewModel: NovaStoreViewModel,
    onAppSelect: (AppEntity) -> Unit
) {
    if (apps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 16.dp)
                .background(Color(0xFF16132D), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No results in this filter", color = Color(0xFF757193), fontSize = 12.sp)
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps) { app ->
                PlayStoreAppCard(
                    app = app,
                    installProgress = viewModel.installProgressMap[app.id],
                    isInstalling = viewModel.installingStateMap[app.id] ?: false,
                    onClick = { onAppSelect(app) },
                    onInstallClick = { viewModel.simulateAppInstallation(app.id) }
                )
            }
        }
    }
}

@Composable
fun PlayStoreAppCard(
    app: AppEntity,
    installProgress: Float?,
    isInstalling: Boolean,
    onClick: () -> Unit,
    onInstallClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .width(155.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() }
            .testTag("play_store_app_card_${app.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131026).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.verticalGradient(
                listOf(
                    Color(0xFF7B2CBF).copy(alpha = 0.5f),
                    Color(0xFF00F0FF).copy(alpha = 0.15f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // App Icon with border
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Color(0xFF070514), RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00F0FF).copy(alpha = 0.5f), Color(0xFF7B2CBF).copy(alpha = 0.5f))
                        ),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                AsyncImage(
                    model = app.logoUrl,
                    contentDescription = app.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App Name
            Text(
                text = app.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Developer
            Text(
                text = app.developer,
                color = Color(0xFF8E8CA8),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating & Size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${app.sizeMb.toInt()} MB",
                    color = Color(0xFF00F5D4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Install Button Box
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isInstalling) {
                    LinearProgressIndicator(
                        progress = installProgress ?: 0f,
                        color = Color(0xFF00F5D4),
                        trackColor = Color(0xFF261F4D),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    Button(
                        onClick = { if (!app.isInstalled) onInstallClick() else onClick() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (app.isInstalled) Color(0xFF1B1736) else Color(0xFF7B2CBF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
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
fun MalwarePledgeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF00F5D4).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Nova Protection Verification",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Every APK submitted by developers undergoes a live VirusTotal multi-engine cloud signature analysis before approval. Built with 100% security.",
                    color = Color(0xFFB0AEC6),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
