package com.github.se.eduverse.ui.authentification

import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.R
import com.github.se.eduverse.isAppInDarkMode
import com.github.se.eduverse.model.NotificationData
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.ui.navigation.TopLevelDestinations
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(navigationActions: NavigationActions, notificationData: NotificationData) {

  val auth = FirebaseAuth.getInstance()

  LaunchedEffect(Unit) {
    delay(1300)
    // Wait for Firebase Auth to initialize
    if (auth.currentUser != null) {
      if (notificationData.isNotification) {
        notificationData.isNotification = false // To avoid being unable to go back
        notificationData.open(navigationActions)
      } else {
        navigationActions.navigateTo(TopLevelDestinations.DASHBOARD)
      }
    } else {
      navigationActions.navigateTo(
          TopLevelDestination(route = Route.AUTH, icon = Icons.Outlined.AutoGraph, textId = "Auth"))
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      content = { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Image(
                  painter =
                      painterResource(
                          id =
                              if (isAppInDarkMode) {
                                R.drawable.eduverse_logo_dark
                              } else {
                                R.drawable.eduverse_logo_light
                              }),
                  contentDescription = "App Logo alone",
                  modifier = Modifier.size(250.dp))

              Text(
                  text = "Welcome in Eduverse!",
                  style =
                      MaterialTheme.typography.bodyLarge.copy(
                          fontWeight = FontWeight.Light, fontSize = 14.sp),
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(horizontal = 24.dp).testTag("welcomeText"))
            }
      })
}
