package com.example.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.ui.components.AddProfileDialog

@Composable
fun ProfilesScreen(
    appData: ExpenseAppData,
    onProfileSelected: (Int) -> Unit,
    onAddProfile: (String) -> Unit,
    onEditProfile: (Int, String) -> Unit,
    onDeleteProfile: (Int) -> Unit
) {
    var showAddProfileDialog by rememberSaveable { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<Int?>(null) }
    var profileToDelete by remember { mutableStateOf<Int?>(null) }
    var showCannotDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEAF2FF),
                        Color(0xFFF8FBFF),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Profiles",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF101828)
                )
                Text(
                    text = "Switch or add new profiles to track separate data",
                    fontSize = 14.sp,
                    color = Color(0xFF667085)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(appData.profiles.size) { index ->
                val profile = appData.profiles[index]
                val isActive = index == appData.activeProfileIndex

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileSelected(index) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isActive) Color(0xFF2563EB) else Color.White
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = if (isActive) 6.dp else 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else Color(0xFF101828)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${profile.transactions.size} transactions",
                                    fontSize = 14.sp,
                                    color = if (isActive) Color(0xFFEAF2FF) else Color(0xFF667085)
                                )
                            }
                            if (isActive) {
                                Text(
                                    text = "Active",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { profileToEdit = index },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isActive) Color.White else Color(0xFF2563EB)
                                )
                            ) {
                                Text("Edit")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    if (appData.profiles.size <= 1) {
                                        showCannotDeleteDialog = true
                                    } else {
                                        profileToDelete = index
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isActive) Color(0xFFFFB4AB) else Color(0xFFDC2626)
                                )
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { showAddProfileDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "+ Add New Profile",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showAddProfileDialog) {
        AddProfileDialog(
            onDismiss = { showAddProfileDialog = false },
            onAdd = { profileName ->
                onAddProfile(profileName)
                showAddProfileDialog = false
            }
        )
    }

    profileToEdit?.let { index ->
        val currentName = appData.profiles[index].name
        EditProfileDialog(
            currentName = currentName,
            onDismiss = { profileToEdit = null },
            onSave = { newName ->
                onEditProfile(index, newName)
                profileToEdit = null
            }
        )
    }

    profileToDelete?.let { index ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete this profile? All transactions in this profile will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProfile(index)
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCannotDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCannotDeleteDialog = false },
            title = { Text("Cannot Delete") },
            text = { Text("At least one profile is required.") },
            confirmButton = {
                Button(onClick = { showCannotDeleteDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var profileName by rememberSaveable { mutableStateOf(currentName) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Name") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                label = { Text("Profile name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (profileName.isNotBlank()) {
                        onSave(profileName.trim())
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
