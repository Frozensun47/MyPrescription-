package com.example.myprescription.ViewModel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.myprescription.MyPrescriptionApplication
import com.example.myprescription.data.repository.AppRepository
import com.example.myprescription.model.Prescription
import com.example.myprescription.model.Report
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Date

class MemberDetailsViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    private val _currentMemberId = MutableStateFlow<String?>(null)

    val prescriptions: StateFlow<List<Prescription>> = _currentMemberId.flatMapLatest { memberId ->
        if (memberId != null) {
            repository.getPrescriptionsForMember(memberId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<Report>> = _currentMemberId.flatMapLatest { memberId ->
        if (memberId != null) {
            repository.getReportsForMember(memberId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dialog States ---
    private val _showAddPrescriptionDialog = MutableStateFlow(false)
    val showAddPrescriptionDialog: StateFlow<Boolean> = _showAddPrescriptionDialog.asStateFlow()

    private val _showAddReportDialog = MutableStateFlow(false)
    val showAddReportDialog: StateFlow<Boolean> = _showAddReportDialog.asStateFlow()

    private val _editingPrescription = MutableStateFlow<Prescription?>(null)
    val editingPrescription: StateFlow<Prescription?> = _editingPrescription.asStateFlow()

    private val _editingReport = MutableStateFlow<Report?>(null)
    val editingReport: StateFlow<Report?> = _editingReport.asStateFlow()

    // --- Upload Targets ---
    private val _targetPrescriptionIdForUpload = MutableStateFlow<String?>(null)
    private val _targetReportIdForUpload = MutableStateFlow<String?>(null)

    fun loadMemberData(memberId: String) {
        _currentMemberId.value = memberId
    }

    private suspend fun saveFileToInternalStorage(context: Context, uri: Uri, type: String, itemId: String): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val mimeType = context.contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            val fileName = "${type}_${itemId}_${System.currentTimeMillis()}" + if (extension != null) ".$extension" else ""
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- CRUD Operations ---

    fun addPrescription(prescriptionData: Prescription) {
        viewModelScope.launch {
            val memberId = _currentMemberId.value ?: return@launch
            val finalPrescription = prescriptionData.copy(memberId = memberId, imageUri = "")
            repository.insertPrescription(finalPrescription)
        }
    }

    fun addReport(reportData: Report) {
        viewModelScope.launch {
            val memberId = _currentMemberId.value ?: return@launch
            val finalReport = reportData.copy(memberId = memberId, fileUri = "")
            repository.insertReport(finalReport)
        }
    }

    fun updatePrescriptionDetails(id: String, doctorName: String, notes: String) {
        viewModelScope.launch {
            val prescription = prescriptions.value.find { it.id == id } ?: return@launch
            val updatedPrescription = prescription.copy(doctorName = doctorName, notes = notes.ifBlank { null })
            repository.updatePrescription(updatedPrescription)
            onDismissDialogs()
        }
    }

    fun updateReportDetails(id: String, reportName: String, notes: String) {
        viewModelScope.launch {
            val report = reports.value.find { it.id == id } ?: return@launch
            val updatedReport = report.copy(reportName = reportName, notes = notes.ifBlank { null })
            repository.updateReport(updatedReport)
            onDismissDialogs()
        }
    }

    fun deletePrescription(prescription: Prescription) {
        viewModelScope.launch {
            prescription.imageUri?.split(',')?.filter { it.isNotBlank() }?.forEach { path ->
                try { File(path).delete() } catch (e: Exception) { e.printStackTrace() }
            }
            repository.deletePrescription(prescription)
        }
    }

    fun deleteReport(report: Report) {
        viewModelScope.launch {
            report.fileUri?.split(',')?.filter { it.isNotBlank() }?.forEach { path ->
                try { File(path).delete() } catch (e: Exception) { e.printStackTrace() }
            }
            repository.deleteReport(report)
        }
    }

    fun deleteFileFromPrescription(prescriptionId: String, pathToDelete: String) {
        viewModelScope.launch {
            val prescription = prescriptions.value.find { it.id == prescriptionId } ?: return@launch
            val updatedPaths = prescription.imageUri
                ?.split(',')
                ?.filter { it.isNotBlank() && it != pathToDelete }
                ?.joinToString(",")
                ?: ""

            repository.updatePrescription(prescription.copy(imageUri = updatedPaths))
            try { File(pathToDelete).delete() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteFileFromReport(reportId: String, pathToDelete: String) {
        viewModelScope.launch {
            val report = reports.value.find { it.id == reportId } ?: return@launch
            val updatedPaths = report.fileUri
                ?.split(',')
                ?.filter { it.isNotBlank() && it != pathToDelete }
                ?.joinToString(",")
                ?: ""

            repository.updateReport(report.copy(fileUri = updatedPaths))
            try { File(pathToDelete).delete() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setTargetPrescriptionForUpload(prescriptionId: String) { _targetPrescriptionIdForUpload.value = prescriptionId }
    fun setTargetReportForUpload(reportId: String) { _targetReportIdForUpload.value = reportId }

    fun updatePrescriptionWithImages(pickedImageUris: List<Uri>) {
        viewModelScope.launch {
            val targetId = _targetPrescriptionIdForUpload.value ?: return@launch
            val currentPrescription = prescriptions.value.find { it.id == targetId } ?: return@launch

            val existingPaths = currentPrescription.imageUri?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            val newPaths = existingPaths.toMutableList()

            for (uri in pickedImageUris) {
                val imagePath = saveFileToInternalStorage(getApplication(), uri, "prescription", targetId)
                imagePath?.let { newPaths.add(it) }
            }

            val updatedPrescription = currentPrescription.copy(imageUri = newPaths.joinToString(","))
            repository.updatePrescription(updatedPrescription)
            _targetPrescriptionIdForUpload.value = null
        }
    }

    fun updateReportWithFiles(pickedFileUris: List<Uri>) {
        viewModelScope.launch {
            val targetId = _targetReportIdForUpload.value ?: return@launch
            val currentReport = reports.value.find { it.id == targetId } ?: return@launch

            val existingPaths = currentReport.fileUri?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            val newPaths = existingPaths.toMutableList()

            for (uri in pickedFileUris) {
                val filePath = saveFileToInternalStorage(getApplication(), uri, "report", targetId)
                filePath?.let { newPaths.add(it) }
            }

            val updatedReport = currentReport.copy(fileUri = newPaths.joinToString(","))
            repository.updateReport(updatedReport)
            _targetReportIdForUpload.value = null
        }
    }

    // --- Restored Notes Functions ---
    fun updatePrescriptionNotes(prescriptionId: String, newNotes: String) {
        viewModelScope.launch {
            val prescription = prescriptions.value.find { it.id == prescriptionId } ?: return@launch
            repository.updatePrescription(prescription.copy(notes = newNotes.ifBlank { null }))
        }
    }

    fun updateReportNotes(reportId: String, newNotes: String) {
        viewModelScope.launch {
            val report = reports.value.find { it.id == reportId } ?: return@launch
            repository.updateReport(report.copy(notes = newNotes.ifBlank { null }))
        }
    }

    // --- Dialog Controls ---
    fun onAddPrescriptionClicked() { _showAddPrescriptionDialog.value = true }
    fun onAddReportClicked() { _showAddReportDialog.value = true }
    fun onEditPrescriptionClicked(prescription: Prescription) { _editingPrescription.value = prescription }
    fun onEditReportClicked(report: Report) { _editingReport.value = report }
    fun onDismissDialogs() {
        _showAddPrescriptionDialog.value = false
        _showAddReportDialog.value = false
        _editingPrescription.value = null
        _editingReport.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return MemberDetailsViewModel(application, (application as MyPrescriptionApplication).repository) as T
            }
        }
    }
}