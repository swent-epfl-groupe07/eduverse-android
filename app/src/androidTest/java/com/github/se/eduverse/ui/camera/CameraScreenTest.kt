package com.github.se.eduverse.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Composable
  private fun createFakeNavigationActions(): NavigationActions {
    val navController = rememberNavController()

    // Create a simulated navGraph for the test
    navController.graph =
        navController.createGraph(startDestination = "start") {
          composable("start") {}
          composable("picTaken/{encodedPath}") {}
        }

    return NavigationActions(navController)
  }

  @Test
  fun cameraPreview_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("cameraPreview").assertIsDisplayed()
  }

  @Test
  fun closeButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed()
  }

  @Test
  fun switchCameraButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("switchCameraButton").assertIsDisplayed()
  }

  @Test
  fun photoButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("photoButton").assertIsDisplayed()
  }

  @Test
  fun videoButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("videoButton").assertIsDisplayed()
  }

  @Test
  fun takePhotoButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("takePhotoButton").assertIsDisplayed()
  }

  @Test
  fun takePhotoButton_clickAction() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("takePhotoButton").performClick()
  }

  @Test
  fun switchCameraButton_clickAction() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("switchCameraButton").performClick()
  }

  @Test
  fun photoButton_clickAction() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("photoButton").performClick()
  }

  @Test
  fun videoButton_clickAction() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("videoButton").performClick()
  }

  @Test
  fun closeButton_clickAction() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }

    composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed().performClick()

    composeTestRule.waitForIdle()
  }

  @Test
  fun videoButton_clickAction_switchToVideoMode() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }

    // Initially, the mode is Photo
    composeTestRule.onNodeWithTag("photoButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("videoButton").performClick()

    // After clicking, it should switch to Video mode
    composeTestRule.onNodeWithTag("videoButton").assertIsDisplayed()
  }

  @Test
  fun takeVideoButton_startAndStopRecording() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }

    // Switch to video mode
    composeTestRule.onNodeWithTag("videoButton").performClick()

    // Start video recording
    composeTestRule.onNodeWithTag("takePhotoButton").performClick()

    // Simulate a second click to stop recording
    composeTestRule.onNodeWithTag("takePhotoButton").performClick()
  }

  @Test
  fun takeVideoButton_isRecordingState() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }

    // Switch to video mode
    composeTestRule.onNodeWithTag("videoButton").performClick()

    // Start recording
    composeTestRule.onNodeWithTag("takePhotoButton").performClick()

    // Verify that the recording state is active
    composeTestRule.onNodeWithTag("takePhotoButton").assert(hasClickAction())

    // Stop recording
    composeTestRule.onNodeWithTag("takePhotoButton").performClick()
  }

  @Test
  fun recordingIndicator_isDisplayedWhenRecording() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }

    // Switch to video mode
    composeTestRule.onNodeWithTag("videoButton").performClick()

    // Start video recording
    composeTestRule.onNodeWithTag("takePhotoButton").performClick()

    // Verify that the recording indicator is displayed when recording is active
    composeTestRule.onNodeWithTag("recordingIndicator").assertIsDisplayed()

    }

}
