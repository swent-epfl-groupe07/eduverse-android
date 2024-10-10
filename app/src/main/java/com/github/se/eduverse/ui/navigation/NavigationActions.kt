// This file was copied and adapted from the bootcamp solution

package com.github.se.eduverse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object Route {
  const val AUTH = "Auth"
  const val DASHBOARD = "Dashboard"
  const val VIDEOS = "Videos"
  const val CAMERA = "Camera"
  const val OTHERS = "Others"
}

object Screen {
  const val AUTH = "Auth Screen"
  const val DASHBOARD = "Dashboard screen"
  const val VIDEOS = "Videos screen"
  const val CAMERA = "Camera screen"
  const val OTHERS = "Others screen"
  const val EDIT_PROFILE = "EditProfile screen"
  const val FOLDER = "Folder screen"
  const val CREATE_FOLDER = "CreateFolder screen"
  const val COURSES = "Courses screen"
  const val PDF_CONVERTER = "PdfConverter screen"
}

data class TopLevelDestination(val route: String, val icon: ImageVector, val textId: String)

object TopLevelDestinations {
  val DASHBOARD =
      TopLevelDestination(route = Route.DASHBOARD, icon = Icons.Outlined.Home, textId = "Home")
  val VIDEOS =
      TopLevelDestination(route = Route.VIDEOS, icon = Icons.Outlined.PlayArrow, textId = "Videos")
  val CAMERA =
      TopLevelDestination(route = Route.CAMERA, icon = Icons.Outlined.CameraAlt, textId = "Camera")
  val OTHERS =
      TopLevelDestination(route = Route.OTHERS, icon = Icons.Outlined.Menu, textId = "Others")
}

val LIST_TOP_LEVEL_DESTINATION =
    listOf(
        TopLevelDestinations.DASHBOARD,
        TopLevelDestinations.VIDEOS,
        TopLevelDestinations.CAMERA,
        TopLevelDestinations.OTHERS)

open class NavigationActions(
    private val navController: NavHostController,
) {
  /**
   * Navigate to the specified [TopLevelDestination]
   *
   * @param destination The top level destination to navigate to Clear the back stack when
   *   navigating to a new destination This is useful when navigating to a new screen from the
   *   bottom navigation bar as we don't want to keep the previous screen in the back stack
   */
  open fun navigateTo(destination: TopLevelDestination) {

    navController.navigate(destination.route) {
      // Pop up to the start destination of the graph to
      // avoid building up a large stack of destinations
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
        inclusive = true
      }

      // Avoid multiple copies of the same destination when reselecting same item
      launchSingleTop = true

      // Restore state when reselecting a previously selected item
      if (destination.route != Route.AUTH) {
        restoreState = true
      }
    }
  }

  /**
   * Navigate to the specified screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: String) {
    navController.navigate(screen)
  }

  /** Navigate back to the previous screen. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
