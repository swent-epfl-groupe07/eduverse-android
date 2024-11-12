import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.ui.camera.NextScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.File
import org.junit.Assert.assertEquals
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

  private lateinit var folderRepository: FolderRepository
  private lateinit var auth: FirebaseAuth

  val folder1 = Folder("uid", emptyList<MyFile>().toMutableList(), "folder1", "1")
  val folder2 = Folder("uid", emptyList<MyFile>().toMutableList(), "folder2", "2")

  @Before
  fun setUp() {
    folderRepository = mock(FolderRepository::class.java)
    auth = mock(FirebaseAuth::class.java)
    val user = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(user)
    `when`(user.uid).thenReturn("")
    `when`(
            folderRepository.getFolders(
                org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any()))
        .then {
          val callback = it.getArgument<(List<Folder>) -> Unit>(1)
          callback(listOf(folder1, folder2))
        }

    pViewModel = mockk(relaxed = true)
    fViewModel = FolderViewModel(folderRepository, auth)
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

  @Test
  fun addToFolderWorksWithVideos() {
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    composeTestRule.setContent {
      NextScreen(
          photoFile = null, // Pas de fichier photo
          videoFile = testVideoFile, // Fichier vidéo valide
          navigationActions = navigationActions,
          pViewModel,
          fViewModel,
          vViewModel)
    }

    composeTestRule.onNodeWithTag("addToFolderButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addToFolderButton").performClick()

    var test = false
    val func = slot<(String, String, Folder) -> Unit>()
    every { vViewModel.saveVideo(any(), folder1, capture(func)) } answers
        {
          test = true
          func.captured("id", "name", folder1)
        }
    composeTestRule.onNodeWithTag("folder_button1").performClick()

    composeTestRule.onNodeWithTag("button_container").assertIsNotDisplayed()

    assert(test)
    org.mockito.kotlin
        .verify(folderRepository)
        .updateFolder(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())
    assertEquals(1, folder1.files.size)
    io.mockk.verify(exactly = 3) { navigationActions.goBack() }
  }
}
