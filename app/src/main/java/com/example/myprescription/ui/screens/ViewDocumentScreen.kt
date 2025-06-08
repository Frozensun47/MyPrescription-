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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myprescription.ViewModel.MemberDetailsViewModel
import com.example.myprescription.util.saveFileToInternalStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private data class ImageViewerState(val filePaths: List<String>, val initialIndex: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewDocumentScreen(
    documentId: String,
    documentType: String,
    documentTitle: String,
    memberDetailsViewModel: MemberDetailsViewModel,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedFilePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isInSelectionMode = selectedFilePaths.isNotEmpty()
    var imageViewerState by remember { mutableStateOf<ImageViewerState?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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

    LaunchedEffect(notesText) {
        if (notesText != initialNotes) {
            delay(1000L)
            if (documentType == "prescription") {
                memberDetailsViewModel.updatePrescriptionNotes(documentId, notesText)
            } else if (documentType == "report") {
                memberDetailsViewModel.updateReportNotes(documentId, notesText)
            }
        }
    }

    val importImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val newPaths = uris.mapNotNull { uri ->
                saveFileToInternalStorage(context, uri, "prescription").takeIf { it.isNotBlank() }
            }

            if (newPaths.isNotEmpty()) {
                val allPaths = (filePaths + newPaths).joinToString(",")
                memberDetailsViewModel.updatePrescriptionImageUris(documentId, allPaths)
                Toast.makeText(context, "${newPaths.size} image(s) imported!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error importing image(s).", Toast.LENGTH_SHORT).show()
            }
        }
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
                    if (isInSelectionMode) {
                        IconButton(onClick = { /* Share Logic */ }) { Icon(Icons.Default.Share, "Share") }
                        IconButton(onClick = { /* Download Logic */ }) { Icon(Icons.Default.Download, "Download") }
                        IconButton(onClick = { showDeleteConfirmation = true }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {
                if (filePaths.isEmpty()) {
                    val emptyMessage = if (documentType == "prescription") "No images have been added yet. Tap the '+' button to add one." else "No files have been added yet."
                    EmptyStateView(emptyMessage, modifier = Modifier.weight(1f))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(filePaths, key = { _, path -> path }) { index, path ->
                            DocumentGridItem(
                                filePath = path,
                                documentType = documentType,
                                displayName = if (documentType == "prescription") "Image ${index + 1}" else "File ${index + 1}",
                                isSelected = path in selectedFilePaths,
                                onClick = {
                                    if (isInSelectionMode) {
                                        selectedFilePaths = if (path in selectedFilePaths) selectedFilePaths - path else selectedFilePaths + path
                                    } else {
                                        if (documentType == "prescription") {
                                            imageViewerState = ImageViewerState(filePaths, index)
                                        } else {
                                            openFile(context, path)
                                        }
                                    }
                                },
                                onLongClick = {
                                    selectedFilePaths += path
                                }
                            )
                        }
                    }
                }

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
                            .height(130.dp)
                            .verticalScroll(rememberScrollState()),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            if (documentType == "prescription" && !isInSelectionMode) {
                FloatingActionButton(
                    onClick = { importImageLauncher.launch("image/*") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 180.dp)
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, "Import Image")
                }
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

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete ${selectedFilePaths.size} file(s)?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        memberDetailsViewModel.deleteMultipleFiles(documentId, documentType, selectedFilePaths)
                        selectedFilePaths = emptySet()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
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
private fun DocumentGridItem(
    filePath: String,
    documentType: String,
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(contentAlignment = Alignment.BottomStart) {
            if (documentType == "prescription") {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current).data(File(filePath)).crossfade(true).build()
                    ),
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = "File Icon",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, "Selected", tint = Color.White)
                }
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
    var isZoomed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${filePaths.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !isZoomed,
                beyondBoundsPageCount = 1
            ) { page ->
                val filePath = filePaths.getOrNull(page)
                if (filePath != null) {
                    ZoomableImage(
                        imagePath = filePath,
                        onZoomChanged = { isZoomed = it }
                    )
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                itemsIndexed(filePaths) { index, filePath ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current).data(File(filePath)).crossfade(true).build()
                            ),
                            contentDescription = "Thumbnail ${index + 1}",
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    imagePath: String,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(imagePath) {
        scale = 1f
        offset = Offset.Zero
        onZoomChanged(false)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RectangleShape),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val boxWidth = with(density) { maxWidth.toPx() }
        val boxHeight = with(density) { maxHeight.toPx() }

        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceAtLeast(1f)
            onZoomChanged(newScale > 1f)

            val newOffset = if (newScale > 1f) {
                val maxOffsetX = (boxWidth * (newScale - 1)) / 2f
                val maxOffsetY = (boxHeight * (newScale - 1)) / 2f
                (offset + panChange).let {
                    Offset(
                        x = it.x.coerceIn(-abs(maxOffsetX), abs(maxOffsetX)),
                        y = it.y.coerceIn(-abs(maxOffsetY), abs(maxOffsetY))
                    )
                }
            } else {
                Offset.Zero
            }

            scale = newScale
            offset = newOffset
        }

        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current).data(File(imagePath)).crossfade(true).build()
            ),
            contentDescription = "Full screen image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}