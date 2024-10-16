package com.github.se.eduverse.ui.camera

import PermissionDeniedScreen
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class PermissionDeniedScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navigationActions: NavigationActions

  @Before
  fun setUp() {
    navController = Mockito.mock(NavHostController::class.java)
    navigationActions = NavigationActions(navController)

    composeTestRule.setContent { PermissionDeniedScreen(navigationActions) }
  }

  @Test
  fun textAndBottomNavigationAreCorrectlyDisplayed() {
    composeTestRule.onNodeWithTag("permissionText").assertIsDisplayed()

    composeTestRule.onNodeWithTag("bottomNavigation").assertIsDisplayed()
  }
}
