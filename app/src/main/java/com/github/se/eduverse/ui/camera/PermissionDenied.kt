package com.github.se.eduverse.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDeniedScreen(navigationActions: NavigationActions) {

  val context = LocalContext.current
  var cameraPermissionGranted by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    cameraPermissionGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
  }

  LaunchedEffect(cameraPermissionGranted) {
    if (cameraPermissionGranted) {
      navigationActions.navigateTo(Screen.CAMERA)
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Permissions Required") }, modifier = Modifier.testTag("TopAppBar"))
      },
      bottomBar = {
        BottomNavigationMenu(
            onTabSelect = { route -> navigationActions.navigateTo(route) },
            tabList = LIST_TOP_LEVEL_DESTINATION,
            selectedItem = navigationActions.currentRoute())
      }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().testTag("PermissionDeniedColumn"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(
                  text = "Camera permission is required.",
                  modifier = Modifier.testTag("PermissionMessage"))
              Spacer(modifier = Modifier.height(16.dp))
              EnablePermissionButton(context)
            }
      }
}

/**
 * Button to navigate to the app settings to enable permissions
 *
 * @param context the context of the app
 */
@Composable
fun EnablePermissionButton(context: Context) {
  Button(
      onClick = {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")))
      },
      modifier = Modifier.testTag("EnableButton")) {
        Text("Enable")
      }
}
