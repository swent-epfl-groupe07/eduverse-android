import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.ui.camera.NextScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import io.mockk.mockk
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*

class NextScreenTest3 {

  private lateinit var pViewModel: PhotoViewModel
  private lateinit var fViewModel: FolderViewModel
  private lateinit var vViewModel: VideoViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var testFile: File
  private lateinit var mockBitmap: Bitmap

  @Before
  fun setUp() {
    pViewModel = mockk(relaxed = true)
    fViewModel = mockk(relaxed = true)
    vViewModel = mockk(relaxed = true)
    navigationActions = mockk(relaxed = true)

    mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    testFile =
        File.createTempFile("test_image", ".jpg").apply {
          outputStream().use { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        }
  }

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testSaveButtonSavesVideoWhenVideoFileIsPresent() {
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    composeTestRule.setContent {
      NextScreen(
          photoFile = null,
          videoFile = testVideoFile, // Fichier vidéo valide
          navigationActions = navigationActions,
          pViewModel,
          fViewModel,
          vViewModel)
    }

    composeTestRule.onNodeWithTag("saveButton").performClick()

    io.mockk.verify { vViewModel.saveVideo(any<Video>()) }

    io.mockk.verify(exactly = 3) { navigationActions.goBack() }
  }
}
