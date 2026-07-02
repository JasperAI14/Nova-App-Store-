package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.NovaStoreViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingScreen(
    viewModel: NovaStoreViewModel,
    onExploreApps: () -> Unit,
    onExploreGames: () -> Unit,
    onLaunchCreator: () -> Unit,
    onGoToPremium: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val userSession by viewModel.userSession.collectAsState()
    val isLoggedIn = userSession?.isLoggedIn == true

    // Fetch dynamic analytics counts
    val profiles by viewModel.allProfiles.collectAsState()
    val referrals by viewModel.allReferrals.collectAsState()

    val totalSignups = profiles.size
    val referralSignups = referrals.size
    val successfulReferrals = referrals.size
    val totalCreditsAwarded = profiles.sumOf { it.bonusGenerations }

    // Signup Input States
    var signupEmail by remember { mutableStateOf("test_visitor@gmail.com") }
    var signupName by remember { mutableStateOf("New Explorer") }
    var manualReferrerInput by remember { mutableStateOf("") }
    
    // Automatically apply referrer code from deep-link if available
    val activeReferrer = viewModel.referrerEmail ?: manualReferrerInput.trim()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C091A), // Pure Cosmic Black
                        Color(0xFF130E29), // Rich Indigo Nebula
                        Color(0xFF05040B)  // Dark Space Void
                    )
                )
            )
            .verticalScroll(scrollState)
    ) {
        // --- HERO BANNER & INTRO SECTION ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_landing_hero),
                contentDescription = "Nova App Store Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Glowing Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x990C091A),
                                Color(0xFF0C091A)
                            )
                        )
                    )
            )

            // Back button if in-app viewer mode
            if (onBack != null) {
                androidx.compose.material3.IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("landing_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Dashboard",
                        tint = Color.White
                    )
                }
            }

            // Hero Brand Title overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_nova_logo_1782386944329),
                        contentDescription = "Nova App Store Logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF00FFFF), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Nova App Store",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Discover Apps, Retro Games, and Creative AI Studio Tools",
                    color = Color(0xFFB0AEC6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // --- GUEST SIGN-UP / ONBOARDING ACCESS BOARD ---
            if (!isLoggedIn) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131024)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF00FFFF),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Guest Onboarding Portal",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Every new account receives 1 free AI Image credit instantly! Joint via referral to unlock an additional credit.",
                            color = Color(0xFFB0AEC6),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Input fields for email and name
                        OutlinedTextField(
                            value = signupEmail,
                            onValueChange = { signupEmail = it },
                            label = { Text("Google Account Email") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_email_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFFF),
                                unfocusedBorderColor = Color(0xFF332D59),
                                focusedLabelColor = Color(0xFF00FFFF),
                                unfocusedLabelColor = Color(0xFF757193)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = signupName,
                            onValueChange = { signupName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_name_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFFF),
                                unfocusedBorderColor = Color(0xFF332D59),
                                focusedLabelColor = Color(0xFF00FFFF),
                                unfocusedLabelColor = Color(0xFF757193)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = manualReferrerInput,
                            onValueChange = { manualReferrerInput = it },
                            label = { Text("Referral Email / Link (Optional)") },
                            placeholder = { Text("e.g. lorrenthaonah@gmail.com") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_referrer_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF7B2CBF),
                                unfocusedBorderColor = Color(0xFF332D59),
                                focusedLabelColor = Color(0xFF7B2CBF),
                                unfocusedLabelColor = Color(0xFF757193)
                            )
                        )

                        // Referral link applied notification banner
                        if (activeReferrer.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF00F5D4).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, Color(0xFF00F5D4).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid",
                                        tint = Color(0xFF00F5D4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Referral code applied: $activeReferrer",
                                        color = Color(0xFF00F5D4),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Button 1: Prominent Continue with Google
                        Button(
                            onClick = {
                                if (signupEmail.trim().isBlank()) {
                                    Toast.makeText(context, "Please enter a valid Google email address.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.signInUser(
                                    email = signupEmail,
                                    displayName = signupName,
                                    referrerEmail = activeReferrer.ifBlank { null },
                                    onComplete = { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("continue_google_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7B2CBF),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Continue with Google", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Button 2: Get Started Free
                        OutlinedButton(
                            onClick = {
                                if (signupEmail.trim().isBlank()) {
                                    Toast.makeText(context, "Please enter a valid Google email address.", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                viewModel.signInUser(
                                    email = signupEmail,
                                    displayName = signupName,
                                    referrerEmail = activeReferrer.ifBlank { null },
                                    onComplete = { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("get_started_free_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF00F5D4)
                            ),
                            border = borderStrokeGlow()
                        ) {
                            Text("Get Started Free", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Authenticated exploration action row
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onExploreApps,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("hero_explore_apps_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7B2CBF),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Explore Apps", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onExploreGames,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("hero_explore_games_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00F5D4)
                        ),
                        border = borderStrokeGlow()
                    ) {
                        Icon(imageVector = Icons.Default.Gamepad, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retro Arcade", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- FEATURES OVERVIEW SECTION ---
            Text(
                text = "Nova Platform Ecosystem",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureCard(
                    icon = Icons.Default.ShoppingBag,
                    title = "App Discovery Hub",
                    description = "Discover productivity tools, fitness companion utilities, weather widgets, and developer integrations.",
                    tint = Color(0xFF8A2BE2),
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Default.Gamepad,
                    title = "Interactive Games",
                    description = "Natively built interactive games including classic retro Snake, memory cards, and tapping simulators.",
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Default.Brush,
                    title = "Creative AI Studio",
                    description = "Generate unique image creations powered by Google Gemini model parameter sets.",
                    tint = Color(0xFFFF007F),
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Default.Security,
                    title = "Integrity Scan",
                    description = "All application assets undergo automatic VirusTotal hashing scans, ensuring clean sandboxes.",
                    tint = Color(0xFF3A86FF),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- HOW IT WORKS SECTION ---
            Text(
                text = "How Nova App Store Works",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(14.dp))

            OnboardingStepRow(step = "1", title = "Explore & Gather Referrals", description = "Enter the platform and get unique links to invite friends. Pre-registered links are automatically validated.")
            OnboardingStepRow(step = "2", title = "Authenticate via Google", description = "Securely verify your credential keys to claim free welcome tokens and preserve your library across device platforms.")
            OnboardingStepRow(step = "3", title = "Play, Install or Publish", description = "Install simulated APK packages directly, launch arcade retro files, or host/moderate your own APK projects.")
            OnboardingStepRow(step = "4", title = "Accrue Creator Rewards", description = "Refer companions to receive persistent +1 bonus tokens credited in real-time to your dashboard balance.")

            Spacer(modifier = Modifier.height(28.dp))

            // --- BENEFITS FOR USERS ---
            Text(
                text = "Benefits For Users",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16122C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BenefitRow(title = "Safe Sandbox downloads", desc = "All hosted products have passed 72 signature checks.")
                    BenefitRow(title = "Natively Embedded Play", desc = "Enjoy responsive arcade gameplay without high download requirements.")
                    BenefitRow(title = "Immediate Token Balance", desc = "Free creation credit allocated to every explorer on account setup.")
                    BenefitRow(title = "Adaptive M3 Dark UI", desc = "Crafted for low-light environments with comfortable displays.")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- BENEFITS FOR DEVELOPERS ---
            Text(
                text = "Benefits For Developers",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16122C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BenefitRow(title = "Self-Service Upload Portal", desc = "Deploy updates, size parameters, and APK binaries directly.")
                    BenefitRow(title = "Automated Integrity Reports", desc = "Receive simulated analysis scans instantly on publishing.")
                    BenefitRow(title = "Zero-Cost Project Hosting", desc = "Scale your deployment with infinite package lists at zero charge.")
                    BenefitRow(title = "Actionable User Feedback", desc = "Observe review logs and score curves directly on your developer view.")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- ANALYTICS TRACKING FOOTER ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF7B2CBF).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0C20)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nova Platform Analytics (Live Statistics)",
                        color = Color(0xFF00FFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$totalSignups", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = "Total Signups", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$referralSignups", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = "Referrals Recorded", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$totalCreditsAwarded", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = "Credits Awarded", color = Color(0xFFB0AEC6), fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- FREQUENTLY ASKED QUESTIONS ---
            Text(
                text = "Frequently Asked Questions",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))

            FaqCard(
                question = "How does the AI failover system safeguard generations?",
                answer = "The AI studio routes creative prompts primarily through Google Gemini. If quota exhaustion occurs, the platform automatically redirects parameters to background local-creative brushes, guaranteeing 100% processing uptime."
            )
            FaqCard(
                question = "Are the APK files safe for physical targets?",
                answer = "Absolutely. Each uploaded binary file is checked by automated hash verification routines mimicking a complete 72-engine VirusTotal signature audit prior to catalog listings."
            )
            FaqCard(
                question = "How do I claim referral image tokens?",
                answer = "Once your referred visitor signs up using your Google Email as their referral code or via your pre-registered referral link, you and your visitor are both instantly awarded +1 bonus credit."
            )
            FaqCard(
                question = "Are there limits to total referrals?",
                answer = "No! You can refer as many developers and consumers as you wish. Multi-account abuse is checked through validation logs to secure fair platform distributions."
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- CALL TO ACTION FOOTER ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF120F26)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Claim Your Free Space Token Today!",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign in to start downloading certified software apps, competing on retro game scoreboards, and utilizing Gemini image algorithms.",
                        color = Color(0xFFB0AEC6),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun BenefitRow(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF00F5D4),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = Color(0xFFB0AEC6),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun FeatureCard(icon: ImageVector, title: String, description: String, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131024)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = Color(0xFFB0AEC6), fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun OnboardingStepRow(step: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color(0xFF7B2CBF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = description, color = Color(0xFFB0AEC6), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
fun FaqCard(question: String, answer: String) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131024)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF00FFFF),
                    modifier = Modifier.size(20.dp)
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = answer, color = Color(0xFFB0AEC6), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

fun borderStrokeGlow(): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF8A2BE2), Color(0xFF00FFFF))
        )
    )
}
