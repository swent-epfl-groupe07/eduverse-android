package com.github.se.eduverse

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.github.se.eduverse.model.NotifAuthorizations
import com.github.se.eduverse.model.NotificationData
import com.github.se.eduverse.model.NotificationType
import com.github.se.eduverse.repository.CommentsRepositoryImpl
import com.github.se.eduverse.repository.DashboardRepositoryImpl
import com.github.se.eduverse.repository.FileRepositoryImpl
import com.github.se.eduverse.repository.NotificationRepository
import com.github.se.eduverse.repository.PhotoRepository
import com.github.se.eduverse.repository.ProfileRepositoryImpl
import com.github.se.eduverse.repository.PublicationRepository
import com.github.se.eduverse.repository.QuizzRepository
import com.github.se.eduverse.repository.SettingsRepository
import com.github.se.eduverse.repository.TimeTableRepositoryImpl
import com.github.se.eduverse.repository.VideoRepository
import com.github.se.eduverse.ui.archive.ArchiveScreen
import com.github.se.eduverse.ui.authentification.LoadingScreen
import com.github.se.eduverse.ui.authentification.SignInScreen
import com.github.se.eduverse.ui.calculator.CalculatorScreen
import com.github.se.eduverse.ui.camera.CameraScreen
import com.github.se.eduverse.ui.camera.CropPhotoScreen
import com.github.se.eduverse.ui.camera.NextScreen
import com.github.se.eduverse.ui.camera.PermissionDeniedScreen
import com.github.se.eduverse.ui.camera.PicTakenScreen
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.folder.CreateFileScreen
import com.github.se.eduverse.ui.folder.CreateFolderScreen
import com.github.se.eduverse.ui.folder.FolderScreen
import com.github.se.eduverse.ui.folder.ListFoldersScreen
import com.github.se.eduverse.ui.gallery.GalleryScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.notifications.NotificationsScreen
import com.github.se.eduverse.ui.pdfGenerator.PdfGeneratorScreen
import com.github.se.eduverse.ui.pomodoro.PomodoroScreen
import com.github.se.eduverse.ui.profile.FollowListScreen
import com.github.se.eduverse.ui.profile.ProfileScreen
import com.github.se.eduverse.ui.quizz.QuizScreen
import com.github.se.eduverse.ui.search.SearchProfileScreen
import com.github.se.eduverse.ui.search.UserProfileScreen
import com.github.se.eduverse.ui.setting.SettingsScreen
import com.github.se.eduverse.ui.theme.EduverseTheme
import com.github.se.eduverse.ui.timetable.DetailsEventScreen
import com.github.se.eduverse.ui.timetable.DetailsTasksScreen
import com.github.se.eduverse.ui.timetable.TimeTableScreen
import com.github.se.eduverse.ui.todo.TodoListScreen
import com.github.se.eduverse.ui.videos.VideoScreen
import com.github.se.eduverse.viewmodel.CommentsViewModel
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PdfGeneratorViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.PublicationViewModel
import com.github.se.eduverse.viewmodel.SettingsViewModel
import com.github.se.eduverse.viewmodel.TimeTableViewModel
import com.github.se.eduverse.viewmodel.TimerViewModel
import com.github.se.eduverse.viewmodel.TodoListViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private var cameraPermissionGranted by mutableStateOf(false)
  private var audioPermissionGranted by mutableStateOf(false)
  private var notificationPermissionGranted by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val notificationData =
        if (intent.getBooleanExtra("isNotification", false)) {
          try {
            NotificationData(
                isNotification = true,
                notificationType =
                    NotificationType.valueOf(intent.getStringExtra("type") ?: "DEFAULT"),
                objectId = intent.getStringExtra("objectId"))
          } catch (e: Exception) {
            NotificationData(false)
          }
        } else {
          NotificationData(false)
        }

    val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    // Retrieve the saved instance or use default
    val json = sharedPreferences.getString("notifAuthKey", null)
    val notifAuthorizations =
        if (json != null) {
          Json.decodeFromString<NotifAuthorizations>(json)
        } else {
          NotifAuthorizations(true, true) // Default value
        }

    // Handling permissions
    val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
          // Update variables based on granted permissions
          cameraPermissionGranted =
              permissions[Manifest.permission.CAMERA] ?: cameraPermissionGranted
          audioPermissionGranted =
              permissions[Manifest.permission.RECORD_AUDIO] ?: audioPermissionGranted
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionGranted =
                permissions[Manifest.permission.POST_NOTIFICATIONS] ?: notificationPermissionGranted
          }

          // Now that permissions are handled, you can set the content
          setContent {
            EduverseTheme {
              Surface(modifier = Modifier.fillMaxSize()) {
                EduverseApp(
                    cameraPermissionGranted = cameraPermissionGranted,
                    notificationData,
                    notifAuthorizations)
              }
            }
          }
        }

    // Check the status of each permission individually
    val cameraPermissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    val audioPermissionStatus =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

    // Notification permission is checked only for Android 13+
    val notificationPermissionStatus =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
          PackageManager.PERMISSION_GRANTED
        }

    // Create a list to store permissions to request
    val permissionsToRequest = mutableListOf<String>()

    if (cameraPermissionStatus == PackageManager.PERMISSION_GRANTED) {
      cameraPermissionGranted = true
    } else {
      permissionsToRequest.add(Manifest.permission.CAMERA)
    }

    if (audioPermissionStatus == PackageManager.PERMISSION_GRANTED) {
      audioPermissionGranted = true
    } else {
      permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (notificationPermissionStatus == PackageManager.PERMISSION_GRANTED) {
        notificationPermissionGranted = true
      } else {
        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    if (permissionsToRequest.isEmpty()) {
      // All permissions are already granted, you can set the content
      setContent {
        EduverseTheme {
          Surface(modifier = Modifier.fillMaxSize()) {
            EduverseApp(
                cameraPermissionGranted = cameraPermissionGranted,
                notificationData,
                notifAuthorizations)
          }
        }
      }
    } else {
      // Request only the missing permissions
      requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }
  }
}

