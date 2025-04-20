package com.example.finalyearproject.ui.screens

import android.content.Context
import android.net.Uri
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
fun leave(navController: NavController) {
    val context = LocalContext.current
    var reason by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }

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
                Text("Submit Leave Request", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Leave Reason") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedUri == null) {
                    Button(onClick = { launcher.launch("application/pdf") }) {
                        Text("Attach Leave Document")
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
                        if (userId != null && reason.isNotBlank() && selectedUri != null) {
                            isUploading = true
                            uploadProgress = 0

                            uploadLeaveDocument(
                                context = context,
                                uri = selectedUri!!,
                                fileName = "Leave_${reason}.pdf",
                                onProgress = { progress -> uploadProgress = progress },
                                onSuccess = { fileUrl ->
                                    saveLeaveRequestToFirebase(reason, fileUrl, userId, context)
                                    reason = ""
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
                    },
                    enabled = !isUploading
                ) {
                    Text(if (isUploading) "Submitting..." else "Submit Leave")
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
// Upload to Google Drive using Service Account
fun uploadLeaveDocument(
    context: Context,
    uri: Uri,
    fileName: String,
    onProgress: (Int) -> Unit,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit
) {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return onFailure()
    val tempFile = java.io.File.createTempFile("leave_doc_", ".pdf", context.cacheDir)

    // Fix: Use tempFile directly for output stream
    FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }

    try {
        val driveService = GoogleDriveServiceHelper.getDriveService(context)
        if (driveService == null) {
            throw Exception("Drive service is not initialized")
        }

        val metadata = File().apply {
            name = fileName
            parents = listOf("1fNpf2OZfPDghVX42XlD0iUdDVfphSNYL") // Your folder ID
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

// Save leave reason + file URL to Firebase
fun saveLeaveRequestToFirebase(reason: String, fileUrl: String, userId: String, context: Context) {
    val database = FirebaseDatabase.getInstance().reference
    val leaveRef = database.child("leaveRequests").child(userId).push()

    val data = mapOf(
        "reason" to reason,
        "documentUrl" to fileUrl,
        "timestamp" to System.currentTimeMillis()
    )

    leaveRef.setValue(data)
        .addOnSuccessListener {
            Toast.makeText(context, "Leave submitted successfully!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { exception ->
            exception.printStackTrace() // Log the error
            Toast.makeText(context, "Failed to save leave data: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}


