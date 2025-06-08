package com.example.myprescription.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myprescription.MyPrescriptionApplication
import com.example.myprescription.ui.screens.*
import com.example.myprescription.util.Prefs
import com.example.myprescription.ViewModel.AuthViewModel
import com.example.myprescription.ViewModel.FamilyViewModel
import com.example.myprescription.ViewModel.MemberDetailsViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object AppDestinations {
    const val LOGIN_ROUTE = "login"
    const val PIN_SETUP_ROUTE = "pin_setup"
    const val PIN_ENTRY_ROUTE = "pin_entry"
    const val FAMILY_MEMBERS_ROUTE = "family_members"
    const val SETTINGS_ROUTE = "settings"
    const val MEMBER_DETAILS_ROUTE = "member_details"
    const val VIEW_DOCUMENT_ROUTE = "view_document"
    const val ABOUT_ROUTE = "about"
    const val HELP_ROUTE = "help"

    const val MEMBER_ID_ARG = "memberId"
    const val MEMBER_NAME_ARG = "memberName"
    const val DOCUMENT_ID_ARG = "documentId"
    const val DOCUMENT_URI_ARG = "documentUri"
    const val DOCUMENT_TYPE_ARG = "documentType"
    const val DOCUMENT_TITLE_ARG = "documentTitle"
}

