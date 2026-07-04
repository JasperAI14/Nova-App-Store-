package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppEntity
import com.example.ui.viewmodel.NovaStoreViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun GameStoreScreen(
    viewModel: NovaStoreViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()
    val appReviews by viewModel.selectedAppReviews.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val bookmarkedApps by viewModel.bookmarkedApps.collectAsState()
    val isBookmarked = selectedApp?.let { app -> bookmarkedApps.any { it.id == app.id } } ?: false

    var activeGameId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070514))
    ) {
        if (activeGameId != null) {
            val app = filteredApps.find { it.id == activeGameId } ?: selectedApp
            if (app != null) {
                PlayableGameSimulator(
                    app = app,
                    onClose = { activeGameId = null }
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
                onOpen = { activeGameId = selectedApp!!.id },
                onSubmitReview = { rating, comment ->
                    val author = userSession?.displayName ?: "Anonymous User"
                    viewModel.submitAppReview(selectedApp!!.id, author, rating, comment)
                },
                viewModel = viewModel
            )
        } else {
            // Main game store browsing
            Column(modifier = Modifier.fillMaxSize()) {
                GameStoreHeader(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) }
                )

                // Category chips
                GameCategoriesBar(
                    onSelectCategory = { viewModel.setSelectedCategory(it) }
                )

                val gamesOnly = filteredApps.filter { it.isGame }

                if (gamesOnly.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideogameAsset,
                                contentDescription = null,
                                tint = Color(0xFF757193),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Arcade Games found", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Try searching with general arcade terms.", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Action & Retro Arcade",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(gamesOnly) { game ->
                            val hasUpdate = game.isInstalled && game.installedVersion.isNotEmpty() && game.installedVersion != game.version
                            AppStoreRowItem(
                                app = game,
                                isDownloading = viewModel.downloadingStateMap[game.id] ?: false,
                                downloadProgress = viewModel.downloadProgressMap[game.id],
                                isInstalling = viewModel.installingStateMap[game.id] ?: false,
                                installProgress = viewModel.installProgressMap[game.id],
                                onClick = { viewModel.selectApp(game.id) },
                                onInstallClick = {
                                    if (hasUpdate) {
                                        viewModel.startAppUpdate(game.id)
                                    } else {
                                        viewModel.simulateAppInstallation(game.id)
                                    }
                                },
                                onOpenClick = { activeGameId = game.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameStoreHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF100D22))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Games Arcade",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(Color(0xFFFF007F).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "NEON PLAY",
                    color = Color(0xFFFF007F),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search arcade, action, puzzles...", color = Color(0xFF757193)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("game_store_search_input"),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF757193)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF191530),
                unfocusedContainerColor = Color(0xFF191530),
                focusedBorderColor = Color(0xFFFF007F),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun GameCategoriesBar(
    onSelectCategory: (String?) -> Unit
) {
    var selectedItem by remember { mutableStateOf("All Games") }
    val categories = listOf("All Games", "Arcade", "RPG", "Puzzle")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF070514))
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedItem
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFFFF007F) else Color(0xFF191530))
                    .clickable {
                        selectedItem = category
                        onSelectCategory(if (category == "All Games") null else category)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Playable Game Simulator Frame
@Composable
fun PlayableGameSimulator(
    app: AppEntity,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05030A))
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
                Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Arcade", tint = Color.White)
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
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF007F), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ARCADE HARDWARE ACTIVATED", color = Color(0xFFFF007F), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (app.id) {
                "game_snake" -> SnakeGamePlayable()
                "game_clicker" -> ClickerGamePlayable()
                "game_memory" -> MemoryGamePlayable()
                else -> GenericAppSimulator(app = app)
            }
        }
    }
}

