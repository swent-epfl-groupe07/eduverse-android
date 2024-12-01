package com.github.se.eduverse.ui.camera

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import io.mockk.mockk
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class CropPhotoScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions

  private lateinit var pViewModel: PhotoViewModel
  private lateinit var testFile: File
  private lateinit var mockBitmap: Bitmap

  @Before
  fun setUp() {
    pViewModel = mockk(relaxed = true)
    navigationActions = mockk(relaxed = true)

    // Création d'une image bitmap et d'un fichier temporaire simulant une photo capturée
    mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    testFile =
        File.createTempFile("test_image", ".jpg").apply {
          outputStream().use { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        }
  }

  @Test
  fun test_cropPhotoScreen_loadsImage() {
    // Set up the composable
    composeTestRule.setContent {
      CropPhotoScreen(
          photoFile = testFile, photoViewModel = pViewModel, navigationActions = navigationActions)
    }

    // Check if the crop image is displayed by finding the Image composable with the testTag
    composeTestRule.onNodeWithTag("cropImage").assertExists()
  }

  @Test
  fun test_cropPhotoScreen_saveButton_clicked() {
    // Set up the composable
    composeTestRule.setContent {
      CropPhotoScreen(
          photoFile = testFile, photoViewModel = pViewModel, navigationActions = navigationActions)
    }

    // Check if the Save button is visible and perform a click action
    composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed().performClick()

    // Verify that the save method in PhotoViewModel was called
    io.mockk.verify { pViewModel.savePhoto(any<Photo>()) }

    // Verify that the navigation back action was triggered
    io.mockk.verify(exactly = 2) { navigationActions.goBack() }
  }

  @Test
  fun test_cropPhotoScreen_cancelButton_clicked() {
    // Set up the composable
    composeTestRule.setContent {
      CropPhotoScreen(
          photoFile = testFile, photoViewModel = pViewModel, navigationActions = navigationActions)
    }

    // Check if the Cancel button is visible and perform a click action
    composeTestRule.onNodeWithTag("cancelButton").assertIsDisplayed().performClick()

    // Verify that the navigation back action was triggered
    io.mockk.verify(exactly = 1) { navigationActions.goBack() }
  }

  @Test
  fun test_cropPhotoScreen_dragCropArea() {
    // Set up the composable
    composeTestRule.setContent {
      CropPhotoScreen(
          photoFile = testFile, photoViewModel = pViewModel, navigationActions = navigationActions)
    }

    // Simulate dragging the top left corner
    composeTestRule.onNodeWithTag("cropImage").performTouchInput {
      down(center)
      moveBy(Offset(10f, 10f)) // Drag the image
    }

    // Check if the UI updates correctly after dragging
    composeTestRule.onNodeWithTag("cropImage").assertExists()
  }
}
