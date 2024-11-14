package com.github.se.eduverse.ui.camera

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class PermissionDeniedScreenTest : TestCase() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navigationActions: NavigationActions

  @Before
  fun setUp() {
    Intents.init()
    navController = Mockito.mock(NavHostController::class.java)
    navigationActions = NavigationActions(navController)
    composeTestRule.setContent { PermissionDeniedScreen(navigationActions = navigationActions) }
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  @Test
  fun permissionScreenElementsAreDisplayed() {
    composeTestRule.onNodeWithTag("TopAppBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("PermissionDeniedColumn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("PermissionMessage").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("PermissionMessage")
        .assertTextEquals("Camera permission is required.")
    composeTestRule.onNodeWithTag("EnableButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("EnableButton").assertTextEquals("Enable")
  }
}
