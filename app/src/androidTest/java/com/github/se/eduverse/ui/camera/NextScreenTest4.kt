package com.github.se.eduverse.ui.camera

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import io.mockk.mockk
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NextScreenPostTests {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var photoViewModel: PhotoViewModel
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var videoViewModel: VideoViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var testPhotoFile: File
  private lateinit var testVideoFile: File

  @Before
  fun setUp() {
    // Simple mockk setup
    photoViewModel = mockk(relaxed = true)
    folderViewModel = mockk(relaxed = true)
    videoViewModel = mockk(relaxed = true)
    navigationActions = mockk(relaxed = true)

    // Create simple test files
    testPhotoFile = File.createTempFile("test_photo", ".jpg")
    testVideoFile = File.createTempFile("test_video", ".mp4")
  }

  @Test
  fun testPhotoPostPath() {
    composeTestRule.setContent {
      NextScreen(
          photoFile = testPhotoFile,
          videoFile = null,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          videoViewModel = videoViewModel)
    }

    // This click will trigger the photo upload code path
    composeTestRule.onNodeWithTag("postButton").performClick()

    // Give some time for the code to execute
    Thread.sleep(500)
  }

  @Test
  fun testVideoPostPath() {
    composeTestRule.setContent {
      NextScreen(
          photoFile = null,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          videoViewModel = videoViewModel)
    }

    // This click will trigger the video upload code path
    composeTestRule.onNodeWithTag("postButton").performClick()

    // Give some time for the code to execute
    Thread.sleep(500)
  }
}
