package com.example.finalyearproject.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

@Composable
fun SignupScreen(navController: NavController) {
    val auid = remember { mutableStateOf("") }
    val name = remember { mutableStateOf("") }
    val batch = remember { mutableStateOf("") }
    val sec = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val repeatPassword = remember { mutableStateOf("") }
    val focusedColor = remember { mutableStateOf(Color(0xFFFF6FD8)) }
    val loading = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    val backgroundGradient = Brush.verticalGradient(
        listOf(Color(0xFFFDEBEB), Color(0xFFE7F0FD))
    )

    val cardColor = Color.White.copy(alpha = 0.95f)

    val allFieldsFilled = auid.value.isNotBlank() &&
            name.value.isNotBlank() &&
            batch.value.isNotBlank() &&
            sec.value.isNotBlank() &&
            email.value.isNotBlank() &&
            password.value.isNotBlank() &&
            repeatPassword.value.isNotBlank()

    if (loading.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = focusedColor.value)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.splashscreen_logo),
            contentDescription = "Splash Screen Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
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
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InputField("AUID", auid, Color(0xFFFF6FD8), focusedColor)
                    InputField("Name", name, Color(0xFFBA68C8), focusedColor)
                    InputField("Batch", batch, Color(0xFF4DD0E1), focusedColor)
                    InputField("Section", sec, Color(0xFF81C784), focusedColor)
                    InputField("Email", email, Color(0xFFFFA726), focusedColor)
                    PasswordField("Password", password, Color(0xFF9575CD), focusedColor)
                    PasswordField("Repeat Password", repeatPassword, Color(0xFFEF5350), focusedColor)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (allFieldsFilled) {
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Button(
                    onClick = {
                        loading.value = true
                        auth.createUserWithEmailAndPassword(email.value.trim(), password.value.trim())
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = task.result?.user?.uid ?: return@addOnCompleteListener
                                    val userMap = mapOf(
                                        "auid" to auid.value,
                                        "name" to name.value,
                                        "batch" to batch.value,
                                        "section" to sec.value,
                                        "email" to email.value
                                    )
                                    database.child("users").child(uid).setValue(userMap)
                                        .addOnCompleteListener { dbTask ->
                                            loading.value = false
                                            if (dbTask.isSuccessful) {
                                                auth.currentUser?.sendEmailVerification()
                                                Toast.makeText(context, "Registered! Please verify your email.", Toast.LENGTH_LONG).show()
                                                navController.navigate("Login")
                                            } else {
                                                Toast.makeText(context, "Failed to save user data.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    loading.value = false
                                    Toast.makeText(context, "Signup failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = focusedColor.value)
                ) {
                    Text("Register", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun InputField(
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

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.DarkGray,
        cursorColor = glowColor,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.DarkGray,
        focusedBorderColor = glowColor,
        unfocusedBorderColor = Color.LightGray
    )

    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = textFieldColors,
        interactionSource = interactionSource
    )
}

@Composable
fun PasswordField(
    label: String,
    state: MutableState<String>,
    glowColor: Color,
    focusedColor: MutableState<Color>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    if (isFocused) {
        focusedColor.value = glowColor
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.DarkGray,
        cursorColor = glowColor,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.DarkGray,
        focusedBorderColor = glowColor,
        unfocusedBorderColor = Color.LightGray
    )

    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = glowColor.copy(alpha = 0.8f)
                )
            }
        },
        colors = textFieldColors,
        interactionSource = interactionSource
    )
}