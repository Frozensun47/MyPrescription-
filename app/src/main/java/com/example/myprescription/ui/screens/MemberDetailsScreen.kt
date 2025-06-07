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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.myprescription.model.Prescription
import com.example.myprescription.model.Report
import com.example.myprescription.ViewModel.MemberDetailsViewModel
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

    // Dialog states
    val showAddPrescriptionDialog by memberDetailsViewModel.showAddPrescriptionDialog.collectAsState()
    val showAddReportDialog by memberDetailsViewModel.showAddReportDialog.collectAsState()
    val editingPrescription by memberDetailsViewModel.editingPrescription.collectAsState()
    val editingReport by memberDetailsViewModel.editingReport.collectAsState()
    var itemToDelete by remember { mutableStateOf<Any?>(null) }


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

    LaunchedEffect(memberId) {
        memberDetailsViewModel.loadMemberData(memberId)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Prescriptions", "Reports")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(memberName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) memberDetailsViewModel.onAddPrescriptionClicked()
                    else memberDetailsViewModel.onAddReportClicked()
                },
                icon = { Icon(Icons.Filled.Add, "Add") },
                text = { Text(if (selectedTabIndex == 0) "Add Prescription" else "Add Report") }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTabIndex) {
                0 -> PrescriptionList(
                    prescriptions = prescriptions,
                    onUploadClick = {
                        memberDetailsViewModel.setTargetPrescriptionForUpload(it.id)
                        prescriptionImagePicker.launch("image/*")
                    },
                    onViewClick = { p -> p.imageUri?.let { onNavigateToViewDocument(p.id, it, "prescription", "Dr. ${p.doctorName}'s P.") } },
                    onDeleteClick = { itemToDelete = it },
                    onEditClick = { memberDetailsViewModel.onEditPrescriptionClicked(it) }
                )
                1 -> ReportList(
                    reports = reports,
                    onUploadClick = {
                        memberDetailsViewModel.setTargetReportForUpload(it.id)
                        reportFilePicker.launch("*/*")
                    },
                    onViewClick = { r -> r.fileUri?.let { onNavigateToViewDocument(r.id, it, "report", r.reportName) } },
                    onDeleteClick = { itemToDelete = it },
                    onEditClick = { memberDetailsViewModel.onEditReportClicked(it) }
                )
            }
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure? This will delete the item and all its files permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        when (val item = itemToDelete) {
                            is Prescription -> memberDetailsViewModel.deletePrescription(item)
                            is Report -> memberDetailsViewModel.deleteReport(item)
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { itemToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showAddPrescriptionDialog) {
        AddOrEditPrescriptionDialog(
            memberId = memberId,
            onDismiss = { memberDetailsViewModel.onDismissDialogs() },
            onAdd = { memberDetailsViewModel.addPrescription(it) }
        )
    }

    editingPrescription?.let {
        AddOrEditPrescriptionDialog(
            memberId = memberId,
            prescriptionToEdit = it,
            onDismiss = { memberDetailsViewModel.onDismissDialogs() },
            onUpdate = { id, doc, notes -> memberDetailsViewModel.updatePrescriptionDetails(id, doc, notes) }
        )
    }

    if (showAddReportDialog) {
        AddOrEditReportDialog(
            memberId = memberId,
            onDismiss = { memberDetailsViewModel.onDismissDialogs() },
            onAdd = { memberDetailsViewModel.addReport(it) }
        )
    }

    editingReport?.let {
        AddOrEditReportDialog(
            memberId = memberId,
            reportToEdit = it,
            onDismiss = { memberDetailsViewModel.onDismissDialogs() },
            onUpdate = { id, name, notes -> memberDetailsViewModel.updateReportDetails(id, name, notes) }
        )
    }
}

@Composable
fun PrescriptionList(
    prescriptions: List<Prescription>,
    onUploadClick: (Prescription) -> Unit,
    onViewClick: (Prescription) -> Unit,
    onDeleteClick: (Prescription) -> Unit,
    onEditClick: (Prescription) -> Unit
) {
    if (prescriptions.isEmpty()) { EmptyStateView("No prescriptions added yet.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(prescriptions, key = { it.id }) { prescription ->
            PrescriptionCard(prescription, onUploadClick, onViewClick, onDeleteClick, onEditClick)
        }
    }
}

@Composable
fun ReportList(
    reports: List<Report>,
    onUploadClick: (Report) -> Unit,
    onViewClick: (Report) -> Unit,
    onDeleteClick: (Report) -> Unit,
    onEditClick: (Report) -> Unit
) {
    if (reports.isEmpty()) { EmptyStateView("No reports added yet.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reports, key = { it.id }) { report ->
            ReportCard(report, onUploadClick, onViewClick, onDeleteClick, onEditClick)
        }
    }
}

