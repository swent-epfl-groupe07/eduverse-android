package com.github.se.eduverse.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*

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

  @After
  fun tearDown() {
    // Clear static mocks to avoid interference with other tests
    unmockkAll()
  }
}
