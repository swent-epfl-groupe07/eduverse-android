package com.github.se.eduverse.ui.authentification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.R
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.ui.navigation.TopLevelDestinations
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(navigationActions: NavigationActions) {

  val auth = FirebaseAuth.getInstance()

  LaunchedEffect(Unit) {
    delay(1300)
    // Wait for Firebase Auth to initialize
    if (auth.currentUser != null) {
      navigationActions.navigateTo(TopLevelDestinations.DASHBOARD)
    } else {
      navigationActions.navigateTo(
          TopLevelDestination(route = Route.AUTH, icon = Icons.Outlined.AutoGraph, textId = "Auth"))
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().background(color = Color.White),
      content = { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .background(color = Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Image(
                  painter = painterResource(id = R.drawable.eduverse_logo),
                  contentDescription = "App Logo alone",
                  modifier = Modifier.size(250.dp))

              Text(
                  text = "Welcome in Eduverse!",
                  style =
                      MaterialTheme.typography.bodyLarge.copy(
                          fontWeight = FontWeight.Light, fontSize = 14.sp),
                  color = Color.Black,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(horizontal = 24.dp).testTag("welcomeText"))
            }
      })
}
