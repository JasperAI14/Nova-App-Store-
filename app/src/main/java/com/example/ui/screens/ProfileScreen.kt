package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.NovaStoreViewModel

@Composable
fun ProfileScreen(
    viewModel: NovaStoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val userSession by viewModel.userSession.collectAsState()
    val savedImages by viewModel.savedImages.collectAsState()
    val developerApps by viewModel.developerApps.collectAsState()
    val bookmarkedApps by viewModel.bookmarkedApps.collectAsState()

    var showGoogleSignInDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var customGoogleEmail by remember { mutableStateOf("") }
    var customGoogleName by remember { mutableStateOf("") }

    var editDisplayName by remember(userSession) { mutableStateOf(userSession?.displayName ?: "") }
    var editAvatarUrl by remember(userSession) { mutableStateOf(userSession?.avatarUrl ?: "") }

    val isLoggedIn = userSession?.isLoggedIn == true
    val isPremium = userSession?.isPremium == true

    // Payment fields
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    // Payment flow states driven by ViewModel
    val paymentState = viewModel.paymentState
    val isProcessing = paymentState is NovaStoreViewModel.PaymentState.Processing
    var pinValue by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }

    LaunchedEffect(paymentState) {
        when (paymentState) {
            is NovaStoreViewModel.PaymentState.Success -> {
                Toast.makeText(context, paymentState.message, Toast.LENGTH_LONG).show()
                viewModel.resetPaymentState()
            }
            is NovaStoreViewModel.PaymentState.Error -> {
                Toast.makeText(context, paymentState.error, Toast.LENGTH_LONG).show()
                viewModel.resetPaymentState()
            }
            else -> {}
        }
    }

    if (paymentState is NovaStoreViewModel.PaymentState.RequirePin) {
        AlertDialog(
            onDismissRequest = { viewModel.resetPaymentState() },
            title = { Text("Enter Card PIN", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("This transaction requires your card's 4-digit PIN for authorization.", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 4) pinValue = it },
                        placeholder = { Text("4-digit PIN", color = Color(0xFF757193)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD100)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("payment_pin_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValue.length == 4) {
                            viewModel.submitPin(pinValue, paymentState.reference) { success ->
                                if (success) {
                                    Toast.makeText(context, "Payment Processed! Welcome to Premium!", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD100)),
                    enabled = pinValue.length == 4,
                    modifier = Modifier.testTag("submit_pin_button")
                ) {
                    Text("Submit PIN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.resetPaymentState() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1736))
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF131026)
        )
    }

    if (paymentState is NovaStoreViewModel.PaymentState.RequireOtp) {
        AlertDialog(
            onDismissRequest = { viewModel.resetPaymentState() },
            title = { Text("Enter OTP", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("A verification code (OTP) was sent by your bank. Please enter it to authorize the purchase.", color = Color(0xFFB0AEC6), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = otpValue,
                        onValueChange = { if (it.length <= 10) otpValue = it },
                        placeholder = { Text("OTP code", color = Color(0xFF757193)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD100)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("payment_otp_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (otpValue.isNotEmpty()) {
                            viewModel.submitOtp(otpValue, paymentState.reference) { success ->
                                if (success) {
                                    Toast.makeText(context, "Payment Processed! Welcome to Premium!", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter OTP code", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD100)),
                    enabled = otpValue.isNotEmpty(),
                    modifier = Modifier.testTag("submit_otp_button")
                ) {
                    Text("Verify OTP", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.resetPaymentState() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1736))
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF131026)
        )
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
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF7B2CBF),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Nova Secure Profile",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Ecosystem Identity and Premium Memberships",
                        color = Color(0xFFB0AEC6),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            if (!isLoggedIn) {
                // --- SIGN IN SCREEN ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF7B2CBF).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF00FFFF), modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "A Unified Cloud Account",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Signing in with your Google account encrypts and secures your custom AI image history, developer uploaded APK bundles, submitted reviews, and premium referral balances.",
                            color = Color(0xFFB0AEC6),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showGoogleSignInDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_signin_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7B2CBF),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Google", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showGoogleSignInDialog) {
                    AlertDialog(
                        onDismissRequest = { showGoogleSignInDialog = false },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("G", color = Color(0xFF4285F4), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("o", color = Color(0xFFEA4335), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("o", color = Color(0xFFFBBC05), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("g", color = Color(0xFF4285F4), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("l", color = Color(0xFF34A853), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("e", color = Color(0xFFEA4335), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sign In", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Choose a Google account to log in and sync your history, reviews, and custom AI image creations securely.",
                                    color = Color(0xFFB0AEC6),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.signInUser(
                                                email = "lorrenthaonah@gmail.com",
                                                displayName = "Lorren Thaonah",
                                                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60"
                                            )
                                            showGoogleSignInDialog = false
                                            Toast.makeText(context, "Logged in as Lorren Thaonah!", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("account_card_lorren"),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221C42)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60",
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Lorren Thaonah", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("lorrenthaonah@gmail.com", color = Color(0xFF8E8CA8), fontSize = 11.sp)
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.signInUser(
                                                email = "developer.beta@gmail.com",
                                                displayName = "Ecosystem Developer",
                                                avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=120&auto=format&fit=crop&q=60"
                                            )
                                            showGoogleSignInDialog = false
                                            Toast.makeText(context, "Logged in as Ecosystem Developer!", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("account_card_dev"),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221C42)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=120&auto=format&fit=crop&q=60",
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Ecosystem Developer", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("developer.beta@gmail.com", color = Color(0xFF8E8CA8), fontSize = 11.sp)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF2E2A4F)))
                                    Text(" OR USE CUSTOM ACCOUNT ", color = Color(0xFF757193), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF2E2A4F)))
                                }

                                OutlinedTextField(
                                    value = customGoogleEmail,
                                    onValueChange = { customGoogleEmail = it },
                                    label = { Text("Google Email", color = Color(0xFF757193), fontSize = 11.sp) },
                                    placeholder = { Text("e.g. user@gmail.com", color = Color(0xFF5A5775), fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF7B2CBF),
                                        unfocusedBorderColor = Color(0xFF2E2A4F)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_google_email_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = customGoogleName,
                                    onValueChange = { customGoogleName = it },
                                    label = { Text("Full Name", color = Color(0xFF757193), fontSize = 11.sp) },
                                    placeholder = { Text("e.g. Alex Mercer", color = Color(0xFF5A5775), fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF7B2CBF),
                                        unfocusedBorderColor = Color(0xFF2E2A4F)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_google_name_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val email = customGoogleEmail.trim()
                                    if (email.isNotEmpty() && email.contains("@")) {
                                        viewModel.signInUser(
                                            email = email,
                                            displayName = customGoogleName.ifEmpty { "Google User" },
                                            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120&auto=format&fit=crop&q=60"
                                        )
                                        showGoogleSignInDialog = false
                                        Toast.makeText(context, "Logged in as ${customGoogleName.ifEmpty { "Google User" }}!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please enter a valid Google email address.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2CBF)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("confirm_custom_google_signin_button")
                            ) {
                                Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showGoogleSignInDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFFB0AEC6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", fontSize = 13.sp)
                            }
                        },
                        containerColor = Color(0xFF131026),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            } else {
                // --- PROFILE VIEW SCREEN ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar Row
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = userSession?.avatarUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, if (isPremium) Color(0xFFFFD100) else Color(0xFF7B2CBF), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .background(if (isPremium) Color(0xFFFFD100) else Color(0xFF7B2CBF), CircleShape)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPremium) Icons.Default.Diamond else Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = if (isPremium) Color.Black else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = userSession?.displayName ?: "Google Member",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = userSession?.email ?: "",
                            color = Color(0xFFB0AEC6),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Premium Status Badge
                        val promo = userSession?.promoCode ?: ""
                        val badgeText = when {
                            promo == "ADMIN" -> "ADMIN CREATOR SYSTEM"
                            promo == "JasperAI" -> "JASPERAI PREMIUM ACCOUNT"
                            isPremium -> "PREMIUM COSMIC MEMBER"
                            else -> "FREE ECOSYSTEM ACCOUNT"
                        }
                        val badgeColor = when {
                            promo == "ADMIN" -> Color(0xFFFFD100)
                            promo == "JasperAI" -> Color(0xFF00FFFF)
                            isPremium -> Color(0xFF00F5D4)
                            else -> Color(0xFFB0AEC6)
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    badgeColor.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A4F)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile Details", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                if (showEditProfileDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditProfileDialog = false },
                        title = { Text("Edit Profile Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = editDisplayName,
                                    onValueChange = { editDisplayName = it },
                                    label = { Text("Display Name", color = Color(0xFF757193), fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF7B2CBF),
                                        unfocusedBorderColor = Color(0xFF2E2A4F)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = editAvatarUrl,
                                    onValueChange = { editAvatarUrl = it },
                                    label = { Text("Avatar Image URL", color = Color(0xFF757193), fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF7B2CBF),
                                        unfocusedBorderColor = Color(0xFF2E2A4F)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_avatar_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.updateProfileSettings(editDisplayName.trim(), editAvatarUrl.trim()) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            showEditProfileDialog = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("save_profile_button")
                            ) {
                                Text("Save Settings", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showEditProfileDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFFB0AEC6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", fontSize = 13.sp)
                            }
                        },
                        containerColor = Color(0xFF131026),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- PREMIUM GATEWAY OR STATUS CARD ---
                if (isPremium) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFFFFD100), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF221A0F)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color(0xFFFFD100).copy(alpha = 0.2f), CircleShape)
                                    .border(1.5.dp, Color(0xFFFFD100), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = Color(0xFFFFD100), modifier = Modifier.size(30.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "PREMIUM ACTIVE",
                                color = Color(0xFFFFD100),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Thank you for supporting Nova App Store! You have unlocked unlimited image generations, instant high-res downloads, and security fast-tracks.",
                                color = Color(0xFFE4DCC4),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFFD100).copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = Color(0xFFFFD100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock Premium features", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("₦1,500/mo", color = Color(0xFF00F5D4), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Benefits listed concisely
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFFD100), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unlimited AI Art Generation (no limits)", color = Color(0xFFB0AEC6), fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFFD100), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Instant PNG saving to device gallery", color = Color(0xFFB0AEC6), fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFFD100), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fast-track secure developer package scan", color = Color(0xFFB0AEC6), fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Credit Card Fields
                            Text("Secure Paystack Checkout", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))

                            ProfilePaymentField(
                                label = "Card Number",
                                value = cardNumber,
                                onValueChange = { if (it.length <= 16) cardNumber = it },
                                placeholder = "4321 0987 6543 2109",
                                keyboardType = KeyboardType.Number,
                                testTag = "premium_card_number"
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ProfilePaymentField(
                                        label = "Expiry Date",
                                        value = cardExpiry,
                                        onValueChange = { if (it.length <= 5) cardExpiry = it },
                                        placeholder = "MM/YY",
                                        keyboardType = KeyboardType.Number,
                                        testTag = "premium_card_expiry"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    ProfilePaymentField(
                                        label = "CVV",
                                        value = cvv,
                                        onValueChange = { if (it.length <= 3) cvv = it },
                                        placeholder = "123",
                                        keyboardType = KeyboardType.Number,
                                        testTag = "premium_card_cvv"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (cardNumber.length < 12 || cardExpiry.length < 4 || cvv.length < 3) {
                                        Toast.makeText(context, "Please enter valid payment details.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.buyPremiumMembership(cardNumber, cardExpiry, cvv) { success ->
                                        // Controlled reactively by paymentState
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("process_paystack_payment"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD100)),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isProcessing && cardNumber.isNotEmpty() && cardExpiry.isNotEmpty() && cvv.isNotEmpty()
                            ) {
                                if (isProcessing) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Processing securely...", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("Pay ₦1,500.00 via Paystack", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- PROMO CODE REDEMPTION CARD ---
                var promoCodeInput by remember { mutableStateOf(userSession?.promoCode ?: "") }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("promo_code_redemption_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16132D)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = Color(0xFF00F5D4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Redeem Promo Code", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter a promotional code to activate premium access or special account limits (e.g. ADMIN or JasperAI).",
                            color = Color(0xFFB0AEC6),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = promoCodeInput,
                                onValueChange = { promoCodeInput = it },
                                placeholder = { Text("Enter Promo Code", color = Color(0xFF757193), fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00F5D4),
                                    unfocusedBorderColor = Color(0xFF2E2A4F)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("promo_code_text_field"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    viewModel.applyPromoCode(promoCodeInput) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            promoCodeInput = promoCodeInput.trim()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("apply_promo_code_button")
                            ) {
                                Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        
                        if (userSession?.promoCode?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Active Code: ${userSession?.promoCode}",
                                    color = Color(0xFF00F5D4),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Clear",
                                    color = Color(0xFFD62246),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.applyPromoCode("") { success, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                if (success) {
                                                    promoCodeInput = ""
                                                }
                                            }
                                        }
                                        .testTag("clear_promo_code_button")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- BOOKMARKED APPLICATIONS ---
                Text("Your Bookmarked Applications", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                if (bookmarkedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF131026), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No bookmarked applications yet.", color = Color(0xFF757193), fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bookmarkedApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF131026), RoundedCornerShape(10.dp))
                                    .clickable { viewModel.selectApp(app.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                    Text(app.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(app.category, color = Color(0xFFB0AEC6), fontSize = 11.sp)
                                }
                                IconButton(
                                    onClick = { viewModel.toggleBookmark(app.id) },
                                    modifier = Modifier.testTag("unbookmark_button_${app.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Unbookmark",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- CLOUD METRICS LIST ---
                Text("Ecosystem Cloud Metrics", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTrackRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Saved AI Creations",
                        value = "${savedImages.size} artworks",
                        tint = Color(0xFFFF007F)
                    )
                    MetricTrackRow(
                        icon = Icons.Default.Share,
                        title = "Platform Referral Shares",
                        value = "${userSession?.sharesCount ?: 0} referrals",
                        tint = Color(0xFF00FFFF)
                    )
                    MetricTrackRow(
                        icon = Icons.Default.Terminal,
                        title = "Referral Bonus Earned",
                        value = "+${userSession?.bonusGenerations ?: 0} allocations",
                        tint = Color(0xFF7B2CBF)
                    )
                    MetricTrackRow(
                        icon = Icons.Default.UploadFile,
                        title = "Developer Published Bundles",
                        value = "${developerApps.size} packages",
                        tint = Color(0xFF00F5D4)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- LOGOUT ACTION ---
                Button(
                    onClick = { viewModel.signOutUser() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("logout_account_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B1736),
                        contentColor = Color(0xFFD62246)
                    )
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFD62246))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout Credentials", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ProfilePaymentField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    testTag: String = ""
) {
    Column {
        Text(label, color = Color(0xFFB0AEC6), fontSize = 10.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF757193), fontSize = 11.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag(testTag),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F0C20),
                unfocusedContainerColor = Color(0xFF0F0C20),
                focusedBorderColor = Color(0xFFFFD100),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}

@Composable
fun MetricTrackRow(
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131026), RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(value, color = Color(0xFF00F5D4), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
