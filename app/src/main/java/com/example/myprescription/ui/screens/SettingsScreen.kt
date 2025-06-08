package com.example.myprescription.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myprescription.util.Prefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    onNavigateUp: () -> Unit,
    onNavigateToChangePin: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    // State for dialogs
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showVerifyPinDialog by remember { mutableStateOf(false) }

    var isPinEnabled by remember { mutableStateOf(prefs.isPinEnabled(userId)) }

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
            SettingsSectionTitle("App")
            SettingsItem(
                title = "Enable Password",
                subtitle = if (isPinEnabled) "PIN is enabled" else "PIN is disabled",
                icon = Icons.Default.Lock,
                onClick = {
                    val newState = !isPinEnabled
                    // Disabling the PIN removes it.
                    if (!newState) {
                        prefs.setPinEnabled(userId, false)
                        isPinEnabled = false
                    } else {
                        // To enable, user must set a new PIN.
                        onNavigateToChangePin()
                    }
                },
                trailingContent = {
                    Switch(
                        checked = isPinEnabled,
                        onCheckedChange = { newState ->
                            if (!newState) {
                                prefs.setPinEnabled(userId, false)
                                isPinEnabled = false
                            } else {
                                onNavigateToChangePin()
                            }
                        }
                    )
                }
            )
            SettingsItem(
                title = "Change Password",
                icon = Icons.Default.Key,
                onClick = {
                    // If a PIN exists, verify it first. Otherwise, go straight to setting one.
                    if (prefs.isPinEnabled(userId)) {
                        showVerifyPinDialog = true
                    } else {
                        onNavigateToChangePin()
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("Data")
            SettingsItem(title = "Connect to Google Drive", icon = Icons.Default.Cloud, onClick = { /* TODO */ })
            SettingsItem(title = "Storage", subtitle = "Using MyPrescription Storage", icon = Icons.Default.Info, onClick = {})

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("General")
            SettingsItem(title = "About MyPrescription", icon = Icons.Default.Info, onClick = { /* TODO: Navigate to About screen */ })
            SettingsItem(title = "Privacy Policy", icon = Icons.Default.PrivacyTip, onClick = { /* TODO */ })
            SettingsItem(title = "Help and Support", icon = Icons.AutoMirrored.Filled.Help, onClick = { /* TODO: Navigate to Help screen */ })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("Account")
            SettingsItem(title = "Logout", icon = Icons.AutoMirrored.Filled.Logout, onClick = onLogout)
            SettingsItem(
                title = "Delete Account",
                subtitle = "This action is permanent",
                icon = Icons.Default.DeleteForever,
                onClick = { showDeleteDialog = true }
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

    if (showVerifyPinDialog) {
        VerifyPinDialog(
            onDismiss = { showVerifyPinDialog = false },
            onConfirm = {
                showVerifyPinDialog = false
                onNavigateToChangePin()
            },
            correctPin = prefs.getPin(userId) ?: ""
        )
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
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
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

@Composable
fun VerifyPinDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    correctPin: String
) {
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify Your Identity") },
        text = {
            Column {
                Text("Please enter your current PIN to continue.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 4) enteredPin = it.filter { c -> c.isDigit() }
                        error = null
                    },
                    label = { Text("Current PIN") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { if(error != null) Text(error!!) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredPin == correctPin) {
                        onConfirm()
                    } else {
                        error = "Incorrect PIN"
                    }
                },
                enabled = enteredPin.length == 4
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}