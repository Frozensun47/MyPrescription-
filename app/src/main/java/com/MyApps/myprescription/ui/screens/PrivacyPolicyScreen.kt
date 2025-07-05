package com.MyApps.myprescription.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Privacy Policy for MyPrescription", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Last updated: June 22, 2025", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("This Privacy Policy describes how your personal information is handled in the MyPrescription mobile application (the \"App\").\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Our Data Philosophy: Your Device, Your Data\n")
                    }
                    append("MyPrescription is designed as an offline-first application. While you provide personal information to the app for it to function, we want to be crystal clear:\n\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("The App does not transmit your personal data to us or any third party.")
                    }
                    append(" The information you enter is for your use only and includes:\n")
                    append("• Your name, age, or gender\n")
                    append("• Your prescriptions or medical history\n")
                    append("• Your doctor's information\n")
                    append("• Any images or files you upload\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Local Data Storage\n")
                    }
                    append("All data you enter into the App is stored exclusively in a private, secure area on your own device. It is never transmitted to us.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Sharing Features\n")
                    }
                    append("The App allows you to share your (or your family members') medical prescriptions and reports with other individuals (e.g., family members, and potentially doctors in the future) whom you explicitly choose. Please note the following regarding sharing:\n")
                    append("• ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Direct Device-to-Device Sharing:")
                    }
                    append(" This sharing occurs directly between your device and the recipient's device. We DO NOT act as an intermediary server and DO NOT store any of your shared data on our servers. Your medical information is transferred directly from your device to the recipient's device.\n")
                    append("• ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("User Identification (User ID):")
                    }
                    append(" To facilitate direct sharing, the App may use a unique ID allocated to each user. This ID is used solely for the purpose of identifying users for direct sharing connections and is NOT linked to any server-side storage of your medical data.\n")
                    append("• ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Your Responsibility:")
                    }
                    append(" You are solely responsible for determining with whom you share your medical information. We have no control over how recipients store, use, or further share the information once it has left your device. Exercise extreme caution and ensure you trust any individual before sharing sensitive medical data with them.\n\n")


                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Third-Party Services\n")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Google Sign-In:")
                    }
                    append(" We use Google Sign-In as our sole method of authentication. When you sign in, you are providing your login credentials directly to Google. We only receive basic account information, such as your email address and display name, to identify your account, and this is also stored only on your device. We do not receive or store your password. Your use of Google Sign-In is governed by Google's Privacy Policy.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Data Deletion and User Rights\n")
                    }
                    append("You have complete control over your data. You can add, edit, and delete any information at any time within the app. When you choose to delete your account from the 'Settings' screen, the following actions are performed:\n")
                    append("• ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("On-Device Data:")
                    }
                    append(" All application data, including your family members' details, prescriptions, and reports, will be permanently deleted from your device's internal storage.\n")
                    append("• ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Shared Data (Important):")
                    }
                    append(" If you have shared medical information with other users, deleting your data from your device will NOT remove the copies of that data from the devices of the recipients you shared it with. You are responsible for any data once it has been shared.\n")
                    append("• ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Account Information:")
                    }
                    append(" Your authentication account with Google will be deleted.\n")
                    append("• ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Backup Files:")
                    }
                    append(" Any backup files created using the app's backup feature are stored in your device's shared storage (e.g., the Downloads folder). The app does not have the permission to automatically delete these files. You are responsible for manually deleting any backup files you have created if you wish to remove them completely.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Handling of Children's Information\n")
                    }
                    append("This App is not directed to children under the age of 13, and the app itself does not knowingly collect or transmit any data. A parent or guardian may choose to enter information about their child into the app for management purposes. In such cases, the parent or guardian is solely responsible for this data, which remains stored only on their device. If a parent or guardian chooses to share a child's information, they are solely responsible for obtaining all necessary consents and ensuring the sharing is appropriate and lawful.\n\n") // Modified for sharing

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Contact Us\n")
                    }
                    append("If you have any questions about this Privacy Policy, you can contact us at support@myapplications.store.")
                }
            )
        }
    }
}