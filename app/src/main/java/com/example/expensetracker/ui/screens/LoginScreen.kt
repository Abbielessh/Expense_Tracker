package com.example.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    repository: com.example.expensetracker.data.SupabaseRepository
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome Back", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF101828))
        Spacer(modifier = Modifier.height(4.dp))
        Text("Sign in to your account", fontSize = 14.sp, color = Color(0xFF667085))
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
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(
                    onClick = { passwordVisible = !passwordVisible },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(if (passwordVisible) "Hide" else "Show", fontSize = 12.sp, color = Color(0xFF667085))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { showForgotPassword = true }, contentPadding = PaddingValues(0.dp)) {
                Text("Forgot Password?", fontSize = 13.sp, color = Color(0xFF00A3FF))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(errorMessage!!, modifier = Modifier.padding(10.dp), fontSize = 13.sp, color = Color(0xFFB71C1C))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        repository.login(email.trim(), password)
                        onLoginSuccess()
                    } catch (e: Exception) {
                        val raw = e.message?.lowercase() ?: ""
                        errorMessage = when {
                            raw.contains("timeout") || e.javaClass.simpleName.contains("Timeout") ->
                                "Network timeout. Please check your internet connection."
                            raw.contains("invalid") || raw.contains("credentials") ->
                                "Invalid email or password. Please try again."
                            else -> e.message ?: "Login failed"
                        }
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A3FF), contentColor = Color.White)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logging in...", fontWeight = FontWeight.SemiBold)
            } else {
                Text("Login", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? ", color = Color(0xFF667085), fontSize = 14.sp)
            TextButton(onClick = onNavigateToSignup, enabled = !isLoading, contentPadding = PaddingValues(0.dp)) {
                Text("Sign up", color = Color(0xFF00A3FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(repository = repository, onDismiss = { showForgotPassword = false })
    }
}

@Composable
fun ForgotPasswordDialog(
    repository: com.example.expensetracker.data.SupabaseRepository,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter your email and we'll send you a reset link.", fontSize = 14.sp, color = Color(0xFF667085))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                if (message != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        )
                    ) {
                        Text(message!!, modifier = Modifier.padding(10.dp), fontSize = 13.sp,
                            color = if (isError) Color(0xFFB71C1C) else Color(0xFF1B5E20))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank()) return@Button
                    scope.launch {
                        isLoading = true; message = null
                        try {
                            repository.sendPasswordResetEmail(email.trim())
                            isError = false
                            message = "✅ Password reset link sent! Check your email (and spam folder)."
                        } catch (e: Exception) {
                            isError = true
                            message = "❌ ${e.message ?: "Failed to send reset email."}"
                        } finally { isLoading = false }
                    }
                },
                enabled = !isLoading, shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp)); Text("Sending...")
                } else { Text("Send Reset Link") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Close") } }
    )
}
