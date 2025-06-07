package com.example.myprescription.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myprescription.R
import com.example.myprescription.model.Prescription
import com.example.myprescription.model.Report
import com.example.myprescription.viewmodel.MemberDetailsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailsScreen(
    memberId: String,
    memberName: String,
    memberDetailsViewModel: MemberDetailsViewModel = viewModel(factory = MemberDetailsViewModel.Factory),
    onNavigateToViewDocument: (documentId: String, documentPath: String, documentType: String, documentTitle: String) -> Unit,
    onNavigateUp: () -> Unit
) {
    val prescriptions by memberDetailsViewModel.prescriptions.collectAsState()
    val reports by memberDetailsViewModel.reports.collectAsState()
    val showAddPrescriptionDialog by memberDetailsViewModel.showAddPrescriptionDialog.collectAsState()
    val showAddReportDialog by memberDetailsViewModel.showAddReportDialog.collectAsState()

    val prescriptionImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { memberDetailsViewModel.updatePrescriptionWithImage(it) }
    }

    val reportFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { memberDetailsViewModel.updateReportWithFile(it) }
    }

    LaunchedEffect(memberId) {
        memberDetailsViewModel.loadMemberData(memberId)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Prescriptions", "Reports")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(memberName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        memberDetailsViewModel.onAddPrescriptionClicked()
                    } else {
                        memberDetailsViewModel.onAddReportClicked()
                    }
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add Item") },
                text = { Text(if (selectedTabIndex == 0) "Add Prescription" else "Add Report") }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant, // Use theme color
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val currentMemberPrescriptions = prescriptions // Already filtered by ViewModel
            val currentMemberReports = reports // Already filtered by ViewModel

            when (selectedTabIndex) {
                0 -> PrescriptionList(
                    prescriptions = currentMemberPrescriptions,
                    onUploadClick = { prescription ->
                        memberDetailsViewModel.setTargetPrescriptionForUpload(prescription.id)
                        prescriptionImagePicker.launch("image/*")
                    },
                    onViewClick = { prescription ->
                        prescription.imageUri?.let { path ->
                            onNavigateToViewDocument(prescription.id, path, "prescription", "Dr. ${prescription.doctorName}'s Prescription")
                        }
                    }
                )
                1 -> ReportList(
                    reports = currentMemberReports,
                    onUploadClick = { report ->
                        memberDetailsViewModel.setTargetReportForUpload(report.id)
                        reportFilePicker.launch("*/*")
                    },
                    onViewClick = { report ->
                        report.fileUri?.let { path ->
                            onNavigateToViewDocument(report.id, path, "report", report.reportName)
                        }
                    }
                )
            }
        }
    }

    if (showAddPrescriptionDialog) {
        AddPrescriptionDialog(
            memberId = memberId,
            onDismiss = { memberDetailsViewModel.onDismissPrescriptionDialog() },
            onConfirm = { prescriptionData ->
                memberDetailsViewModel.addPrescription(prescriptionData)
                memberDetailsViewModel.onDismissPrescriptionDialog()
            }
        )
    }
    if (showAddReportDialog) {
        AddReportDialog(
            memberId = memberId,
            onDismiss = { memberDetailsViewModel.onDismissReportDialog() },
            onConfirm = { reportData ->
                memberDetailsViewModel.addReport(reportData)
                memberDetailsViewModel.onDismissReportDialog()
            }
        )
    }
}

@Composable
fun PrescriptionList(
    prescriptions: List<Prescription>,
    onUploadClick: (Prescription) -> Unit,
    onViewClick: (Prescription) -> Unit
) {
    if (prescriptions.isEmpty()) {
        EmptyStateView("No prescriptions added yet for this member.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(prescriptions, key = { it.id }) { prescription ->
            PrescriptionCard(prescription, onUploadClick, onViewClick)
        }
    }
}

