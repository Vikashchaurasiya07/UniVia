@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.finalyearproject.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.*

data class CertificateItem(
    val certificateName: String,
    val documentUrl: String,
    val timestamp: Long,
    val semester: String,
    val section: String,
    val studentName: String = "Unknown" // Added student name
)

@Composable
fun TeacherCertificateViewer(navController: NavController) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf<String?>(null) }
    var selectedSemester by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf<String?>(null) }

    // Changed to match your database format
    val certificateTypes = listOf("eca", "leave", "course")
    val semesters = listOf("1", "2", "3", "4", "5", "6", "7", "8") // Numbers only
    val sections = listOf("A", "B", "C", "D", "E")

    var allCertificates by remember { mutableStateOf<List<CertificateItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Certificates") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (selectedType == null) {
                // Certificate type selection
                Text("Select Certificate Type", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                certificateTypes.forEach { type ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                selectedType = type
                                errorMessage = null
                            },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(type.uppercase())
                        }
                    }
                }
            } else {
                // Semester and section selection
                Text("Filter Certificates", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // Semester dropdown
                var semesterExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = semesterExpanded,
                    onExpandedChange = { semesterExpanded = it }
                ) {
                    TextField(
                        value = selectedSemester ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Semester") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(semesterExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = semesterExpanded,
                        onDismissRequest = { semesterExpanded = false }
                    ) {
                        semesters.forEach { semester ->
                            DropdownMenuItem(
                                text = { Text("Semester $semester") },
                                onClick = {
                                    selectedSemester = semester
                                    semesterExpanded = false
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Section dropdown
                var sectionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = sectionExpanded,
                    onExpandedChange = { sectionExpanded = it }
                ) {
                    TextField(
                        value = selectedSection ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Section") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sectionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sectionExpanded,
                        onDismissRequest = { sectionExpanded = false }
                    ) {
                        sections.forEach { section ->
                            DropdownMenuItem(
                                text = { Text("Section $section") },
                                onClick = {
                                    selectedSection = section
                                    sectionExpanded = false
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedSemester != null && selectedSection != null) {
                    LaunchedEffect(selectedType, selectedSemester, selectedSection) {
                        isLoading = true
                        errorMessage = null
                        fetchCertificatesFiltered(
                            selectedType!!,
                            selectedSemester!!,
                            selectedSection!!,
                            onSuccess = { certs ->
                                allCertificates = certs
                                if (certs.isEmpty()) {
                                    errorMessage = "No ${selectedType!!.uppercase()} certificates found for Semester ${selectedSemester}, Section ${selectedSection}"
                                }
                                isLoading = false
                            },
                            onError = { message ->
                                errorMessage = message
                                isLoading = false
                            }
                        )
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        errorMessage?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        if (allCertificates.isNotEmpty()) {
                            LazyColumn {
                                items(allCertificates.sortedByDescending { it.timestamp }) { cert ->
                                    CertificateCard(cert) {
                                        downloadFile(context, cert.documentUrl, cert.certificateName)
                                    }
                                }
                            }
                        }
                    }
                }

                // Back button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        selectedType = null
                        selectedSemester = null
                        selectedSection = null
                        allCertificates = emptyList()
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Types")
                }
            }
        }
    }
}

private fun fetchCertificatesFiltered(
    type: String,
    semester: String,
    section: String,
    onSuccess: (List<CertificateItem>) -> Unit,
    onError: (String) -> Unit
) {
    val certificatesRef = FirebaseDatabase.getInstance().getReference("certificates")
    val usersRef = FirebaseDatabase.getInstance().getReference("users")

    usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(usersSnapshot: DataSnapshot) {
            val userMap = mutableMapOf<String, String>() // userID to name

            // First get all users in the selected section
            for (user in usersSnapshot.children) {
                val userSection = user.child("section").getValue(String::class.java) ?: ""
                if (userSection.equals(section, ignoreCase = true)) {
                    val userId = user.key ?: continue
                    val userName = user.child("name").getValue(String::class.java) ?: "Unknown"
                    userMap[userId] = userName
                }
            }

            if (userMap.isEmpty()) {
                onError("No students found in Section $section")
                return
            }

            certificatesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(certsSnapshot: DataSnapshot) {
                    val result = mutableListOf<CertificateItem>()

                    for ((userId, userName) in userMap) {
                        val typeSnap = certsSnapshot.child(userId).child(type)
                        if (!typeSnap.exists()) continue

                        for (cert in typeSnap.children) {
                            val certSemester = cert.child("semester").getValue(String::class.java) ?: ""
                            if (certSemester != semester) continue

                            val name = cert.child("certificateName").getValue(String::class.java) ?: "Unnamed"
                            val url = cert.child("documentUrl").getValue(String::class.java) ?: continue
                            val timestamp = cert.child("timestamp").getValue(Long::class.java) ?: 0L
                            val certSection = cert.child("section").getValue(String::class.java) ?: section

                            result.add(
                                CertificateItem(
                                    certificateName = name,
                                    documentUrl = url,
                                    timestamp = timestamp,
                                    semester = certSemester,
                                    section = certSection,
                                    studentName = userName
                                )
                            )
                        }
                    }

                    onSuccess(result)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError("Database error: ${error.message}")
                }
            })
        }

        override fun onCancelled(error: DatabaseError) {
            onError("Failed to fetch users: ${error.message}")
        }
    })
}

@Composable
fun CertificateCard(cert: CertificateItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(cert.studentName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(cert.certificateName, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Semester: ${cert.semester} | Section: ${cert.section}")
        }
    }
}

private fun downloadFile(context: Context, url: String, fileName: String) {
    try {
        val directUrl = convertGoogleDriveUrlToDirect(url)
        val request = DownloadManager.Request(Uri.parse(directUrl)).apply {
            setTitle("Downloading $fileName")
            setDescription("Certificate download in progress")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Certificates/${fileName.trim().replace(" ", "_")}.pdf"
            )
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    } catch (e: Exception) {
        Log.e("CertificateDownload", "Error downloading file", e)
    }
}

private fun convertGoogleDriveUrlToDirect(url: String): String {
    val regex = Regex("""https://drive\.google\.com/file/d/([a-zA-Z0-9_-]+)/?.*""")
    val matchResult = regex.find(url)
    val fileId = matchResult?.groups?.get(1)?.value
    return if (fileId != null) {
        "https://drive.google.com/uc?export=download&id=$fileId"
    } else {
        url // fallback, if it's already direct
    }
}
