package com.github.se.eduverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.se.eduverse.repository.DashboardRepositoryImpl
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.theme.EduverseTheme
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Using Jetpack Compose's viewModel function to instantiate DashboardViewModel
    setContent {
      val viewModel: DashboardViewModel = DashboardViewModel(DashboardRepositoryImpl(firestore = Firebase.firestore))
      EduverseApp(viewModel = viewModel)
    }
  }
}


@Composable
fun EduverseApp(viewModel: DashboardViewModel) {
  MaterialTheme {
    // You can replace this with your actual logic to get userId
    val userId = remember { "exampleUserId123" }

    // Pass the ViewModel to the DashboardScreen
    DashboardScreen(viewModel = viewModel, userId = userId)
  }
}




