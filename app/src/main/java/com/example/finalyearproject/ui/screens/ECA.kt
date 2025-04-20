package com.example.finalyearproject.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ECA(navController: NavController) {
    val context = LocalContext.current
    var courseTitle by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            fileName = getFileNameFromUri(context, it) ?: "Selected Certificate"
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = uploadProgress.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "uploadProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3FF)),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Upload ECA Certificate", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = courseTitle,
                    onValueChange = { courseTitle = it },
                    label = { Text("Certificate Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedUri == null) {
                    Button(onClick = { launcher.launch("application/pdf") }) {
                        Text("Attach ECA Certificate")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE0E0F8), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(fileName, fontSize = 16.sp)
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    selectedUri = null
                                    fileName = ""
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (userId != null && courseTitle.isNotBlank() && selectedUri != null) {
                            isUploading = true
                            coroutineScope.launch {
                                val uploadedUrl = uploadToDriveUsingServiceAccount(
                                    courseTitle, selectedUri!!, context
                                ) { progress ->
                                    uploadProgress = progress
                                }

                                if (uploadedUrl != null) {
                                    saveCertificateLinkToFirebase(courseTitle, uploadedUrl, context)
                                    selectedUri = null
                                    fileName = ""
                                    courseTitle = ""
                                }
                                isUploading = false
                            }
                        } else {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isUploading
                ) {
                    Text(if (isUploading) "Uploading..." else "Upload Certificate")
                }

                if (isUploading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(progress = animatedProgress / 100f)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Uploading: ${animatedProgress.toInt()}%", fontSize = 14.sp)
                }
            }
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

suspend fun uploadToDriveUsingServiceAccount(
    title: String,
    uri: Uri,
    context: Context,
    onProgress: (Int) -> Unit
): String? = withContext(Dispatchers.IO) {
    try {
        val credentialsStream = context.resources.openRawResource(
            context.resources.getIdentifier("drive_credentials", "raw", context.packageName)
        )

        val googleCredential = GoogleCredential.fromStream(credentialsStream)
            .createScoped(listOf("https://www.googleapis.com/auth/drive.file"))

        val driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            googleCredential
        ).setApplicationName("UniVia").build()

        val folderId = "1zBESOEYxnwCBOrjd6V35r4cc60qedbZS"

        // Read the file as ByteArray for accurate size and safe reuse
        val inputBytes = context.contentResolver.openInputStream(uri)?.readBytes()
        val inputStream = inputBytes?.inputStream()
        val fileSize = inputBytes?.size ?: 1

        val fileMetadata = File().apply {
            name = "$title.pdf"
            parents = listOf(folderId)
        }

        val mediaContent = InputStreamContent("application/pdf", inputStream).apply {
            length = fileSize.toLong()
        }

        val progressListener = com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener { uploader ->
            val progress = (uploader.progress * 100).toInt()
            onProgress(progress.coerceIn(0, 100))
        }

        val upload = driveService.files().create(fileMetadata, mediaContent)
        upload.mediaHttpUploader.apply {
            isDirectUploadEnabled = false
            setProgressListener(progressListener)
        }

        val uploadedFile = upload.setFields("id, webViewLink").execute()
        uploadedFile.webViewLink
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        null
    }
}

fun saveCertificateLinkToFirebase(courseTitle: String, driveLink: String, context: Context) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId != null) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("certificates").child(userId).child(courseTitle).setValue(driveLink)
            .addOnSuccessListener {
                Toast.makeText(context, "Certificate uploaded successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save link in Firebase", Toast.LENGTH_SHORT).show()
            }
    }
}
