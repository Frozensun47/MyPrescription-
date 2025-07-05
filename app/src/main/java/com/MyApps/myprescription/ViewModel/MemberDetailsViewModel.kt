// frozensun47/myprescription-/MyPrescription--e4ea256193f6bab959107a3c7e7eea1813571356/app/src/main/java/com/MyApps/myprescription/ViewModel/MemberDetailsViewModel.kt
package com.MyApps.myprescription.ViewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.MyApps.myprescription.MyPrescriptionApplication
import com.MyApps.myprescription.data.repository.AppRepository
import com.MyApps.myprescription.model.Doctor
import com.MyApps.myprescription.model.Prescription
import com.MyApps.myprescription.model.Report
import com.MyApps.myprescription.util.saveFileToInternalStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MemberDetailsViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    private val _currentMemberId = MutableStateFlow<String?>(null)
    val currentMemberId: StateFlow<String?> = _currentMemberId.asStateFlow() // Expose public flow

    private val _selectedDoctorId = MutableStateFlow<String?>(null)

    // --- STATE FLOWS ---
    val allPrescriptions: StateFlow<List<Prescription>> = _currentMemberId.flatMapLatest { memberId ->
        if (memberId != null) repository.getPrescriptionsForMember(memberId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<Report>> = _currentMemberId.flatMapLatest { memberId ->
        if (memberId != null) repository.getReportsForMember(memberId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doctors: StateFlow<List<Doctor>> = _currentMemberId.flatMapLatest { memberId ->
        if (memberId != null) repository.getDoctorsForMember(memberId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prescriptionsForSelectedDoctor: StateFlow<List<Prescription>> = _selectedDoctorId.flatMapLatest { docId ->
        if (docId != null) {
            allPrescriptions.map { list -> list.filter { it.doctorId == docId } }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportsForSelectedDoctor: StateFlow<List<Report>> = _selectedDoctorId.flatMapLatest { docId ->
        if (docId != null) {
            allReports.map { list -> list.filter { it.doctorId == docId } }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetDoctorForAttachment = MutableStateFlow<Doctor?>(null)

    private val _showAddPrescriptionDialog = MutableStateFlow(false)
    val showAddPrescriptionDialog: StateFlow<Boolean> = _showAddPrescriptionDialog.asStateFlow()
    private val _showAddReportDialog = MutableStateFlow(false)
    val showAddReportDialog: StateFlow<Boolean> = _showAddReportDialog.asStateFlow()
    private val _showAddDoctorDialog = MutableStateFlow(false)
    val showAddDoctorDialog: StateFlow<Boolean> = _showAddDoctorDialog.asStateFlow()
    private val _editingPrescription = MutableStateFlow<Prescription?>(null)
    val editingPrescription: StateFlow<Prescription?> = _editingPrescription.asStateFlow()
    private val _editingReport = MutableStateFlow<Report?>(null)
    val editingReport: StateFlow<Report?> = _editingReport.asStateFlow()
    private val _editingDoctor = MutableStateFlow<Doctor?>(null)
    val editingDoctor: StateFlow<Doctor?> = _editingDoctor.asStateFlow()
    private var targetDoctorIdForAdd: String? = null
    private val _targetPrescriptionIdForUpload = MutableStateFlow<String?>(null)
    private val _targetReportIdForUpload = MutableStateFlow<String?>(null)


    // --- FUNCTIONS ---
    fun loadMemberData(memberId: String) { _currentMemberId.value = memberId }
    fun selectDoctor(doctorId: String) { _selectedDoctorId.value = doctorId }

    // DOCTOR methods
    fun addDoctor(doctor: Doctor) = viewModelScope.launch { repository.insertDoctor(doctor) }
    fun updateDoctor(doctor: Doctor) = viewModelScope.launch { repository.updateDoctor(doctor) }
    fun deleteDoctor(doctor: Doctor) = viewModelScope.launch { repository.deleteDoctor(doctor) }

    // PRESCRIPTION methods
    fun addPrescription(prescription: Prescription) = viewModelScope.launch { repository.insertPrescription(prescription.copy(doctorId = targetDoctorIdForAdd)) }
    fun deletePrescription(prescription: Prescription) = viewModelScope.launch {
        prescription.imageUri?.split(',')?.filter { it.isNotBlank() }?.forEach { path -> try { File(path).delete() } catch (e: Exception) { e.printStackTrace() } }
        repository.deletePrescription(prescription)
    }
    fun createPrescriptionWithAttachments(uris: List<Uri>) {
        viewModelScope.launch {
            val doctor = targetDoctorForAttachment.value ?: return@launch
            val memberId = _currentMemberId.value ?: return@launch

            val imagePaths = uris.mapNotNull { saveFileToInternalStorage(getApplication(), it, "prescription").takeIf(String::isNotBlank) }
            if (imagePaths.isEmpty()) return@launch

            val newPrescription = Prescription(
                memberId = memberId,
                doctorId = doctor.id,
                doctorName = doctor.name,
                date = Date(),
                notes = null,
                imageUri = imagePaths.joinToString(",")
            )
            repository.insertPrescription(newPrescription)
            targetDoctorForAttachment.value = null
        }
    }
    fun updatePrescriptionDetails(id: String, doctorName: String, notes: String, date: Date) {
        viewModelScope.launch {
            val prescription = allPrescriptions.value.find { it.id == id } ?: return@launch
            val updatedPrescription = prescription.copy(doctorName = doctorName, notes = notes.ifBlank { null }, date = date)
            repository.updatePrescription(updatedPrescription)
        }
    }

    // REPORT methods
    fun addReport(report: Report) = viewModelScope.launch { repository.insertReport(report.copy(doctorId = targetDoctorIdForAdd)) }
    fun deleteReport(report: Report) = viewModelScope.launch {
        report.fileUri?.split(',')?.filter { it.isNotBlank() }?.forEach { path -> try { File(path).delete() } catch (e: Exception) { e.printStackTrace() } }
        repository.deleteReport(report)
    }
    fun createReportWithAttachments(uris: List<Uri>) {
        viewModelScope.launch {
            val doctor = targetDoctorForAttachment.value ?: return@launch
            val memberId = _currentMemberId.value ?: return@launch

            val filePaths = uris.mapNotNull { saveFileToInternalStorage(getApplication(), it, "report").takeIf(String::isNotBlank) }
            if (filePaths.isEmpty()) return@launch

            val newReport = Report(
                memberId = memberId,
                doctorId = doctor.id,
                reportName = "New report from ${SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date())}",
                date = Date(),
                fileUri = filePaths.joinToString(",")
            )
            repository.insertReport(newReport)
            targetDoctorForAttachment.value = null
        }
    }
    fun updateReportDetails(id: String, reportName: String, notes: String, date: Date) {
        viewModelScope.launch {
            val report = allReports.value.find { it.id == id } ?: return@launch
            val updatedReport = report.copy(reportName = reportName, notes = notes.ifBlank { null }, date = date)
            repository.updateReport(updatedReport)
        }
    }

    // FILE HANDLING
    fun setTargetPrescriptionForUpload(prescriptionId: String) { _targetPrescriptionIdForUpload.value = prescriptionId }
    fun setTargetReportForUpload(reportId: String) { _targetReportIdForUpload.value = reportId }
    fun updatePrescriptionWithImages(pickedImageUris: List<Uri>) {
        viewModelScope.launch {
            val targetId = _targetPrescriptionIdForUpload.value ?: return@launch
            val currentPrescription = allPrescriptions.value.find { it.id == targetId } ?: return@launch
            val existingPaths = currentPrescription.imageUri?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            val newPaths = existingPaths.toMutableList()
            for (uri in pickedImageUris) {
                val imagePath = saveFileToInternalStorage(getApplication(), uri, "prescription")
                if(imagePath.isNotBlank()) newPaths.add(imagePath)
            }
            val updatedPrescription = currentPrescription.copy(imageUri = newPaths.joinToString(","))
            repository.updatePrescription(updatedPrescription)
            _targetPrescriptionIdForUpload.value = null
        }
    }

    fun addImagesToPrescription(prescriptionId: String, pickedImageUris: List<Uri>) {
        viewModelScope.launch {
            val currentPrescription = allPrescriptions.value.find { it.id == prescriptionId } ?: return@launch
            val existingPaths = currentPrescription.imageUri?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            val newPaths = existingPaths.toMutableList()

            for (uri in pickedImageUris) {
                val imagePath = saveFileToInternalStorage(getApplication(), uri, "prescription")
                if (imagePath.isNotBlank()) {
                    newPaths.add(imagePath)
                }
            }

            val updatedPrescription = currentPrescription.copy(imageUri = newPaths.joinToString(","))
            repository.updatePrescription(updatedPrescription)
        }
    }

    fun updateReportWithFiles(pickedFileUris: List<Uri>) {
        viewModelScope.launch {
            val targetId = _targetReportIdForUpload.value ?: return@launch
            val currentReport = allReports.value.find { it.id == targetId } ?: return@launch
            val existingPaths = currentReport.fileUri?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            val newPaths = existingPaths.toMutableList()
            for (uri in pickedFileUris) {
                val filePath = saveFileToInternalStorage(getApplication(), uri, "report")
                if(filePath.isNotBlank()) newPaths.add(filePath)
            }
            val updatedReport = currentReport.copy(fileUri = newPaths.joinToString(","))
            repository.updateReport(updatedReport)
            _targetReportIdForUpload.value = null
        }
    }
    fun updatePrescriptionNotes(prescriptionId: String, newNotes: String) {
        viewModelScope.launch {
            val prescription = allPrescriptions.value.find { it.id == prescriptionId } ?: return@launch
            repository.updatePrescription(prescription.copy(notes = newNotes.ifBlank { null }))
        }
    }
    fun updateReportNotes(reportId: String, newNotes: String) {
        viewModelScope.launch {
            val report = allReports.value.find { it.id == reportId } ?: return@launch
            repository.updateReport(report.copy(notes = newNotes.ifBlank { null }))
        }
    }
    fun deleteMultipleFiles(documentId: String, documentType: String, pathsToDelete: Set<String>) {
        viewModelScope.launch {
            if (documentType == "prescription") {
                val prescription = allPrescriptions.value.find { it.id == documentId } ?: return@launch
                val updatedPaths = prescription.imageUri?.split(',')?.filter { it.isNotBlank() && it !in pathsToDelete }?.joinToString(",") ?: ""
                repository.updatePrescription(prescription.copy(imageUri = updatedPaths))
            } else {
                val report = allReports.value.find { it.id == documentId } ?: return@launch
                val updatedPaths = report.fileUri?.split(',')?.filter { it.isNotBlank() && it !in pathsToDelete }?.joinToString(",") ?: ""
                repository.updateReport(report.copy(fileUri = updatedPaths))
            }
            pathsToDelete.forEach { path -> try { File(path).delete() } catch (e: Exception) { e.printStackTrace() } }
        }
    }

    // DIALOG VISIBILITY & EDITING STATE
    fun onAddDoctorClicked() { _editingDoctor.value = null; _showAddDoctorDialog.value = true }
    fun onEditDoctorClicked(doctor: Doctor) { _editingDoctor.value = doctor; _showAddDoctorDialog.value = true }
    fun onAddPrescriptionClicked(doctorId: String) { targetDoctorIdForAdd = doctorId; _editingPrescription.value = null; _showAddPrescriptionDialog.value = true }
    fun onAddReportClicked(doctorId: String) { targetDoctorIdForAdd = doctorId; _editingReport.value = null; _showAddReportDialog.value = true }
    fun onEditPrescriptionClicked(prescription: Prescription) { _editingPrescription.value = prescription; _showAddPrescriptionDialog.value = true }
    fun onEditReportClicked(report: Report) { _editingReport.value = report; _showAddReportDialog.value = true }
    fun onDismissDialogs() {
        _showAddPrescriptionDialog.value = false; _showAddReportDialog.value = false; _showAddDoctorDialog.value = false
        _editingPrescription.value = null; _editingReport.value = null; _editingDoctor.value = null
        targetDoctorIdForAdd = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MyPrescriptionApplication
                val repository = application.repository ?: throw IllegalStateException("Repository not initialized")
                return MemberDetailsViewModel(application, repository) as T
            }
        }
    }
}