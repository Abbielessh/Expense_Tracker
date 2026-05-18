package com.example.expensetracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN_SECONDS = 60

// ─────────────────────────────────────────────────────────────────────────────
// Helper — maps raw exception messages to user-friendly strings
// ─────────────────────────────────────────────────────────────────────────────
private fun mapResendError(e: Exception): String {
    val raw = e.message?.lowercase() ?: ""
    return when {
        raw.contains("429") ||
        raw.contains("rate") ||
        raw.contains("too many") ||
        raw.contains("limit") ->
            "📬 Email limit reached. Please wait up to 1 hour and try again."

        raw.contains("timeout") ||
        e.javaClass.simpleName.contains("Timeout") ||
        raw.contains("network") ||
        raw.contains("unable to resolve") ||
        raw.contains("connection") ->
            "📶 Network error. Please check your internet and try again."

        else ->
            "❌ ${e.message ?: "Failed to resend. Please try again."}"
    }
}

private fun mapSignupError(e: Exception): String {
    val raw = e.message?.lowercase() ?: ""
    return when {
        raw.contains("timeout") || e.javaClass.simpleName.contains("Timeout") ->
            "Network timeout. Please check your internet connection."
        raw.contains("user already registered") ->
            "This email is already registered. Please log in."
        raw.contains("invalid email") ->
            "Please enter a valid email address."
        raw.contains("password") && (raw.contains("short") || raw.contains("weak")) ->
            "Password must be at least 6 characters."
        else -> e.message ?: "Sign up failed. Please try again."
    }
}

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    repository: com.example.expensetracker.data.SupabaseRepository
) {
    // rememberSaveable survives recompositions AND configuration changes
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Email-confirmation state
    var emailSent by rememberSaveable { mutableStateOf(false) }
    var pendingEmail by rememberSaveable { mutableStateOf("") }   // frozen after signup

    // Resend state
    var isResending by rememberSaveable { mutableStateOf(false) }
    var resendFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    var resendIsError by rememberSaveable { mutableStateOf(false) }
    var cooldownSeconds by rememberSaveable { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    // Countdown ticker — re-triggered every second while cooldown > 0
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1_000L)
            cooldownSeconds--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (emailSent) {
                // ── Confirmation-pending state ─────────────────────────────

                Text(
                    text = "Check your email",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101828)
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Green confirmation card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📧", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Confirm your email",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We sent a confirmation link to:",
                            fontSize = 13.sp,
                            color = Color(0xFF388E3C),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pendingEmail,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B5E20),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tap the link in the email — the app will open automatically and log you in.",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32),
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Helper text about Supabase rate limits
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "ℹ️ Supabase may limit confirmation emails. If you don't receive it, check your Spam / Promotions folder or try again later.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF795548),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Animated resend feedback (success or error)
                AnimatedVisibility(
                    visible = resendFeedback != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    resendFeedback?.let { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (resendIsError) Color(0xFFFFEBEE)
                                                else Color(0xFFE3F2FD)
                            )
                        ) {
                            Text(
                                text = msg,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                color = if (resendIsError) Color(0xFFB71C1C)
                                        else Color(0xFF0D47A1),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Resend button with cooldown
                Button(
                    onClick = {
                        scope.launch {
                            isResending = true
                            resendFeedback = null
                            try {
                                repository.resendConfirmationEmail(pendingEmail)
                                resendIsError = false
                                resendFeedback =
                                    "✅ Confirmation email sent again. Please check your inbox and spam folder."
                                cooldownSeconds = RESEND_COOLDOWN_SECONDS
                            } catch (e: Exception) {
                                resendIsError = true
                                resendFeedback = mapResendError(e)
                                // Still apply a short cooldown on error to avoid spam-clicking
                                cooldownSeconds = RESEND_COOLDOWN_SECONDS
                            } finally {
                                isResending = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isResending && cooldownSeconds == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1565C0),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBBDEFB),
                        disabledContentColor = Color(0xFF0D47A1)
                    )
                ) {
                    when {
                        isResending -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resending...", fontWeight = FontWeight.SemiBold)
                        }
                        cooldownSeconds > 0 -> {
                            Text(
                                text = "Resend again in ${cooldownSeconds}s",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else -> {
                            Text(
                                text = "Resend confirmation email",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Back to Login — prominent outlined button
                OutlinedButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1565C0)
                    )
                ) {
                    Text("Back to Login", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subtle text link
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Already confirmed? Go to Login",
                        color = Color(0xFF667085),
                        fontSize = 13.sp
                    )
                }

            } else {
                // ── Sign-up form ───────────────────────────────────────────

                Text(
                    text = "Create Account",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF101828)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sign up to start tracking your expenses",
                    fontSize = 14.sp,
                    color = Color(0xFF667085)
                )
                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Password must be at least 6 characters",
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Text(
                                text = msg,
                                modifier = Modifier.padding(10.dp),
                                fontSize = 13.sp,
                                color = Color(0xFFB71C1C),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                repository.signUp(email.trim(), password)
                                pendingEmail = email.trim()
                                emailSent = true
                            } catch (e: Exception) {
                                errorMessage = mapSignupError(e)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00A3FF),
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Signing up...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Sign Up", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = Color(0xFF667085),
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = !isLoading,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Login",
                            color = Color(0xFF00A3FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
