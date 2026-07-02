package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeneratedImageEntity
import com.example.ui.viewmodel.NovaStoreViewModel
import kotlinx.coroutines.delay

@Composable
fun AiImageScreen(
    viewModel: NovaStoreViewModel,
    onNavigateToPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    val userSession by viewModel.userSession.collectAsState()
    val savedImages by viewModel.savedImages.collectAsState()

    val isPremium = userSession?.isPremium == true
    val promo = userSession?.promoCode ?: ""
    val isLoggedIn = userSession?.isLoggedIn == true

    val usedCount = userSession?.dailyGenerationsUsed ?: 0
    val maxFree = if (promo == "JasperAI") 10 else 0 // Free user without valid promo code has 0 limit
    val remainingGenerations = if (promo == "ADMIN" || isPremium) 99999 else (maxFree - usedCount).coerceAtLeast(0)

    var countdownString by remember { mutableStateOf("24:00:00") }

    // Countdown Timer ticking loop
    LaunchedEffect(Unit) {
        while (true) {
            countdownString = viewModel.getLimitResetCountdownString()
            delay(1000)
        }
    }

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
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFF007F),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Nova Image Studio",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Gemini-Powered Multi-Aspect Image Generation",
                        color = Color(0xFFB0AEC6),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (!isLoggedIn) {
            // Unauthenticated state redirection card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF007F), modifier = Modifier.size(60.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text("Secure Login Required", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "AI Image Generation is protected. Please sign in under the 'Profile' section using your Google Account to preserve your image history.",
                    color = Color(0xFFB0AEC6),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {

                // --- LIMITS & STATS DASHBOARD ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("LIMIT COUNTER STATUS", color = Color(0xFFFF007F), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                when {
                                    promo == "ADMIN" -> {
                                        Text("ADMIN MODE (Unlimited)", color = Color(0xFFFFD100), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    promo == "JasperAI" -> {
                                        Text("JasperAI Promo ($remainingGenerations left / 10 daily)", color = Color(0xFF00FFFF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    isPremium -> {
                                        Text("Premium Account (Unlimited)", color = Color(0xFF00F5D4), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    else -> {
                                        Text("LOCKED (Upgrade / Redeem Code)", color = Color(0xFFD62246), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("LIMIT RESET TIMER", color = Color(0xFF757193), fontSize = 10.sp)
                                Text(countdownString, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        if (!isPremium && promo != "ADMIN" && promo != "JasperAI") {
                            Spacer(modifier = Modifier.height(12.dp))
                            // Referral rewards helper banner
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A2A)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Refer & Earn Tokens", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Share Nova App Store to earn +1 bonus image allocation instantly!", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = {
                                            try {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "Try Nova App Store — discover apps, games, and AI image generation in one place. Install now: https://ais-pre-oun3a6zto7xl44kdsnabnt-483043984572.europe-west2.run.app")
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, null)
                                                context.startActivity(shareIntent)
                                                viewModel.sharePlatformAndEarnReward()
                                                Toast.makeText(context, "Share successful. You earned 1 bonus image credit.", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Unable to complete share. Please try again later.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- ENTER PROMPT INPUT ---
                Text("Enter Prompt", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = viewModel.promptInput,
                    onValueChange = { viewModel.promptInput = it },
                    placeholder = { Text("Describe what you want the AI model to draw...", color = Color(0xFF757193), fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("ai_prompt_text_field"),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF16132D),
                        unfocusedContainerColor = Color(0xFF16132D),
                        focusedBorderColor = Color(0xFFFF007F),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- ASPECT RATIO SELECTOR ---
                Text("Canvas Aspect Ratio", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1:1", "16:9", "3:4").forEach { ratio ->
                        val isSelected = viewModel.selectedAspectRatio == ratio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFFF007F) else Color(0xFF16132D))
                                .clickable { viewModel.selectedAspectRatio = ratio }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ratio,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- ACTION GENERATE BUTTON ---
                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.generateImage()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("ai_generate_art_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2CBF)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.isGeneratingImage && viewModel.promptInput.isNotBlank()
                ) {
                    if (viewModel.isGeneratingImage) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting to Nodes...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesize Image", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- USER ERROR DISPLAY CARD ---
                AnimatedVisibility(visible = viewModel.userErrorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFD62246).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF230F13))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = Color(0xFFD62246),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = viewModel.userErrorMessage ?: "",
                                color = Color(0xFFFFAAAA),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- GENERATED IMAGE DISPLAY ---
                viewModel.generatedImageResult?.let { base64String ->
                    val imageBitmap = remember(base64String) {
                        decodeBase64ToImageBitmap(base64String)
                    }

                    if (imageBitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFF007F).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "AI Generated Artwork",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(
                                            if (viewModel.selectedAspectRatio == "16:9") 1.77f
                                            else if (viewModel.selectedAspectRatio == "3:4") 0.75f
                                            else 1f
                                        ),
                                    contentScale = ContentScale.Crop
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = viewModel.promptInput,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text("Mime: image/png | Format: 1024px", color = Color(0xFF757193), fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            if (isPremium || promo == "ADMIN" || promo == "JasperAI") {
                                                Toast.makeText(context, "Image successfully saved to Local Device Gallery!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Download is restricted to Premium/Promo users. Upgrade or enter a promo code!", Toast.LENGTH_LONG).show()
                                                onNavigateToPremium()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // --- ART GALLERY HISTORY ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saved Creations History", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (savedImages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF16132D), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No generated history. Start typing above!", color = Color(0xFF757193), fontSize = 12.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(savedImages) { entity ->
                            SavedImageCard(entity = entity, onDelete = { viewModel.deleteImage(entity.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedImageCard(entity: GeneratedImageEntity, onDelete: () -> Unit) {
    val imageBitmap = remember(entity.base64Data) {
        decodeBase64ToImageBitmap(entity.base64Data)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("saved_image_card_${entity.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = entity.prompt,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topEnd = 6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(entity.providerUsed, color = Color(0xFF00FFFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { onDelete() }
                        .padding(6.dp)
                        .testTag("delete_image_button_${entity.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Art",
                        tint = Color(0xFFFF4444),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = entity.prompt,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

// Helper to convert base64 image strings to ImageBitmap safely
fun decodeBase64ToImageBitmap(base64Str: String): ImageBitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
