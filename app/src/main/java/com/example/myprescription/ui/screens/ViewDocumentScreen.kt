package com.example.myprescription.ui.screens

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Notes
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.myprescription.ViewModel.MemberDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewDocumentScreen(
    documentId: String,
    documentUriString: String,
    documentType: String,
    documentTitle: String,
    memberDetailsViewModel: MemberDetailsViewModel = viewModel(factory = MemberDetailsViewModel.Factory),
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFilePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isInSelectionMode = selectedFilePaths.isNotEmpty()

    val latestDocument = when (documentType) {
        "prescription" -> memberDetailsViewModel.prescriptions.collectAsState().value.find { it.id == documentId }?.imageUri
        "report" -> memberDetailsViewModel.reports.collectAsState().value.find { it.id == documentId }?.fileUri
        else -> ""
    } ?: ""

    val filePaths = remember(latestDocument) {
        latestDocument.split(',').filter { it.isNotBlank() }
    }

    var fileToDelete by remember { mutableStateOf<String?>(null) }

    val initialNotes = remember(documentId, documentType) {
        when (documentType) {
            "prescription" -> memberDetailsViewModel.prescriptions.value.find { it.id == documentId }?.notes
            "report" -> memberDetailsViewModel.reports.value.find { it.id == documentId }?.notes
            else -> null
        } ?: ""
    }
    var notesText by remember(initialNotes) { mutableStateOf(initialNotes) }

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

    fun downloadFilesAsZip(paths: List<String>) {
        coroutineScope.launch {
            if (paths.isEmpty()) {
                Toast.makeText(context, "No files to download.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val zipFileName = "${documentTitle.replace(" ", "_")}_${System.currentTimeMillis()}"
                val tempZipFile = File(context.cacheDir, "$zipFileName.zip")

                withContext(Dispatchers.IO) {
                    ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZipFile))).use { zos ->
                        for (path in paths) {
                            val file = File(path)
                            if (file.exists()) {
                                FileInputStream(file).use { fis ->
                                    val zipEntry = ZipEntry(file.name)
                                    zos.putNextEntry(zipEntry)
                                    fis.copyTo(zos)
                                    zos.closeEntry()
                                }
                            }
                        }
                    }

                    val resolver = context.contentResolver
                    val downloadsCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else { return@withContext }

                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "$zipFileName.zip")
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val uri = resolver.insert(downloadsCollection, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            FileInputStream(tempZipFile).use { fis -> fis.copyTo(os) }
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                }
                Toast.makeText(context, "Saved to Downloads as $zipFileName.zip", Toast.LENGTH_LONG).show()
                tempZipFile.delete()
                selectedFilePaths = emptySet()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error creating ZIP file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (isInSelectionMode) Text("${selectedFilePaths.size} selected")
                    else Text(documentTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        IconButton(onClick = { downloadFilesAsZip(selectedFilePaths.toList()) }) {
                            Icon(Icons.Default.Download, "Download Selected")
                        }
                        IconButton(onClick = { shareFiles(selectedFilePaths.toList()) }) {
                            Icon(Icons.Default.Share, "Share Selected")
                        }
                    } else if (filePaths.isNotEmpty()) {
                        IconButton(onClick = { downloadFilesAsZip(filePaths) }) {
                            Icon(Icons.Default.Download, "Download All")
                        }
                        IconButton(onClick = { shareFiles(filePaths) }) {
                            Icon(Icons.Default.Share, "Share All")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (isInSelectionMode) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (filePaths.isEmpty()) {
                EmptyStateView("No files or images have been added yet.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filePaths) { path ->
                        DocumentItemCard(
                            filePath = path,
                            documentType = documentType,
                            isSelected = path in selectedFilePaths,
                            isInSelectionMode = isInSelectionMode,
                            onItemClick = { selectedFilePaths = if (path in selectedFilePaths) selectedFilePaths - path else selectedFilePaths + path },
                            onItemLongClick = { selectedFilePaths += path },
                            onDeleteClick = { fileToDelete = path }
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = notesText, onValueChange = { notesText = it }, label = { Text("Add or edit notes here...") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    if (documentType == "prescription") memberDetailsViewModel.updatePrescriptionNotes(documentId, notesText)
                    else if (documentType == "report") memberDetailsViewModel.updateReportNotes(documentId, notesText)
                    Toast.makeText(context, "Notes Saved!", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.align(Alignment.End)
                ) { Text("Save Notes") }
            }
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
    val context = LocalContext.current
    val file = remember { File(filePath) }

    val viewFile: () -> Unit = {
        try {
            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open with..."))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = { if (isInSelectionMode) onItemClick() else viewFile() },
                    onLongClick = { onItemLongClick() }
                )
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(contentAlignment = Alignment.Center) {
                if (documentType == "prescription") {
                    Image(rememberAsyncImagePainter(model = file), file.name, contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    Icon(Icons.AutoMirrored.Filled.InsertDriveFile, "File Icon", modifier = Modifier.size(60.dp))
                }
                if (isSelected) {
                    Box(modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
                    Icon(Icons.Filled.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Text(file.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.DeleteOutline, "Delete File", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}