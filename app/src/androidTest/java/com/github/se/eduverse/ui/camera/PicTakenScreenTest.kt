import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.camera.PicTakenScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import io.mockk.*
import java.io.File
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
          photoFile = testFile, navigationActions = navigationActions, viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("capturedImage").assertIsDisplayed()
  }

  @Test
  fun testGoogleLogoDisplayedWhenBitmapIsNull() {
    // Reconfigurer le contenu avec un fichier `photoFile` nul
    composeTestRule.setContent {
      PicTakenScreen(photoFile = null, navigationActions = navigationActions, viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("googleLogoImage").assertExists().assertIsDisplayed()
  }

  @Test
  fun testSaveButtonFunctionality() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile, navigationActions = navigationActions, viewModel = viewModel)
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
          photoFile = testFile, navigationActions = navigationActions, viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("nextButton").performClick()

    // Vérifier si la navigation vers l'écran suivant est déclenchée
    val encodedPath = Uri.encode(testFile.absolutePath)
    verify { navigationActions.navigateTo("nextScreen/$encodedPath") }
  }

  @Test
  fun testCloseButtonFunctionality() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile, navigationActions = navigationActions, viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("closeButton").performClick()

    // Vérifier si `navigationActions.goBack()` est appelé pour fermer l'écran
    verify { navigationActions.goBack() }
  }

  @Test
  fun testCropAndFilterIconsAreClickable() {
    composeTestRule.setContent {
      PicTakenScreen(
          photoFile = testFile, navigationActions = navigationActions, viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("cropIcon").assertIsDisplayed().assertHasClickAction()
    composeTestRule.onNodeWithTag("filterIcon").assertIsDisplayed().assertHasClickAction()
  }
}
