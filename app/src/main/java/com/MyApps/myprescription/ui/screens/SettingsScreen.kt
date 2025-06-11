package com.MyApps.myprescription.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.MyApps.myprescription.ViewModel.FamilyViewModel
import com.MyApps.myprescription.ui.components.PinInput
import com.MyApps.myprescription.util.Prefs
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    familyViewModel: FamilyViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToChangePin: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showVerifyPinDialog by remember { mutableStateOf(false) }
    var isPinEnabled by remember { mutableStateOf(prefs.isPinEnabled(userId)) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let { familyViewModel.exportBackup(it) }
                ?: Toast.makeText(context, "Backup cancelled.", Toast.LENGTH_SHORT).show()
        }
    )

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { familyViewModel.importBackup(it) }
                ?: Toast.makeText(context, "Restore cancelled.", Toast.LENGTH_SHORT).show()
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SettingsSectionTitle("Security")
                SettingsItem(
                    title = "Enable PIN Lock",
                    subtitle = if (isPinEnabled) "PIN is enabled" else "PIN is disabled",
                    icon = Icons.Default.Lock,
                    onClick = {},
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
                    title = "Change PIN",
                    icon = Icons.Default.Key,
                    enabled = isPinEnabled,
                    onClick = { if (isPinEnabled) showVerifyPinDialog = true else onNavigateToChangePin() }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                SettingsSectionTitle("Data Management")
                SettingsItem(
                    title = "Backup Data",
                    subtitle = "Save all data to a local .zip file",
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        backupLauncher.launch("MyPrescription_Backup_$timestamp.zip")
                    }
                )
                SettingsItem(
                    title = "Restore Data",
                    subtitle = "Restore data from a backup file",
                    icon = Icons.Default.CloudDownload,
                    onClick = { restoreLauncher.launch("*/*") }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                SettingsSectionTitle("Account")
                SettingsItem(title = "Logout", icon = Icons.AutoMirrored.Filled.Logout, onClick = onLogout)
                SettingsItem(
                    title = "Delete Account",
                    subtitle = "This action is permanent",
                    icon = Icons.Default.DeleteForever,
                    isDestructive = true,
                    onClick = { showDeleteDialog = true }
                )
            }
        }
    }

    if (showDeleteDialog) { /* ... unchanged ... */ }
    if (showVerifyPinDialog) { /* ... unchanged ... */ }
}


// --- The other composables in this file (SettingsItem, etc.) remain unchanged ---
@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val titleColor = if (!enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    else if (isDestructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (!enabled) titleColor
    else if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (!enabled) titleColor
    else if (isDestructive) titleColor
    else MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                trailingContent()
            }
        }
    }
}

@Composable
fun DeleteAccountConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var confirmationText by remember { mutableStateOf("") }
    val isButtonEnabled = confirmationText.equals("Delete", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, "Delete Account", tint = MaterialTheme.colorScheme.error) },
        title = { Text("Are you absolutely sure?") },
        text = {
            Column {
                Text("This action cannot be undone. This will permanently delete your account and all associated data.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    label = { Text("Type \"Delete\" to confirm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Confirm Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun VerifyPinDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, correctPin: String) {
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify Your Identity") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Please enter your current PIN to continue.", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                PinInput(
                    pin = enteredPin,
                    onPinChange = {
                        if (it.length <= 4) enteredPin = it
                        error = null
                    },
                    isError = error != null
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredPin == correctPin) {
                        onConfirm()
                    } else {
                        error = "Incorrect PIN"
                        enteredPin = ""
                    }
                },
                enabled = enteredPin.length == 4
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}