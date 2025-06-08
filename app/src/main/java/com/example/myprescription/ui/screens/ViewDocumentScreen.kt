package com.example.myprescription.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myprescription.ViewModel.MemberDetailsViewModel
import com.example.myprescription.util.saveFileToInternalStorage
import java.io.File
import kotlinx.coroutines.launch

// Data class to hold the state for the image viewer
private data class ImageViewerState(val filePaths: List<String>, val initialIndex: Int)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewDocumentScreen(
    memberId: String,
    documentId: String,
    documentUriString: String,
    documentType: String,
    documentTitle: String,
    memberDetailsViewModel: MemberDetailsViewModel = viewModel(factory = MemberDetailsViewModel.Factory),
    onNavigateUp: () -> Unit
) {
    LaunchedEffect(memberId) {
        memberDetailsViewModel.loadMemberData(memberId)
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedFilePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isInSelectionMode = selectedFilePaths.isNotEmpty()
    var imageViewerState by remember { mutableStateOf<ImageViewerState?>(null) }

    val latestDocument by remember(documentType, documentId) {
        derivedStateOf {
            when (documentType) {
                "prescription" -> memberDetailsViewModel.prescriptions.value.find { it.id == documentId }?.imageUri
                "report" -> memberDetailsViewModel.reports.value.find { it.id == documentId }?.fileUri
                else -> ""
            } ?: ""
        }
    }

    val filePaths by remember(latestDocument) {
        derivedStateOf {
            latestDocument.split(',').filter { it.isNotBlank() }
        }
    }

    var fileToDelete by remember { mutableStateOf<String?>(null) }

    val initialNotes by remember(documentId, documentType) {
        derivedStateOf {
            when (documentType) {
                "prescription" -> memberDetailsViewModel.prescriptions.value.find { it.id == documentId }?.notes
                "report" -> memberDetailsViewModel.reports.value.find { it.id == documentId }?.notes
                else -> null
            } ?: ""
        }
    }
    var notesText by remember(initialNotes) { mutableStateOf(initialNotes) }

    val importImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val newPaths = mutableListOf<String>()
            for(uri in uris) {
                val filePath = saveFileToInternalStorage(context, uri, "prescription")
                if(filePath.isNotBlank()) {
                    newPaths.add(filePath)
                }
            }
            if (newPaths.isNotEmpty()) {
                val currentPaths = filePaths
                val allPaths = (currentPaths + newPaths).joinToString(",")
                if (documentType == "prescription") {
                    memberDetailsViewModel.updatePrescriptionImageUris(documentId, allPaths)
                }
                Toast.makeText(context, "${newPaths.size} image(s) imported!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error importing image(s).", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareFiles(paths: List<String>) {
        if (paths.isEmpty()) return

        val urisToShare = ArrayList<Uri>()
        for (path in paths) {
            val file = File(path)
            if (file.exists()) {
                val authority = "${context.packageName}.provider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                urisToShare.add(uri)
            }
        }

        if (urisToShare.isNotEmpty()) {
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisToShare)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Files via..."))
        } else {
            Toast.makeText(context, "No valid files found to share.", Toast.LENGTH_SHORT).show()
        }
        selectedFilePaths = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isInSelectionMode) Text("${selectedFilePaths.size} selected")
                    else Text(documentTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    if (isInSelectionMode) {
                        IconButton(onClick = { selectedFilePaths = emptySet() }) {
                            Icon(Icons.Default.Close, "Clear Selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    val pathsToActOn = if (isInSelectionMode) selectedFilePaths.toList() else filePaths
                    if (pathsToActOn.isNotEmpty()) {
                        IconButton(onClick = { /* TODO: Download Logic */ }) {
                            Icon(Icons.Default.Download, "Download")
                        }
                        IconButton(onClick = { shareFiles(pathsToActOn) }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isInSelectionMode) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                )
            )
        },
        floatingActionButton = {
            if (documentType == "prescription" && !isInSelectionMode) {
                FloatingActionButton(onClick = { importImageLauncher.launch("image/*") }) {
                    Icon(Icons.Filled.AddPhotoAlternate, "Import Image")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (filePaths.isEmpty()) {
                val emptyMessage = if (documentType == "prescription") "No images have been added yet. Tap the '+' button to add one." else "No files have been added yet."
                EmptyStateView(emptyMessage)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filePaths, key = { it }) { path ->
                        DocumentItemCard(
                            filePath = path,
                            documentType = documentType,
                            isSelected = path in selectedFilePaths,
                            isInSelectionMode = isInSelectionMode,
                            onItemClick = {
                                if (isInSelectionMode) {
                                    selectedFilePaths = if (path in selectedFilePaths) selectedFilePaths - path else selectedFilePaths + path
                                } else {
                                    if (documentType == "prescription") {
                                        val clickedIndex = filePaths.indexOf(path)
                                        if (clickedIndex != -1) {
                                            imageViewerState = ImageViewerState(filePaths, clickedIndex)
                                        }
                                    } else {
                                        openFile(context, path)
                                    }
                                }
                            },
                            onItemLongClick = { selectedFilePaths += path },
                            onDeleteClick = { fileToDelete = path }
                        )
                    }
                }
            }

            // Notes Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                    .padding(16.dp)
            ) {
                Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Add or edit notes here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (documentType == "prescription") memberDetailsViewModel.updatePrescriptionNotes(documentId, notesText)
                        else if (documentType == "report") memberDetailsViewModel.updateReportNotes(documentId, notesText)
                        Toast.makeText(context, "Notes Saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Save Notes") }
            }
        }
    }

    AnimatedVisibility(
        visible = imageViewerState != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        imageViewerState?.let { state ->
            ImagePager(
                filePaths = state.filePaths,
                initialIndex = state.initialIndex,
                onDismiss = { imageViewerState = null }
            )
        }
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete File?") },
            text = { Text("Are you sure you want to delete this file permanently?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (documentType == "prescription") memberDetailsViewModel.deleteFileFromPrescription(documentId, fileToDelete!!)
                        else memberDetailsViewModel.deleteFileFromReport(documentId, fileToDelete!!)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { fileToDelete = null }) { Text("Cancel") } }
        )
    }
}

private fun openFile(context: android.content.Context, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) {
        Toast.makeText(context, "File not found.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val authority = "${context.packageName}.provider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val mimeType = context.contentResolver.getType(uri)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open file with..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file. No application found.", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentItemCard(
    filePath: String,
    documentType: String,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val file = remember { File(filePath) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = onItemClick,
                    onLongClick = onItemLongClick
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (documentType == "prescription") {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(file)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.InsertDriveFile,
                        "File Icon",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                    Icon(
                        Icons.Filled.CheckCircle,
                        "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Text(file.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.DeleteOutline, "Delete File", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagePager(
    filePaths: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { filePaths.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 1 // Pre-load adjacent images
            ) { page ->
                val filePath = filePaths.getOrNull(page)
                if (filePath != null) {
                    ZoomableImage(imagePath = filePath)
                }
            }

            // Top aligned close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }

            // Bottom aligned page indicator
            Text(
                text = "${pagerState.currentPage + 1} / ${filePaths.size}",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun ZoomableImage(imagePath: String) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceAtLeast(1f) // Prevent zooming out
        offset += offsetChange
    }

    // Reset the image's state when a new image is composed
    LaunchedEffect(imagePath) {
        scale = 1f
        offset = Offset.Zero
    }

    Image(
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(File(imagePath))
                .crossfade(true)
                .build()
        ),
        contentDescription = "Full screen image",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformableState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
    )
}