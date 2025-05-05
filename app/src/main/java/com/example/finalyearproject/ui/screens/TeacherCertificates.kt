@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.finalyearproject.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import java.net.URLEncoder
import java.util.regex.Pattern

data class CertificateItem(
    val certificateName: String,
    val documentUrl: String,
    val timestamp: Long,
    val semester: String,
    val section: String,
    val studentName: String = "Unknown"
)

@Composable
fun TeacherCertificateViewer(navController: NavController) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf<String?>(null) }
    var selectedSemester by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf<String?>(null) }

    val certificateTypes = listOf("eca", "leave", "course")
    val semesters = listOf("1", "2", "3", "4", "5", "6", "7", "8")
    val sections = listOf("A", "B", "C", "D", "E")

    var allCertificates by remember { mutableStateOf<List<CertificateItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Certificates") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (selectedType == null) {
                CertificateTypeSelection(
                    types = certificateTypes,
                    onTypeSelected = { selectedType = it }
                )
            } else {
                CertificateFilterSection(
                    semesters = semesters,
                    sections = sections,
                    selectedSemester = selectedSemester,
                    selectedSection = selectedSection,
                    onSemesterSelected = { selectedSemester = it },
                    onSectionSelected = { selectedSection = it },
                    onBack = { selectedType = null }
                )

                if (selectedSemester != null && selectedSection != null) {
                    CertificateListSection(
                        type = selectedType!!,
                        semester = selectedSemester!!,
                        section = selectedSection!!,
                        onFetchCertificates = { type, semester, section, onSuccess, onError ->
                            fetchCertificatesFiltered(type, semester, section, onSuccess, onError)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificateTypeSelection(
    types: List<String>,
    onTypeSelected: (String) -> Unit
) {
    Column {
        Text("Select Certificate Type", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        types.forEach { type ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onTypeSelected(type) },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(type.uppercase())
                }
            }
        }
    }
}

@Composable
private fun CertificateFilterSection(
    semesters: List<String>,
    sections: List<String>,
    selectedSemester: String?,
    selectedSection: String?,
    onSemesterSelected: (String) -> Unit,
    onSectionSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Column {
        Text("Filter Certificates", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Semester dropdown
        var semesterExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = semesterExpanded,
            onExpandedChange = { semesterExpanded = it }
        ) {
            TextField(
                value = selectedSemester?.let { "Semester $it" } ?: "",
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
                            onSemesterSelected(semester)
                            semesterExpanded = false
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
                value = selectedSection?.let { "Section $it" } ?: "",
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
                            onSectionSelected(section)
                            sectionExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Types")
        }
    }
}

@Composable
private fun CertificateListSection(
    type: String,
    semester: String,
    section: String,
    onFetchCertificates: (
        type: String,
        semester: String,
        section: String,
        onSuccess: (List<CertificateItem>) -> Unit,
        onError: (String) -> Unit
    ) -> Unit
) {
    var certificates by remember { mutableStateOf<List<CertificateItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(type, semester, section) {
        isLoading = true
        errorMessage = null
        onFetchCertificates(type, semester, section,
            { certs ->
                certificates = certs
                if (certs.isEmpty()) {
                    errorMessage = "No ${type.uppercase()} certificates found for Semester $semester, Section $section"
                }
                isLoading = false
            },
            { message ->
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

        if (certificates.isNotEmpty()) {
            LazyColumn {
                items(certificates.sortedByDescending { it.timestamp }) { cert ->
                    CertificateCard(cert)
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
            val userMap = mutableMapOf<String, String>()

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
                            val url = cert.child("documentUrl").getValue(String::class.java) ?: ""
                            val timestamp = cert.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                            result.add(
                                CertificateItem(name, url, timestamp, certSemester, section, userName)
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
fun CertificateCard(cert: CertificateItem) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = cert.certificateName,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Uploaded by: ${cert.studentName}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Semester: ${cert.semester}, Section: ${cert.section}")
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    status = "Opening document..."
                    openDocument(context, cert.documentUrl)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Certificate")
            }

            status?.let {
                Text(text = it, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

fun openDocument(context: Context, url: String) {
    try {
        val uri = Uri.parse(url)

        // Create a basic VIEW intent
        val baseIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        // For Google Drive links, use the web viewer
        if (url.contains("drive.google.com")) {
            val fileId = extractFileIdFromUrl(url)
            if (fileId != null) {
                // Try to open with Drive Intent first
                try {
                    val driveIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://drive.google.com/file/d/$fileId/view")
                        setPackage("com.google.android.apps.docs")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(driveIntent)
                    return
                } catch (e: Exception) {
                    // Fall through to web viewer if Drive app fails
                }

                // Fallback to web viewer
                val webViewerUrl = "https://docs.google.com/viewer?url=${URLEncoder.encode(url, "UTF-8")}"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webViewerUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage(null) // Let user choose browser
                }
                context.startActivity(webIntent)
                return
            }
        }

        // For non-Drive links or if all else fails
        val chooserIntent = Intent.createChooser(baseIntent, "Open document with")
        context.startActivity(chooserIntent)

    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Error: Could not open document. ${e.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun extractFileIdFromUrl(url: String): String? {
    val patterns = listOf(
        Pattern.compile("https://drive\\.google\\.com/file/d/([^/]+)/.*"),
        Pattern.compile("https://drive\\.google\\.com/open\\?id=([^&]+)"),
        Pattern.compile("https://docs\\.google\\.com/document/d/([^/]+)/.*")
    )

    for (pattern in patterns) {
        val matcher = pattern.matcher(url)
        if (matcher.find()) {
            return matcher.group(1)
        }
    }
    return null
}