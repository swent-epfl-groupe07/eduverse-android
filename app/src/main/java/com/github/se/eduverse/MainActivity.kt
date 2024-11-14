package com.github.se.eduverse

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.github.se.eduverse.repository.DashboardRepositoryImpl
import com.github.se.eduverse.repository.FileRepositoryImpl
import com.github.se.eduverse.repository.FolderRepositoryImpl
import com.github.se.eduverse.repository.PhotoRepository
import com.github.se.eduverse.repository.ProfileRepositoryImpl
import com.github.se.eduverse.repository.PublicationRepository
import com.github.se.eduverse.repository.VideoRepository
import com.github.se.eduverse.ui.Pomodoro.PomodoroScreen
import com.github.se.eduverse.ui.VideoScreen
import com.github.se.eduverse.ui.authentification.LoadingScreen
import com.github.se.eduverse.ui.authentification.SignInScreen
import com.github.se.eduverse.ui.calculator.CalculatorScreen
import com.github.se.eduverse.ui.camera.CameraScreen
import com.github.se.eduverse.ui.camera.CropPhotoScreen
import com.github.se.eduverse.ui.camera.NextScreen
import com.github.se.eduverse.ui.camera.PicTakenScreen
import com.github.se.eduverse.ui.converter.PdfConverterScreen
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.folder.CreateFileScreen
import com.github.se.eduverse.ui.folder.CreateFolderScreen
import com.github.se.eduverse.ui.folder.FolderScreen
import com.github.se.eduverse.ui.folder.ListFoldersScreen
import com.github.se.eduverse.ui.gallery.GalleryScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.profile.ProfileScreen
import com.github.se.eduverse.ui.setting.SettingsScreen
import com.github.se.eduverse.ui.theme.EduverseTheme
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.PublicationViewModel
import com.github.se.eduverse.viewmodel.TimerViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private lateinit var auth: FirebaseAuth
  private var cameraPermissionGranted by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {

    super.onCreate(savedInstanceState)

    // Initialiser Firebase Auth
    auth = FirebaseAuth.getInstance()
    if (auth.currentUser != null) {
      auth.signOut()
    }

    // Ajout du VideoRepository et du VideoViewModel

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
        Surface(modifier = Modifier.fillMaxSize()) { EduverseApp(cameraPermissionGranted) }
      }
    }
  }
}

