package com.example.finalyearproject.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.finalyearproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val isPasswordVisible = remember { mutableStateOf(false) }
    val focusedColor = remember { mutableStateOf(Color(0xFF9575CD)) }
    val isLoading = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFDEBEB), Color(0xFFE7F0FD))
    )

    val cardColor = Color.White.copy(alpha = 0.95f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.splashscreen_logo),
                contentDescription = "Splash Screen Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(275.dp)
                    .padding(top = 50.dp, bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                focusedColor.value,
                                focusedColor.value.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(2.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = 0.98f
                            shadowElevation = 16f
                        },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StyledInput("Email", email, Color(0xFF81C784), focusedColor)

                        Spacer(modifier = Modifier.height(16.dp))

                        StyledPasswordInput("Password", password, Color(0xFF9575CD), focusedColor, isPasswordVisible)

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                isLoading.value = true
                                coroutineScope.launch {
                                    val auth = FirebaseAuth.getInstance()
                                    val db = FirebaseDatabase.getInstance().reference
                                    auth.signInWithEmailAndPassword(email.value.trim(), password.value.trim())
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val uid = auth.currentUser?.uid
                                                if (uid != null) {
                                                    db.child("users").child(uid).get()
                                                        .addOnSuccessListener { snapshot ->
                                                            if (snapshot.exists()) {
                                                                val isDetained = snapshot.child("detained").getValue(Boolean::class.java) ?: false
                                                                if (isDetained) {
                                                                    auth.signOut()
                                                                    isLoading.value = false
                                                                    Toast.makeText(context, "Access Denied: You are detained.", Toast.LENGTH_LONG).show()
                                                                } else {
                                                                    isLoading.value = false
                                                                    saveFcmToken(uid, false)
                                                                    navController.navigate("StudentDashboard")
                                                                }
                                                            } else {
                                                                db.child("teachers").child(uid).get()
                                                                    .addOnSuccessListener { teacherSnapshot ->
                                                                        if (teacherSnapshot.exists()) {
                                                                            isLoading.value = false
                                                                            saveFcmToken(uid, true)
                                                                            navController.navigate("TeacherDashboard")
                                                                        } else {
                                                                            auth.signOut()
                                                                            isLoading.value = false
                                                                            Toast.makeText(context, "Access Denied (User not found)", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                    .addOnFailureListener { exception ->
                                                                        auth.signOut()
                                                                        isLoading.value = false
                                                                        Toast.makeText(context, "Failed to fetch teacher data: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                                        Log.e("LoginError", "Failed to fetch teacher data", exception)
                                                                    }
                                                            }
                                                        }
                                                        .addOnFailureListener { exception ->
                                                            isLoading.value = false
                                                            Toast.makeText(context, "Failed to fetch user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                            Log.e("LoginError", "Failed to fetch user data", exception)
                                                        }
                                                } else {
                                                    isLoading.value = false
                                                    Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                isLoading.value = false
                                                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = focusedColor.value)
                        ) {
                            Text("Login", color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Don't have an account? Sign Up",
                            color = focusedColor.value,
                            modifier = Modifier.clickable {
                                isLoading.value = true
                                coroutineScope.launch {
                                    isLoading.value = false
                                    navController.navigate("signup")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Forgot Password?",
                            color = focusedColor.value,
                            modifier = Modifier.clickable {
                                isLoading.value = true
                                coroutineScope.launch {
                                    isLoading.value = false
                                    navController.navigate("forgotPassword")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun saveFcmToken(uid: String, isTeacher: Boolean) {
    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
        val userRef = if (isTeacher) {
            FirebaseDatabase.getInstance().getReference("teachers").child(uid)
        } else {
            FirebaseDatabase.getInstance().getReference("users").child(uid)
        }
        userRef.child("fcmToken").setValue(token)
            .addOnSuccessListener {
                Log.d("FCM", "FCM token saved successfully")
            }
            .addOnFailureListener { exception ->
                Log.e("FCM", "Failed to save FCM token: ${exception.message}")
            }
    }
}

@Composable
fun StyledInput(
    label: String,
    state: MutableState<String>,
    glowColor: Color,
    focusedColor: MutableState<Color>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    if (isFocused) {
        focusedColor.value = glowColor
    }

    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = glowColor,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.DarkGray,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.DarkGray,
            cursorColor = glowColor
        ),
        interactionSource = interactionSource
    )
}

@Composable
fun StyledPasswordInput(
    label: String,
    state: MutableState<String>,
    glowColor: Color,
    focusedColor: MutableState<Color>,
    isPasswordVisible: MutableState<Boolean>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    if (isFocused) {
        focusedColor.value = glowColor
    }

    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = { Text(label) },
        visualTransformation = if (isPasswordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isPasswordVisible.value = !isPasswordVisible.value }) {
                Icon(
                    imageVector = if (isPasswordVisible.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isPasswordVisible.value) "Hide password" else "Show password",
                    tint = glowColor
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = glowColor,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.DarkGray,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.DarkGray,
            cursorColor = glowColor
        ),
        interactionSource = interactionSource
    )
}