@SuppressLint("ComposableDestinationInComposeScope")
@Composable
fun EduverseApp(
    cameraPermissionGranted: Boolean,
    notificationData: NotificationData,
    notifAuthorizations: NotifAuthorizations
) {
  val firestore = FirebaseFirestore.getInstance()
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val dashboardRepo = DashboardRepositoryImpl(firestore = firestore)
  val dashboardViewModel = DashboardViewModel(dashboardRepo)
  val profileRepo =
      ProfileRepositoryImpl(
          firestore = FirebaseFirestore.getInstance(), storage = FirebaseStorage.getInstance())
  val profileViewModel = ProfileViewModel(profileRepo)
  val CommentsRepository = CommentsRepositoryImpl(FirebaseFirestore.getInstance())
  val CommentsViewModel = CommentsViewModel(CommentsRepository, profileRepo)
  val folderViewModel: FolderViewModel = viewModel(factory = FolderViewModel.Factory)
  val pomodoroViewModel: TimerViewModel = viewModel()
  val fileRepo = FileRepositoryImpl(db = firestore, storage = FirebaseStorage.getInstance())
  val fileViewModel = FileViewModel(fileRepo)
  val photoRepo = PhotoRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
  val photoViewModel = PhotoViewModel(photoRepo, fileRepo)
  val videoRepo = VideoRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
  val videoViewModel = VideoViewModel(videoRepo, fileRepo)
  val todoListViewModel: TodoListViewModel = viewModel(factory = TodoListViewModel.Factory)
  val timeTableRepo = TimeTableRepositoryImpl(firestore)
  val notifRepo = NotificationRepository(LocalContext.current, notifAuthorizations)
  val timeTableViewModel = TimeTableViewModel(timeTableRepo, notifRepo, FirebaseAuth.getInstance())
  val pdfGeneratorViewModel: PdfGeneratorViewModel =
      viewModel(factory = PdfGeneratorViewModel.Factory)

  val pubRepo = PublicationRepository(firestore)
  val publicationViewModel = PublicationViewModel(pubRepo)

  val settingsRepo = SettingsRepository(firestore)
  val settingsViewModel = SettingsViewModel(settingsRepo, FirebaseAuth.getInstance())

  notificationData.viewModel =
      when (notificationData.notificationType) {
        NotificationType.TASK,
        NotificationType.EVENT -> timeTableViewModel
        NotificationType.DEFAULT,
        null -> null
      }

  NavHost(navController = navController, startDestination = Route.LOADING) {
    navigation(
        startDestination = Screen.LOADING,
        route = Route.LOADING,
    ) {
      composable(Screen.LOADING) { LoadingScreen(navigationActions, notificationData) }
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
      composable(Screen.TODO_LIST) { TodoListScreen(navigationActions, todoListViewModel) }
      composable(Screen.PDF_GENERATOR) {
        PdfGeneratorScreen(navigationActions, pdfGeneratorViewModel)
      }
      composable(Screen.SEARCH) {
        SearchProfileScreen(navigationActions, viewModel = profileViewModel)
      }
      composable(Screen.TIME_TABLE) {
        TimeTableScreen(timeTableViewModel, todoListViewModel, navigationActions)
      }
      composable(Screen.DETAILS_EVENT) { DetailsEventScreen(timeTableViewModel, navigationActions) }
      composable(Screen.DETAILS_TASKS) {
        DetailsTasksScreen(timeTableViewModel, todoListViewModel, navigationActions)
      }
    }

    composable(
        route = Screen.USER_PROFILE.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
          val userId =
              backStackEntry.arguments?.getString("userId")
                  ?: return@composable // Handle missing userId

          UserProfileScreen(
              navigationActions = navigationActions, viewModel = profileViewModel, userId = userId)
        }

    navigation(
        startDestination = Screen.VIDEOS,
        route = Route.VIDEOS,
    ) {
      composable(Screen.VIDEOS) {
        VideoScreen(
            navigationActions,
            publicationViewModel,
            profileViewModel,
            CommentsViewModel,
        )
      }
    }

    navigation(
        startDestination = Screen.CALCULATOR,
        route = Route.CALCULATOR,
    ) {
      composable(Screen.CALCULATOR) { CalculatorScreen(navigationActions) }
    }

    navigation(
        startDestination = Screen.QUIZZ,
        route = Route.QUIZZ,
    ) {
      composable(Screen.QUIZZ) {
        QuizScreen(navigationActions, QuizzRepository(OkHttpClient(), BuildConfig.OPENAI_API_KEY))
      }
    }

    navigation(
        startDestination = Screen.CAMERA,
        route = Route.CAMERA,
    ) {
      composable(Screen.CAMERA) {
        if (cameraPermissionGranted) {
          CameraScreen(navigationActions)
        } else {
          PermissionDeniedScreen(navigationActions)
        }
      }
    }

    composable(
        route = Screen.FOLLOWERS.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
          val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
          FollowListScreen(
              navigationActions = navigationActions,
              viewModel = profileViewModel,
              userId = userId,
              isFollowersList = true)
        }

    composable(
        route = Screen.FOLLOWING.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
          val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
          FollowListScreen(
              navigationActions = navigationActions,
              viewModel = profileViewModel,
              userId = userId,
              isFollowersList = false)
        }

    navigation(
        startDestination = Screen.PROFILE,
        route = Route.PROFILE,
    ) {
      composable(Screen.PROFILE) { ProfileScreen(navigationActions, profileViewModel) }

      composable(Screen.SETTING) { SettingsScreen(navigationActions, settingsViewModel) }

      composable(Screen.EDIT_PROFILE) { ProfileScreen(navigationActions, profileViewModel) }

      composable(Screen.GALLERY) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        GalleryScreen(
            ownerId = ownerId,
            photoViewModel = photoViewModel,
            videoViewModel,
            folderViewModel,
            navigationActions)
        Log.d("GalleryScreen", "Current Owner ID: $ownerId")
      }

      composable(Screen.NOTIFICATIONS) {
        NotificationsScreen(notifAuthorizations, navigationActions)
      }
    }

    navigation(startDestination = Screen.ARCHIVE, route = Route.ARCHIVE) {
      composable(Screen.ARCHIVE) { ArchiveScreen(navigationActions, folderViewModel) }
    }

    navigation(startDestination = Screen.LIST_FOLDERS, route = Route.LIST_FOLDERS) {
      composable(Screen.LIST_FOLDERS) { ListFoldersScreen(navigationActions, folderViewModel) }
      composable(Screen.CREATE_FOLDER) {
        CreateFolderScreen(navigationActions, folderViewModel, fileViewModel)
      }
      composable(Screen.FOLDER) { FolderScreen(navigationActions, folderViewModel, fileViewModel) }
      composable(Screen.CREATE_FILE) { CreateFileScreen(navigationActions, fileViewModel) }
    }

    navigation(
        startDestination = Screen.POMODORO,
        route = Route.POMODORO,
    ) {
      composable(Screen.POMODORO) {
        PomodoroScreen(navigationActions, pomodoroViewModel, todoListViewModel)
      }
      composable(Screen.SETTING) { SettingsScreen(navigationActions, settingsViewModel) }
    }

    // video
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
          // Get photo and video paths from arguments
          val photoPath = backStackEntry.arguments?.getString("photoPath")
          val videoPath = backStackEntry.arguments?.getString("videoPath")

          // Create the corresponding files if the paths exist
          val photoFile = photoPath?.let { File(it) }
          val videoFile = videoPath?.let { File(it) }

          // Call PicTakenScreen with the photo and video files
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
