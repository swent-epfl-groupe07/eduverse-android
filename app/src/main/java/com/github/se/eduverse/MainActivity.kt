package com.github.se.eduverse

import PermissionDeniedScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.github.se.eduverse.repository.DashboardRepositoryImpl
import com.github.se.eduverse.ui.authentification.LoadingScreen
import com.github.se.eduverse.ui.authentification.SignInScreen
import com.github.se.eduverse.ui.camera.CameraScreen
import com.github.se.eduverse.ui.camera.PicTakenScreen
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
import java.io.File

class MainActivity : ComponentActivity() {

  private lateinit var auth: FirebaseAuth
  private var cameraPermissionGranted by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    auth = FirebaseAuth.getInstance()
    auth.currentUser?.let {
      auth.signOut()
    }

    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean
          ->
          cameraPermissionGranted = isGranted
        }

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED) {
      cameraPermissionGranted = true
    } else {
      requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    setContent {
      EduverseTheme {
        Surface(modifier = Modifier.fillMaxSize()) { EduverseApp(cameraPermissionGranted) }
      }
    }
  }
}

@Composable
fun EduverseApp(cameraPermissionGranted: Boolean) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val dashboardRepo = DashboardRepositoryImpl(firestore = FirebaseFirestore.getInstance())
  val dashboardViewModel = DashboardViewModel(dashboardRepo)

  NavHost(navController = navController, startDestination = Route.LOADING) {
    navigation(
      startDestination = Screen.LOADING,
      route = Route.LOADING,
    ) {
      composable(Screen.LOADING) { LoadingScreen(navigationActions) }
    }

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
      composable(Screen.CAMERA) {
        if (cameraPermissionGranted) {
          CameraScreen(navigationActions)
        } else {
          PermissionDeniedScreen()
        }
      }
    }

    navigation(
        startDestination = Screen.OTHERS,
        route = Route.OTHERS,
    ) {
      composable(Screen.OTHERS) { OthersScreen(navigationActions) }
    }

    composable("picTaken/{photoPath}") { backStackEntry ->
      val photoPath = backStackEntry.arguments?.getString("photoPath")
      val photoFile = photoPath?.let { File(it) }
      PicTakenScreen(photoFile, navigationActions)
    }
  }
}