@Composable
fun ReportList(
    reports: List<Report>,
    onUploadClick: (Report) -> Unit,
    onViewClick: (Report) -> Unit
) {
    if (reports.isEmpty()) {
        EmptyStateView("No reports added yet for this member.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reports, key = { it.id }) { report ->
            ReportCard(report, onUploadClick, onViewClick)
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatDate(date: Date): String {
    return SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault()).format(date)
}

@Composable
fun PrescriptionCard(
    prescription: Prescription,
    onUploadClick: (Prescription) -> Unit,
    onViewClick: (Prescription) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !prescription.imageUri.isNullOrEmpty()) {
                if (!prescription.imageUri.isNullOrEmpty()) onViewClick(prescription)
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!prescription.imageUri.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = File(prescription.imageUri)), // Load File path
                        contentDescription = "Prescription Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Image, contentDescription = "Prescription Icon", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dr. ${prescription.doctorName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Date: ${formatDate(prescription.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                prescription.notes?.let { if (it.isNotBlank()) { Spacer(modifier = Modifier.height(4.dp)); Text("Notes: $it", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
                Spacer(modifier = Modifier.height(12.dp))

                if (prescription.imageUri.isNullOrEmpty()) {
                    Button(onClick = { onUploadClick(prescription) }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Upload Prescription", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Image")
                    }
                } else {
                    OutlinedButton(onClick = { onViewClick(prescription) }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.Visibility, contentDescription = "View Prescription", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Image")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(
    report: Report,
    onUploadClick: (Report) -> Unit,
    onViewClick: (Report) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !report.fileUri.isNullOrEmpty()) {
                if(!report.fileUri.isNullOrEmpty()) onViewClick(report)
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "Report Icon", tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(report.reportName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Date: ${formatDate(report.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                report.notes?.let { if (it.isNotBlank()) { Spacer(modifier = Modifier.height(4.dp)); Text("Notes: $it", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
                Spacer(modifier = Modifier.height(12.dp))

                if (report.fileUri.isNullOrEmpty()) {
                    Button(onClick = { onUploadClick(report) }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Upload Report", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload File")
                    }
                } else {
                    OutlinedButton(onClick = { onViewClick(report) }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.Visibility, contentDescription = "View Report", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View File")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrescriptionDialog(
    memberId: String,
    onDismiss: () -> Unit,
    onConfirm: (Prescription) -> Unit // Dialog returns Prescription data, image upload is separate
) {
    var doctorName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var doctorError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add New Prescription", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it; doctorError = null },
                    label = { Text("Doctor's Name") },
                    singleLine = true,
                    isError = doctorError != null,
                    supportingText = { if (doctorError != null) Text(doctorError!!) }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    minLines = 3,
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        if (doctorName.isBlank()) {
                            doctorError = "Doctor's name cannot be empty"
                        } else {
                            onConfirm(
                                Prescription( // ID will be generated by model, imageUri is null initially
                                    memberId = memberId,
                                    doctorName = doctorName.trim(),
                                    notes = notes.trim().takeIf { it.isNotBlank() },
                                    date = Date()
                                )
                            )
                        }
                    }) { Text("Add Prescription") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReportDialog(
    memberId: String,
    onDismiss: () -> Unit,
    onConfirm: (Report) -> Unit // Dialog returns Report data, file upload is separate
) {
    var reportName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var reportNameError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add New Report", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = reportName,
                    onValueChange = { reportName = it; reportNameError = null },
                    label = { Text("Report Name (e.g., Blood Test)") },
                    singleLine = true,
                    isError = reportNameError != null,
                    supportingText = { if (reportNameError != null) Text(reportNameError!!) }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    minLines = 3,
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        if (reportName.isBlank()) {
                            reportNameError = "Report name cannot be empty"
                        } else {
                            onConfirm(
                                Report( // ID will be generated by model, fileUri is null initially
                                    memberId = memberId,
                                    reportName = reportName.trim(),
                                    notes = notes.trim().takeIf { it.isNotBlank() },
                                    date = Date()
                                )
                            )
                        }
                    }) { Text("Add Report") }
                }
            }
        }
    }
}
