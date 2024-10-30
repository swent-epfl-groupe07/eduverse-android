package com.github.se.eduverse.ui

import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.ui.navigation.TopLevelDestinations
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class NavigationActionsTest {

  private lateinit var navigationDestination: NavDestination
  private lateinit var navHostController: NavHostController
  private lateinit var navigationActions: NavigationActions

  @Before
  fun setUp() {
    navigationDestination = mock(NavDestination::class.java)
    navHostController = mock(NavHostController::class.java)
    navigationActions = NavigationActions(navHostController)
  }

  @Test
  fun navigateToCallsController() {
    navigationActions.navigateTo(TopLevelDestinations.DASHBOARD)
    verify(navHostController).navigate(eq(Route.DASHBOARD), any<NavOptionsBuilder.() -> Unit>())

    navigationActions.navigateTo(TopLevelDestinations.CAMERA)
    verify(navHostController).navigate(eq(Route.CAMERA), any<NavOptionsBuilder.() -> Unit>())

    navigationActions.navigateTo(TopLevelDestinations.PROFILE)
    verify(navHostController).navigate(eq(Route.PROFILE), any<NavOptionsBuilder.() -> Unit>())

    navigationActions.navigateTo(TopLevelDestinations.VIDEOS)
    verify(navHostController).navigate(eq(Route.VIDEOS), any<NavOptionsBuilder.() -> Unit>())

    navigationActions.navigateTo(TopLevelDestinations.FOLDERS)
    verify(navHostController).navigate(eq(Route.LIST_FOLDERS), any<NavOptionsBuilder.() -> Unit>())

    navigationActions.navigateTo(Screen.CAMERA)
    verify(navHostController).navigate(Screen.CAMERA)

    navigationActions.navigateTo(Screen.PROFILE)
    verify(navHostController).navigate(Screen.PROFILE)

    navigationActions.navigateTo(Screen.LIST_FOLDERS)
    verify(navHostController).navigate(Screen.LIST_FOLDERS)

    navigationActions.navigateTo(Screen.CREATE_FOLDER)
    verify(navHostController).navigate(Screen.CREATE_FOLDER)

    navigationActions.navigateTo(Screen.CREATE_FILE)
    verify(navHostController).navigate(Screen.CREATE_FILE)

    navigationActions.navigateTo(Screen.CALCULATOR)
    verify(navHostController).navigate(Screen.CALCULATOR)

    navigationActions.navigateTo(Screen.POMODORO)
    verify(navHostController).navigate(Screen.POMODORO)

    navigationActions.navigateTo(Screen.PDF_CONVERTER)
    verify(navHostController).navigate(Screen.PDF_CONVERTER)

    navigationActions.navigateTo(Screen.VIDEOS)
    verify(navHostController).navigate(Screen.VIDEOS)

    navigationActions.navigateTo(Screen.AUTH)
    verify(navHostController).navigate(Screen.AUTH)

    navigationActions.navigateTo(Screen.FOLDER)
    verify(navHostController).navigate(Screen.FOLDER)

    navigationActions.navigateTo(Screen.DASHBOARD)
    verify(navHostController).navigate(Screen.DASHBOARD)

    navigationActions.navigateTo(Screen.EDIT_PROFILE)
    verify(navHostController).navigate(Screen.EDIT_PROFILE)

    navigationActions.navigateTo(Screen.COURSES)
    verify(navHostController).navigate(Screen.COURSES)
  }

  @Test
  fun goBackCallsController() {
    navigationActions.goBack()
    verify(navHostController).popBackStack()
  }

  @Test
  fun currentRouteWorksWithDestination() {
    `when`(navHostController.currentDestination).thenReturn(navigationDestination)
    `when`(navigationDestination.route).thenReturn(Route.DASHBOARD)

    assertThat(navigationActions.currentRoute(), `is`(Route.DASHBOARD))
  }
}
