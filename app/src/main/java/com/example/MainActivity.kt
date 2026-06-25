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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
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
    var hasEnteredApp by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(StoreTab.HOME) }
    val userSession by viewModel.userSession.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.installEvent.collect { action ->
            when (action) {
                is NovaStoreViewModel.InstallAction.DownloadApk -> {
                    Toast.makeText(context, "Initiating Secure Download for ${action.appName} APK...", Toast.LENGTH_LONG).show()
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ais-pre-oun3a6zto7xl44kdsnabnt-483043984572.europe-west2.run.app/downloads/${action.fileName}"))
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to open download link. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
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
            }
        }
    }

    if (!hasEnteredApp) {
        LandingScreen(
            onExploreApps = {
                hasEnteredApp = true
                currentTab = StoreTab.HOME
            },
            onExploreGames = {
                hasEnteredApp = true
                currentTab = StoreTab.GAMES
            },
            onLaunchCreator = {
                hasEnteredApp = true
                currentTab = StoreTab.AI_IMAGES
            },
            onGoToPremium = {
                hasEnteredApp = true
                currentTab = StoreTab.PROFILE
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFF0D0B1C),
            bottomBar = {
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
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    StoreTab.HOME -> MarketplaceHomeScreen(
                        viewModel = viewModel,
                        onNavigateToPremium = { currentTab = StoreTab.PROFILE },
                        onNavigateToProfile = { currentTab = StoreTab.PROFILE }
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
        }
    }
}
