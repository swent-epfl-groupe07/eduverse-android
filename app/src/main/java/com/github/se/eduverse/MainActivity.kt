package com.github.se.eduverse

import PermissionDeniedScreen
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.github.se.eduverse.repository.DashboardRepositoryImpl
import com.github.se.eduverse.repository.FolderRepositoryImpl
import com.github.se.eduverse.repository.PhotoRepository
import com.github.se.eduverse.repository.ProfileRepositoryImpl
import com.github.se.eduverse.ui.Pomodoro.PomodoroScreen
import com.github.se.eduverse.ui.authentification.LoadingScreen
import com.github.se.eduverse.ui.authentification.SignInScreen
import com.github.se.eduverse.ui.calculator.CalculatorScreen
import com.github.se.eduverse.ui.camera.CameraScreen
import com.github.se.eduverse.ui.camera.NextScreen
import com.github.se.eduverse.ui.camera.PicTakenScreen
import com.github.se.eduverse.ui.converter.PdfConverterScreen
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.folder.CreateFIleScreen
import com.github.se.eduverse.ui.folder.CreateFolderScreen
import com.github.se.eduverse.ui.folder.FolderScreen
import com.github.se.eduverse.ui.folder.ListFoldersScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.others.OthersScreen
import com.github.se.eduverse.ui.others.profile.ProfileScreen
import com.github.se.eduverse.ui.others.setting.SettingsScreen
import com.github.se.eduverse.ui.screens.GalleryScreen
import com.github.se.eduverse.ui.theme.EduverseTheme
import com.github.se.eduverse.ui.videos.VideosScreen
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModelFactory
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.TimerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class MainActivity : ComponentActivity() {

  private lateinit var auth: FirebaseAuth
  private var cameraPermissionGranted by mutableStateOf(false)
  private lateinit var photoViewModel: PhotoViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialiser Firebase Auth
    auth = FirebaseAuth.getInstance()
    if (auth.currentUser != null) {
      auth.signOut()
    }

    // Instanciez le repository et le ViewModel
    val photoRepository =
        PhotoRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
    val photoViewModelFactory = PhotoViewModelFactory(photoRepository)
    photoViewModel = ViewModelProvider(this, photoViewModelFactory)[PhotoViewModel::class.java]

    // Gestion des permissions de la caméra
    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
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
        Surface(modifier = Modifier.fillMaxSize()) {
          EduverseApp(cameraPermissionGranted, photoViewModel)
        }
      }
    }
  }
}

@SuppressLint("ComposableDestinationInComposeScope")
@Composable
fun EduverseApp(cameraPermissionGranted: Boolean, photoViewModel: PhotoViewModel) {
  val firestore = FirebaseFirestore.getInstance()
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val dashboardRepo = DashboardRepositoryImpl(firestore = firestore)
  val dashboardViewModel = DashboardViewModel(dashboardRepo)
  val profileRepo = ProfileRepositoryImpl(firestore = FirebaseFirestore.getInstance())
  val profileViewModel = ProfileViewModel(profileRepo)
  val folderRepo = FolderRepositoryImpl(db = firestore)
  val folderViewModel = FolderViewModel(folderRepo, FirebaseAuth.getInstance())
  val pomodoroViewModel: TimerViewModel = viewModel()

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
        startDestination = Screen.CALCULATOR,
        route = Route.CALCULATOR,
    ) {
      composable(Screen.CALCULATOR) { CalculatorScreen(navigationActions) }
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

      composable(Screen.SETTING) { SettingsScreen(navigationActions) }
      composable(Screen.LIST_FOLDERS) { ListFoldersScreen(navigationActions, folderViewModel) }

      composable(Screen.EDIT_PROFILE) { ProfileScreen(profileViewModel, navigationActions) }
      composable(Screen.LIST_FOLDERS) { ListFoldersScreen(navigationActions, folderViewModel) }

      composable(Screen.CREATE_FOLDER) { CreateFolderScreen(navigationActions, folderViewModel) }
      composable(Screen.FOLDER) { FolderScreen(navigationActions, folderViewModel) }
      composable(Screen.CREATE_FILE) { CreateFIleScreen() }
      composable(Screen.GALLERY) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        GalleryScreen(ownerId = ownerId, viewModel = photoViewModel, navigationActions)
        Log.d("GalleryScreen", "Current Owner ID: $ownerId")
      }
      composable(Screen.PDF_CONVERTER) { PdfConverterScreen(navigationActions) }
    }

    // Écran pour afficher la photo prise
    navigation(
        startDestination = Screen.POMODORO,
        route = Route.POMODORO,
    ) {
      composable(Screen.POMODORO) { PomodoroScreen(navigationActions, pomodoroViewModel) }
      composable(Screen.SETTING) { SettingsScreen(navigationActions) }
    }

    // Ajoute une route dynamique pour PicTakenScreen
    composable("picTaken/{photoPath}") { backStackEntry ->
      val photoPath = backStackEntry.arguments?.getString("photoPath")
      val photoFile = photoPath?.let { File(it) }
      PicTakenScreen(photoFile, navigationActions, photoViewModel)
    }
    composable(
        "nextScreen/{photoPath}",
        arguments = listOf(navArgument("photoPath") { type = NavType.StringType })) { backStackEntry
          ->
          val photoPath = backStackEntry.arguments?.getString("photoPath")
          val photoFile = if (photoPath != null) File(photoPath) else null
          NextScreen(photoFile = photoFile, navigationActions = navigationActions, photoViewModel)
        }
  }
}

@Composable
fun PermissionDeniedScreen() {
  Text("Permission Denied")
}
