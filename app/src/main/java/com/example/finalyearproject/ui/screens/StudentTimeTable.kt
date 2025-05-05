@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.finalyearproject.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.example.finalyearproject.data.ExamData

@Composable
fun StudentTimeTable(navController: NavController) {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().getReference("exam_timetable")

    // State to hold the exam timetable data
    var allExamSets by remember { mutableStateOf<Map<String, List<ExamData>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch the timetable data from Firebase
    LaunchedEffect(Unit) {
        // Listen for changes to the timetable data
        database.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val examMap = snapshot.children.mapNotNull { examSnapshot ->
                    val examTitle = examSnapshot.key ?: return@mapNotNull null
                    val subjects = examSnapshot.children.mapNotNull { subjectSnapshot ->
                        subjectSnapshot.getValue<ExamData>()?.copy(id = subjectSnapshot.key ?: "")
                    }
                    examTitle to subjects
                }.toMap()

                allExamSets = examMap
            } else {
                errorMessage = "No exam data found!"
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
            errorMessage = "Failed to load timetable"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Student Timetable", style = typography.titleLarge) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                // Loading indicator while fetching data
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (!errorMessage.isNullOrEmpty()) {
                // Show error message if there was an issue
                Text(
                    text = errorMessage!!,
                    style = typography.bodyLarge.copy(color = Color.Red),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (allExamSets.isEmpty()) {
                // Show when no exams are available
                Text("No exams scheduled", style = typography.bodyLarge, textAlign = TextAlign.Center)
            } else {
                // Display the exam timetable
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(allExamSets.toList()) { (examTitle, exams) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F5)),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = examTitle,
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                exams.forEachIndexed { index, exam ->
                                    ExamCard(exam = exam, isLastItem = index == exams.size - 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamCard(exam: ExamData, isLastItem: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLastItem) 0.dp else 12.dp), // Add bottom padding except for the last item
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // This will add space between each subject inside the card
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("📅 ${exam.date}", style = typography.bodyMedium)
                    Text("⏰ ${exam.time}", style = typography.bodySmall)
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = exam.subject,
                        style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
