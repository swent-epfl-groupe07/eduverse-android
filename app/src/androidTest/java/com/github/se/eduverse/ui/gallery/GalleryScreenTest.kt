// File: GalleryScreenTest.kt
package com.github.se.eduverse.ui.gallery

import android.net.Uri
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
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class GalleryScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakePhotoViewModel: FakePhotoViewModel
  private lateinit var fakeVideoViewModel: FakeVideoViewModel
  private lateinit var fakeFolderViewModel: FolderViewModel
  private lateinit var folderRepository: FolderRepository
  private lateinit var auth: FirebaseAuth

  private val folder1 =
      Folder("uid", emptyList<MyFile>().toMutableList(), "folder1", "1", archived = false)
  private val folder2 =
      Folder("uid", emptyList<MyFile>().toMutableList(), "folder2", "2", archived = false)

  private lateinit var mockNavigationActions: aliBouiri
  private lateinit var mockFileDownloader: FileDownloader

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

    mockNavigationActions = aliBouiri(navController = mock())
    mockFileDownloader = mock(FileDownloader::class.java)
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
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
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
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
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
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
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
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
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
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
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
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
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
    verify(folderRepository).updateFolder(any(), any(), any())

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

    composeTestRule.onNodeWithTag("VideoItem", useUnmergedTree = true).assertExists()

    composeTestRule
        .onNodeWithTag("MediaThumbnail", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

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
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
    }

    // Verifies that the GoBackButton is displayed
    composeTestRule.onNodeWithTag("goBackButton").assertExists().assertIsDisplayed()

    // Simulates a click on the GoBackButton
    composeTestRule.onNodeWithTag("goBackButton").performClick()

    // Verifies that the `goBack()` navigation action was called
    assert(mockNavigationActions.backCalled)
  }

  @Test
  fun testPostLocalPhotoNavigatesCorrectly() {
    // Arrange
    val localPhotoFile = File("path/to/local/photo.jpg")
    val photo =
        Photo(ownerId = "testOwner", photo = ByteArray(0), path = localPhotoFile.absolutePath)
    fakePhotoViewModel.setPhotos(listOf(photo))
    fakeVideoViewModel.setVideos(emptyList())

    // Configure mockFileDownloader to return the localPhotoFile when called with the local path
    runBlocking {
      `when`(mockFileDownloader.ensureLocalFile(any(), eq(localPhotoFile.absolutePath)))
          .thenReturn(localPhotoFile)
    }

    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
    }

    // Act
    // Simule le clic sur l'élément photo pour ouvrir le dialog
    composeTestRule.onNodeWithTag("PhotoItem_${photo.path}").performClick()
    // Vérifie que le dialog s'affiche
    composeTestRule.onNodeWithTag("MediaDetailDialog").assertExists()

    // Clique sur le bouton "Post"
    composeTestRule.onNodeWithTag("PostButton").performClick()

    // Attendre que l'UI soit dans un état stable
    composeTestRule.waitForIdle()

    // Assert
    val expectedRoute = "picTaken/${Uri.encode(localPhotoFile.absolutePath)}"
    assertEquals(expectedRoute, mockNavigationActions.navigatedRoute)
  }

  @Test
  fun testPostRemotePhotoNavigatesCorrectly() {
    // Arrange
    val remotePhotoUrl =
        "https://firebasestorage.googleapis.com/v0/b/testBucket/o/testPhoto.jpg?alt=media&token=abcd1234"
    val photo = Photo(ownerId = "testOwner", photo = ByteArray(0), path = remotePhotoUrl)
    fakePhotoViewModel.setPhotos(listOf(photo))
    fakeVideoViewModel.setVideos(emptyList())

    val downloadedFile = File("path/to/downloaded/photo.jpg")

    // Configure mockFileDownloader to return the downloadedFile when called with the remote URL
    runBlocking {
      `when`(mockFileDownloader.ensureLocalFile(any(), eq(remotePhotoUrl)))
          .thenReturn(downloadedFile)
    }

    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
    }

    // Act
    composeTestRule.onNodeWithTag("PhotoItem_${photo.path}").performClick()
    composeTestRule.onNodeWithTag("MediaDetailDialog").assertExists()

    // Clique sur le bouton "Post"
    composeTestRule.onNodeWithTag("PostButton").performClick()

    // Attendre que l'UI soit dans un état stable
    composeTestRule.waitForIdle()

    // Assert
    val expectedRoute = "picTaken/${Uri.encode(downloadedFile.absolutePath)}"
    assertEquals(expectedRoute, mockNavigationActions.navigatedRoute)
  }

  @Test
  fun testPostLocalVideoNavigatesCorrectly() {
    // Arrange
    val localVideoFile = File("path/to/local/video.mp4")
    val video =
        Video(ownerId = "testOwner", video = ByteArray(0), path = localVideoFile.absolutePath)
    fakeVideoViewModel.setVideos(listOf(video))
    fakePhotoViewModel.setPhotos(emptyList())

    // Configure mockFileDownloader to return the localVideoFile when called with the local path
    runBlocking {
      `when`(mockFileDownloader.ensureLocalFile(any(), eq(localVideoFile.absolutePath)))
          .thenReturn(localVideoFile)
    }

    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
    }

    // Act
    composeTestRule.onNodeWithTag("VideoItem_${video.path}").performClick()
    composeTestRule.onNodeWithTag("MediaDetailDialog").assertExists()

    // Clique sur le bouton "Post"
    composeTestRule.onNodeWithTag("PostButton").performClick()

    // Attendre que l'UI soit dans un état stable
    composeTestRule.waitForIdle()

    // Assert
    val expectedRoute = "picTaken/null?videoPath=${Uri.encode(localVideoFile.absolutePath)}"
    assertEquals(expectedRoute, mockNavigationActions.navigatedRoute)
  }

  @Test
  fun testPostRemoteVideoNavigatesCorrectly() {
    // Arrange
    val remoteVideoUrl =
        "https://firebasestorage.googleapis.com/v0/b/testBucket/o/testVideo.mp4?alt=media&token=efgh5678"
    val video = Video(ownerId = "testOwner", video = ByteArray(0), path = remoteVideoUrl)
    fakeVideoViewModel.setVideos(listOf(video))
    fakePhotoViewModel.setPhotos(emptyList())

    val downloadedFile = File("path/to/downloaded/video.mp4")

    // Configure mockFileDownloader to return the downloadedFile when called with the remote URL
    runBlocking {
      `when`(mockFileDownloader.ensureLocalFile(any(), eq(remoteVideoUrl)))
          .thenReturn(downloadedFile)
    }

    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwner",
          photoViewModel = fakePhotoViewModel,
          videoViewModel = fakeVideoViewModel,
          folderViewModel = fakeFolderViewModel,
          navigationActions = mockNavigationActions,
          fileDownloader = mockFileDownloader // Inject mockFileDownloader
          )
    }

    // Act
    composeTestRule.onNodeWithTag("VideoItem_${video.path}").performClick()
    composeTestRule.onNodeWithTag("MediaDetailDialog").assertExists()

    // Clique sur le bouton "Post"
    composeTestRule.onNodeWithTag("PostButton").performClick()

    // Attendre que l'UI soit dans un état stable
    composeTestRule.waitForIdle()

    // Assert
    val expectedRoute = "picTaken/null?videoPath=${Uri.encode(downloadedFile.absolutePath)}"
    assertEquals(expectedRoute, mockNavigationActions.navigatedRoute)
  }

  // Classe de MockNavigationActions
  class aliBouiri(navController: NavHostController) : NavigationActions(navController) {
    var navigatedRoute: String? = null
    var backCalled: Boolean = false

    override fun navigateTo(route: String) {
      navigatedRoute = route
    }

    override fun goBack() {
      backCalled = true
    }
  }
}

// FakePhotoViewModel and FakeVideoViewModel remain unchanged
class FakePhotoViewModel : PhotoViewModel(mockRepository(), mock(FileRepository::class.java)) {

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
