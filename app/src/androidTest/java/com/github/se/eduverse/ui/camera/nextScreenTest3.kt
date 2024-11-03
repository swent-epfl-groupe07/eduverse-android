import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.ui.camera.NextScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
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
  private lateinit var vViewModel: VideoViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var testFile: File
  private lateinit var mockBitmap: Bitmap

  @Before
  fun setUp() {
    pViewModel = mockk(relaxed = true)
    vViewModel = mockk(relaxed = true)
    navigationActions = mockk(relaxed = true)

    // Création d'une image bitmap et d'un fichier temporaire simulant une photo capturée
    mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    testFile =
        File.createTempFile("test_image", ".jpg").apply {
          outputStream().use { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        }
  }

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testSaveButtonSavesVideoWhenVideoFileIsPresent() {
    // Crée un fichier vidéo temporaire pour simuler une vidéo capturée
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    composeTestRule.setContent {
      NextScreen(
          photoFile = null, // Pas de fichier photo
          videoFile = testVideoFile, // Fichier vidéo valide
          navigationActions = navigationActions,
          pViewModel,
          vViewModel)
    }

    // Effectuer un clic sur le bouton "Save"
    composeTestRule.onNodeWithTag("saveButton").performClick()

    // Vérifier que `videoViewModel.saveVideo` est appelé avec un objet `Video`
    io.mockk.verify { vViewModel.saveVideo(any<Video>()) }

    // Vérifier que `navigationActions.goBack()` est appelé deux fois après la sauvegarde
    io.mockk.verify(exactly = 3) { navigationActions.goBack() }
  }
}
