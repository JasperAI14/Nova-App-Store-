package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.IconButton
import android.app.Activity
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AiImageScreen
import com.example.ui.screens.AppStoreScreen
import com.example.ui.screens.DeveloperScreen
import com.example.ui.screens.GameStoreScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.MarketplaceHomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NovaStoreViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NovaAppMain()
            }
        }
    }
}

enum class StoreTab(val title: String, val icon: ImageVector, val tag: String) {
    HOME("Home", Icons.Default.Home, "tab_home"),
    APPS("Apps", Icons.Default.ShoppingBag, "tab_apps"),
    GAMES("Games", Icons.Default.Gamepad, "tab_games"),
    AI_IMAGES("AI Images", Icons.Default.AutoAwesome, "tab_images"),
    PROFILE("Profile", Icons.Default.Person, "tab_profile")
}

@Composable
fun NovaAppMain() {
    val viewModel: NovaStoreViewModel = viewModel()
    var currentTab by remember { mutableStateOf(StoreTab.HOME) }
    var activeSharedPage by remember { mutableStateOf<String?>(null) }
    val userSession by viewModel.userSession.collectAsState()

    val context = LocalContext.current
    
    // Deep Linking & Intent routing handler
    val activity = context as? Activity
    val intent = activity?.intent
    LaunchedEffect(intent) {
        if (intent != null) {
            val uri = intent.data
            val pageFromUri = uri?.getQueryParameter("page")
            val appIdFromUri = uri?.getQueryParameter("appId")
            val referrerFromUri = uri?.getQueryParameter("referrer") ?: uri?.getQueryParameter("ref")

            if (!referrerFromUri.isNullOrEmpty()) {
                viewModel.referrerEmail = referrerFromUri
            }

            val pageFromExtra = intent.getStringExtra("page")
            val appIdFromExtra = intent.getStringExtra("appId")

            val page = pageFromUri ?: pageFromExtra
            val appId = appIdFromUri ?: appIdFromExtra

            if (!appId.isNullOrEmpty()) {
                activeSharedPage = null
                currentTab = StoreTab.HOME
                viewModel.selectApp(appId)
            } else if (!page.isNullOrEmpty()) {
                activeSharedPage = page
                when (page) {
                    "landing" -> { currentTab = StoreTab.HOME }
                    "ai_images" -> { currentTab = StoreTab.AI_IMAGES }
                    "profile" -> { currentTab = StoreTab.PROFILE }
                    "apps" -> { currentTab = StoreTab.APPS }
                    "games" -> { currentTab = StoreTab.GAMES }
                }
            } else {
                activeSharedPage = null
                currentTab = StoreTab.HOME
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.installEvent.collect { action ->
            when (action) {
                is NovaStoreViewModel.InstallAction.DownloadApk -> {
                    Toast.makeText(context, "Initiating direct device download for ${action.appName}...", Toast.LENGTH_SHORT).show()
                    viewModel.downloadApkFile(action.fileName, action.appName)
                }
                is NovaStoreViewModel.InstallAction.StartPwa -> {
                    Toast.makeText(context, "Configuring secure PWA local installation offline for ${action.appName}...", Toast.LENGTH_LONG).show()
                }
                is NovaStoreViewModel.InstallAction.OpenStore -> {
                    Toast.makeText(context, "Redirecting to official Play Store page for ${action.appName}...", Toast.LENGTH_LONG).show()
                    try {
                        val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(action.storeUrl))
                        context.startActivity(storeIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to redirect. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
                }
                is NovaStoreViewModel.InstallAction.ShowDemoCantInstall -> {
                    Toast.makeText(context, "Demo Mode: ${action.appName} cannot be installed in this demo sandbox.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (userSession == null || !userSession!!.isLoggedIn) {
        LandingScreen(
            viewModel = viewModel,
            onExploreApps = {
                activeSharedPage = null
                currentTab = StoreTab.HOME
            },
            onExploreGames = {
                activeSharedPage = null
                currentTab = StoreTab.GAMES
            },
            onLaunchCreator = {
                activeSharedPage = null
                currentTab = StoreTab.AI_IMAGES
            },
            onGoToPremium = {
                activeSharedPage = null
                currentTab = StoreTab.PROFILE
            },
            onBack = null
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFF0D0B1C),
            bottomBar = {
            if (activeSharedPage == null) {
                NavigationBar(
                    containerColor = Color(0xFF131026),
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("nova_bottom_navigation_bar")
                ) {
                    listOf(StoreTab.HOME, StoreTab.APPS, StoreTab.GAMES, StoreTab.AI_IMAGES, StoreTab.PROFILE).forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00F5D4),
                                unselectedIconColor = Color(0xFF757193),
                                selectedTextColor = Color(0xFF00F5D4),
                                unselectedTextColor = Color(0xFF757193),
                                indicatorColor = Color(0xFF261F4D)
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activeSharedPage != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Shared Page Navigation Header to go back to Nova App Store
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF131026))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                activeSharedPage = null
                                currentTab = StoreTab.HOME
                                viewModel.selectApp(null)
                            },
                            modifier = Modifier.testTag("shared_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to Store",
                                tint = Color(0xFF00F5D4)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (activeSharedPage) {
                                "landing" -> "Nova App Store Hub"
                                "ai_images" -> "AI Image Creator"
                                "profile" -> "Developer Profile"
                                "apps" -> "Nova Apps Store"
                                "games" -> "Retro Games Store"
                                else -> "Nova Store"
                            },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (activeSharedPage) {
                            "landing" -> LandingScreen(
                                viewModel = viewModel,
                                onExploreApps = {
                                    activeSharedPage = null
                                    currentTab = StoreTab.HOME
                                },
                                onExploreGames = {
                                    activeSharedPage = null
                                    currentTab = StoreTab.GAMES
                                },
                                onLaunchCreator = {
                                    activeSharedPage = null
                                    currentTab = StoreTab.AI_IMAGES
                                },
                                onGoToPremium = {
                                    activeSharedPage = null
                                    currentTab = StoreTab.PROFILE
                                },
                                onBack = {
                                    activeSharedPage = null
                                    currentTab = StoreTab.HOME
                                }
                            )
                            "ai_images" -> AiImageScreen(
                                viewModel = viewModel,
                                onNavigateToPremium = {
                                    activeSharedPage = null
                                    currentTab = StoreTab.PROFILE
                                }
                            )
                            "profile" -> ProfileScreen(
                                viewModel = viewModel
                            )
                            "apps" -> AppStoreScreen(
                                viewModel = viewModel
                            )
                            "games" -> GameStoreScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            } else {
                when (currentTab) {
                    StoreTab.HOME -> MarketplaceHomeScreen(
                        viewModel = viewModel,
                        onNavigateToPremium = { currentTab = StoreTab.PROFILE },
                        onNavigateToProfile = { currentTab = StoreTab.PROFILE },
                        onNavigateToLanding = { activeSharedPage = "landing" }
                    )
                    StoreTab.APPS -> AppStoreScreen(
                        viewModel = viewModel
                    )
                    StoreTab.GAMES -> GameStoreScreen(
                        viewModel = viewModel
                    )
                    StoreTab.AI_IMAGES -> AiImageScreen(
                        viewModel = viewModel,
                        onNavigateToPremium = { currentTab = StoreTab.PROFILE }
                    )
                    StoreTab.PROFILE -> ProfileScreen(
                        viewModel = viewModel
                    )
                }
            }

                // Global Download Progress Overlay Banner
                val downloadProgress = viewModel.activeDownloadProgress
                val downloadAppName = viewModel.activeDownloadAppName
                val downloadStatus = viewModel.activeDownloadStatus

                if (downloadProgress != null && downloadAppName != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        androidx.compose.material3.Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF131026)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* prevent click-through */ }
                                .testTag("download_progress_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (downloadStatus == "Success") Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (downloadStatus == "Success") Color(0xFF00F5D4) else if (downloadStatus == "Error") Color(0xFFD62246) else Color(0xFFFFD100),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (downloadStatus == "Success") "Download Complete!" else "Downloading Device Installer...",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$downloadAppName (${(downloadProgress * 100).toInt()}%)",
                                        color = Color(0xFFB0AEC6),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = downloadProgress,
                                        color = if (downloadStatus == "Success") Color(0xFF00F5D4) else Color(0xFFFFD100),
                                        trackColor = Color(0xFF261F4D),
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                    )
                                    if (downloadStatus == "Success") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Saved directly to device downloads folder",
                                            color = Color(0xFF00F5D4),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.clearActiveDownload() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
