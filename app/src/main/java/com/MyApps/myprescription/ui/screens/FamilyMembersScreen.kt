package com.MyApps.myprescription.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.MyApps.myprescription.R
import com.MyApps.myprescription.ViewModel.AuthViewModel
import com.MyApps.myprescription.ViewModel.FamilyViewModel
import com.MyApps.myprescription.model.Member
import com.MyApps.myprescription.ui.components.AppMenuTray
import com.MyApps.myprescription.ui.components.ShimmerEffect
import com.MyApps.myprescription.util.Prefs
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMembersScreen(
    familyViewModel: FamilyViewModel,
    authViewModel: AuthViewModel,
    onNavigateToMemberDetails: (memberId: String, memberName: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onChangeAccountClick: () -> Unit
) {
    val members by familyViewModel.members.collectAsState()
    val isLoading by familyViewModel.isLoading.collectAsState()
    val showDialog by familyViewModel.showAddMemberDialog.collectAsState()
    val editingMember by familyViewModel.editingMember.collectAsState()
    var memberToDelete by remember { mutableStateOf<Member?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val user by authViewModel.user.collectAsState()
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    LaunchedEffect(Unit) {
        if (prefs.isFirstRun()) {
            familyViewModel.restoreBackupFromDrive()
            prefs.setFirstRun(false)
        }
    }

    if (drawerState.isOpen) {
        BackHandler {
            scope.launch {
                drawerState.close()
            }
        }
    }

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
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Family") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { familyViewModel.onAddMemberClicked() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Member")
                }
            }
        ) { paddingValues ->
            when {
                isLoading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding(), bottom = 80.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(5) { ShimmerEffect() }
                    }
                }
                members.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { memberToDelete = null }) { Text("Cancel") }
                    }
                )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCardClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageModel: Any? = member.profileImageUri?.let {
                if (it.startsWith("content://")) Uri.parse(it) else File(it)
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(imageModel)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "${member.name}'s profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = "Default Profile Icon",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${member.relation} • ${member.age} yrs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Member", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete Member", tint = MaterialTheme.colorScheme.error)
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
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (memberToEdit == null) "Add New Member" else "Edit Member",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.clickable { profilePhotoPickerLauncher.launch("image/*") }
                    ) {
                        if (displayImage != null) {
                            Image(
                                painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(displayImage).build()),
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AddAPhoto, "Add Profile Photo", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(40.dp))
                            }
                        }
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { validateAndSave() }) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Save")
                    }
                }
            }
        }
    }
}
@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.app_logo_foreground),
            contentDescription = "Empty State Illustration",
            modifier = Modifier.size(150.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your Family Circle",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add family members to start managing their prescriptions and health reports all in one place.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}