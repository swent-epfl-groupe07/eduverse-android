package com.github.se.eduverse.ui.others

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import com.github.se.eduverse.ui.navigation.NavigationActions
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class mockNavigationActions(navController: NavHostController) : NavigationActions(navController) {
  fun navigate(route: String) {
    // No-op for testing
  }
}

class OthersScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testOthersScreenUI() {
    // Create a mock for NavigationActions

    // Set the content for the test
    composeTestRule.setContent {
      OthersScreen(navigationActions = mockNavigationActions(navController = mock()))
    }

    // Verify that the OthersScreen is displayed
    composeTestRule.onNodeWithTag("othersScreen").assertIsDisplayed()

    // Check if all buttons are displayed
    composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    composeTestRule.onNodeWithText("About").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pomodoro Timer").assertIsDisplayed()
    composeTestRule.onNodeWithText("Field #5").assertIsDisplayed()

    // Check if the buttons are clickable (perform click actions)
    composeTestRule.onNodeWithText("Settings").performClick()
    composeTestRule.onNodeWithText("Profile").performClick()
    composeTestRule.onNodeWithText("About").performClick()
    composeTestRule.onNodeWithText("Pomodoro Timer").performClick()
    composeTestRule.onNodeWithText("Field #5").performClick()

    // Assert that the buttons are present in the layout
    composeTestRule.onAllNodesWithText("Settings").assertCountEquals(1)
    composeTestRule.onAllNodesWithText("Profile").assertCountEquals(1)
    composeTestRule.onAllNodesWithText("About").assertCountEquals(1)
    composeTestRule.onAllNodesWithText("Pomodoro Timer").assertCountEquals(1)
    composeTestRule.onAllNodesWithText("Field #5").assertCountEquals(1)
  }
}