@Composable
fun PrescriptionCard(
    prescription: Prescription,
    onUploadClick: (Prescription) -> Unit,
    onViewClick: (Prescription) -> Unit,
    onDeleteClick: (Prescription) -> Unit,
    onEditClick: (Prescription) -> Unit
) {
    val imagePaths = prescription.imageUri?.split(',')?.filter { it.isNotBlank() } ?: emptyList()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(enabled = imagePaths.isNotEmpty()) { onViewClick(prescription) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (imagePaths.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = File(imagePaths.first())),
                        contentDescription = "Prescription Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (imagePaths.size > 1) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("+${imagePaths.size - 1}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                } else {
                    Icon(Icons.Filled.Image, "No Images", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dr. ${prescription.doctorName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Date: ${formatDate(prescription.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                prescription.notes?.let { if (it.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text("Notes: $it", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { onUploadClick(prescription) }, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Filled.AddPhotoAlternate, "Add Images", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add")
                }
            }
            Column {
                IconButton(onClick = { onEditClick(prescription) }) { Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.secondary) }
                IconButton(onClick = { onDeleteClick(prescription) }) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun ReportCard(
    report: Report,
    onUploadClick: (Report) -> Unit,
    onViewClick: (Report) -> Unit,
    onDeleteClick: (Report) -> Unit,
    onEditClick: (Report) -> Unit
) {
    val filePaths = report.fileUri?.split(',')?.filter { it.isNotBlank() } ?: emptyList()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(enabled = filePaths.isNotEmpty()) { onViewClick(report) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.Top) {
            Icon(if (filePaths.size > 1) Icons.Filled.FolderCopy else Icons.AutoMirrored.Filled.InsertDriveFile, "Report Icon", modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(report.reportName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Date: ${formatDate(report.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${filePaths.size} file(s) attached", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { onUploadClick(report) }, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Filled.FileUpload, "Add Files", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add")
                }
            }
            Column {
                IconButton(onClick = { onEditClick(report) }) { Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.secondary) }
                IconButton(onClick = { onDeleteClick(report) }) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}



fun formatDate(date: Date): String {
    return SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault()).format(date)
}

@Composable
fun AddOrEditPrescriptionDialog(
    memberId: String,
    prescriptionToEdit: Prescription? = null,
    onDismiss: () -> Unit,
    onAdd: (Prescription) -> Unit = {},
    onUpdate: (id: String, doctorName: String, notes: String) -> Unit = {_,_,_ ->}
) {
    var doctorName by remember { mutableStateOf(prescriptionToEdit?.doctorName ?: "") }
    var notes by remember { mutableStateOf(prescriptionToEdit?.notes ?: "") }
    var doctorError by remember { mutableStateOf<String?>(null) }
    val isEditing = prescriptionToEdit != null

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if(isEditing) "Edit Prescription" else "Add New Prescription", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = doctorName, onValueChange = { doctorName = it; doctorError = null }, label = { Text("Doctor's Name") }, isError = doctorError != null, supportingText = { if (doctorError != null) Text(doctorError!!) })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, minLines = 3)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (doctorName.isBlank()) {
                            doctorError = "Doctor's name cannot be empty"
                        } else {
                            if (isEditing) {
                                onUpdate(prescriptionToEdit!!.id, doctorName.trim(), notes.trim())
                            } else {
                                onAdd(Prescription(memberId = memberId, doctorName = doctorName.trim(), date = Date(), notes = notes.trim().ifBlank { null }))
                            }
                            onDismiss()
                        }
                    }) { Text(if (isEditing) "Save Changes" else "Add") }
                }
            }
        }
    }
}

@Composable
fun AddOrEditReportDialog(
    memberId: String,
    reportToEdit: Report? = null,
    onDismiss: () -> Unit,
    onAdd: (Report) -> Unit = {},
    onUpdate: (id: String, name: String, notes: String) -> Unit = {_,_,_ ->}
) {
    var reportName by remember { mutableStateOf(reportToEdit?.reportName ?: "") }
    var notes by remember { mutableStateOf(reportToEdit?.notes ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val isEditing = reportToEdit != null

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (isEditing) "Edit Report" else "Add New Report", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = reportName, onValueChange = { reportName = it; nameError = null }, label = { Text("Report Name") }, isError = nameError != null, supportingText = { if (nameError != null) Text(nameError!!) })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, minLines = 3)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (reportName.isBlank()) {
                            nameError = "Report name cannot be empty"
                        } else {
                            if (isEditing) {
                                onUpdate(reportToEdit!!.id, reportName.trim(), notes.trim())
                            } else {
                                onAdd(Report(memberId = memberId, reportName = reportName.trim(), date = Date(), notes = notes.trim().ifBlank { null }))
                            }
                            onDismiss()
                        }
                    }) { Text(if (isEditing) "Save Changes" else "Add") }
                }
            }
        }
    }
}