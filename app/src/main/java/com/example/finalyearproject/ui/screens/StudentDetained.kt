package com.example.finalyearproject.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Detained(navController: NavController) {
    val context = LocalContext.current
    val usersRef = FirebaseDatabase.getInstance().getReference("users")
    val students = remember { mutableStateListOf<Student>() }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                students.clear()
                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key ?: continue
                    val name = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val detained = userSnapshot.child("detained").getValue(Boolean::class.java) ?: false
                    val auid = userSnapshot.child("auid").getValue(String::class.java) ?: "N/A"
                    students.add(Student(uid, name, auid, detained))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error loading students", Toast.LENGTH_SHORT).show()
            }
        })
    }

    val filteredStudents = students.filter {
        it.auid.contains(searchQuery.trim(), ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Manage Detained Students") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by AUID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(filteredStudents) { student ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "AUID: ${student.auid}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = if (student.detained) "Status: Detained" else "Status: Free",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (student.detained) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            Switch(
                                checked = student.detained,
                                onCheckedChange = { newStatus ->
                                    usersRef.child(student.uid).child("detained").setValue(newStatus)
                                    val message = if (newStatus) {
                                        "${student.name} marked as detained"
                                    } else {
                                        "${student.name} removed from detained list"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.error,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

data class Student(
    val uid: String,
    val name: String,
    val auid: String,
    val detained: Boolean
)
