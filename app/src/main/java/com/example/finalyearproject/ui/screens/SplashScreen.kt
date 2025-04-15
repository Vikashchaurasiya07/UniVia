package com.example.finalyearproject.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import com.example.finalyearproject.R
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(.2f) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(true) {

        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        delay(300)
        if (auth.currentUser != null) {
            navController.navigate("studentDashboard") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.splashscreen_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(300.dp)
                .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
        )
    }
}

