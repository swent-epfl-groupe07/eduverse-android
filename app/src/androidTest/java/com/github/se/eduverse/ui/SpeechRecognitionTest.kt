package com.github.se.eduverse.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.ui.speechRecognition.SpeechRecognizerInterface
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SpeechRecognizerTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockContext: Context
  private lateinit var context: Context

  private lateinit var mockOnResult: (String) -> Unit

  @Before
  fun setUp() {
    mockContext = mock(Context::class.java)
    context = ApplicationProvider.getApplicationContext()
    mockOnResult = mock()
  }

  @Test
  fun test_SpeechRecognizer_permissionDenied_correctlyDisplaysPermissionDialog() {
    // Mock micro permission denied
    `when`(mockContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO))
        .thenReturn(PackageManager.PERMISSION_DENIED)

    composeTestRule.setContent {
      SpeechRecognizerInterface(
          context = mockContext,
          title = "Test Title",
          description = "Test Description",
          onDismiss = {},
          onResult = {},
          finishButton = {})
    }

    // Verify that permission dialog is correctly displayed when permission is denied
    composeTestRule.onNodeWithTag("permissionDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speechDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("permissionDialogTitle").assertTextEquals("Permission Denied")
    composeTestRule
        .onNodeWithTag("permissionDialogMessage")
        .assertTextEquals(
            "Audio recording permission is required to use this feature. Please enable the permission in the app settings, to be able to proceed.")
    composeTestRule.onNodeWithTag("permissionDialogDismissButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("permissionDialogDismissButton").assertTextEquals("Cancel")
    composeTestRule.onNodeWithTag("permissionDialogDismissButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("EnableButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("EnableButton").assertTextEquals("Enable")
    composeTestRule.onNodeWithTag("EnableButton").assertHasClickAction()
  }

  @Test
  fun test_SpeechRecognizer_permissionGranted_correctlyDisplaysSpeechRecognitionDialog() {
    // Mock microphone permission granted
    `when`(mockContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO))
        .thenReturn(PackageManager.PERMISSION_GRANTED)

    composeTestRule.setContent {
      SpeechRecognizerInterface(
          context = mockContext,
          title = "Test Title",
          description = "Test Description",
          onDismiss = {},
          onResult = {},
          finishButton = { enabled ->
            Button(onClick = {}, enabled = enabled, modifier = Modifier.testTag("finish")) {
              Text("Finish")
            }
          })
    }

    // Verify Speech Recognition Dialog is correctly displayed when permission is granted
    composeTestRule.onNodeWithTag("speechDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("permissionDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("speechDialogTitle").assertTextEquals("Test Title")
    composeTestRule.onNodeWithTag("speechDialogDescription").assertTextEquals("Test Description")
    composeTestRule.onNodeWithTag("speechDialogRecordButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speechDialogRecordButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("speechDialogDismissButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speechDialogDismissButton").assertTextEquals("Cancel")
    composeTestRule.onNodeWithTag("speechDialogDismissButton").assertHasClickAction()
    composeTestRule
        .onNodeWithTag("speechDialogRecordInstructions")
        .assertTextEquals(
            "Press the button below to start/stop recording. Speak clearly when recording. If you stay silent for a while, the recording will stop automatically.")
    composeTestRule.onNodeWithTag("finish").assertIsDisplayed()
    composeTestRule.onNodeWithTag("finish").assertTextEquals("Finish")
    composeTestRule.onNodeWithTag("finish").assertHasClickAction()
  }
}
