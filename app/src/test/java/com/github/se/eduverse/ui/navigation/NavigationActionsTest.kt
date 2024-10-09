package com.github.se.eduverse


import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import org.junit.Assert.assertThat
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
        navigationActions.navigateTo(TopLevelDestinations.HOME)
        verify(navHostController).navigate(eq(Route.DASHBOARD), any<NavOptionsBuilder.() -> Unit>())

        navigationActions.navigateTo(TopLevelDestinations.CAMERA)
        verify(navHostController).navigate(eq(Route.CAMERA), any<NavOptionsBuilder.() -> Unit>())

        navigationActions.navigateTo(TopLevelDestinations.OTHERS)
        verify(navHostController).navigate(eq(Route.OTHERS), any<NavOptionsBuilder.() -> Unit>())

        navigationActions.navigateTo(TopLevelDestinations.VIDEOS)
        verify(navHostController).navigate(eq(Route.VIDEOS), any<NavOptionsBuilder.() -> Unit>())

        navigationActions.navigateTo(Screen.CAMERA)
        verify(navHostController).navigate(eq(Screen.CAMERA))

        navigationActions.navigateTo(Screen.OTHERS)
        verify(navHostController).navigate(eq(Screen.OTHERS))

        navigationActions.navigateTo(Screen.VIDEOS)
        verify(navHostController).navigate(eq(Screen.VIDEOS))

        navigationActions.navigateTo(Screen.AUTH)
        verify(navHostController).navigate(eq(Screen.AUTH))

        navigationActions.navigateTo(Screen.FOLDER)
        verify(navHostController).navigate(eq(Screen.FOLDER))

        navigationActions.navigateTo(Screen.DASHBOARD)
        verify(navHostController).navigate(eq(Screen.DASHBOARD))

        navigationActions.navigateTo(Screen.EDIT_PROFILE)
        verify(navHostController).navigate(eq(Screen.EDIT_PROFILE))
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