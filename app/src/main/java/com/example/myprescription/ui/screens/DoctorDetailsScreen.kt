package com.example.myprescription.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myprescription.ViewModel.MemberDetailsViewModel
import com.example.myprescription.model.Doctor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DoctorDetailsScreen(
    doctorId: String,
    doctorName: String,
    memberDetailsViewModel: MemberDetailsViewModel,
    onNavigateToViewDocument: (documentId: String, documentType: String, documentTitle: String) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    // --- STATE COLLECTION ---
    val prescriptions by memberDetailsViewModel.prescriptionsForSelectedDoctor.collectAsState()
    val reports by memberDetailsViewModel.reportsForSelectedDoctor.collectAsState()
    val showAddPrescriptionDialog by memberDetailsViewModel.showAddPrescriptionDialog.collectAsState()
    val showAddReportDialog by memberDetailsViewModel.showAddReportDialog.collectAsState()
    val editingPrescription by memberDetailsViewModel.editingPrescription.collectAsState()
    val editingReport by memberDetailsViewModel.editingReport.collectAsState()
    val currentMemberId by memberDetailsViewModel.currentMemberId.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    // Find the current doctor object to pass to the ViewModel
    val doctors by memberDetailsViewModel.doctors.collectAsState()
    val currentDoctor = remember(doctors, doctorId) { doctors.find { it.id == doctorId } }


    // Load data for the specific doctor when the screen is shown
    LaunchedEffect(doctorId) {
        memberDetailsViewModel.selectDoctor(doctorId)
    }

    // Launcher for creating a new prescription silently
    val quickPrescriptionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            memberDetailsViewModel.createPrescriptionWithAttachments(uris)
        }
    }

    // Launchers for uploading files from within this screen
    val prescriptionImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) memberDetailsViewModel.updatePrescriptionWithImages(uris)
    }
    val reportFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) memberDetailsViewModel.updateReportWithFiles(uris)
    }

    val tabs = listOf("Prescriptions", "Reports")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dr. $doctorName") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) {
                        // MODIFIED: Instead of showing a dialog, launch the image picker directly
                        if (currentDoctor != null) {
                            memberDetailsViewModel.targetDoctorForAttachment.value = currentDoctor
                            quickPrescriptionLauncher.launch("image/*")
                        } else {
                            Toast.makeText(context, "Doctor details not found.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Kept original behavior for reports, as not requested
                        memberDetailsViewModel.onAddReportClicked(doctorId)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, if (pagerState.currentPage == 0) "Add Prescription" else "Add Report")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal) },
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> PrescriptionList(
                        prescriptions = prescriptions,
                        onUploadClick = {
                            memberDetailsViewModel.setTargetPrescriptionForUpload(it.id)
                            prescriptionImagePicker.launch("image/*")
                        },
                        onViewClick = { p -> if (p.imageUri?.isNotBlank() == true) onNavigateToViewDocument(p.id, "prescription", "Dr. ${p.doctorName}'s P.") },
                        onDeleteClick = { memberDetailsViewModel.deletePrescription(it) },
                        onEditClick = { memberDetailsViewModel.onEditPrescriptionClicked(it) }
                    )
                    1 -> ReportList(
                        reports = reports,
                        onUploadClick = {
                            memberDetailsViewModel.setTargetReportForUpload(it.id)
                            reportFilePicker.launch("*/*")
                        },
                        onViewClick = { r -> if (r.fileUri?.isNotBlank() == true) onNavigateToViewDocument(r.id, "report", r.reportName) },
                        onDeleteClick = { memberDetailsViewModel.deleteReport(it) },
                        onEditClick = { memberDetailsViewModel.onEditReportClicked(it) }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---
    if (showAddPrescriptionDialog || editingPrescription != null) {
        AddOrEditPrescriptionDialog(
            memberId = currentMemberId ?: "",
            prescriptionToEdit = editingPrescription,
            onDismiss = { memberDetailsViewModel.onDismissDialogs() },
            onAdd = { memberDetailsViewModel.addPrescription(it) },
            onUpdate = { id, doc, notes -> memberDetailsViewModel.updatePrescriptionDetails(id, doc, notes) }
        )
    }

    if (showAddReportDialog || editingReport != null) {
        AddOrEditReportDialog(
            memberId = currentMemberId ?: "",
            reportToEdit = editingReport,
            onDismiss = { memberDetailsViewModel.onDismissDialogs() },
            onAdd = { memberDetailsViewModel.addReport(it) },
            onUpdate = { id, name, notes -> memberDetailsViewModel.updateReportDetails(id, name, notes) }
        )
    }
}