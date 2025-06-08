package com.example.myprescription.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myprescription.util.Prefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToChangePin: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var isPinEnabled by remember { mutableStateOf(prefs.isPinEnabled) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // New "App" Section
            SettingsSectionTitle("App")
            SettingsItem(
                title = "Enable Password",
                subtitle = "Use a PIN to unlock the app",
                icon = Icons.Default.Lock,
                onClick = {
                    isPinEnabled = !isPinEnabled
                    prefs.isPinEnabled = isPinEnabled
                },
                trailingContent = {
                    Switch(
                        checked = isPinEnabled,
                        onCheckedChange = {
                            isPinEnabled = it
                            prefs.isPinEnabled = it
                        }
                    )
                }
            )
            SettingsItem(
                title = "Change Password",
                icon = Icons.Default.Key,
                onClick = onNavigateToChangePin
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("Data")
            SettingsItem(title = "Connect to Google Drive", icon = Icons.Default.Cloud, onClick = { /* TODO */ })
            SettingsItem(title = "Storage", subtitle = "Using MyPrescription Storage", icon = Icons.Default.Info, onClick = {})

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("General")
            SettingsItem(title = "About MyPrescription", icon = Icons.Default.Info, onClick = { /* TODO */ })
            SettingsItem(title = "Privacy Policy", icon = Icons.Default.PrivacyTip, onClick = { /* TODO */ })
            SettingsItem(title = "Help and Support", icon = Icons.AutoMirrored.Filled.Help, onClick = { /* TODO */ })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("Account")
            SettingsItem(title = "Logout", icon = Icons.AutoMirrored.Filled.Logout, onClick = onLogout)
            SettingsItem(
                title = "Delete Account",
                subtitle = "This action is permanent",
                icon = Icons.Default.DeleteForever,
                onClick = { showDeleteDialog = true } // Open the confirmation dialog
            )
        }
    }

    if (showDeleteDialog) {
        DeleteAccountConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDeleteAccount()
            }
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailingContent()
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun DeleteAccountConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var confirmationText by remember { mutableStateOf("") }
    val isButtonEnabled = confirmationText.equals("Delete", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.DeleteForever, "Delete Account") },
        title = { Text("Are you absolutely sure?") },
        text = {
            Column {
                Text(
                    "This action cannot be undone. This will permanently delete your account, " +
                            "all family members, and all associated medical records from this device."
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Please type \"Delete\" to confirm.", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    label = { Text("Type \"Delete\"") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Confirm Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}