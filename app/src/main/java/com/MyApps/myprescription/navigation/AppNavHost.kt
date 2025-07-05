package com.MyApps.myprescription.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.MyApps.myprescription.MyPrescriptionApplication
import com.MyApps.myprescription.ViewModel.AuthViewModel
import com.MyApps.myprescription.ViewModel.FamilyViewModel
import com.MyApps.myprescription.ViewModel.MemberDetailsViewModel
import com.MyApps.myprescription.ui.screens.*
import com.MyApps.myprescription.util.Prefs
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object AppDestinations {
    const val LOGIN_ROUTE = "login"
    const val PIN_SETUP_ROUTE = "pin_setup"
    const val PIN_ENTRY_ROUTE = "pin_entry"
    const val TUTORIAL_ROUTE = "tutorial"
    const val FAMILY_MEMBERS_ROUTE = "family_members"
    const val MEMBER_DETAILS_FLOW_ROUTE = "member_details_flow"
    const val MEMBER_DETAILS_ROUTE = "member_details"
    const val VIEW_DOCUMENT_ROUTE = "view_document"
    const val DOCTOR_DETAILS_ROUTE = "doctor_details"
    const val SETTINGS_ROUTE = "settings"
    const val ABOUT_ROUTE = "about"
    const val HELP_ROUTE = "help"
    const val TERMS_AND_CONDITIONS_ROUTE = "terms_and_conditions"
    const val PRIVACY_POLICY_ROUTE = "privacy_policy"

    const val MEMBER_ID_ARG = "memberId"
    const val MEMBER_NAME_ARG = "memberName"
    const val DOCUMENT_ID_ARG = "documentId"
    const val DOCUMENT_TYPE_ARG = "documentType"
    const val DOCUMENT_TITLE_ARG = "documentTitle"
    const val DOCTOR_ID_ARG = "doctorId"
    const val DOCTOR_NAME_ARG = "doctorName"
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
                        navController.navigate(nextRoute) { popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true } }
                    }
                },
                onNavigateToTerms = {
                    navController.navigate(AppDestinations.TERMS_AND_CONDITIONS_ROUTE)
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
                        if (prefs.hasSeenTutorial(userId)) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(AppDestinations.TUTORIAL_ROUTE) {
                                popUpTo(AppDestinations.PIN_SETUP_ROUTE) { inclusive = true }
                            }
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
                            val nextRoute = if (!prefs.hasSeenTutorial(userId)) {
                                AppDestinations.TUTORIAL_ROUTE
                            } else {
                                AppDestinations.FAMILY_MEMBERS_ROUTE
                            }
                            navController.navigate(nextRoute) {
                                popUpTo(AppDestinations.PIN_ENTRY_ROUTE) { inclusive = true }
                            }
                        } else {
                            error = "Incorrect PIN"
                        }
                    }
                }
            )
        }

        composable(AppDestinations.TUTORIAL_ROUTE) {
            TutorialScreen(
                onTutorialFinished = {
                    Firebase.auth.currentUser?.uid?.let { userId ->
                        prefs.setTutorialSeen(userId)
                    }
                    navController.navigate(AppDestinations.FAMILY_MEMBERS_ROUTE) {
                        popUpTo(navController.graph.id) { inclusive = true }
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
                    navController.navigate("${AppDestinations.MEMBER_DETAILS_FLOW_ROUTE}/$memberId/$memberName")
                },
                onNavigateToSettings = { navController.navigate(AppDestinations.SETTINGS_ROUTE) },
                onChangeAccountClick = {
                    authViewModel.logout()
                    application.onUserLogout()
                    navController.navigate(AppDestinations.LOGIN_ROUTE) { popUpTo(navController.graph.startDestinationId) { inclusive = true } }
                }
            )
        }

        composable(AppDestinations.SETTINGS_ROUTE) {
            val authViewModel: AuthViewModel = viewModel()
            val familyViewModel: FamilyViewModel = viewModel(factory = FamilyViewModel.Factory)
            val firebaseUser by authViewModel.user.collectAsState()
            val currentUserId = firebaseUser?.uid

            if (currentUserId != null) {
                SettingsScreen(
                    userId = currentUserId,
                    familyViewModel = familyViewModel,
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToChangePin = { navController.navigate(AppDestinations.PIN_SETUP_ROUTE) },
                    onNavigateToAbout = { navController.navigate(AppDestinations.ABOUT_ROUTE) },
                    onNavigateToHelp = { navController.navigate(AppDestinations.HELP_ROUTE) },
                    onNavigateToTerms = { navController.navigate(AppDestinations.TERMS_AND_CONDITIONS_ROUTE) },
                    onNavigateToPrivacyPolicy = { navController.navigate(AppDestinations.PRIVACY_POLICY_ROUTE) },
                    onLogout = {
                        authViewModel.logout()
                        application.onUserLogout()
                        navController.navigate(AppDestinations.LOGIN_ROUTE) { popUpTo(navController.graph.startDestinationId) { inclusive = true } }
                    },
                    onDeleteAccount = {
                        coroutineScope.launch {
                            val userToDelete = Firebase.auth.currentUser
                            if (userToDelete != null && userToDelete.uid == currentUserId) {
                                withContext(Dispatchers.IO) {
                                    application.repository?.clearAllDatabaseTables()
                                }
                                prefs.clearAllData()
                                userToDelete.delete().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        authViewModel.logout()
                                        application.onUserLogout()
                                        coroutineScope.launch(Dispatchers.Main) {
                                            navController.navigate(AppDestinations.LOGIN_ROUTE) {
                                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(AppDestinations.LOGIN_ROUTE) { popUpTo(navController.graph.startDestinationId) { inclusive = true } }
                }
            }
        }

        composable(AppDestinations.ABOUT_ROUTE) {
            AboutScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToTerms = { navController.navigate(AppDestinations.TERMS_AND_CONDITIONS_ROUTE) },
                onNavigateToPrivacyPolicy = { navController.navigate(AppDestinations.PRIVACY_POLICY_ROUTE) }
            )
        }
        composable(AppDestinations.HELP_ROUTE) { HelpScreen(onNavigateUp = { navController.navigateUp() }) }
        composable(AppDestinations.TERMS_AND_CONDITIONS_ROUTE) {
            TermsAndConditionsScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable(AppDestinations.PRIVACY_POLICY_ROUTE) {
            PrivacyPolicyScreen(onNavigateUp = { navController.navigateUp() })
        }

        navigation(
            route = "${AppDestinations.MEMBER_DETAILS_FLOW_ROUTE}/{${AppDestinations.MEMBER_ID_ARG}}/{${AppDestinations.MEMBER_NAME_ARG}}",
            startDestination = AppDestinations.MEMBER_DETAILS_ROUTE
        ) {
            composable(route = AppDestinations.MEMBER_DETAILS_ROUTE) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(backStackEntry.destination.parent!!.route!!)
                }
                val memberDetailsViewModel: MemberDetailsViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = MemberDetailsViewModel.Factory)
                val memberId = parentEntry.arguments?.getString(AppDestinations.MEMBER_ID_ARG)
                val memberName = parentEntry.arguments?.getString(AppDestinations.MEMBER_NAME_ARG)

                if (memberId != null && memberName != null) {
                    MemberDetailsScreen(
                        memberId = memberId,
                        memberName = memberName,
                        memberDetailsViewModel = memberDetailsViewModel,
                        onNavigateToViewDocument = { docId, docType, docTitle ->
                            navController.navigate("${AppDestinations.VIEW_DOCUMENT_ROUTE}/$docId/$docType/${docTitle.encodeUri()}")
                        },
                        onNavigateToDoctorDetails = { doctorId, dName ->
                            navController.navigate("${AppDestinations.DOCTOR_DETAILS_ROUTE}/$doctorId/${dName.encodeUri()}")
                        },
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = "${AppDestinations.VIEW_DOCUMENT_ROUTE}/{${AppDestinations.DOCUMENT_ID_ARG}}/{${AppDestinations.DOCUMENT_TYPE_ARG}}/{${AppDestinations.DOCUMENT_TITLE_ARG}}",
                arguments = listOf(
                    navArgument(AppDestinations.DOCUMENT_ID_ARG) { type = NavType.StringType },
                    navArgument(AppDestinations.DOCUMENT_TYPE_ARG) { type = NavType.StringType },
                    navArgument(AppDestinations.DOCUMENT_TITLE_ARG) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(backStackEntry.destination.parent!!.route!!)
                }
                val memberDetailsViewModel: MemberDetailsViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = MemberDetailsViewModel.Factory)
                val documentId = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_ID_ARG)
                val documentType = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_TYPE_ARG)
                val documentTitle = backStackEntry.arguments?.getString(AppDestinations.DOCUMENT_TITLE_ARG)?.decodeUri()

                if (documentId != null && documentType != null && documentTitle != null) {
                    ViewDocumentScreen(
                        documentId = documentId,
                        documentType = documentType,
                        documentTitle = documentTitle,
                        memberDetailsViewModel = memberDetailsViewModel,
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = "${AppDestinations.DOCTOR_DETAILS_ROUTE}/{${AppDestinations.DOCTOR_ID_ARG}}/{${AppDestinations.DOCTOR_NAME_ARG}}",
                arguments = listOf(
                    navArgument(AppDestinations.DOCTOR_ID_ARG) { type = NavType.StringType },
                    navArgument(AppDestinations.DOCTOR_NAME_ARG) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(backStackEntry.destination.parent!!.route!!)
                }
                val memberDetailsViewModel: MemberDetailsViewModel = viewModel(viewModelStoreOwner = parentEntry, factory = MemberDetailsViewModel.Factory)
                val doctorId = backStackEntry.arguments?.getString(AppDestinations.DOCTOR_ID_ARG)
                val doctorName = backStackEntry.arguments?.getString(AppDestinations.DOCTOR_NAME_ARG)?.decodeUri()

                if (doctorId != null && doctorName != null) {
                    DoctorDetailsScreen(
                        doctorId = doctorId,
                        doctorName = doctorName,
                        memberDetailsViewModel = memberDetailsViewModel,
                        onNavigateToViewDocument = { docId, docType, docTitle ->
                            navController.navigate("${AppDestinations.VIEW_DOCUMENT_ROUTE}/$docId/$docType/${docTitle.encodeUri()}")
                        },
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}