package com.example.finalyearproject.ui.screens

import TeacherMessage
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.work.*
import com.example.finalyearproject.R
import com.example.finalyearproject.navigation.NotificationWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import androidx.core.app.NotificationManagerCompat


@Composable
fun StudentDashboard(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference
    val uid = auth.currentUser?.uid

    val name = remember { mutableStateOf("") }
    val auid = remember { mutableStateOf("") }
    val section = remember { mutableStateOf("") }
    val batch = remember { mutableStateOf("") }
    val loading = remember { mutableStateOf(true) }
    val showDialog = remember { mutableStateOf(false) }

    val teacherMessages = remember { mutableStateListOf<TeacherMessage>() }
    val clickedMessages = remember { mutableStateListOf<String>() }

    val sharedPref = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    val hasSeenMessage = sharedPref.getBoolean("message_seen", false)

    fun fetchTeacherMessages() {
        database.child("messages").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                teacherMessages.clear()
                for (messageSnapshot in snapshot.children) {
                    val content = messageSnapshot.child("content").getValue(String::class.java).orEmpty()
                    val date = messageSnapshot.child("date").getValue(String::class.java).orEmpty()
                    if (content.isNotEmpty() && date.isNotEmpty()) {
                        teacherMessages.add(TeacherMessage(content, date))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    LaunchedEffect(Unit) {
        if (uid != null) {
            database.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    name.value = snapshot.child("name").getValue(String::class.java).orEmpty()
                    auid.value = snapshot.child("auid").getValue(String::class.java).orEmpty()
                    section.value = snapshot.child("section").getValue(String::class.java).orEmpty()
                    batch.value = snapshot.child("batch").getValue(String::class.java).orEmpty()
                    loading.value = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error loading user: ${error.message}", Toast.LENGTH_SHORT).show()
                    loading.value = false
                }
            })
        }

        fetchTeacherMessages()

        if (!hasSeenMessage) {
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "TeacherMessageWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        listOf(Color(0xFFFDEBEB), Color(0xFFE7F0FD))
    )

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { showDialog.value = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logout),
                            contentDescription = "Logout",
                            tint = Color(0xFFBA68C8)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
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
                                Text("AUID: ${auid.value}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Batch: ${batch.value}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Section: ${section.value}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Notifications",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7E57C2)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 10.dp)
                ) {
                    items(teacherMessages.sortedByDescending {
                        runCatching { sdf.parse(it.date) }.getOrNull() ?: Date(0)
                    }) { msg ->
                        val isClicked = remember { mutableStateOf(clickedMessages.contains(msg.content)) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isClicked.value) {
                                        isClicked.value = true
                                        clickedMessages.add(msg.content)
                                        sharedPref.edit { putBoolean("message_seen", true) }
                                        NotificationManagerCompat.from(context).cancel(1)
                                        WorkManager.getInstance(context).cancelUniqueWork("TeacherMessageWorker")
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isClicked.value)
                                    Color(0xFFD0F0C0) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = msg.content, fontSize = 16.sp, color = Color.Black)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Date: ${msg.date}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Composable
            fun navItem(icon: Int, label: String, onClick: () -> Unit) {
                Column(
                    modifier = Modifier.clickable(onClick = onClick),
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            navItem(R.drawable.eca, "ECA") { navController.navigate("ECA") }
            navItem(R.drawable.leave, "Leave") { navController.navigate("leave") }
            navItem(R.drawable.certificate, "Certs") { navController.navigate("Certificates") }
            navItem(R.drawable.timetable, "Timetable") { navController.navigate("timetable") }
        }

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                confirmButton = {
                    TextButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("student_dashboard") { inclusive = true }
                        }
                        showDialog.value = false
                    }) {
                        Text("Yes", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") }
            )
        }
    }
}
