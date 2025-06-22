// frozensun47/myprescription-/MyPrescription--81e5414b6e706cfd87c9dd01262402647362da55/app/src/main/java/com/MyApps/myprescription/ui/screens/TutorialScreen.kt
package com.MyApps.myprescription.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.MyApps.myprescription.R
import kotlinx.coroutines.launch

// A data class to hold the content for each tutorial page
data class TutorialPage(
    val title: String,
    val description: String,
    val imageResId: Int
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(
    onTutorialFinished: () -> Unit
) {
    val pages = listOf(
        TutorialPage(
            "Welcome to MyPrescription!",
            "Securely store and manage medical records for you and your family, all in one place, right on your device.",
            R.mipmap.my_prescription_foreground
        ),
        TutorialPage(
            "Add Family Members",
            "Tap the '+' button on the main screen to create a profile for each family member. All their records will be neatly organized under their name.",
            R.mipmap.my_prescription_foreground
        ),
        TutorialPage(
            "Manage Prescriptions & Reports",
            "Inside a member's profile, easily add prescriptions and health reports. Upload photos or files to keep everything together.",
            R.mipmap.my_prescription_foreground
        ),
        TutorialPage(
            "Backup & Restore Your Data",
            "Your data is precious. Use the 'Backup' feature in Settings to save a copy of all your records. You can restore it anytime on any device.",
            R.mipmap.my_prescription_foreground
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    TextButton(onClick = onTutorialFinished) {
                        Text("Skip")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Updated
                )
            )
        },
        bottomBar = {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        val width = animateDpAsState(targetValue = if (pagerState.currentPage == iteration) 24.dp else 8.dp, label = "indicator_width")
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(width = width.value, height = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(visible = pagerState.currentPage > 0) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (isLastPage) {
                                onTutorialFinished()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                    ) {
                        Text(if (isLastPage) "Get Started" else "Next")
                        if (!isLastPage) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(paddingValues)
        ) { pageIndex ->
            TutorialPageContent(page = pages[pageIndex])
        }
    }
}

@Composable
fun TutorialPageContent(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = page.imageResId),
            contentDescription = page.title,
            modifier = Modifier.size(200.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}