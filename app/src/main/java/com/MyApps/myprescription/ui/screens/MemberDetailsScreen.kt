// frozensun47/myprescription-/MyPrescription--e4ea256193f6bab959107a3c7e7eea1813571356/app/src/main/java/com/MyApps/myprescription/ui/screens/MemberDetailsScreen.kt
package com.MyApps.myprescription.ui.screens

import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.MyApps.myprescription.ViewModel.MemberDetailsViewModel
import com.MyApps.myprescription.model.Doctor
import com.MyApps.myprescription.model.Prescription
import com.MyApps.myprescription.model.Report
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemberDetailsScreen(
    memberId: String,
    memberName: String,
    memberDetailsViewModel: MemberDetailsViewModel,
    onNavigateToViewDocument: (documentId: String, documentType: String, documentTitle: String) -> Unit,
    onNavigateToDoctorDetails: (doctorId: String, doctorName: String) -> Unit,
    onNavigateUp: () -> Unit
) {
    val prescriptions by memberDetailsViewModel.allPrescriptions.collectAsState()
    val reports by memberDetailsViewModel.allReports.collectAsState()
    val doctors by memberDetailsViewModel.doctors.collectAsState()
    val showAddDoctorDialog by memberDetailsViewModel.showAddDoctorDialog.collectAsState()
    val editingDoctor by memberDetailsViewModel.editingDoctor.collectAsState()
    val showAddPrescriptionDialog by memberDetailsViewModel.showAddPrescriptionDialog.collectAsState()
    val showAddReportDialog by memberDetailsViewModel.showAddReportDialog.collectAsState()
    val editingPrescription by memberDetailsViewModel.editingPrescription.collectAsState()
    val editingReport by memberDetailsViewModel.editingReport.collectAsState()
    var itemToDelete by remember { mutableStateOf<Any?>(null) }
    val tabs = listOf("Doctors", "All Prescriptions", "All Reports")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // --- MODIFICATION START ---
    // This state flag prevents multiple navigation events from being triggered.
    var isNavigatingBack by remember { mutableStateOf(false) }

    // This function will be called for all back navigation actions.
    val handleBackNavigation = {
        if (!isNavigatingBack) {
            isNavigatingBack = true
            onNavigateUp()
        }
    }

    // Use the handler for the system back button.
    BackHandler {
        handleBackNavigation()
    }
    // --- MODIFICATION END ---


    val quickPrescriptionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            memberDetailsViewModel.createPrescriptionWithAttachments(uris)
        }
    }

    val quickReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            memberDetailsViewModel.createReportWithAttachments(uris)
        }
    }

    LaunchedEffect(memberId) {
        memberDetailsViewModel.loadMemberData(memberId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(memberName) },
                // Use the new handler for the top app bar icon as well.
                navigationIcon = { IconButton(onClick = handleBackNavigation) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
            )
        },
        floatingActionButton = {
            if (pagerState.currentPage == 0) {
                FloatingActionButton(
                    onClick = { memberDetailsViewModel.onAddDoctorClicked() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, "Add Doctor")
                }
            }
        },
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
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> DoctorList(
                        doctors = doctors,
                        onDoctorClick = { onNavigateToDoctorDetails(it.id, it.name) },
                        onAddPrescriptionClick = {
                            memberDetailsViewModel.targetDoctorForAttachment.value = it
                            quickPrescriptionLauncher.launch("image/*")
                        },
                        onAddReportClick = {
                            memberDetailsViewModel.targetDoctorForAttachment.value = it
                            quickReportLauncher.launch("*/*")
                        },
                        onDeleteClick = { itemToDelete = it },
                        onEditClick = { memberDetailsViewModel.onEditDoctorClicked(it) }
                    )
                    1 -> PrescriptionList(
                        prescriptions = prescriptions,
                        onUploadClick = { memberDetailsViewModel.setTargetPrescriptionForUpload(it.id) },
                        onViewClick = { p -> if (p.imageUri?.isNotBlank() == true) onNavigateToViewDocument(p.id, "prescription", "Dr. ${p.doctorName}'s P.") },
                        onDeleteClick = { itemToDelete = it },
                        onEditClick = { memberDetailsViewModel.onEditPrescriptionClicked(it) }
                    )
                    2 -> ReportList(
                        reports = reports,
                        onUploadClick = { memberDetailsViewModel.setTargetReportForUpload(it.id) },
                        onViewClick = { r -> if (r.fileUri?.isNotBlank() == true) onNavigateToViewDocument(r.id, "report", r.reportName) },
                        onDeleteClick = { itemToDelete = it },
                        onEditClick = { memberDetailsViewModel.onEditReportClicked(it) }
                    )
                }
            }
        }
    }

    if (showAddDoctorDialog || editingDoctor != null) {
        AddOrEditDoctorDialog(
            memberId = memberId,
            doctorToEdit = editingDoctor,
            onDismiss = { memberDetailsViewModel.onDismissDialogs() },
            onConfirm = { doctor ->
                if (editingDoctor == null) memberDetailsViewModel.addDoctor(doctor)
                else memberDetailsViewModel.updateDoctor(doctor)
            }
        )
    }

    if (showAddPrescriptionDialog || editingPrescription != null) {
        AddOrEditPrescriptionDialog(
            memberId = memberId,
            prescriptionToEdit = editingPrescription,
            onDismiss = { memberDetailsViewModel.onDismissDialogs() },
            onAdd = { memberDetailsViewModel.addPrescription(it) },
            onUpdate = { id, doc, notes, date -> memberDetailsViewModel.updatePrescriptionDetails(id, doc, notes, date) }
        )
    }

    if (showAddReportDialog || editingReport != null) {
        AddOrEditReportDialog(
            memberId = memberId,
            reportToEdit = editingReport,
            onDismiss = { memberDetailsViewModel.onDismissDialogs() },
            onAdd = { memberDetailsViewModel.addReport(it) },
            onUpdate = { id, name, notes, date -> memberDetailsViewModel.updateReportDetails(id, name, notes, date) }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure? This action is permanent.") },
            confirmButton = {
                Button(
                    onClick = {
                        when (val item = itemToDelete) {
                            is Prescription -> memberDetailsViewModel.deletePrescription(item)
                            is Report -> memberDetailsViewModel.deleteReport(item)
                            is Doctor -> memberDetailsViewModel.deleteDoctor(item)
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { itemToDelete = null }) { Text("Cancel") } }
        )
    }
}

// --- ALL COMPOSABLE FUNCTIONS BELOW ARE NOW AT THE TOP-LEVEL ---

@Composable
fun DoctorList(
    doctors: List<Doctor>,
    onDoctorClick: (Doctor) -> Unit,
    onAddPrescriptionClick: (Doctor) -> Unit,
    onAddReportClick: (Doctor) -> Unit,
    onDeleteClick: (Doctor) -> Unit,
    onEditClick: (Doctor) -> Unit
) {
    if (doctors.isEmpty()) {
        EmptyStateView("No doctors added yet. Tap the '+' button to add one.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(doctors, key = { it.id }) { doctor ->
            DoctorCard(
                doctor = doctor,
                onAddPrescriptionClick = { onAddPrescriptionClick(doctor) },
                onAddReportClick = { onAddReportClick(doctor) },
                onDeleteClick = { onDeleteClick(doctor) },
                onEditClick = { onEditClick(doctor) },
                onCardClick = { onDoctorClick(doctor) }
            )
        }
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
    if (prescriptions.isEmpty()) {
        EmptyStateView("No prescriptions added yet for this member.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
    if (reports.isEmpty()) {
        EmptyStateView("No reports added yet for this member.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(reports, key = { it.id }) { report ->
            ReportCard(report, onUploadClick, onViewClick, onDeleteClick, onEditClick)
        }
    }
}

@Composable
fun DoctorCard(
    doctor: Doctor,
    onCardClick: () -> Unit,
    onAddPrescriptionClick: () -> Unit,
    onAddReportClick: () -> Unit,
    onDeleteClick: (Doctor) -> Unit,
    onEditClick: (Doctor) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) // Updated alpha
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Doctor Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dr. ${doctor.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!doctor.specialization.isNullOrBlank()) {
                        Text(
                            doctor.specialization,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { onEditClick(doctor) }) { Icon(Icons.Default.Edit, "Edit Doctor") }
                IconButton(onClick = { onDeleteClick(doctor) }) { Icon(Icons.Default.Delete, "Delete Doctor", tint = MaterialTheme.colorScheme.error) }
            }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (imagePaths.isNotEmpty()) onViewClick(prescription) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) // Updated alpha
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MedicalServices,
                    contentDescription = "Prescription Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dr. ${prescription.doctorName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(formatDate(prescription.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onEditClick(prescription) }) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = { onDeleteClick(prescription) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }

            if (!prescription.notes.isNullOrBlank()) {
                Text(
                    "Notes: ${prescription.notes}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (imagePaths.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onViewClick(prescription) }
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = File(imagePaths.first())),
                        contentDescription = "Prescription Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (imagePaths.size == 1) "View 1 image" else "View ${imagePaths.size} images",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, "View", modifier = Modifier.size(16.dp))
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { onUploadClick(prescription) }) {
                    Icon(Icons.Default.AddPhotoAlternate, "Add prescription", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Prescription")
                }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (filePaths.isNotEmpty()) onViewClick(report) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) // Updated alpha
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = "Report Icon",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        report.reportName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatDate(report.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onEditClick(report) }) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = { onDeleteClick(report) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }

            if (!report.notes.isNullOrBlank()) {
                Text(
                    "Notes: ${report.notes}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (filePaths.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onViewClick(report) }
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = "Folder Icon",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (filePaths.size == 1) "View 1 file" else "View ${filePaths.size} files",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, "View", modifier = Modifier.size(16.dp))
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { onUploadClick(report) }) {
                    Icon(Icons.Default.FileUpload, "Add files", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Files")
                }
            }
        }
    }
}

