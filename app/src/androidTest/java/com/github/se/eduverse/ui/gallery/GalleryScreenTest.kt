package com.github.se.eduverse.ui.gallery

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class GalleryScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakePhotoViewModel: FakePhotoViewModel
  private lateinit var fakeVideoViewModel: FakeVideoViewModel
  private val mockNavigationActions = aliBouiri(navController = mock())

  private lateinit var fakeFolderViewModel: FolderViewModel
  private lateinit var folderRepository: FolderRepository
  private lateinit var auth: FirebaseAuth

  val folder1 = Folder("uid", emptyList<MyFile>().toMutableList(), "folder1", "1", archived = false)
  val folder2 = Folder("uid", emptyList<MyFile>().toMutableList(), "folder2", "2", archived = false)

  @Before
  fun setup() {
    fakePhotoViewModel = FakePhotoViewModel()
    fakeVideoViewModel = FakeVideoViewModel()

    folderRepository = mock(FolderRepository::class.java)
    auth = mock(FirebaseAuth::class.java)
    val user = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(user)

    `when`(user.uid).thenReturn("uid")

    `when`(
            folderRepository.getFolders(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any()))
        .then {
          val callback = it.getArgument<(List<Folder>) -> Unit>(2)
          callback(listOf(folder1, folder2))
        }
    fakeFolderViewModel = FolderViewModel(folderRepository, auth)
  }

  @Test
  fun whenGalleryLoads_showsNoMediaMessage() {
    // Simulates empty lists for ViewModels
    fakePhotoViewModel.setPhotos(emptyList())
    fakeVideoViewModel.setVideos(emptyList())

    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions)
    }

    composeTestRule.waitForIdle()

    // Verifies the existence of the Box with the tag "NoPhotosBox"
    composeTestRule.onNodeWithTag("NoPhotosBox").assertExists()

    // Verifies that the Text with the tag "NoPhotosText" contains the expected text
    composeTestRule
        .onNodeWithTag("NoPhotosText")
        .assertExists()
        .assertTextEquals("No media available")
  }

  @Test
  fun whenMediaIsAvailable_showsMediaGrid() {
    val photo = Photo(ownerId = "testOwner", photo = ByteArray(0), path = "testPhotoPath")
    val video = Video(ownerId = "testOwner", video = ByteArray(0), path = "testVideoPath")

    fakePhotoViewModel.setPhotos(listOf(photo))
    fakeVideoViewModel.setVideos(listOf(video))

    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions)
    }

    composeTestRule.onNodeWithTag("PhotoItem_testPhotoPath").assertExists()
    composeTestRule.onNodeWithTag("VideoItem_testVideoPath").assertExists()
  }

  @Test
  fun testVideoItemClick() {
    val video = Video(ownerId = "testOwner", video = ByteArray(0), path = "testVideoPath")
    fakeVideoViewModel.setVideos(listOf(video))

    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions)
    }

    composeTestRule.onNodeWithTag("VideoItem_testVideoPath").performClick()
    composeTestRule.onNodeWithTag("MediaDetailDialog").assertExists()
  }

  @Test
  fun testVideoDialogDownload() {
    val video = Video(ownerId = "testOwner", video = ByteArray(0), path = "testVideoPath")
    fakeVideoViewModel.setVideos(listOf(video))

    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions)
    }

    composeTestRule.onNodeWithTag("VideoItem_testVideoPath").performClick()
    composeTestRule.onNodeWithTag("MediaDetailDialog").assertExists()

    composeTestRule.onNodeWithTag("DownloadVideoButton").performClick()
    // Verify that the download action is triggered (add logs or mocks to check this)
  }

  @Test
  fun testVideoDialogDismiss() {
    val video = Video(ownerId = "testOwner", video = ByteArray(0), path = "testVideoPath")
    fakeVideoViewModel.setVideos(listOf(video))

    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions)
    }

    composeTestRule.onNodeWithTag("VideoItem_testVideoPath").performClick()
    composeTestRule.onNodeWithTag("MediaDetailDialog").assertExists()

    composeTestRule.onNodeWithTag("CloseButton").performClick()
    composeTestRule.onNodeWithTag("MediaDetailDialog").assertDoesNotExist()
  }

  @Test
  fun testAddPhotoToFolder() {
    val photo = Photo(ownerId = "testOwner", photo = ByteArray(0), path = "testPhotoPath")
    fakePhotoViewModel.setPhotos(listOf(photo))

    // Simulates adding a photo to a folder
    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions)
    }

    // Simulates clicking on the photo item to open the dialog
    composeTestRule.onNodeWithTag("PhotoItem_testPhotoPath").performClick()
    composeTestRule.onNodeWithTag("MediaDetailDialog").assertExists()

    // Simulates clicking the "Add to folder" button
    composeTestRule.onNodeWithTag("AddPhotoToFolderButton").performClick()

    composeTestRule.waitForIdle()

    // Verifies that folder buttons are displayed
    composeTestRule.onNodeWithTag("button_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folder_button1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folder_button2").assertIsDisplayed()

    // Simulates clicking on one of the folder buttons to add the photo
    composeTestRule.onNodeWithTag("folder_button1").performClick()
    composeTestRule.onNodeWithTag("button_container").assertIsNotDisplayed()

    // Verifies that the `updateFolder` method is called on the repository
    verify(folderRepository)
        .updateFolder(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())

    // Verifies that the photo was added to the folder `folder1`
    assertEquals(1, folder1.files.size)
  }

  @Test
  fun testPublicationItemDisplaysVideoCorrectly() {
    composeTestRule.setContent {
      PublicationItem(
          mediaType = MediaType.VIDEO,
          thumbnailUrl = "testThumbnailUrl",
          onClick = {},
          modifier = Modifier.testTag("VideoItem"))
    }

    // Verifies that the PublicationItem component with the thumbnail is displayed
    composeTestRule.onNodeWithTag("VideoItem", useUnmergedTree = true).assertExists()
    composeTestRule
        .onNodeWithTag("VideoThumbnail", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Verifies that the video overlay and play icon are displayed
    composeTestRule
        .onNodeWithTag("VideoOverlay", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("PlayIcon", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun testPublicationItemPhotoTag() {
    composeTestRule.setContent {
      PublicationItem(
          mediaType = MediaType.PHOTO,
          thumbnailUrl = "testPhotoUrl",
          onClick = {},
          modifier = Modifier.testTag("PhotoCard"))
    }

    // Verifies that the item with the tag "PhotoCard" exists and is displayed
    composeTestRule
        .onNodeWithTag("PhotoCard", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun testGoBackButton() {
    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions)
    }

    // Verifies that the GoBackButton is displayed
    composeTestRule.onNodeWithTag("GoBackButton").assertExists().assertIsDisplayed()

    // Simulates a click on the GoBackButton
    composeTestRule.onNodeWithTag("GoBackButton").performClick()

    // Verifies that the `goBack()` navigation action was called
    assert(mockNavigationActions.backCalled)
  }
}

class FakePhotoViewModel : PhotoViewModel(mockRepository(), mock(FileRepository::class.java)) {

  // Simulates a list of photos
  override val _photos =
      MutableStateFlow(
          listOf(
              Photo(ownerId = "1", photo = ByteArray(0), path = "path1"),
              Photo(ownerId = "2", photo = ByteArray(0), path = "path2")))
  override val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

  private val _savePhotoState = MutableStateFlow(false)
  override val savePhotoState: StateFlow<Boolean> = _savePhotoState.asStateFlow()

  override fun getPhotosByOwner(ownerId: String) {
    // No-op for tests
  }

  override fun makeFileFromPhoto(photo: Photo, onSuccess: (String) -> Unit) {
    onSuccess("success")
  }

  override fun makeFileFromPhoto(photo: Photo, onSuccess: (String) -> Unit) {
    onSuccess("success")
  }

  fun setPhotos(newPhotos: List<Photo>) {
    _photos.value = newPhotos
  }
}

private fun mockRepository() =
    object : com.github.se.eduverse.model.repository.IPhotoRepository {
      override suspend fun savePhoto(photo: Photo): Boolean = true

      override suspend fun updatePhoto(photoId: String, photo: Photo): Boolean = true

      override suspend fun deletePhoto(photoId: String): Boolean = true

      override suspend fun getPhotosByOwner(ownerId: String): List<Photo> = emptyList()
    }

class FakeVideoViewModel : VideoViewModel(mockVideoRepository(), mock(FileRepository::class.java)) {

  // Simulates a list of videos
  override val _videos =
      MutableStateFlow(
          listOf(
              Video(ownerId = "1", video = ByteArray(0), path = "videoPath1"),
              Video(ownerId = "2", video = ByteArray(0), path = "videoPath2")))
  override val videos: StateFlow<List<Video>> = _videos.asStateFlow()

  private val _saveVideoState = MutableStateFlow(false)
  override val saveVideoState: StateFlow<Boolean> = _saveVideoState.asStateFlow()

  override fun getVideosByOwner(ownerId: String) {
    // No-op for tests
  }

  fun setVideos(newVideos: List<Video>) {
    _videos.value = newVideos
  }
}

private fun mockVideoRepository() =
    object : com.github.se.eduverse.model.repository.IVideoRepository {
      override suspend fun saveVideo(video: Video): Boolean = true

      override suspend fun updateVideo(videoId: String, video: Video): Boolean = true

      override suspend fun deleteVideo(videoId: String): Boolean = true

      override suspend fun getVideosByOwner(ownerId: String): List<Video> = emptyList()
    }

class aliBouiri(navController: NavHostController) : NavigationActions(navController) {
  var backCalled = false

  override fun goBack() {
    backCalled = true
  }
}
