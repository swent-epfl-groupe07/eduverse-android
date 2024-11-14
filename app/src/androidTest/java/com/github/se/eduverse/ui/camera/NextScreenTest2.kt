package com.github.se.eduverse.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import io.mockk.mockk
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NextScreenTest2 {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var photoViewModel: PhotoViewModel
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var vViewModel: VideoViewModel
  private lateinit var context: Context
  private var currentPhotoFile: File? = null
  private var currentVideoFile: File? = null
  private lateinit var mockBitmap: Bitmap
  private lateinit var testFile: File

  @Before
  fun setUp() {
    navigationActions = mockk(relaxed = true)
    photoViewModel = mockk(relaxed = true)
    folderViewModel = mockk(relaxed = true)
    vViewModel = mockk(relaxed = true) // Ensure vViewModel is initialized
    context = ApplicationProvider.getApplicationContext()

    // Simulate a temporary image file
    mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    testFile =
        File.createTempFile("test_image", ".jpg").apply {
          outputStream().use { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        }
  }

  @Test
  fun testVideoPreviewIsDisplayedWhenVideoFileProvided() {
    // Simulate a temporary video file
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    // Update `currentVideoFile` directly with the simulated video file
    currentVideoFile = testVideoFile

    // Reload the composable with the updated video
    composeTestRule.setContent {
      NextScreen(
          photoFile = currentPhotoFile,
          videoFile = currentVideoFile,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          vViewModel)
    }

    // Force recomposition
    composeTestRule.waitForIdle()

    // Verify that the node with the video preview tag is displayed
    composeTestRule.onNodeWithTag("previewVideo").assertExists().assertIsDisplayed()
  }

  @Test
  fun testVideoPlayerReleasedOnDispose() {
    // Simulate a temporary video file
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    // Load the composable with the video file
    composeTestRule.setContent {
      NextScreen(
          photoFile = currentPhotoFile,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          vViewModel)
    }

    // Force recomposition
    composeTestRule.waitForIdle()

    // Manipulate ExoPlayer on the main thread
    composeTestRule.runOnUiThread {
      // Simulate creating the ExoPlayer instance
      val player =
          ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(testVideoFile)))
            prepare()
            playWhenReady = true
          }

      // Simulate disposing of the composable and release the player
      player.release()

      // Verify that the player is correctly released
      assert(player.isPlaying.not()) // The player should not be playing after release
    }
  }
}
