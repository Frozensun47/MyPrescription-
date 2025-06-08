package com.example.myprescription.ViewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.myprescription.MyPrescriptionApplication
import com.example.myprescription.data.repository.AppRepository
import com.example.myprescription.model.Member
import com.example.myprescription.util.saveFileToInternalStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class FamilyViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    val members: StateFlow<List<Member>> = repository.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddMemberDialog = MutableStateFlow(false)
    val showAddMemberDialog: StateFlow<Boolean> = _showAddMemberDialog.asStateFlow()

    private val _editingMember = MutableStateFlow<Member?>(null)
    val editingMember: StateFlow<Member?> = _editingMember.asStateFlow()

    fun addMember(memberData: Member, profilePhotoUri: Uri?) {
        viewModelScope.launch {
            var finalMember = memberData
            if (profilePhotoUri != null) {
                val imagePath = saveFileToInternalStorage(getApplication(), profilePhotoUri, "profile")
                if (imagePath.isNotBlank()) {
                    finalMember = memberData.copy(profileImageUri = imagePath)
                }
            }
            repository.insertMember(finalMember)
            _showAddMemberDialog.value = false // Close dialog after adding
        }
    }

    fun updateMember(memberData: Member, profilePhotoUri: Uri?) {
        viewModelScope.launch {
            var finalMember = memberData
            // Only save if new URI
            if (profilePhotoUri != null && profilePhotoUri.toString() != memberData.profileImageUri) {
                val imagePath = saveFileToInternalStorage(getApplication(), profilePhotoUri, "profile")
                if (imagePath.isNotBlank()) {
                    finalMember = memberData.copy(profileImageUri = imagePath)
                }
            } else if (profilePhotoUri == null && memberData.profileImageUri != null) {
                finalMember = memberData.copy(profileImageUri = null)
            }
            repository.updateMember(finalMember)
            _editingMember.value = null
            _showAddMemberDialog.value = false // Close dialog after updating
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            if (member.profileImageUri != null) {
                try { File(member.profileImageUri).delete() } catch (e: Exception) { e.printStackTrace() }
            }
            repository.deleteMember(member)
        }
    }

    fun onAddMemberClicked() {
        _editingMember.value = null
        _showAddMemberDialog.value = true
    }

    fun onEditMemberClicked(member: Member) {
        _editingMember.value = member
        _showAddMemberDialog.value = true
    }

    fun onDismissDialog() {
        _showAddMemberDialog.value = false
        _editingMember.value = null
    }


    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MyPrescriptionApplication
                val repository = application.repository ?: throw IllegalStateException("Repository not initialized, user must be logged in.")
                return FamilyViewModel(
                    application,
                    repository
                ) as T
            }
        }
    }
}