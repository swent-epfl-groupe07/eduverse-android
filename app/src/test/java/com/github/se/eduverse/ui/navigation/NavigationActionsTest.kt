package com.github.se.eduverse.ui.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
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

    navigationActions.navigateTo(TopLevelDestinations.OTHERS)
    verify(navHostController).navigate(eq(Route.OTHERS), any<NavOptionsBuilder.() -> Unit>())

    navigationActions.navigateTo(TopLevelDestinations.VIDEOS)
    verify(navHostController).navigate(eq(Route.VIDEOS), any<NavOptionsBuilder.() -> Unit>())

    navigationActions.navigateTo(Screen.CAMERA)
    verify(navHostController).navigate(Screen.CAMERA)

    navigationActions.navigateTo(Screen.OTHERS)
    verify(navHostController).navigate(Screen.OTHERS)

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
