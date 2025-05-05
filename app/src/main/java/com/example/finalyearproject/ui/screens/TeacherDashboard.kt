package com.example.finalyearproject.ui.screens

import TeacherMessage
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.finalyearproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherDashboard(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    // State variables
    val name = remember { mutableStateOf("") }
    val phone = remember { mutableStateOf("") }
    val post = remember { mutableStateOf("") }
    val loading = remember { mutableStateOf(true) }
    val showLogoutDialog = remember { mutableStateOf(false) }
    val showPostDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val newMessage = remember { mutableStateOf("") }
    val teacherMessages = remember { mutableStateListOf<TeacherMessage>() }
    val selectedMessageId = remember { mutableStateOf<String?>(null) }

    val backgroundGradient = Brush.verticalGradient(
        listOf(Color(0xFFFDEBEB), Color(0xFFE7F0FD))
    )

    val uid = auth.currentUser?.uid
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        if (uid != null) {
            // Fetch teacher info
            database.child("teachers").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    name.value = snapshot.child("name").getValue(String::class.java) ?: ""
                    phone.value = snapshot.child("phone").value?.toString() ?: ""
                    post.value = snapshot.child("post").getValue(String::class.java) ?: ""
                    loading.value = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
                    loading.value = false
                }
            })

            // Fetch teacher messages with IDs
            database.child("messages").orderByChild("date").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    teacherMessages.clear()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(TeacherMessage::class.java)
                        message?.let {
                            teacherMessages.add(it.copy(id = messageSnapshot.key))
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TeacherDashboard", "Failed to load messages: ${error.message}")
                }
            })
        } else {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            loading.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .padding(16.dp)
    ) {
        if (loading.value) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFBA68C8)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with logout button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { showLogoutDialog.value = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logout),
                            contentDescription = "Logout",
                            tint = Color(0xFFBA68C8)
                        )
                    }
                }

                // Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome, ${name.value}!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7E57C2)
                        )
                        Text(
                            text = "“Believe in yourself and all that you are.”",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.profile),
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            )

                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Name: ${name.value}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Phone: ${phone.value}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Post: ${post.value}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Notifications Section
                Text(
                    text = "Notifications",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7E57C2)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Messages List
                val sortedMessages = teacherMessages.sortedByDescending {
                    try {
                        dateFormat.parse(it.date)
                    } catch (e: Exception) {
                        null
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedMessages) { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = msg.content, fontSize = 16.sp, color = Color.Black)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Date: ${msg.date}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                // Delete Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedMessageId.value = msg.id
                                            showDeleteDialog.value = true
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = Color.Red
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button for New Post
        FloatingActionButton(
            onClick = { showPostDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp) // Changed from 16.dp to 72.dp
                .offset(y = (-16).dp),   // Added offset to move up
            containerColor = Color(0xFF7E57C2),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.add),
                contentDescription = "Add Notification",
                tint = Color.White
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween, // Changed from SpaceBetween
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Composable
            fun navItem(icon: Int, label: String, onClick: () -> Unit) {
                Column(
                    modifier = Modifier
                        .clickable(onClick = onClick)
                        .width(80.dp), // Added fixed width for equal spacing
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = label,
                        tint = Color(0xFF7E57C2),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = Color(0xFF7E57C2),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center // Added for better text alignment
                    )
                }
            }

            navItem(R.drawable.certificate, "Certs") { navController.navigate("TeacherCertificates") }
            navItem(R.drawable.timetable, "Timetable") { navController.navigate("timetable") }
            navItem(R.drawable.eca, "StudentDetained") { navController.navigate("Detained") }
        }

        // Logout Dialog
        if (showLogoutDialog.value) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog.value = false },
                confirmButton = {
                    TextButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("TeacherDashboard") { inclusive = true }
                        }
                    }) {
                        Text("Yes", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog.value = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") }
            )
        }

        // Post New Notification Dialog
        if (showPostDialog.value) {
            AlertDialog(
                onDismissRequest = { showPostDialog.value = false },
                confirmButton = {
                    TextButton(onClick = {
                        if (newMessage.value.isNotBlank()) {
                            val currentDate = dateFormat.format(Date())
                            val message = TeacherMessage(
                                content = newMessage.value,
                                date = currentDate
                            )

                            database.child("messages").push().setValue(message)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Notification posted!", Toast.LENGTH_SHORT).show()
                                    newMessage.value = ""
                                    showPostDialog.value = false
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to post: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Post", color = Color(0xFF7E57C2))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPostDialog.value = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("New Notification") },
                text = {
                    OutlinedTextField(
                        value = newMessage.value,
                        onValueChange = { newMessage.value = it },
                        label = { Text("Enter notification content") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog.value) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog.value = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedMessageId.value?.let { messageId ->
                                database.child("messages").child(messageId).removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            showDeleteDialog.value = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog.value = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Delete Message") },
                text = { Text("Are you sure you want to delete this message?") }
            )
        }
    }
}