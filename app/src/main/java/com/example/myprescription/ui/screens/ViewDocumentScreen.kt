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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    val filePaths = remember(documentUriString) {
        documentUriString.split(',').filter { it.isNotBlank() }
    }

    val initialNotes = remember(documentId, documentType) {
        when (documentType) {
            "prescription" -> memberDetailsViewModel.prescriptions.value.find { it.id == documentId }?.notes
            "report" -> memberDetailsViewModel.reports.value.find { it.id == documentId }?.notes
            else -> null
        } ?: ""
    }
    var notesText by remember(initialNotes) { mutableStateOf(initialNotes) }

    fun downloadAllFilesAsZip() {
        coroutineScope.launch {
            if (filePaths.isEmpty()) {
                Toast.makeText(context, "No files to download.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val zipFileName = "${documentTitle.replace(" ", "_")}_${System.currentTimeMillis()}"
                val tempZipFile = File(context.cacheDir, "$zipFileName.zip")

                withContext(Dispatchers.IO) {
                    ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZipFile))).use { zos ->
                        for (path in filePaths) {
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
                    } else {
                        // Fallback for older APIs if needed
                        return@withContext
                    }

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

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error creating ZIP file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(documentTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (filePaths.isNotEmpty()) {
                        IconButton(onClick = { downloadAllFilesAsZip() }) {
                            Icon(Icons.Filled.Download, contentDescription = "Download All as ZIP")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (filePaths.isEmpty()) {
                EmptyStateView("No files or images have been added yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filePaths) { path ->
                        DocumentItemCard(filePath = path, documentType = documentType)
                    }
                }
            }

            // Notes Section
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Add or edit notes here...") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes Icon") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (documentType == "prescription") {
                            memberDetailsViewModel.updatePrescriptionNotes(documentId, notesText)
                        } else if (documentType == "report") {
                            memberDetailsViewModel.updateReportNotes(documentId, notesText)
                        }
                        Toast.makeText(context, "Notes Saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Notes")
                }
            }
        }
    }
}

@Composable
private fun DocumentItemCard(filePath: String, documentType: String) {
    val context = LocalContext.current
    val file = remember { File(filePath) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (documentType == "prescription") {
                Image(
                    painter = rememberAsyncImagePainter(model = file),
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = "File Icon",
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(file.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = {
                try {
                    val authority = "${context.packageName}.provider"
                    val uri: Uri = FileProvider.getUriForFile(context, authority, file)
                    val mimeType = context.contentResolver.getType(uri)

                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(viewIntent, "Open with..."))
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }) {
                Icon(Icons.Filled.Visibility, contentDescription = "View File")
            }
        }
    }
}