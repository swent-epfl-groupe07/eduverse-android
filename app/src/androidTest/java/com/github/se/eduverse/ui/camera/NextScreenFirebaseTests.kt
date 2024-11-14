package com.github.se.eduverse.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.*
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.mockito.kotlin.capture

class NextScreenFirebaseTests {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var navigationActions: NavigationActions
  private lateinit var photoViewModel: PhotoViewModel
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var videoViewModel: VideoViewModel
  private lateinit var testPhotoFile: File
  private lateinit var testVideoFile: File
  private lateinit var mockBitmap: Bitmap
  private lateinit var mockStorage: FirebaseStorage
  private lateinit var mockStorageRef: StorageReference
  private lateinit var mockUploadTask: UploadTask
  private lateinit var mockDownloadUrlTask: Task<Uri>
  private lateinit var mockUri: Uri

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    // Initialize test files
    mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    testPhotoFile =
        File.createTempFile("test_photo", ".jpg").apply {
          outputStream().use { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        }
    testVideoFile = File.createTempFile("test_video", ".mp4")

    // Mock Firebase Auth
    val mockAuth = mock(FirebaseAuth::class.java)
    val mockUser = mock(FirebaseUser::class.java)
    `when`(mockUser.uid).thenReturn("test-user-id")
    `when`(mockAuth.currentUser).thenReturn(mockUser)

    // Mock Firebase Storage components
    mockStorage = mock(FirebaseStorage::class.java)
    mockStorageRef = mock(StorageReference::class.java)
    mockUploadTask = mock(UploadTask::class.java)
    mockDownloadUrlTask = mock(Task::class.java) as Task<Uri>
    mockUri = mock(Uri::class.java)

    // Set up basic storage chain
    `when`(mockStorage.reference).thenReturn(mockStorageRef)
    `when`(mockStorageRef.child(anyString())).thenReturn(mockStorageRef)
    `when`(mockStorageRef.putBytes(any())).thenReturn(mockUploadTask)
    `when`(mockStorageRef.putFile(any())).thenReturn(mockUploadTask)
    `when`(mockStorageRef.downloadUrl).thenReturn(mockDownloadUrlTask)
    `when`(mockUri.toString()).thenReturn("https://test-url.com/test.jpg")

    // Mock success callbacks
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<UploadTask.TaskSnapshot>
          listener.onSuccess(mock(UploadTask.TaskSnapshot::class.java))
          mockUploadTask
        }
        .`when`(mockUploadTask)
        .addOnSuccessListener(any<OnSuccessListener<UploadTask.TaskSnapshot>>())

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<Uri>
          listener.onSuccess(mockUri)
          mockDownloadUrlTask
        }
        .`when`(mockDownloadUrlTask)
        .addOnSuccessListener(any<OnSuccessListener<Uri>>())

    // Mock Firestore
    val mockFirestore = mock(FirebaseFirestore::class.java)
    val mockCollectionRef = mock(CollectionReference::class.java)
    val mockDocumentRef = mock(DocumentReference::class.java)
    val mockFirestoreTask = mock(Task::class.java) as Task<DocumentReference>

    `when`(mockFirestore.collection(anyString())).thenReturn(mockCollectionRef)
    `when`(mockCollectionRef.add(any())).thenReturn(mockFirestoreTask)

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<DocumentReference>
          listener.onSuccess(mockDocumentRef)
          mockFirestoreTask
        }
        .`when`(mockFirestoreTask)
        .addOnSuccessListener(any<OnSuccessListener<DocumentReference>>())

    // Mock static methods
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth

    mockkStatic(FirebaseStorage::class)
    every { FirebaseStorage.getInstance() } returns mockStorage

    mockkStatic(FirebaseFirestore::class)
    every { FirebaseFirestore.getInstance() } returns mockFirestore

    // Initialize view models and navigation
    navigationActions = mockk(relaxed = true)
    photoViewModel = mockk(relaxed = true)
    folderViewModel = mockk(relaxed = true)
    videoViewModel = mockk(relaxed = true)
  }

  @Test
  fun testPhotoPostButtonClick() {
    composeTestRule.setContent {
      NextScreen(
          photoFile = testPhotoFile,
          videoFile = null,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          videoViewModel = videoViewModel)
    }

    composeTestRule.onNodeWithTag("postButton").performClick()
  }

  @Test
  fun testVideoPostButtonClick() {
    composeTestRule.setContent {
      NextScreen(
          photoFile = null,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          videoViewModel = videoViewModel)
    }

    composeTestRule.onNodeWithTag("postButton").performClick()
  }

  @Test
  fun testUploadFailure() {
    // Create new mocks for failure scenario
    val mockFailureUploadTask = mock(UploadTask::class.java)
    `when`(mockStorageRef.putBytes(any())).thenReturn(mockFailureUploadTask)

    // Setup failure callback
    doAnswer { invocation -> mockFailureUploadTask }
        .`when`(mockFailureUploadTask)
        .addOnSuccessListener(any<OnSuccessListener<UploadTask.TaskSnapshot>>())

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnFailureListener
          listener.onFailure(Exception("Upload failed"))
          mockFailureUploadTask
        }
        .`when`(mockFailureUploadTask)
        .addOnFailureListener(any<OnFailureListener>())

    composeTestRule.setContent {
      NextScreen(
          photoFile = testPhotoFile,
          videoFile = null,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          videoViewModel = videoViewModel)
    }

    composeTestRule.onNodeWithTag("postButton").performClick()
  }

  @Test
  fun testVideoPostButtonClickWithThumbnail() {
    // Mock thumbnail generation first
    val testBytes = ByteArray(100) { 1 }
    mockkStatic(::generateVideoThumbnail)
    every { generateVideoThumbnail(any(), any()) } returns testBytes

    // Reset storage mocks
    reset(mockStorage, mockStorageRef)

    // Mock additional components for video upload
    val mockThumbnailRef = mock(StorageReference::class.java)
    val mockVideoRef = mock(StorageReference::class.java)
    val mockThumbnailUploadTask = mock(UploadTask::class.java)
    val mockVideoUploadTask = mock(UploadTask::class.java)
    val mockThumbnailDownloadUrlTask = mock(Task::class.java) as Task<Uri>
    val mockVideoDownloadUrlTask = mock(Task::class.java) as Task<Uri>
    val mockFirestoreTask = mock(Task::class.java) as Task<DocumentReference>
    val mockDocRef = mock(DocumentReference::class.java)

    // Capture the publication for verification
    val publicationCaptor = ArgumentCaptor.forClass(Publication::class.java)

    // Mock storage chain
    `when`(mockStorage.reference).thenReturn(mockStorageRef)
    `when`(mockStorageRef.child(matches(".*/thumbnails/.*"))).thenReturn(mockThumbnailRef)
    `when`(mockStorageRef.child(matches(".*/media/.*"))).thenReturn(mockVideoRef)

    // Mock thumbnail operations
    `when`(mockThumbnailRef.putBytes(any())).thenReturn(mockThumbnailUploadTask)
    `when`(mockThumbnailRef.downloadUrl).thenReturn(mockThumbnailDownloadUrlTask)

    // Mock video operations
    `when`(mockVideoRef.putFile(any(Uri::class.java))).thenReturn(mockVideoUploadTask)
    `when`(mockVideoRef.downloadUrl).thenReturn(mockVideoDownloadUrlTask)

    // Mock URIs
    val mockThumbnailUri = mock(Uri::class.java)
    val mockVideoUri = mock(Uri::class.java)
    `when`(mockThumbnailUri.toString()).thenReturn("https://test-url.com/thumbnail.jpg")
    `when`(mockVideoUri.toString()).thenReturn("https://test-url.com/video.mp4")

    // Set up success callbacks
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<UploadTask.TaskSnapshot>
          listener.onSuccess(mock(UploadTask.TaskSnapshot::class.java))
          mockThumbnailUploadTask
        }
        .`when`(mockThumbnailUploadTask)
        .addOnSuccessListener(any<OnSuccessListener<UploadTask.TaskSnapshot>>())

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<Uri>
          listener.onSuccess(mockThumbnailUri)
          mockThumbnailDownloadUrlTask
        }
        .`when`(mockThumbnailDownloadUrlTask)
        .addOnSuccessListener(any<OnSuccessListener<Uri>>())

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<UploadTask.TaskSnapshot>
          listener.onSuccess(mock(UploadTask.TaskSnapshot::class.java))
          mockVideoUploadTask
        }
        .`when`(mockVideoUploadTask)
        .addOnSuccessListener(any<OnSuccessListener<UploadTask.TaskSnapshot>>())

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<Uri>
          listener.onSuccess(mockVideoUri)
          mockVideoDownloadUrlTask
        }
        .`when`(mockVideoDownloadUrlTask)
        .addOnSuccessListener(any<OnSuccessListener<Uri>>())

    // Mock Firestore
    val mockFirestore = mock(FirebaseFirestore::class.java)
    val mockCollectionRef = mock(CollectionReference::class.java)
    every { FirebaseFirestore.getInstance() } returns mockFirestore
    `when`(mockFirestore.collection(anyString())).thenReturn(mockCollectionRef)
    `when`(mockCollectionRef.add(any(Publication::class.java))).thenReturn(mockFirestoreTask)
    `when`(mockDocRef.id).thenReturn("test-doc-id")

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<DocumentReference>
          listener.onSuccess(mockDocRef)
          mockFirestoreTask
        }
        .`when`(mockFirestoreTask)
        .addOnSuccessListener(any<OnSuccessListener<DocumentReference>>())

    // Mock static method again just before test
    every { FirebaseStorage.getInstance() } returns mockStorage

    composeTestRule.setContent {
      NextScreen(
          photoFile = null,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          videoViewModel = videoViewModel)
    }

    composeTestRule.onNodeWithTag("postButton").performClick()

    // Verify the upload chain
    verify(mockThumbnailRef).putBytes(any())
    verify(mockThumbnailRef).downloadUrl
    verify(mockVideoRef).putFile(any(Uri::class.java))
    verify(mockVideoRef).downloadUrl

    // Verify the publication creation
    verify(mockCollectionRef).add(capture(publicationCaptor))
    val capturedPublication = publicationCaptor.value

    assertEquals("test-user-id", capturedPublication.userId)
    assertEquals("My Publication", capturedPublication.title)
    assertEquals("https://test-url.com/thumbnail.jpg", capturedPublication.thumbnailUrl)
    assertEquals("https://test-url.com/video.mp4", capturedPublication.mediaUrl)
    assertEquals(MediaType.VIDEO, capturedPublication.mediaType)
    assertTrue(capturedPublication.timestamp > 0)

    // Verify navigation
    verify(exactly = 3) { navigationActions.goBack() }
  }

  @Test
  fun testVideoUploadThumbnailFailure() {
    // Mock storage references
    val mockThumbnailRef = mock(StorageReference::class.java)
    val mockThumbnailUploadTask = mock(UploadTask::class.java)

    // Reset and set up storage mocks
    reset(mockStorage, mockStorageRef)
    `when`(mockStorage.reference).thenReturn(mockStorageRef)
    `when`(mockStorageRef.child(anyString())).thenReturn(mockThumbnailRef)
    `when`(mockThumbnailRef.putBytes(any())).thenReturn(mockThumbnailUploadTask)

    // Mock the static Firebase instance
    every { FirebaseStorage.getInstance() } returns mockStorage

    // Set up failure callback for thumbnail upload
    doAnswer { invocation -> mockThumbnailUploadTask }
        .`when`(mockThumbnailUploadTask)
        .addOnSuccessListener(any<OnSuccessListener<UploadTask.TaskSnapshot>>())

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnFailureListener
          listener.onFailure(Exception("Thumbnail upload failed"))
          mockThumbnailUploadTask
        }
        .`when`(mockThumbnailUploadTask)
        .addOnFailureListener(any<OnFailureListener>())

    // Mock generateVideoThumbnail to return some test bytes
    val testBytes = ByteArray(100) { 1 }
    mockkStatic(::generateVideoThumbnail)
    every { generateVideoThumbnail(any(), any()) } returns testBytes

    composeTestRule.setContent {
      NextScreen(
          photoFile = null,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          videoViewModel = videoViewModel)
    }

    composeTestRule.onNodeWithTag("postButton").performClick()

    // Verify that the thumbnail upload was attempted
    verify(mockThumbnailRef).putBytes(any())

    // Verify that no navigation occurred due to failure
    verify(exactly = 0) { navigationActions.goBack() }
  }

  @Test
  fun testVideoUploadFailure() {
    // Mock thumbnail generation
    val testBytes = ByteArray(100) { 1 }
    mockkStatic(::generateVideoThumbnail)
    every { generateVideoThumbnail(any(), any()) } returns testBytes

    // Reset storage mocks
    reset(mockStorage, mockStorageRef)

    val mockThumbnailRef = mock(StorageReference::class.java)
    val mockVideoRef = mock(StorageReference::class.java)
    val mockThumbnailUploadTask = mock(UploadTask::class.java)
    val mockVideoUploadTask = mock(UploadTask::class.java)
    val mockThumbnailDownloadUrlTask = mock(Task::class.java) as Task<Uri>

    // Mock storage chain
    `when`(mockStorage.reference).thenReturn(mockStorageRef)
    `when`(mockStorageRef.child(matches(".*/thumbnails/.*"))).thenReturn(mockThumbnailRef)
    `when`(mockStorageRef.child(matches(".*/media/.*"))).thenReturn(mockVideoRef)

    // Mock thumbnail success
    `when`(mockThumbnailRef.putBytes(any())).thenReturn(mockThumbnailUploadTask)
    `when`(mockThumbnailRef.downloadUrl).thenReturn(mockThumbnailDownloadUrlTask)

    // Mock video failure
    `when`(mockVideoRef.putFile(any(Uri::class.java))).thenReturn(mockVideoUploadTask)

    // Set up thumbnail success callbacks
    val mockThumbnailUri = mock(Uri::class.java)
    `when`(mockThumbnailUri.toString()).thenReturn("https://test-url.com/thumbnail.jpg")

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<UploadTask.TaskSnapshot>
          listener.onSuccess(mock(UploadTask.TaskSnapshot::class.java))
          mockThumbnailUploadTask
        }
        .`when`(mockThumbnailUploadTask)
        .addOnSuccessListener(any<OnSuccessListener<UploadTask.TaskSnapshot>>())

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnSuccessListener<Uri>
          listener.onSuccess(mockThumbnailUri)
          mockThumbnailDownloadUrlTask
        }
        .`when`(mockThumbnailDownloadUrlTask)
        .addOnSuccessListener(any<OnSuccessListener<Uri>>())

    // Set up video failure callback
    doAnswer { invocation -> mockVideoUploadTask }
        .`when`(mockVideoUploadTask)
        .addOnSuccessListener(any<OnSuccessListener<UploadTask.TaskSnapshot>>())

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnFailureListener
          listener.onFailure(Exception("Video upload failed"))
          mockVideoUploadTask
        }
        .`when`(mockVideoUploadTask)
        .addOnFailureListener(any<OnFailureListener>())

    // Mock static method again just before test
    every { FirebaseStorage.getInstance() } returns mockStorage

    composeTestRule.setContent {
      NextScreen(
          photoFile = null,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel,
          videoViewModel = videoViewModel)
    }

    composeTestRule.onNodeWithTag("postButton").performClick()

    // Verify that thumbnail succeeded but video failed
    verify(mockThumbnailRef).putBytes(any())
    verify(mockThumbnailRef).downloadUrl
    verify(mockVideoRef).putFile(any(Uri::class.java))
    verify(exactly = 0) { navigationActions.goBack() }
  }

  @After
  fun tearDown() {
    // Clear static mocks to avoid interference with other tests
    unmockkAll()
  }
}
