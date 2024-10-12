package com.github.se.eduverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.github.se.eduverse.repository.DashboardRepositoryImpl
import com.github.se.eduverse.ui.authentification.SignInScreen
import com.github.se.eduverse.ui.camera.CameraScreen
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.others.OthersScreen
import com.github.se.eduverse.ui.theme.EduverseTheme
import com.github.se.eduverse.ui.videos.VideosScreen
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

  private lateinit var auth: FirebaseAuth

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize Firebase Auth
    auth = FirebaseAuth.getInstance()
    auth.currentUser?.let {
      // Sign out the user if they are already signed in
      // This is useful for testing purposes
      auth.signOut()
    }
    setContent { EduverseTheme { Surface(modifier = Modifier.fillMaxSize()) { EduverseApp() } } }
  }
}

@Composable
fun EduverseApp() {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val dashboardRepo = DashboardRepositoryImpl(firestore = FirebaseFirestore.getInstance())
  val dashboardViewModel = DashboardViewModel(dashboardRepo)

  NavHost(navController = navController, startDestination = Route.AUTH) {
    navigation(
        startDestination = Screen.AUTH,
        route = Route.AUTH,
    ) {
      composable(Screen.AUTH) { SignInScreen(navigationActions) }
    }

    navigation(
        startDestination = Screen.DASHBOARD,
        route = Route.DASHBOARD,
    ) {
      composable(Screen.DASHBOARD) { DashboardScreen(navigationActions, dashboardViewModel) }
    }

    navigation(
        startDestination = Screen.VIDEOS,
        route = Route.VIDEOS,
    ) {
      composable(Screen.VIDEOS) { VideosScreen(navigationActions) }
    }

    navigation(
        startDestination = Screen.CAMERA,
        route = Route.CAMERA,
    ) {
      composable(Screen.CAMERA) { CameraScreen(navigationActions) }
    }

    navigation(
        startDestination = Screen.OTHERS,
        route = Route.OTHERS,
    ) {
      composable(Screen.OTHERS) { OthersScreen(navigationActions) }
    }
  }
}
