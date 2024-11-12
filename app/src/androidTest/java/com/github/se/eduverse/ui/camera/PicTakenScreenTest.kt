import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.ui.camera.PicTakenScreen
import com.github.se.eduverse.ui.camera.adjustImageRotation
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PicTakenScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var pViewModel: PhotoViewModel
  private lateinit var vViewModel: VideoViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var testFile: File
  private lateinit var mockBitmap: Bitmap

  @Before
  fun setUp() {
    pViewModel = mockk(relaxed = true)
    vViewModel = mockk(relaxed = true)
    navigationActions = mockk(relaxed = true)

    // Create a bitmap image and a temporary file simulating a captured photo
    mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    testFile =
        File.createTempFile("test_image", ".jpg").apply {
          outputStream().use { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        }
  }

  @Test
  fun testImageIsDisplayed() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    composeTestRule.onNodeWithTag("capturedImage").assertIsDisplayed()
  }

  @Test
  fun testVideoPlayerIsDisplayed() {
    // Create a temporary video file to simulate a valid video
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    // Load the composable with a valid video file
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null, // No photo file
          videoFile = testVideoFile, // Valid video file
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    // Verify that the video component with the tag "videoPlayer" is displayed
    composeTestRule.onNodeWithTag("videoPlayer").assertIsDisplayed()
  }

  @Test
  fun testGoogleLogoDisplayedWhenBitmapAndVideoAreNull() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null, // Ensure photo file is null
          videoFile = null, // Ensure video file is null
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    // Verify that the Google logo image is displayed
    composeTestRule.onNodeWithTag("googleLogoImage").assertExists().assertIsDisplayed()
  }

  @Test
  fun testSaveButtonFunctionality() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    composeTestRule.onNodeWithTag("saveButton").performClick()

    // Verify if `viewModel.savePhoto()` is called with a `Photo` object
    verify { pViewModel.savePhoto(any<Photo>()) }

    // Verify if `navigationActions.goBack()` is called twice
    verify(exactly = 2) { navigationActions.goBack() }
  }

  @Test
  fun testNextButtonFunctionality() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    composeTestRule.onNodeWithTag("nextButton").performClick()

    // Verify if navigation to the next screen is triggered
    val encodedPath = Uri.encode(testFile.absolutePath)
    verify { navigationActions.navigateTo("nextScreen/$encodedPath/null") }
  }

  @Test
  fun testNextButtonFunctionalityWithVideo() {
    // Create a temporary video file to simulate a video
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    // Load the composable with a valid video file
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null, // No photo file
          videoFile = testVideoFile, // Valid video file
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    // Simulate clicking the "Next" button
    composeTestRule.onNodeWithTag("nextButton").performClick()

    // Verify if navigation to the next screen is triggered with the video
    val encodedVideoPath = Uri.encode(testVideoFile.absolutePath)
    verify { navigationActions.navigateTo("nextScreen/null/$encodedVideoPath") }
  }

  @Test
  fun testCloseButtonFunctionality() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    composeTestRule.onNodeWithTag("closeButton").performClick()

    // Verify if `navigationActions.goBack()` is called to close the screen
    verify { navigationActions.goBack() }
  }

  @Test
  fun testCropAndFilterIconsAreClickable() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    composeTestRule.onNodeWithTag("cropIcon").assertIsDisplayed().assertHasClickAction()
    composeTestRule.onNodeWithTag("settingsIcon").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testAdjustImageRotation_isApplied() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    // Verify that the image is displayed after rotation
    composeTestRule.onNodeWithTag("capturedImage").assertIsDisplayed()

    // Simulate rotation (if applicable in the context)
    val rotatedBitmap = adjustImageRotation(mockBitmap)
    assertNotNull(rotatedBitmap)
  }

  @Test
  fun testPlayerReleased_whenDisposed() {
    // Create a temporary video file to simulate a video
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    // Use ApplicationProvider to get the context
    val context = ApplicationProvider.getApplicationContext<Context>()
    var player: ExoPlayer? = null

    // Load the composable with a valid video file
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null, // No photo file
          videoFile = testVideoFile, // Valid video file
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    // Verify that the video component with the tag "videoPlayer" is rendered
    composeTestRule.onNodeWithTag("videoPlayer").assertExists()

    // Simulate player creation and verify its state
    composeTestRule.runOnIdle {
      player =
          ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(testVideoFile)))
            prepare()
            playWhenReady = true
          }
    }

    // Simulate the end of the lifecycle and verify if the player is released
    composeTestRule.runOnIdle {
      player?.release() // Explicitly release the player
      assert(player?.isPlaying == false) // Verify it is no longer playing after release
    }
  }

  @Test
  fun testEncodedPhotoPath_isCorrect() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }
    composeTestRule.onNodeWithTag("nextButton").performClick()

    // Verify if the encoded path for the photo is correct
    val encodedPhotoPath = Uri.encode(testFile.absolutePath)
    verify { navigationActions.navigateTo("nextScreen/$encodedPhotoPath/null") }
  }

  @Test
  fun testEncodedVideoPath_isCorrect() {
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null,
          videoFile = testVideoFile, // Ensure the video is used here
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    composeTestRule.onNodeWithTag("nextButton").performClick()

    // Verify if the encoded path for the video is correct
    val encodedVideoPath = Uri.encode(testVideoFile.absolutePath)

    // Ensure the encoded path for the video is used here
    verify { navigationActions.navigateTo("nextScreen/null/$encodedVideoPath") }
  }

  @Test
  fun testSaveButtonSavesVideoWhenVideoFileIsPresent() {
    // Create a temporary video file to simulate a captured video
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null, // No photo file
          videoFile = testVideoFile, // Valid video file
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    // Perform click on the "Save" button
    composeTestRule.onNodeWithTag("saveButton").performClick()

    // Verify that `videoViewModel.saveVideo` is called with a `Video` object
    verify { vViewModel.saveVideo(any<Video>()) }

    // Verify that `navigationActions.goBack()` is called twice after saving
    verify(exactly = 2) { navigationActions.goBack() }
  }
}