// 1. Playable Snake Game
@Composable
fun SnakeGamePlayable() {
    val size = 15 // grid size 15x15
    var snake = remember { mutableStateListOf(Pair(7, 7), Pair(7, 8), Pair(7, 9)) }
    var direction by remember { mutableStateOf(Pair(0, -1)) } // North
    var food by remember { mutableStateOf(Pair(3, 3)) }
    var score by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }

    // Snake movement timer loop
    LaunchedEffect(direction, gameOver) {
        while (!gameOver) {
            delay(180) // speed tick
            val head = snake.first()
            val newHead = Pair(
                (head.first + direction.first + size) % size,
                (head.second + direction.second + size) % size
            )

            if (snake.contains(newHead)) {
                gameOver = true
                break
            }

            snake.add(0, newHead)
            if (newHead == food) {
                score += 10
                // spawn new food outside snake
                var newFood = Pair((0 until size).random(), (0 until size).random())
                while (snake.contains(newFood)) {
                    newFood = Pair((0 until size).random(), (0 until size).random())
                }
                food = newFood
            } else {
                snake.removeAt(snake.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Score Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ENERGY LEVEL: $score", color = Color(0xFFFF007F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (gameOver) {
                Text("MATRIX FRACTURED!", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Grid Canvas Box
        BoxWithConstraints(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .border(2.dp, Color(0xFFFF007F).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(Color(0xFF030107))
        ) {
            val cellWidth = maxWidth / size
            val cellHeight = maxHeight / size

            // Render food
            Box(
                modifier = Modifier
                    .offset(x = cellWidth * food.first, y = cellHeight * food.second)
                    .size(cellWidth, cellHeight)
                    .padding(2.dp)
                    .background(Color(0xFF00F5D4), CircleShape)
            )

            // Render snake body
            snake.forEachIndexed { idx, cell ->
                val isHead = idx == 0
                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * cell.first, y = cellHeight * cell.second)
                        .size(cellWidth, cellHeight)
                        .padding(2.dp)
                        .background(
                            if (isHead) Color(0xFFFF007F) else Color(0xFF7B2CBF),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        // Arrow Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            if (gameOver) {
                Button(
                    onClick = {
                        snake.clear()
                        snake.addAll(listOf(Pair(7, 7), Pair(7, 8), Pair(7, 9)))
                        direction = Pair(0, -1)
                        food = Pair(3, 3)
                        score = 0
                        gameOver = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reboot System", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                IconButton(
                    onClick = { if (direction.second != 1) direction = Pair(0, -1) },
                    modifier = Modifier.background(Color(0xFF1B1736), CircleShape).size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = { if (direction.first != 1) direction = Pair(-1, 0) },
                        modifier = Modifier.background(Color(0xFF1B1736), CircleShape).size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    IconButton(
                        onClick = { if (direction.first != -1) direction = Pair(1, 0) },
                        modifier = Modifier.background(Color(0xFF1B1736), CircleShape).size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = Color.White)
                    }
                }

                IconButton(
                    onClick = { if (direction.second != -1) direction = Pair(0, 1) },
                    modifier = Modifier.background(Color(0xFF1B1736), CircleShape).size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White)
                }
            }
        }
    }
}

// 2. Playable Clicker RPG Game
@Composable
fun ClickerGamePlayable() {
    var monsterHp by remember { mutableIntStateOf(100) }
    var playerGold by remember { mutableIntStateOf(0) }
    var damagePerTap by remember { mutableIntStateOf(10) }
    var titansActive by remember { mutableIntStateOf(0) }
    var bossLevel by remember { mutableIntStateOf(1) }

    val bossNames = listOf("Neon Cyber-Viper", "Giga Byte Golem", "Dark Quantum Phoenix", "Spanner Void Lord")
    val bossName = bossNames[(bossLevel - 1) % bossNames.size]

    // Automated passive titan DPS tick
    LaunchedEffect(titansActive) {
        while (titansActive > 0) {
            delay(1000)
            val dps = titansActive * 5
            if (monsterHp > dps) {
                monsterHp -= dps
            } else {
                // Boss defeated
                playerGold += 50 * bossLevel
                bossLevel++
                monsterHp = 100 * bossLevel
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GOLD: $playerGold G", color = Color(0xFFFFD100), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("DPS: ${titansActive * 5}", color = Color(0xFF00F5D4), fontSize = 12.sp)
        }

        // Boss Monster Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable {
                    if (monsterHp > damagePerTap) {
                        monsterHp -= damagePerTap
                    } else {
                        playerGold += 50 * bossLevel
                        bossLevel++
                        monsterHp = 100 * bossLevel
                    }
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF110B24)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LEVEL $bossLevel BOSS",
                    color = Color(0xFFFF007F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = bossName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Monster HP Bar
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$monsterHp / ${100 * bossLevel} HP", color = Color.White, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (monsterHp.toFloat() / (100 * bossLevel)).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFD62246), Color(0xFFFF007F))
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFFFF007F).copy(alpha = 0.1f), CircleShape)
                        .border(2.dp, Color(0xFFFF007F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("TAP TO SLASH", color = Color(0xFF757193), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Shop Upgrades
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("UPGRADE ARCADE STATS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            // Weapon upgrade
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131026), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Laser Blade (+10 tap DMG)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Cost: ${damagePerTap * 5} Gold", color = Color(0xFFFFD100), fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        val cost = damagePerTap * 5
                        if (playerGold >= cost) {
                            playerGold -= cost
                            damagePerTap += 10
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2CBF)),
                    enabled = playerGold >= damagePerTap * 5,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Buy", fontSize = 11.sp)
                }
            }

            // Titans upgrade
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131026), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto Titan Bot (+5 passive DPS)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Cost: ${(titansActive + 1) * 40} Gold", color = Color(0xFFFFD100), fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        val cost = (titansActive + 1) * 40
                        if (playerGold >= cost) {
                            playerGold -= cost
                            titansActive++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2CBF)),
                    enabled = playerGold >= (titansActive + 1) * 40,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Hire", fontSize = 11.sp)
                }
            }
        }
    }
}

