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
import com.example.myprescription.ui.screens.*
import com.example.myprescription.util.Prefs
import com.example.myprescription.ViewModel.AuthViewModel
import com.example.myprescription.ViewModel.FamilyViewModel
import com.example.myprescription.ViewModel.MemberDetailsViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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
    val prefs = remember { Prefs(context) }
    val authViewModel: AuthViewModel = viewModel()
    val user by authViewModel.user.collectAsState()

    val startDestination = remember(user) {
        when {
            user == null -> AppDestinations.LOGIN_ROUTE
            prefs.pin == null -> AppDestinations.PIN_SETUP_ROUTE
            else -> AppDestinations.PIN_ENTRY_ROUTE
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppDestinations.LOGIN_ROUTE) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(AppDestinations.PIN_SETUP_ROUTE) {
                    popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
                }
            })
        }

        composable(AppDestinations.PIN_SETUP_ROUTE) {
            PinScreen(
                mode = PinScreenMode.SET,
                error = null,
                onPinSet = { pin ->
                    prefs.pin = pin
                    navController.navigate(AppDestinations.FAMILY_MEMBERS_ROUTE) {
                        popUpTo(AppDestinations.PIN_SETUP_ROUTE) { inclusive = true }
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
                    if (pin == prefs.pin) {
                        navController.navigate(AppDestinations.FAMILY_MEMBERS_ROUTE) {
                            popUpTo(AppDestinations.PIN_ENTRY_ROUTE) { inclusive = true }
                        }
                    } else {
                        error = "Incorrect PIN"
                    }
                }
            )
        }

        composable(AppDestinations.FAMILY_MEMBERS_ROUTE) {
            val familyViewModel: FamilyViewModel = viewModel(factory = FamilyViewModel.Factory)
            FamilyMembersScreen(
                familyViewModel = familyViewModel,
                onNavigateToMemberDetails = { memberId, memberName ->
                    navController.navigate("${AppDestinations.MEMBER_DETAILS_ROUTE}/$memberId/$memberName")
                },
                onNavigateToSettings = { navController.navigate(AppDestinations.SETTINGS_ROUTE) }
            )
        }

        composable(AppDestinations.SETTINGS_ROUTE) {
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onLogout = {
                    authViewModel.logout()
                    prefs.clear()
                    navController.navigate(AppDestinations.LOGIN_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeleteAccount = {
                    // TODO: Add logic to delete all data from database and files
                    Firebase.auth.currentUser?.delete()
                    authViewModel.logout()
                    prefs.clear()
                    navController.navigate(AppDestinations.LOGIN_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        val memberDetailsRoute = "${AppDestinations.MEMBER_DETAILS_ROUTE}/{${AppDestinations.MEMBER_ID_ARG}}/{${AppDestinations.MEMBER_NAME_ARG}}"
        composable(
            route = memberDetailsRoute,
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
                        navController.navigate("${AppDestinations.VIEW_DOCUMENT_ROUTE}/$docId/${docPath.encodeUri()}/$docType/${docTitle.encodeUri()}")
                    },
                    onNavigateUp = { navController.navigateUp() }
                )
            } else {
                navController.navigateUp()
            }
        }
        composable(
            route = "${AppDestinations.VIEW_DOCUMENT_ROUTE}/{${AppDestinations.DOCUMENT_ID_ARG}}/{${AppDestinations.DOCUMENT_URI_ARG}}/{${AppDestinations.DOCUMENT_TYPE_ARG}}/{${AppDestinations.DOCUMENT_TITLE_ARG}}",
            arguments = listOf(
                navArgument(AppDestinations.DOCUMENT_ID_ARG) { type = NavType.StringType },
                navArgument(AppDestinations.DOCUMENT_URI_ARG) { type = NavType.StringType },
                navArgument(AppDestinations.DOCUMENT_TYPE_ARG) { type = NavType.StringType },
                navArgument(AppDestinations.DOCUMENT_TITLE_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(memberDetailsRoute)
            }
            val memberDetailsViewModel: MemberDetailsViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = MemberDetailsViewModel.Factory)
            val documentId = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_ID_ARG)
            val documentPath = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_URI_ARG)?.decodeUri()
            val documentType = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_TYPE_ARG)
            val documentTitle = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_TITLE_ARG)?.decodeUri()

            if (documentId != null && documentPath != null && documentType != null && documentTitle != null) {
                ViewDocumentScreen(
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