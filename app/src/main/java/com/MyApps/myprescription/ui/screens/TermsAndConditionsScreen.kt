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
fun TermsAndConditionsScreen(
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms and Conditions") },
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
            Text("Terms and Conditions for MyPrescription", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Last updated: June 22, 2025", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("Welcome to MyPrescription! We are happy to have you on board. Before you start using the app, please read these Terms and Conditions carefully. By downloading, accessing, or using the MyPrescription mobile application (the \"App\"), you agree to be bound by these terms. If you do not agree with any part of these terms, you should not use the App.\n\n")
                    append("These Terms and Conditions are specifically for our users in India.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("1. About the App\n")
                    }
                    append("MyPrescription is a simple, offline-first application designed to help you store and manage your medical prescriptions digitally on your own device. You can add details about your prescriptions and keep them organized for easy access.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("2. Your Privacy and Data\n")
                    }
                    append("This is the most important part, so please read it carefully.\n\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("We Do Not Store Your Data:")
                    }
                    append(" MyPrescription does not have any online servers. We do not collect, store, or have access to any of your personal information or your prescription data.\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Data Stays on Your Device:")
                    }
                    append(" All the information you enter into the App, including your name, doctor's details, and images of your prescriptions, is stored only on your personal mobile device.\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Authentication Data:")
                    }
                    append(" To provide a secure and easy login experience, we use Google Sign-In for authentication. This means your login information (such as your email address and name) is managed by Google. We do not store your password. Your use of Google Sign-In is subject to Google's Terms of Service and Privacy Policy.\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Your Responsibility:")
                    }
                    append(" Because your data is stored locally, the security of that data is in your hands. You are responsible for keeping your device secure (for example, by using a password or fingerprint lock). If you lose your device or it gets damaged, we cannot recover your data for you.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("3. Medical Disclaimer\n")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Not a Medical Device:")
                    }
                    append(" MyPrescription is an organizational tool. It is not a medical device and should not be considered a substitute for professional medical advice, diagnosis, or treatment.\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Always Consult Your Doctor:")
                    }
                    append(" Always seek the advice of your physician or other qualified health provider with any questions you may have regarding a medical condition. Never disregard professional medical advice or delay in seeking it because of something you have read or stored on this App.\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Accuracy of Information:")
                    }
                    append(" You are responsible for the accuracy and completeness of the information you enter into the App.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("4. Using the App\n")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("License to Use:")
                    }
                    append(" We grant you a limited, non-exclusive, non-transferable, and revocable license to use the App for your own personal, non-commercial use.\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Account Deletion:")
                    }
                    append(" You have the right to delete your account at any time from the 'Settings' screen. Deleting your account is a permanent action that will erase all data stored within the app on your device.\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("What You Cannot Do:")
                    }
                    append(" You agree not to:\n")
                    append("Use the App for any illegal purpose.\n")
                    append("Try to copy, modify, or distribute the App's content without our permission.\n")
                    append("Attempt to reverse engineer or extract the source code of the App.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("5. No Warranty\n")
                    }
                    append("The App is provided to you \"AS IS\" and \"AS AVAILABLE\" without any warranties of any kind. We do not guarantee that the App will always be secure, error-free, or that it will always function without disruptions, delays, or imperfections.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("6. Limitation of Liability\n")
                    }
                    append("To the fullest extent permitted by law in India, the developer of MyPrescription shall not be liable for any direct, indirect, incidental, special, consequential, or punitive damages, or any loss of data, opportunities, reputation, or profits, arising out of your use or inability to use the App. This includes any loss of data from your device.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("7. Changes to These Terms\n")
                    }
                    append("We may update these Terms and Conditions from time to time. We will notify you of any changes by posting the new Terms and Conditions within the App. You are advised to review this page periodically for any changes. Your continued use of the App after any changes constitutes your acceptance of the new terms.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("8. Governing Law and Jurisdiction\n")
                    }
                    append("These Terms and Conditions are governed by and construed in accordance with the laws of the Republic of India. Any disputes arising out of or in connection with these terms shall be subject to the exclusive jurisdiction of the courts in Mumbai, India.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("9. Contact Us\n")
                    }
                    append("If you have any questions or suggestions about our Terms and Conditions, do not hesitate to contact us at support@myapplications.store.")
                }
            )
        }
    }
}