fun String.encodeUri(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
fun String.decodeUri(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as MyPrescriptionApplication
    val prefs = remember { Prefs(context) }
    val coroutineScope = rememberCoroutineScope()

    // Determine the start destination once
    val startDestination = remember {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            application.initializeDependenciesForUser(currentUser.uid)
            if (prefs.getPin(currentUser.uid) == null) {
                AppDestinations.PIN_SETUP_ROUTE
            } else {
                AppDestinations.PIN_ENTRY_ROUTE
            }
        } else {
            AppDestinations.LOGIN_ROUTE
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppDestinations.LOGIN_ROUTE) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    val loggedInUser = Firebase.auth.currentUser
                    if (loggedInUser != null) {
                        application.initializeDependenciesForUser(loggedInUser.uid)
                        val nextRoute = if (prefs.getPin(loggedInUser.uid) == null) {
                            AppDestinations.PIN_SETUP_ROUTE
                        } else {
                            AppDestinations.PIN_ENTRY_ROUTE
                        }
                        navController.navigate(nextRoute) {
                            popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(AppDestinations.PIN_SETUP_ROUTE) {
            PinScreen(
                mode = PinScreenMode.SET,
                error = null,
                onPinSet = { pin ->
                    Firebase.auth.currentUser?.uid?.let { userId ->
                        prefs.setPin(userId, pin)
                        navController.navigate(AppDestinations.FAMILY_MEMBERS_ROUTE) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
                onPinEntered = {}
            )
        }

        composable(AppDestinations.PIN_ENTRY_ROUTE) {
            var error by remember { mutableStateOf<String?>(null) }
            PinScreen(
                mode = PinScreenMode.ENTER,
                error = error,
                onPinSet = {},
                onPinEntered = { pin ->
                    Firebase.auth.currentUser?.uid?.let { userId ->
                        if (pin == prefs.getPin(userId)) {
                            navController.navigate(AppDestinations.FAMILY_MEMBERS_ROUTE) {
                                popUpTo(AppDestinations.PIN_ENTRY_ROUTE) { inclusive = true }
                            }
                        } else {
                            error = "Incorrect PIN"
                        }
                    }
                }
            )
        }

        composable(AppDestinations.FAMILY_MEMBERS_ROUTE) {
            val familyViewModel: FamilyViewModel = viewModel(factory = FamilyViewModel.Factory)
            val authViewModel: AuthViewModel = viewModel()
            FamilyMembersScreen(
                familyViewModel = familyViewModel,
                authViewModel = authViewModel,
                onNavigateToMemberDetails = { memberId, memberName ->
                    navController.navigate("${AppDestinations.MEMBER_DETAILS_ROUTE}/$memberId/$memberName")
                },
                onNavigateToSettings = { navController.navigate(AppDestinations.SETTINGS_ROUTE) },
                onNavigateToHelp = { navController.navigate(AppDestinations.HELP_ROUTE) },
                onNavigateToAbout = { navController.navigate(AppDestinations.ABOUT_ROUTE) },
                onChangeAccountClick = {
                    authViewModel.logout()
                    application.onUserLogout()
                    navController.navigate(AppDestinations.LOGIN_ROUTE) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestinations.SETTINGS_ROUTE) {
            val authViewModel: AuthViewModel = viewModel()
            SettingsScreen(
                userId = Firebase.auth.currentUser?.uid ?: "",
                onNavigateUp = { navController.navigateUp() },
                onNavigateToChangePin = { navController.navigate(AppDestinations.PIN_SETUP_ROUTE) },
                onLogout = {
                    authViewModel.logout()
                    application.onUserLogout()
                    navController.navigate(AppDestinations.LOGIN_ROUTE) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                },
                onDeleteAccount = {
                    coroutineScope.launch {
                        val repository = application.repository
                        if (repository != null) {
                            val members = repository.getAllMembersOnce()
                            for (member in members) {
                                member.profileImageUri?.let { File(it).delete() }
                                val prescriptions = repository.getPrescriptionsForMember(member.id).first()
                                prescriptions.forEach { p -> p.imageUri?.split(',')?.filter{it.isNotBlank()}?.forEach { path -> File(path).delete() } }
                                val reports = repository.getReportsForMember(member.id).first()
                                reports.forEach { r -> r.fileUri?.split(',')?.filter{it.isNotBlank()}?.forEach { path -> File(path).delete() } }
                            }
                            repository.clearAllDatabaseTables()
                        }
                        Firebase.auth.currentUser?.delete()?.addOnCompleteListener {
                            authViewModel.logout()
                            application.onUserLogout()
                            prefs.clearAllData()
                            navController.navigate(AppDestinations.LOGIN_ROUTE) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(AppDestinations.ABOUT_ROUTE) { AboutScreen(onNavigateUp = { navController.navigateUp() }) }
        composable(AppDestinations.HELP_ROUTE) { HelpScreen(onNavigateUp = { navController.navigateUp() }) }

        composable(
            route = "${AppDestinations.MEMBER_DETAILS_ROUTE}/{${AppDestinations.MEMBER_ID_ARG}}/{${AppDestinations.MEMBER_NAME_ARG}}",
            arguments = listOf(
                navArgument(AppDestinations.MEMBER_ID_ARG) { type = NavType.StringType },
                navArgument(AppDestinations.MEMBER_NAME_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString(AppDestinations.MEMBER_ID_ARG)
            val memberName = backStackEntry.arguments?.getString(AppDestinations.MEMBER_NAME_ARG)
            val memberDetailsViewModel: MemberDetailsViewModel = viewModel(factory = MemberDetailsViewModel.Factory)

            if (memberId != null && memberName != null) {
                MemberDetailsScreen(
                    memberId = memberId,
                    memberName = memberName,
                    memberDetailsViewModel = memberDetailsViewModel,
                    onNavigateToViewDocument = { docId, docPath, docType, docTitle ->
                        navController.navigate("${AppDestinations.VIEW_DOCUMENT_ROUTE}/$memberId/$docId/${docPath.encodeUri()}/$docType/${docTitle.encodeUri()}")
                    },
                    onNavigateUp = { navController.navigateUp() }
                )
            } else {
                navController.navigateUp()
            }
        }

        composable(
            route = "${AppDestinations.VIEW_DOCUMENT_ROUTE}/{${AppDestinations.MEMBER_ID_ARG}}/{${AppDestinations.DOCUMENT_ID_ARG}}/{${AppDestinations.DOCUMENT_URI_ARG}}/{${AppDestinations.DOCUMENT_TYPE_ARG}}/{${AppDestinations.DOCUMENT_TITLE_ARG}}",
            arguments = listOf(
                navArgument(AppDestinations.MEMBER_ID_ARG) { type = NavType.StringType },
                navArgument(AppDestinations.DOCUMENT_ID_ARG) { type = NavType.StringType },
                navArgument(AppDestinations.DOCUMENT_URI_ARG) { type = NavType.StringType },
                navArgument(AppDestinations.DOCUMENT_TYPE_ARG) { type = NavType.StringType },
                navArgument(AppDestinations.DOCUMENT_TITLE_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val memberDetailsViewModel: MemberDetailsViewModel = viewModel(factory = MemberDetailsViewModel.Factory)
            val memberId = backStackEntry.arguments?.getString(AppDestinations.MEMBER_ID_ARG)
            val documentId = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_ID_ARG)
            val documentPath = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_URI_ARG)?.decodeUri()
            val documentType = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_TYPE_ARG)
            val documentTitle = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_TITLE_ARG)?.decodeUri()

            if (memberId != null && documentId != null && documentPath != null && documentType != null && documentTitle != null) {
                ViewDocumentScreen(
                    memberId = memberId,
                    documentId = documentId,
                    documentUriString = documentPath,
                    documentType = documentType,
                    documentTitle = documentTitle,
                    memberDetailsViewModel = memberDetailsViewModel,
                    onNavigateUp = { navController.navigateUp() }
                )
            } else {
                navController.navigateUp()
            }
        }
    }
}