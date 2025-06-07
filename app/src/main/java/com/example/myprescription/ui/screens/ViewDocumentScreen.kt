package com.example.myprescription.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap // Added import
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.myprescription.R
import com.example.myprescription.viewmodel.MemberDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

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
    val displayableFile = remember(documentUriString) { File(documentUriString) }

    val prescriptions by memberDetailsViewModel.prescriptions.collectAsState()
    val reports by memberDetailsViewModel.reports.collectAsState()

    val initialNotes = remember(documentId, documentType, prescriptions, reports) {
        when (documentType) {
            "prescription" -> prescriptions.find { it.id == documentId }?.notes
            "report" -> reports.find { it.id == documentId }?.notes
            else -> null
        } ?: ""
    }
    var notesText by remember(initialNotes) { mutableStateOf(initialNotes) }
    var showSavedMessage by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        val newOffsetX = (offset.x + offsetChange.x * scale)
        val newOffsetY = (offset.y + offsetChange.y * scale)
        offset = Offset(newOffsetX, newOffsetY)
    }

    fun saveImageToGallery(context: Context, imagePath: String, title: String) {
        coroutineScope.launch {
            try {
                val file = File(imagePath)
                if (!file.exists()) {
                    Toast.makeText(context, "Source file not found!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap == null) {
                    Toast.makeText(context, "Failed to decode image for saving", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val resolver = context.contentResolver
                val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$title-${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyPrescriptions")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                var newImageUri: Uri? = null
                withContext(Dispatchers.IO) {
                    resolver.insert(imageCollection, contentValues)?.let { uri ->
                        newImageUri = uri
                        resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                            FileInputStream(file).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                        }
                    } ?: throw Exception("MediaStore insert failed.")
                }
                Toast.makeText(context, "Image saved to Gallery!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(documentTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (documentType == "prescription" && displayableFile.exists()) {
                        IconButton(onClick = {
                            saveImageToGallery(context, displayableFile.absolutePath, documentTitle.replace(" ", "_"))
                        }) {
                            Icon(Icons.Filled.Download, contentDescription = "Download Image")
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .then(
                        if (documentType == "prescription" && displayableFile.exists()) {
                            Modifier.transformable(state = transformableState)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (documentType == "prescription") {
                    if (displayableFile.exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = displayableFile,
                                placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                                error = painterResource(id = R.drawable.ic_launcher_foreground)
                            ),
                            contentDescription = "Document Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center){
                            Icon(Icons.Filled.Warning, contentDescription = "Image not found", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Image not found.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "File type not viewable",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "File preview not available for this type.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Text(
                            "Filename: ${displayableFile.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Notes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it; showSavedMessage = false },
                label = { Text("Add or edit notes here...") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes Icon")},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .defaultMinSize(minHeight = 120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (documentType == "prescription") {
                        memberDetailsViewModel.updatePrescriptionNotes(documentId, notesText)
                    } else if (documentType == "report") {
                        memberDetailsViewModel.updateReportNotes(documentId, notesText)
                    }
                    showSavedMessage = true
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Notes")
            }
            if (showSavedMessage) {
                Text(
                    "Notes saved!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}