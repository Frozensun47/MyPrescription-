// frozensun47/myprescription-/MyPrescription--e4ea256193f6bab959107a3c7e7eea1813571356/app/src/main/java/com/MyApps/myprescription/ui/screens/SettingsScreen.kt
package com.MyApps.myprescription.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    familyViewModel: FamilyViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToChangePin: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showVerifyPinDialog by remember { mutableStateOf(false) }
    var isPinEnabled by remember { mutableStateOf(prefs.isPinEnabled(userId)) }
    var autoBackupEnabled by remember { mutableStateOf(prefs.isAutoBackupEnabled()) }
    val backupStatus by familyViewModel.backupStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item { SettingsSectionTitle("Security") }
            item {
                SettingsItem(
                    title = "Enable PIN Lock",
                    subtitle = if (isPinEnabled) "PIN is enabled" else "PIN is disabled",
                    icon = Icons.Default.Lock,
                    onClick = {},
                    trailingContent = {
                        Switch(
                            checked = isPinEnabled,
                            onCheckedChange = { newState ->
                                if (newState) {
                                    onNavigateToChangePin()
                                } else {
                                    // You might want to verify PIN before disabling
                                    prefs.setPinEnabled(userId, false)
                                    isPinEnabled = false
                                }
                            }
                        )
                    }
                )
            }
            item {
                SettingsItem(
                    title = "Change PIN",
                    icon = Icons.Default.Key,
                    enabled = isPinEnabled,
                    onClick = { if (isPinEnabled) showVerifyPinDialog = true else onNavigateToChangePin() },
                    isNavigation = true
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }

            item { SettingsSectionTitle("Data Management") }
            item {
                SettingsItem(
                    title = "Automatic Backup",
                    subtitle = "Backup your data to Google Drive daily",
                    icon = Icons.Default.CloudSync,
                    onClick = {
                        autoBackupEnabled = !autoBackupEnabled
                        familyViewModel.setAutoBackupEnabled(autoBackupEnabled)
                    },
                    trailingContent = {
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = {
                                autoBackupEnabled = it
                                familyViewModel.setAutoBackupEnabled(it)
                            }
                        )
                    }
                )
            }
            item {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
                    Text(
                        text = "Status: $backupStatus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { familyViewModel.backupNow() },
                        enabled = autoBackupEnabled
                    ) {
                        Text("Backup Now")
                    }
                }
            }


            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }

            item { SettingsSectionTitle("Information") }
            item {
                SettingsItem(
                    title = "Contact & Feedback",
                    icon = Icons.Outlined.Email,
                    onClick = {
                        val subject = "MyPrescription Feedback: "
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:feedback@myapplications.store?subject=${Uri.encode(subject)}")
                        }
                        context.startActivity(Intent.createChooser(emailIntent, "Send feedback"))
                    }
                )
            }

            item {
                SettingsItem(title = "Terms & Conditions", icon = Icons.AutoMirrored.Filled.Article, onClick = onNavigateToTerms, isNavigation = true)
            }
            item {
                SettingsItem(title = "Privacy Policy", icon = Icons.Default.Policy, onClick = onNavigateToPrivacyPolicy, isNavigation = true)
            }
            item {
                SettingsItem(title = "Help", icon = Icons.AutoMirrored.Filled.Help, onClick = onNavigateToHelp, isNavigation = true)
            }
            item {
                SettingsItem(title = "About", icon = Icons.Default.Info, onClick = onNavigateToAbout, isNavigation = true)
            }


            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }

            item { SettingsSectionTitle("Account") }
            item {
                SettingsItem(title = "Logout", icon = Icons.AutoMirrored.Filled.Logout, onClick = onLogout)
            }
            item {
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
        val correctPin = prefs.getPin(userId) ?: ""
        VerifyPinDialog(
            onDismiss = { showVerifyPinDialog = false },
            onConfirm = {
                showVerifyPinDialog = false
                onNavigateToChangePin()
            },
            correctPin = correctPin
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    isNavigation: Boolean = false,
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
        } else if (isNavigation) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f)
            )
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