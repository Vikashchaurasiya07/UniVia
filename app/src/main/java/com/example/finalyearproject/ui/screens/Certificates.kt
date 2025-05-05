package com.example.finalyearproject.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
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
import com.example.finalyearproject.data.GoogleDriveServiceHelper
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import java.io.FileOutputStream
import com.example.finalyearproject.data.getFileNameFromUri

@Composable
fun Certificates(navController: NavController) {
    val context = LocalContext.current
    var certificateName by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    var semester by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var semesterError by remember { mutableStateOf("") }

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val animatedProgress by animateFloatAsState(
        targetValue = uploadProgress.toFloat(),
        animationSpec = tween(durationMillis = 1000)
    )

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            fileName = getFileNameFromUri(context, it) ?: "Selected Document"
        }
    }

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
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3)),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Submit Certificate", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = certificateName,
                    onValueChange = { certificateName = it },
                    label = { Text("Certificate Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = semester,
                    onValueChange = {
                        semester = it
                        semesterError = "" // Clear error when the user is typing
                    },
                    label = { Text("Semester (1-8)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = semesterError.isNotEmpty()
                )
                if (semesterError.isNotEmpty()) {
                    Text(semesterError, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedUri == null) {
                    Button(onClick = { launcher.launch("application/pdf") }) {
                        Text("Attach Certificate Document")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFE0E0), RoundedCornerShape(10.dp))
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
                        // Semester validation
                        if (semester.toIntOrNull() !in 1..8) {
                            semesterError = "Please enter a valid semester (1-8)"
                        } else if (section.isBlank()) {
                            Toast.makeText(context, "Section is required", Toast.LENGTH_SHORT).show()
                        } else {
                            semesterError = "" // Clear error

                            if (userId != null && certificateName.isNotBlank() && selectedUri != null) {
                                isUploading = true
                                uploadProgress = 0

                                uploadCertificateDocument(
                                    context = context,
                                    uri = selectedUri!!,
                                    fileName = "Certificate_${certificateName}.pdf",
                                    onProgress = { progress -> uploadProgress = progress },
                                    onSuccess = { fileUrl ->
                                        saveCertificateToFirebase(certificateName, fileUrl, userId, semester, section, context)
                                        certificateName = ""
                                        selectedUri = null
                                        fileName = ""
                                        isUploading = false
                                    },
                                    onFailure = {
                                        Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                                        isUploading = false
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isUploading
                ) {
                    Text(if (isUploading) "Submitting..." else "Submit Certificate")
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

// Upload to Google Drive using Service Account
fun uploadCertificateDocument(
    context: Context,
    uri: Uri,
    fileName: String,
    onProgress: (Int) -> Unit,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit
) {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return onFailure()
    val tempFile = java.io.File.createTempFile("cert_doc_", ".pdf", context.cacheDir)
    FileOutputStream(tempFile.absolutePath).use { output -> inputStream.copyTo(output) }

    try {
        val driveService = GoogleDriveServiceHelper.getDriveService(context)
        if (driveService == null) {
            throw Exception("Drive service is not initialized")
        }

        val metadata = File().apply {
            name = fileName
            parents = listOf("1mF3Fs8wm3K_GCwNgdAKbyuZVGP4bWtsZ") // Your folder ID
        }

        val mediaContent = FileContent("application/pdf", tempFile)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = driveService.files().create(metadata, mediaContent)
                    .setFields("id")
                    .execute()

                for (i in 1..100 step 10) {
                    delay(150)
                    withContext(Dispatchers.Main) { onProgress(i) }
                }

                val fileId = file.id
                val fileUrl = "https://drive.google.com/file/d/$fileId/view?usp=sharing"

                withContext(Dispatchers.Main) { onSuccess(fileUrl) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onFailure() }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error initializing Google Drive: ${e.message}", Toast.LENGTH_LONG).show()
        onFailure()
    }
}

fun saveCertificateToFirebase(
    courseTitle: String,
    driveLink: String,
    userId: String,
    semester: String,
    section: String,
    context: Context
) {
    val db = FirebaseDatabase.getInstance().reference
    val certificateData = mapOf(
        "certificateName" to courseTitle,
        "documentUrl" to driveLink,
        "semester" to semester,
        "section" to section,
        "timestamp" to System.currentTimeMillis()
    )

    // Save to the correct path: certificates/{userId}/course/
    db.child("certificates").child(userId).child("course").push()
        .setValue(certificateData)
        .addOnSuccessListener {
            Toast.makeText(context, "Course certificate uploaded successfully", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to upload course certificate: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("COURSE_CERT", "Failed to upload certificate", e)
        }
}
