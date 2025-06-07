package com.example.myprescription.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myprescription.ui.screens.FamilyMembersScreen
import com.example.myprescription.ui.screens.MemberDetailsScreen
import com.example.myprescription.ui.screens.ViewDocumentScreen
import com.example.myprescription.ViewModel.FamilyViewModel
import com.example.myprescription.ViewModel.MemberDetailsViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object AppDestinations {
    const val FAMILY_MEMBERS_ROUTE = "family_members"
    const val MEMBER_DETAILS_ROUTE = "member_details"
    const val VIEW_DOCUMENT_ROUTE = "view_document"

    const val MEMBER_ID_ARG = "memberId"
    const val MEMBER_NAME_ARG = "memberName"
    const val DOCUMENT_ID_ARG = "documentId"
    const val DOCUMENT_URI_ARG = "documentUri" // This will now be an internal file path
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
    NavHost(
        navController = navController,
        startDestination = AppDestinations.FAMILY_MEMBERS_ROUTE,
        modifier = modifier
    ) {
        composable(AppDestinations.FAMILY_MEMBERS_ROUTE) {
            val familyViewModel: FamilyViewModel = viewModel(factory = FamilyViewModel.Factory)
            FamilyMembersScreen(
                familyViewModel = familyViewModel,
                onNavigateToMemberDetails = { memberId, memberName ->
                    navController.navigate("${AppDestinations.MEMBER_DETAILS_ROUTE}/$memberId/$memberName")
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
            // ViewModel created and scoped to this destination
            val memberDetailsViewModel: MemberDetailsViewModel = viewModel(factory = MemberDetailsViewModel.Factory)

            if (memberId != null && memberName != null) {
                MemberDetailsScreen(
                    memberId = memberId,
                    memberName = memberName,
                    memberDetailsViewModel = memberDetailsViewModel,
                    onNavigateToViewDocument = { docId, docPath, docType, docTitle ->
                        navController.navigate(
                            "${AppDestinations.VIEW_DOCUMENT_ROUTE}/$docId/${docPath.encodeUri()}/$docType/${docTitle.encodeUri()}"
                        )
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
            // This is the key change: We get the parent back stack entry...
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(memberDetailsRoute)
            }
            // ...and scope the ViewModel to it, ensuring we get the SAME instance as the parent screen.
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