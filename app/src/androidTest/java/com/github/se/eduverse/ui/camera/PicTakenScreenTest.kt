package com.github.se.eduverse.ui.camera

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import io.mockk.mockkStatic
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertIsDisplayed
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest

@RunWith(AndroidJUnit4::class)
class PicTakenScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule
  val instantExecutorRule = InstantTaskExecutorRule() // Helps with LiveData testing

  private val navigationActions = mockk<NavigationActions>(relaxed = true)
  private val viewModel = mockk<PhotoViewModel>(relaxed = true)
  private val photoFile = mockk<File>()
  private val bitmapMock = mockk<Bitmap>()

  @Before
  fun setUp() {
    every { photoFile.exists() } returns false
    every { photoFile.path } returns "test/path"

    mockkStatic(BitmapFactory::class)
    every { BitmapFactory.decodeFile("test/path") } returns bitmapMock

    every { bitmapMock.width } returns 100
    every { bitmapMock.height } returns 100
  }

  /*@Test
  fun showsCapturedImage_whenPhotoFileExists() {
    // Arrange
    every { photoFile.exists() } returns true

    // Act
    composeTestRule.setContent {
      PicTakenScreen(photoFile = photoFile, navigationActions = navigationActions, viewModel = viewModel)
    }

    // Assert
    composeTestRule.onNodeWithTag("capturedImage").assertIsDisplayed()
  }*/

  @Test
  fun showsGoogleLogoImage_whenPhotoFileDoesNotExist() {
    // Act
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = photoFile, navigationActions = navigationActions, viewModel = viewModel)
    }

    // Assert
    composeTestRule.onNodeWithTag("googleLogoImage").assertIsDisplayed()
  }

  @Test
  fun cropIcon_isDisplayed() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = photoFile, navigationActions = navigationActions, viewModel = viewModel)
    }
    composeTestRule.onNodeWithTag("cropIcon").assertIsDisplayed()
  }

  @Test
  fun filterIcon_isDisplayed() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = photoFile, navigationActions = navigationActions, viewModel = viewModel)
    }
    composeTestRule.onNodeWithTag("filterIcon").assertIsDisplayed()
  }

  @Test
  fun saveButton_isDisplayed() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = photoFile, navigationActions = navigationActions, viewModel = viewModel)
    }
    composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed()
  }

  @Test
  fun publishButton_isDisplayed() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = photoFile, navigationActions = navigationActions, viewModel = viewModel)
    }
    composeTestRule.onNodeWithTag("publishButton").assertIsDisplayed()
  }

  @Test
  fun closeButton_isDisplayed() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = photoFile, navigationActions = navigationActions, viewModel = viewModel)
    }
    composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed()
  }

  /*@Test
  fun saveButton_onClick_callsSavePhotoAndNavigatesBack() = runBlocking  {
    // Arrange
    composeTestRule.setContent {
      PicTakenScreen(
        photoFile = photoFile,
        navigationActions = navigationActions,
        viewModel = viewModel
      )
    }

    // Act: Click on the save button
    composeTestRule.onNodeWithTag("saveButton").performClick()

    // Assert: Use coVerify for coroutines
    coVerify(exactly = 1) {
      viewModel.savePhoto(match { photo ->
        photo.ownerId == "user123" &&
                photo.path.startsWith("photos/user123/") &&
                photo.photo.isNotEmpty()
      })
    }
    coVerify(exactly = 2) { navigationActions.goBack() }
  }*/
}
