package com.example.myprescription.ViewModel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.myprescription.MyPrescriptionApplication
import com.example.myprescription.data.repository.AppRepository
import com.example.myprescription.model.Member
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FamilyViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    val members: StateFlow<List<Member>> = repository.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddMemberDialog = MutableStateFlow(false)
    val showAddMemberDialog: StateFlow<Boolean> = _showAddMemberDialog.asStateFlow()

    private val _editingMember = MutableStateFlow<Member?>(null)
    val editingMember: StateFlow<Member?> = _editingMember.asStateFlow()

    private suspend fun saveImageToInternalStorage(context: Context, uri: Uri, memberId: String): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val fileName = "profile_${memberId}_${System.currentTimeMillis()}.jpg"
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

    fun addMember(memberData: Member, profilePhotoUri: Uri?) {
        viewModelScope.launch {
            var finalMember = memberData
            if (profilePhotoUri != null) {
                val imagePath = saveImageToInternalStorage(getApplication(), profilePhotoUri, memberData.id)
                if (imagePath != null) {
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
            if (profilePhotoUri != null && profilePhotoUri.toString() != memberData.profileImageUri /* Only save if new URI */) {
                val imagePath = saveImageToInternalStorage(getApplication(), profilePhotoUri, memberData.id)
                if (imagePath != null) {
                    // TODO: Optionally delete the old profile image file from internal storage
                    // if (memberData.profileImageUri != null && memberData.profileImageUri != imagePath) { File(memberData.profileImageUri).delete() }
                    finalMember = memberData.copy(profileImageUri = imagePath)
                }
            } else if (profilePhotoUri == null && memberData.profileImageUri != null) {
                // This case handles if the user explicitly wants to remove the photo
                // TODO: Optionally delete the old profile image file from internal storage
                // if (memberData.profileImageUri != null) { File(memberData.profileImageUri).delete() }
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
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return FamilyViewModel(
                    application,
                    (application as MyPrescriptionApplication).repository
                ) as T
            }
        }
    }
}