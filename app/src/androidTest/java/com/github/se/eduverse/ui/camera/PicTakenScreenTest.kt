package com.github.se.eduverse.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.github.se.eduverse.ui.navigation.NavigationActions
import org.junit.Rule
import org.junit.Test

class PicTakenScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Composable
  private fun createFakeNavigationActions(): NavigationActions {
    val navController = rememberNavController()
    return NavigationActions(navController)
  }

  @Test
  fun testCloseButtonIsDisplayedAndClickable() {
    composeTestRule.setContent {
      PicTakenScreen(null, navigationActions = createFakeNavigationActions())
    }
    composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testImageDisplayedWhenBitmapIsNull() {
    composeTestRule.setContent {
      PicTakenScreen(null, navigationActions = createFakeNavigationActions())
    }
    composeTestRule.onNodeWithTag("googleLogoImage").assertIsDisplayed()
  }

  @Test
  fun testCropIconIsDisplayedAndClickable() {
    composeTestRule.setContent {
      PicTakenScreen(null, navigationActions = createFakeNavigationActions())
    }
    composeTestRule.onNodeWithTag("cropIcon").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testFilterIconIsDisplayedAndClickable() {
    composeTestRule.setContent {
      PicTakenScreen(null, navigationActions = createFakeNavigationActions())
    }
    composeTestRule.onNodeWithTag("filterIcon").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testSaveButtonIsDisplayedAndClickable() {
    composeTestRule.setContent {
      PicTakenScreen(null, navigationActions = createFakeNavigationActions())
    }
    composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testPublishButtonIsDisplayedAndClickable() {
    composeTestRule.setContent {
      PicTakenScreen(null, navigationActions = createFakeNavigationActions())
    }
    composeTestRule.onNodeWithTag("publishButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun saveButton_clickAction() {
    composeTestRule.setContent {
      PicTakenScreen(photoFile = null, navigationActions = createFakeNavigationActions())
    }
    composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed().performClick()
  }

  @Test
  fun publishButton_clickAction() {
    composeTestRule.setContent {
      PicTakenScreen(photoFile = null, navigationActions = createFakeNavigationActions())
    }
    composeTestRule.onNodeWithTag("publishButton").assertIsDisplayed().performClick()
  }
}
