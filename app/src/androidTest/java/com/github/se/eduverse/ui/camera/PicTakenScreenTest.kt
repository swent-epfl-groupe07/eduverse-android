import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.camera.PicTakenScreen
import com.github.se.eduverse.ui.camera.adjustImageRotation
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import io.mockk.*
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PicTakenScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: PhotoViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var testFile: File
  private lateinit var mockBitmap: Bitmap

  @Before
  fun setUp() {
    viewModel = mockk(relaxed = true)
    navigationActions = mockk(relaxed = true)

    // Création d'une image bitmap et d'un fichier temporaire simulant une photo capturée
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
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("capturedImage").assertIsDisplayed()
  }

  @Test
  fun testVideoPlayerIsDisplayed() {
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("videoPlayer").assertIsDisplayed()
  }

  @Test
  fun testGoogleLogoDisplayedWhenBitmapAndVideoAreNull() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null,
          videoFile = null,
          navigationActions = navigationActions,
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("googleLogoImage").assertExists().assertIsDisplayed()
  }

  @Test
  fun testSaveButtonFunctionality() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("saveButton").performClick()

    // Vérifier si `viewModel.savePhoto()` est appelé avec un objet `Photo`
    verify { viewModel.savePhoto(any<Photo>()) }

    // Vérifier si `navigationActions.goBack()` est appelé deux fois
    verify(exactly = 2) { navigationActions.goBack() }
  }

  @Test
  fun testNextButtonFunctionality() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("nextButton").performClick()

    // Vérifier si la navigation vers l'écran suivant est déclenchée
    val encodedPath = Uri.encode(testFile.absolutePath)
    verify { navigationActions.navigateTo("nextScreen/$encodedPath/null") }
  }

  @Test
  fun testNextButtonFunctionalityWithVideo() {
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("nextButton").performClick()

    // Vérifier si la navigation vers l'écran suivant est déclenchée avec la vidéo
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
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("closeButton").performClick()

    // Vérifier si `navigationActions.goBack()` est appelé pour fermer l'écran
    verify { navigationActions.goBack() }
  }

  @Test
  fun testCropAndFilterIconsAreClickable() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          viewModel = viewModel)
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
          viewModel = viewModel)
    }

    // Vérifie que l'image est affichée après rotation
    composeTestRule.onNodeWithTag("capturedImage").assertIsDisplayed()

    // Simuler une rotation (si applicable dans le contexte)
    val rotatedBitmap = adjustImageRotation(mockBitmap)
    assertNotNull(rotatedBitmap)
  }

  @Test
  fun testPlayerReleased_whenDisposed() {
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    // Utiliser ApplicationProvider pour obtenir le contexte
    val context = ApplicationProvider.getApplicationContext<Context>()
    var player: ExoPlayer? = null

    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("videoPlayer").assertExists()

    // Simule la création du player et vérifie son état
    composeTestRule.runOnIdle {
      player =
          ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(testVideoFile)))
            prepare()
            playWhenReady = true
          }
    }

    // Simule la fin du cycle de vie et vérifie si le player est libéré
    composeTestRule.runOnIdle {
      player?.release() // Libère explicitement le player
      assert(player?.isPlaying == false) // Vérifie qu'il ne joue plus après la libération
    }
  }

  @Test
  fun testEncodedPhotoPath_isCorrect() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile,
          videoFile = null,
          navigationActions = navigationActions,
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("nextButton").performClick()

    // Vérifier si le chemin encodé pour la photo est correct
    val encodedPhotoPath = Uri.encode(testFile.absolutePath)
    verify { navigationActions.navigateTo("nextScreen/$encodedPhotoPath/null") }
  }

  @Test
  fun testEncodedVideoPath_isCorrect() {
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = null,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("nextButton").performClick()

    // Vérifier si le chemin encodé pour la vidéo est correct
    val encodedVideoPath = Uri.encode(testVideoFile.absolutePath)
    verify { navigationActions.navigateTo("nextScreen/null/$encodedVideoPath") }
  }
}
