package com.example.myprescription.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myprescription.R
import com.example.myprescription.ViewModel.AuthViewModel
import com.example.myprescription.model.Member
import com.example.myprescription.ViewModel.FamilyViewModel
import com.example.myprescription.ui.components.AppMenuTray
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMembersScreen(
    familyViewModel: FamilyViewModel = viewModel(factory = FamilyViewModel.Factory),
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToMemberDetails: (memberId: String, memberName: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onChangeAccountClick: () -> Unit
) {
    val members by familyViewModel.members.collectAsState()
    val showDialog by familyViewModel.showAddMemberDialog.collectAsState()
    val editingMember by familyViewModel.editingMember.collectAsState()
    var memberToDelete by remember { mutableStateOf<Member?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val user by authViewModel.user.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppMenuTray(
                user = user,
                onChangeAccountClick = {
                    scope.launch { drawerState.close() }
                    onChangeAccountClick()
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
                onHelpClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToHelp()
                },
                onAboutClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToAbout()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Family Members", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                    onClick = { familyViewModel.onAddMemberClicked() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Add Member") },
                    text = { Text("Add Member") }
                )
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                if (members.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No family members added yet.\nClick 'Add Member' to get started.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(members, key = { it.id }) { member ->
                            MemberCard(
                                member = member,
                                onCardClick = { onNavigateToMemberDetails(member.id, member.name) },
                                onEditClick = { familyViewModel.onEditMemberClicked(member) },
                                onDeleteClick = { memberToDelete = member }
                            )
                        }
                    }
                }

                if (showDialog) {
                    AddEditMemberDialog(
                        memberToEdit = editingMember,
                        onDismiss = { familyViewModel.onDismissDialog() },
                        onConfirm = { memberData, profilePhotoUri ->
                            if (editingMember == null) {
                                familyViewModel.addMember(memberData, profilePhotoUri)
                            } else {
                                familyViewModel.updateMember(memberData, profilePhotoUri)
                            }
                        }
                    )
                }

                if (memberToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { memberToDelete = null },
                        title = { Text("Delete ${memberToDelete!!.name}?") },
                        text = { Text("Are you sure? All of this member's prescriptions and reports will be permanently deleted.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    familyViewModel.deleteMember(memberToDelete!!)
                                    memberToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { memberToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun MemberCard(
    member: Member,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageModel: Any? = member.profileImageUri?.let {
                if (it.startsWith("content://")) Uri.parse(it) else File(it)
            }

            if (imageModel != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(imageModel)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .build()
                    ),
                    contentDescription = "${member.name}'s profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Member Icon",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = member.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${member.relation} • ${member.age} years • ${member.gender}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Member", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Member", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberDialog(
    memberToEdit: Member?,
    onDismiss: () -> Unit,
    onConfirm: (Member, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(memberToEdit?.name ?: "") }
    var ageString by remember { mutableStateOf(memberToEdit?.age?.toString() ?: "") }
    var relation by remember { mutableStateOf(memberToEdit?.relation ?: "") }
    var gender by remember { mutableStateOf(memberToEdit?.gender ?: "Male") }
    var tempProfileImageUri by remember { mutableStateOf<Uri?>(null) }
    val displayImage: Any? = tempProfileImageUri ?: memberToEdit?.profileImageUri?.let {
        if (it.startsWith("content://")) Uri.parse(it) else File(it)
    }

    val genderOptions = listOf("Male", "Female", "Other")
    var expandedGenderDropdown by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var relationError by remember { mutableStateOf<String?>(null) }

    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        tempProfileImageUri = uri
    }

    fun validateAndSave() {
        nameError = if (name.isBlank()) "Name cannot be empty" else null
        val ageNum = ageString.toIntOrNull()
        ageError = when {
            ageString.isBlank() -> "Age cannot be empty"
            ageNum == null -> "Invalid age"
            ageNum <= 0 || ageNum > 120 -> "Age must be between 1 and 120"
            else -> null
        }
        relationError = if (relation.isBlank()) "Relation cannot be empty" else null

        if (nameError == null && ageError == null && relationError == null) {
            val memberData = Member(
                id = memberToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                name = name.trim(),
                age = ageString.toInt(),
                relation = relation.trim(),
                gender = gender,
                profileImageUri = memberToEdit?.profileImageUri
            )
            onConfirm(memberData, tempProfileImageUri)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = if (memberToEdit == null) "Add New Member" else "Edit Member",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    IconButton(
                        onClick = { validateAndSave() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Member", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(bottom = 8.dp)) {
                        if (displayImage != null) {
                            Image(
                                painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(displayImage).build()),
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(100.dp).clip(CircleShape).clickable { profilePhotoPickerLauncher.launch("image/*") }
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).clickable { profilePhotoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AddAPhoto, "Add Profile Photo", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                    TextButton(onClick = { profilePhotoPickerLauncher.launch("image/*") }) {
                        Text(if (displayImage != null) "Change Photo" else "Add Photo")
                    }

                    OutlinedTextField(value = name, onValueChange = { name = it; nameError = null }, label = { Text("Full Name") }, isError = nameError != null, supportingText = { if (nameError != null) Text(nameError!!) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ageString, onValueChange = { ageString = it.filter { char -> char.isDigit() }; ageError = null }, label = { Text("Age") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number), isError = ageError != null, supportingText = { if (ageError != null) Text(ageError!!) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = relation, onValueChange = { relation = it; relationError = null }, label = { Text("Relation (e.g., Self, Spouse)") }, isError = relationError != null, supportingText = { if (relationError != null) Text(relationError!!) }, modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(expanded = expandedGenderDropdown, onExpandedChange = { expandedGenderDropdown = !expandedGenderDropdown }, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = gender, onValueChange = {}, readOnly = true, label = { Text("Gender") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGenderDropdown) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expandedGenderDropdown, onDismissRequest = { expandedGenderDropdown = false }) {
                            genderOptions.forEach { selectionOption ->
                                DropdownMenuItem(text = { Text(selectionOption) }, onClick = {
                                    gender = selectionOption
                                    expandedGenderDropdown = false
                                })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}