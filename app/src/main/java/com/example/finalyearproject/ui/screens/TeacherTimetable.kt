package com.example.finalyearproject.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

data class ExamData(
    val title: String = "",
    val subject: String = "",
    val time: String = "",
    val date: String = "",
    val id: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherTimetable(navController: NavController ) {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().getReference("exam_timetable")

    var selectedDate by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var examTitle by remember { mutableStateOf("") }
    var isExamTitleSet by remember { mutableStateOf(false) }

    val allExamSets = remember { mutableStateMapOf<String, MutableList<ExamData>>() }
    val currentExamList = remember { mutableStateListOf<ExamData>() }
    val showDeleteDialog = remember { mutableStateOf<Pair<String, String>?>(null) }

    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun openDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker() {
        val currentTime = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                startTime = String.format("%02d:%02d", hour, minute)
            },
            currentTime.get(Calendar.HOUR_OF_DAY),
            currentTime.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun uploadTimetable() {
        if (selectedDate.isNotEmpty() && subject.isNotEmpty() && startTime.isNotEmpty()) {
            val uniqueId = database.child(examTitle).push().key ?: UUID.randomUUID().toString()
            val examData = ExamData(examTitle, subject, startTime, selectedDate, uniqueId)
            database.child(examTitle).child(uniqueId).setValue(examData).addOnSuccessListener {
                Toast.makeText(context, "Subject Uploaded", Toast.LENGTH_SHORT).show()
                currentExamList.add(examData)
                subject = ""
                selectedDate = ""
                startTime = ""
            }
        } else {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }

    fun finishTimetable() {
        if (currentExamList.isNotEmpty()) {
            allExamSets[examTitle] = mutableStateListOf<ExamData>().apply { addAll(currentExamList) }
            currentExamList.clear()
            examTitle = ""
            isExamTitleSet = false
            Toast.makeText(context, "Timetable saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No subjects to save", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteSubject(title: String, subjectId: String) {
        // Remove the subject from the database
        database.child(title).child(subjectId).removeValue().addOnSuccessListener {
            // Check if there are any subjects left for this exam title
            database.child(title).get().addOnSuccessListener { dataSnapshot ->
                // If there are no subjects left under this exam title, delete the title itself
                if (!dataSnapshot.hasChildren()) {
                    database.child(title).removeValue().addOnSuccessListener {
                        // Remove the title from the UI state (allExamSets map)
                        allExamSets.remove(title)
                        Toast.makeText(context, "Exam Title Deleted", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to delete exam title", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Error checking subjects for title", Toast.LENGTH_SHORT).show()
            }

            // Update the UI state by removing the deleted subject from the list
            allExamSets[title]?.let { list ->
                val index = list.indexOfFirst { it.id == subjectId }
                if (index != -1) list.removeAt(index)
            }

            // Display success message for subject deletion
            Toast.makeText(context, "Subject deleted", Toast.LENGTH_SHORT).show()
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Exam Timetable", style = typography.titleLarge) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isExamTitleSet) {
                OutlinedTextField(
                    value = examTitle,
                    onValueChange = { examTitle = it },
                    label = { Text("Exam Title (e.g. Internal 1)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (examTitle.isNotEmpty()) isExamTitleSet = true
                        else Toast.makeText(context, "Enter an exam title", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set Title")
                }

            } else {
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    label = { Text("Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDatePicker() },
                    enabled = false,
                    readOnly = true
                )

                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    label = { Text("Start Time") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openTimePicker() },
                    enabled = false,
                    readOnly = true
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { uploadTimetable() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload Subject")
                }

                if (currentExamList.isNotEmpty()) {
                    Divider()
                    Text(
                        text = examTitle,
                        style = typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    currentExamList.forEach { exam ->
                        ExamCard(exam, onDelete = null)
                    }

                    Button(
                        onClick = { finishTimetable() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finish Timetable")
                    }
                }
            }

            if (allExamSets.isNotEmpty()) {
                Divider()
                Text("All Timetables", style = typography.titleLarge)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(allExamSets.toList()) { (title, exams) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F5)),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = title,
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                exams.forEachIndexed { index, exam ->
                                    ExamCard(exam = exam) {
                                        showDeleteDialog.value = title to exam.id
                                    }
                                    if (index != exams.lastIndex) Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        showDeleteDialog.value?.let { (title, subjectId) ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog.value = null },
                confirmButton = {
                    TextButton(onClick = {
                        deleteSubject(title, subjectId)
                        showDeleteDialog.value = null
                    }) {
                        Text("Yes", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog.value = null }) {
                        Text("No")
                    }
                },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete this subject?") }
            )
        }
    }
}

@Composable
fun ExamCard(exam: ExamData, onDelete: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("📅 ${exam.date}", style = typography.bodyMedium)
                Text("⏰ ${exam.time}", style = typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = exam.subject,
                    style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.End
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