// 3. Playable Memory Card matching game
@Composable
fun MemoryGamePlayable() {
    // 12 cards total, 6 pairs of unicode emoji symbols
    val emojis = listOf("👾", "🚀", "🛸", "🔥", "💎", "⭐", "👾", "🚀", "🛸", "🔥", "💎", "⭐")
    val cards = remember { mutableStateListOf<MemoryCardState>() }
    var moves by remember { mutableIntStateOf(0) }
    var matchesFound by remember { mutableIntStateOf(0) }

    // Init cards shuffled
    LaunchedEffect(Unit) {
        if (cards.isEmpty()) {
            val shuffled = emojis.shuffled().map { MemoryCardState(symbol = it, isFlipped = false, isMatched = false) }
            cards.addAll(shuffled)
        }
    }

    var selectedIndex1 by remember { mutableStateOf<Int?>(null) }
    var selectedIndex2 by remember { mutableStateOf<Int?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MOVES MADE: $moves", color = Color(0xFF00F5D4), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("MATCHES: $matchesFound / 6", color = Color(0xFFFF007F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (matchesFound == 6) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEURAL MATRIX COMPLETED!", color = Color(0xFF00F5D4), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        cards.clear()
                        val shuffled = emojis.shuffled().map { MemoryCardState(symbol = it, isFlipped = false, isMatched = false) }
                        cards.addAll(shuffled)
                        moves = 0
                        matchesFound = 0
                        selectedIndex1 = null
                        selectedIndex2 = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2CBF))
                ) {
                    Text("Sync Matrix Again")
                }
            }
        } else {
            // Lazy Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(cards.toList()) { index, card ->
                    val isRevealed = card.isFlipped || card.isMatched

                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                if (isChecking || card.isMatched || index == selectedIndex1) return@clickable

                                cards[index] = card.copy(isFlipped = true)

                                if (selectedIndex1 == null) {
                                    selectedIndex1 = index
                                } else {
                                    selectedIndex2 = index
                                    moves++
                                    isChecking = true
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRevealed) Color(0xFF131026) else Color(0xFF7B2CBF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF007F).copy(alpha = 0.4f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isRevealed) card.symbol else "?",
                                fontSize = 32.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Card match checking loop
        LaunchedEffect(selectedIndex2) {
            if (selectedIndex2 != null && selectedIndex1 != null) {
                delay(800)
                val card1 = cards[selectedIndex1!!]
                val card2 = cards[selectedIndex2!!]

                if (card1.symbol == card2.symbol) {
                    cards[selectedIndex1!!] = card1.copy(isMatched = true)
                    cards[selectedIndex2!!] = card2.copy(isMatched = true)
                    matchesFound++
                } else {
                    cards[selectedIndex1!!] = card1.copy(isFlipped = false)
                    cards[selectedIndex2!!] = card2.copy(isFlipped = false)
                }

                selectedIndex1 = null
                selectedIndex2 = null
                isChecking = false
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

data class MemoryCardState(
    val symbol: String,
    val isFlipped: Boolean,
    val isMatched: Boolean
)
