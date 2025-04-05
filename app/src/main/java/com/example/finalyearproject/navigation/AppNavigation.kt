package com.example.finalyearproject.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.finalyearproject.ui.screens.SplashScreen
import com.example.finalyearproject.ui.screens.LoginScreen
import com.example.finalyearproject.ui.screens.SignupScreen
import com.example.finalyearproject.ui.screens.forgotpassword

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignupScreen(navController) }
        composable("forgot password") { forgotpassword(navController)}
    }
}