@SuppressLint("ComposableDestinationInComposeScope")
@Composable
fun EduverseApp(cameraPermissionGranted: Boolean) {
  val firestore = FirebaseFirestore.getInstance()
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val dashboardRepo = DashboardRepositoryImpl(firestore = firestore)
  val dashboardViewModel = DashboardViewModel(dashboardRepo)
  val profileRepo =
      ProfileRepositoryImpl(
          firestore = FirebaseFirestore.getInstance(), storage = FirebaseStorage.getInstance())
  val profileViewModel = ProfileViewModel(profileRepo)
  val folderRepo = FolderRepositoryImpl(db = firestore)
  val folderViewModel = FolderViewModel(folderRepo, FirebaseAuth.getInstance())
  val pomodoroViewModel: TimerViewModel = viewModel()
  val fileRepo = FileRepositoryImpl(db = firestore, storage = FirebaseStorage.getInstance())
  val fileViewModel = FileViewModel(fileRepo)
  val photoRepo = PhotoRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
  val photoViewModel = PhotoViewModel(photoRepo, fileRepo)
  val videoRepo = VideoRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
  val videoViewModel = VideoViewModel(videoRepo, fileRepo)

  val pubRepo = PublicationRepository(firestore)
  val publicationViewModel = PublicationViewModel(pubRepo)

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
      composable(Screen.PDF_CONVERTER) { PdfConverterScreen(navigationActions) }
    }

    navigation(
        startDestination = Screen.VIDEOS,
        route = Route.VIDEOS,
    ) {
      composable(Screen.VIDEOS) { VideoScreen(navigationActions, publicationViewModel) }
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
        startDestination = Screen.PROFILE,
        route = Route.PROFILE,
    ) {
      composable(Screen.PROFILE) { ProfileScreen(navigationActions, profileViewModel) }

      composable(Screen.SETTING) { SettingsScreen(navigationActions) }

      composable(Screen.EDIT_PROFILE) { ProfileScreen(navigationActions, profileViewModel) }

      composable(Screen.GALLERY) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        GalleryScreen(
            ownerId = ownerId, photoViewModel = photoViewModel, folderViewModel, navigationActions)
        Log.d("GalleryScreen", "Current Owner ID: $ownerId")
      }
    }

    navigation(startDestination = Screen.LIST_FOLDERS, route = Route.LIST_FOLDERS) {
      composable(Screen.LIST_FOLDERS) { ListFoldersScreen(navigationActions, folderViewModel) }
      composable(Screen.CREATE_FOLDER) {
        CreateFolderScreen(navigationActions, folderViewModel, fileViewModel)
      }
      composable(Screen.FOLDER) { FolderScreen(navigationActions, folderViewModel, fileViewModel) }
      composable(Screen.CREATE_FILE) { CreateFileScreen(navigationActions, fileViewModel) }
    }

    // Écran pour afficher la photo prise
    navigation(
        startDestination = Screen.POMODORO,
        route = Route.POMODORO,
    ) {
      composable(Screen.POMODORO) { PomodoroScreen(navigationActions, pomodoroViewModel) }
      composable(Screen.SETTING) { SettingsScreen(navigationActions) }
    }

    // Ajoute une route dynamique pour PicTakenScreen avec des arguments optionnels pour photo et
    // vidéo
    composable(
        "picTaken/{photoPath}?videoPath={videoPath}",
        arguments =
            listOf(
                navArgument("photoPath") {
                  type = NavType.StringType
                  nullable = true
                },
                navArgument("videoPath") {
                  type = NavType.StringType
                  nullable = true
                })) { backStackEntry ->
          // Récupère les chemins de photo et de vidéo depuis les arguments
          val photoPath = backStackEntry.arguments?.getString("photoPath")
          val videoPath = backStackEntry.arguments?.getString("videoPath")

          // Crée les fichiers correspondants si les chemins existent
          val photoFile = photoPath?.let { File(it) }
          val videoFile = videoPath?.let { File(it) }

          // Appelle PicTakenScreen avec les fichiers de photo et de vidéo
          PicTakenScreen(photoFile, videoFile, navigationActions, photoViewModel, videoViewModel)
        }
    composable(
        route = "cropPhotoScreen/{photoPath}",
        arguments = listOf(navArgument("photoPath") { type = NavType.StringType })) { backStackEntry
          ->
          val photoPath = backStackEntry.arguments?.getString("photoPath") ?: ""
          val photoFile = File(photoPath)
          CropPhotoScreen(
              photoFile = photoFile,
              photoViewModel = photoViewModel,
              navigationActions = navigationActions)
        }

    composable(
        "nextScreen/{photoPath}/{videoPath}",
        arguments =
            listOf(
                navArgument("photoPath") {
                  nullable = true
                  type = NavType.StringType
                },
                navArgument("videoPath") {
                  nullable = true
                  type = NavType.StringType
                })) { backStackEntry ->
          val photoPath = backStackEntry.arguments?.getString("photoPath")
          val videoPath = backStackEntry.arguments?.getString("videoPath")

          val photoFile = if (photoPath != null) File(photoPath) else null
          val videoFile = if (videoPath != null) File(videoPath) else null

          NextScreen(
              photoFile = photoFile,
              videoFile = videoFile,
              navigationActions = navigationActions,
              photoViewModel,
              folderViewModel,
              videoViewModel)
        }
  }
}

@Composable
fun PermissionDeniedScreen() {
  Text("Permission Denied")
}
