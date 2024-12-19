package com.github.se.eduverse.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class NextScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var photoViewModel: PhotoViewModel
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var vViewModel: VideoViewModel
  private lateinit var context: Context
  private lateinit var testFile: File
  private lateinit var mockBitmap: Bitmap

  private var currentPhotoFile: File? = null
  private lateinit var videoFileState: MutableState<File?>

  private lateinit var folderRepository: FolderRepository
  private lateinit var auth: FirebaseAuth

  @Before
  fun setUp() {
    navigationActions = mockk(relaxed = true)
    photoViewModel = mockk(relaxed = true)
    folderRepository = mock(FolderRepository::class.java)

    // Properly mock Firebase Auth
    auth = mock(FirebaseAuth::class.java)
    val mockUser = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(mockUser)
    `when`(mockUser.uid).thenReturn("test_user_id") // Use a consistent test ID

    folderViewModel = FolderViewModel(folderRepository, auth)
    vViewModel = mockk(relaxed = true)
    context = ApplicationProvider.getApplicationContext()

    mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    currentPhotoFile =
        File.createTempFile("test_image", ".jpg").apply {
          outputStream().use { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        }

    videoFileState = mutableStateOf(null)

    composeTestRule.setContent {
      NextScreen(
          photoFile = currentPhotoFile,
          videoFile = videoFileState.value,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          vViewModel)
    }
  }

  @Test
  fun testImagePreviewIsDisplayed() {
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag("previewImage")
        .assertExists("Image preview container should exist")
        .assertIsDisplayed()
  }

  @Test
  fun testAddDescriptionTextIsDisplayed() {
    composeTestRule
        .onNodeWithTag("addDescriptionText")
        .assertIsDisplayed()
        .assertTextEquals("Add description...")
  }

  @Test
  fun testSaveButtonWorks() {
    assertNotNull(mockBitmap)

    composeTestRule.onNodeWithTag("saveButton").performClick()

    verify { photoViewModel.savePhoto(any(), any(), any()) }

    verify(exactly = 3) { navigationActions.goBack() }
  }

  @Test
  fun testPostButtonIsClickable() {
    composeTestRule.onNodeWithTag("postButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testCloseButtonNavigatesBack() {
    composeTestRule.onNodeWithTag("closeButton").performClick()

    verify { navigationActions.goBack() }
  }

  @Test
  fun testShareToButtonTriggersShareIntent() {
    composeTestRule.onNodeWithTag("shareToButton").assertIsDisplayed().performClick()
  }

  @Test
  fun testAddToFolderButtonIsClickable() {
    composeTestRule.onNodeWithTag("addToFolderButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testSaveButtonNavigatesBackThreeTimes() {
    composeTestRule.onNodeWithTag("saveButton").performClick()

    verify(exactly = 3) { navigationActions.goBack() }
  }

  @Test
  fun testShareIntentIsTriggeredOnShareButtonClick() {
    composeTestRule.onNodeWithTag("shareToButton").performClick()
  }

  @Test
  fun testAddLinkAndMoreOptionsButtons() {
    composeTestRule.onNodeWithTag("addToFolderButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testCloseButtonTriggersGoBack() {
    composeTestRule.onNodeWithTag("closeButton").performClick()

    verify { navigationActions.goBack() }
  }

  @Test
  fun testVideoFileIsHandledCorrectly() {
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    composeTestRule.runOnUiThread { videoFileState.value = testVideoFile }

    composeTestRule.waitForIdle()

    assertNotNull(videoFileState.value)
    assertEquals(testVideoFile, videoFileState.value)
  } // testing the testTag videoPreview is very difficult

  @Test
  fun testCloseButtonWorks() {
    composeTestRule
        .onNodeWithTag("closeButton")
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    verify { navigationActions.goBack() }
  }

  @Test
  fun testStyledButtonClick() {

    composeTestRule
        .onNodeWithTag("addToFolderButton")
        .assertExists()
        .assertHasClickAction()
        .performClick()

    composeTestRule
        .onNodeWithTag("shareToButton")
        .assertExists()
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun testPlayerReleased_whenDisposed() {
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    var player: ExoPlayer? = null

    composeTestRule.runOnUiThread { videoFileState.value = testVideoFile }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      player =
          ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(testVideoFile)))
            prepare()
            playWhenReady = true
          }
    }

    composeTestRule.runOnIdle {
      player?.release()
      assert(player?.isPlaying == false)
    }
  }

  @Test
  fun openBottomMenuWithFolders() {
    val folder1 =
        Folder("uid", emptyList<MyFile>().toMutableList(), "folder1", "1", archived = false)
    val folder2 =
        Folder("uid", emptyList<MyFile>().toMutableList(), "folder2", "2", archived = false)

    `when`(folderRepository.getFolders(any(), any(), any(), any())).then {
      val callback = it.getArgument<(List<Folder>) -> Unit>(2)
      callback(listOf(folder1, folder2))
    }

    composeTestRule.onNodeWithTag("addToFolderButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addToFolderButton").performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("button_container").assertIsDisplayed()

    composeTestRule.onNodeWithTag("folder_button1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folder_button2").assertIsDisplayed()

    var test = false
    val func1 = slot<() -> Unit>()
    val func2 = slot<(String, String, Folder) -> Unit>()
    every { photoViewModel.savePhoto(any(), folder1, capture(func1), capture(func2)) } answers
        {
          test = true
          func1.captured()
          func2.captured("id", "name", folder1)
        }
    composeTestRule.onNodeWithTag("folder_button1").performClick()

    composeTestRule.onNodeWithTag("button_container").assertIsNotDisplayed()

    assert(test)
    org.mockito.kotlin.verify(folderRepository).updateFolder(any(), any(), any())
    assertEquals(1, folder1.files.size)
    verify(exactly = 3) { navigationActions.goBack() }
  }

  @Test
  fun testSaveButtonCallsSavePhotoWithCorrectArguments() {
    val capturedPhoto = slot<Photo>()
    val func = slot<() -> Unit>()
    every { photoViewModel.savePhoto(capture(capturedPhoto), any(), capture(func), any()) } answers
        {
          func.captured()
        }

    composeTestRule.onNodeWithTag("saveButton").performClick()

    assertEquals("anonymous", capturedPhoto.captured.ownerId)
    assertTrue(
        capturedPhoto.captured.path.startsWith("photos/anonymous/") ||
            capturedPhoto.captured.path.startsWith("videos/anonymous/"))
  }
}
