package com.example.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordDialog(
    repository: com.example.expensetracker.data.SupabaseRepository,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Change Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter a new password for your account.", fontSize = 14.sp, color = Color(0xFF667085))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(
                            onClick = { newVisible = !newVisible },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(if (newVisible) "Hide" else "Show", fontSize = 12.sp, color = Color(0xFF667085))
                        }
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(
                            onClick = { confirmVisible = !confirmVisible },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(if (confirmVisible) "Hide" else "Show", fontSize = 12.sp, color = Color(0xFF667085))
                        }
                    }
                )

                Text("Password must be at least 6 characters.", fontSize = 11.sp, color = Color(0xFF9E9E9E))

                if (message != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        )
                    ) {
                        Text(
                            text = message!!,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 13.sp,
                            color = if (isError) Color(0xFFB71C1C) else Color(0xFF1B5E20)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newPassword.length < 6 -> { isError = true; message = "❌ Password must be at least 6 characters." }
                        newPassword != confirmPassword -> { isError = true; message = "❌ Passwords do not match." }
                        else -> {
                            scope.launch {
                                isLoading = true; message = null
                                try {
                                    repository.updatePassword(newPassword)
                                    isError = false
                                    message = "✅ Password changed successfully!"
                                } catch (e: Exception) {
                                    isError = true
                                    message = "❌ ${e.message ?: "Failed to change password."}"
                                } finally { isLoading = false }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Updating...")
                } else {
                    Text("Update Password")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
        }
    )
}