fun formatDate(date: Date): String {
    return SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault()).format(date)
}

@Composable
fun EmptyStateView(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AddOrEditDoctorDialog(
    memberId: String,
    doctorToEdit: Doctor?,
    onDismiss: () -> Unit,
    onConfirm: (Doctor) -> Unit
) {
    var name by remember { mutableStateOf(doctorToEdit?.name ?: "") }
    var specialization by remember { mutableStateOf(doctorToEdit?.specialization ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val isEditing = doctorToEdit != null

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if(isEditing) "Edit Doctor" else "Add New Doctor",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(24.dp)
                )
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it; nameError = null }, label = { Text("Doctor's Name") }, isError = nameError != null, supportingText = { if (nameError != null) Text(nameError!!) })
                    OutlinedTextField(value = specialization, onValueChange = { specialization = it }, label = { Text("Specialization / Clinic Name (Optional)") })
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (name.isBlank()) {
                            nameError = "Doctor's name cannot be empty"
                        } else {
                            onConfirm(
                                Doctor(
                                    id = doctorToEdit?.id ?: UUID.randomUUID().toString(),
                                    memberId = memberId,
                                    name = name.trim(),
                                    specialization = specialization.trim().ifBlank { null }
                                )
                            )
                            onDismiss()
                        }
                    }) { Text(if (isEditing) "Save Changes" else "Add") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditPrescriptionDialog(
    memberId: String,
    prescriptionToEdit: Prescription? = null,
    onDismiss: () -> Unit,
    onAdd: (Prescription) -> Unit = {},
    onUpdate: (id: String, doctorName: String, notes: String, date: Date) -> Unit = { _, _, _, _ -> }
) {
    var doctorName by remember { mutableStateOf(prescriptionToEdit?.doctorName ?: "") }
    var notes by remember { mutableStateOf(prescriptionToEdit?.notes ?: "") }
    var date by remember(prescriptionToEdit) { mutableStateOf(prescriptionToEdit?.date ?: Date()) }
    var doctorError by remember { mutableStateOf<String?>(null) }
    val isEditing = prescriptionToEdit != null

    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if(isEditing) "Edit Prescription" else "Add New Prescription",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(24.dp)
                )
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it; doctorError = null },
                        label = { Text("Doctor's Name") },
                        isError = doctorError != null,
                        supportingText = { if (doctorError != null) Text(doctorError!!) }
                    )

                    // Corrected Date/Time Field
                    Box(modifier = Modifier.clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = formatDate(date),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date and Time") },
                            trailingIcon = { Icon(Icons.Default.EditCalendar, "Edit Date and Time") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false, // Makes it visually clear it's not a standard text input
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        minLines = 3
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (doctorName.isBlank()) {
                            doctorError = "Doctor's name cannot be empty"
                        } else {
                            if (isEditing) {
                                onUpdate(prescriptionToEdit!!.id, doctorName.trim(), notes.trim(), date)
                            } else {
                                onAdd(Prescription(memberId = memberId, doctorName = doctorName.trim(), date = date, notes = notes.trim().ifBlank { null }, doctorId = null))
                            }
                            onDismiss()
                        }
                    }) { Text(if (isEditing) "Save Changes" else "Add") }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        date = Date(datePickerState.selectedDateMillis ?: date.time)
                        val calendar = Calendar.getInstance().apply { time = date }
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val cal = Calendar.getInstance().apply { time = date }
                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                cal.set(Calendar.MINUTE, minute)
                                date = cal.time
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        ).show()
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditReportDialog(
    memberId: String,
    reportToEdit: Report? = null,
    onDismiss: () -> Unit,
    onAdd: (Report) -> Unit = {},
    onUpdate: (id: String, name: String, notes: String, date: Date) -> Unit = { _, _, _, _ -> }
) {
    var reportName by remember { mutableStateOf(reportToEdit?.reportName ?: "") }
    var notes by remember { mutableStateOf(reportToEdit?.notes ?: "") }
    var date by remember(reportToEdit) { mutableStateOf(reportToEdit?.date ?: Date()) }
    var nameError by remember { mutableStateOf<String?>(null) }
    val isEditing = reportToEdit != null
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEditing) "Edit Report" else "Add New Report",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(24.dp)
                )
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = reportName,
                        onValueChange = { reportName = it; nameError = null },
                        label = { Text("Report Name") },
                        isError = nameError != null,
                        supportingText = { if (nameError != null) Text(nameError!!) }
                    )

                    // Corrected Date/Time Field
                    Box(modifier = Modifier.clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = formatDate(date),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date and Time") },
                            trailingIcon = { Icon(Icons.Default.EditCalendar, "Edit Date and Time") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false, // Makes it visually clear it's not a standard text input
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        minLines = 3
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (reportName.isBlank()) {
                            nameError = "Report name cannot be empty"
                        } else {
                            if (isEditing) {
                                onUpdate(reportToEdit!!.id, reportName.trim(), notes.trim(), date)
                            } else {
                                onAdd(Report(memberId = memberId, reportName = reportName.trim(), date = date, notes = notes.trim().ifBlank { null }, doctorId = null))
                            }
                            onDismiss()
                        }
                    }) { Text(if (isEditing) "Save Changes" else "Add") }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        date = Date(datePickerState.selectedDateMillis ?: date.time)
                        val calendar = Calendar.getInstance().apply { time = date }
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val cal = Calendar.getInstance().apply { time = date }
                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                cal.set(Calendar.MINUTE, minute)
                                date = cal.time
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        ).show()
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}