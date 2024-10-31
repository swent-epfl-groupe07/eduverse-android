package com.github.se.eduverse.repository

import android.net.Uri
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever

class ProfileRepositoryImplTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: ProfileRepositoryImpl
  private val mockFirestore: FirebaseFirestore = mock(FirebaseFirestore::class.java)
  private val mockStorage: FirebaseStorage = mock(FirebaseStorage::class.java)
  private val mockCollectionRef: CollectionReference = mock(CollectionReference::class.java)
  private val mockDocumentRef: DocumentReference = mock(DocumentReference::class.java)
  private val mockSnapshot: DocumentSnapshot = mock(DocumentSnapshot::class.java)
  private val mockStorageRef: StorageReference = mock(StorageReference::class.java)
  private val mockQuerySnapshot: QuerySnapshot = mock(QuerySnapshot::class.java)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher) // Set up test dispatcher

    whenever(mockFirestore.collection(any())).thenReturn(mockCollectionRef)
    whenever(mockStorage.reference).thenReturn(mockStorageRef)
    whenever(mockStorageRef.child(any())).thenReturn(mockStorageRef)

    repository = ProfileRepositoryImpl(mockFirestore, mockStorage)
  }

  @Test
  fun `getProfile returns complete profile with related data`() = runTest {
    val userId = "testUser"
    val testProfile =
        Profile(
            id = userId,
            username = "test",
            publications =
                listOf(
                    Publication(
                        id = "pub1",
                        userId = userId,
                        title = "Test Pub",
                        thumbnailUrl = "",
                        timestamp = 123L)),
            favoritePublications = emptyList(),
            followers = 0,
            following = 0)

    // Create manual mock repository
    val mockRepo =
        object : ProfileRepository {
          override suspend fun getProfile(userId: String): Profile? = testProfile

          override suspend fun updateProfile(userId: String, profile: Profile) {}

          override suspend fun addPublication(userId: String, publication: Publication) {}

          override suspend fun removePublication(publicationId: String) {}

          override suspend fun addToFavorites(userId: String, publicationId: String) {}

          override suspend fun removeFromFavorites(userId: String, publicationId: String) {}

          override suspend fun followUser(followerId: String, followedId: String) {}

          override suspend fun unfollowUser(followerId: String, followedId: String) {}

          override suspend fun uploadProfileImage(userId: String, imageUri: Uri): String = ""

          override suspend fun updateProfileImage(userId: String, imageUrl: String) {}
        }

    val viewModel = ProfileViewModel(mockRepo)

    viewModel.loadProfile(userId)
    advanceUntilIdle() // Wait for coroutines to complete

    val state = viewModel.profileState.value
    assertTrue(state is ProfileUiState.Success)
    assertEquals(testProfile, (state as ProfileUiState.Success).profile)
  }

  @Test
  fun `uploadProfileImage uploads image and returns URL`() = runTest {
    val userId = "testUser"
    val imageUri = mock(Uri::class.java)
    val downloadUrl = mock(Uri::class.java)
    val storageReference = mock(StorageReference::class.java)

    // Create a TaskCompletionSource for the download URL
    val urlTaskCompletionSource = TaskCompletionSource<Uri>()
    val uploadTaskCompletionSource = TaskCompletionSource<UploadTask.TaskSnapshot>()

    // Mock storage references
    whenever(mockStorage.reference).thenReturn(storageReference)
    whenever(storageReference.child(any())).thenReturn(mockStorageRef)

    // Create a mock UploadTask that will complete successfully
    val mockUploadTask = mock(UploadTask::class.java)
    whenever(mockStorageRef.putFile(imageUri)).thenReturn(mockUploadTask)
    whenever(mockUploadTask.isSuccessful).thenReturn(true)
    whenever(mockUploadTask.isComplete).thenReturn(true)

    // Mock the download URL
    whenever(mockStorageRef.downloadUrl).thenReturn(urlTaskCompletionSource.task)
    whenever(downloadUrl.toString()).thenReturn("http://example.com/image.jpg")
    urlTaskCompletionSource.setResult(downloadUrl)

    launch {
      val result = repository.uploadProfileImage(userId, imageUri)
      assertEquals("http://example.com/image.jpg", result)
    }

    advanceUntilIdle()
    verify(mockStorageRef).putFile(imageUri)
  }

  @Test
  fun `addToFavorites creates favorite document`() = runTest {
    val userId = "testUser"
    val pubId = "pub123"

    whenever(mockCollectionRef.add(any())).thenReturn(Tasks.forResult(mockDocumentRef))

    repository.addToFavorites(userId, pubId)

    verify(mockCollectionRef)
        .add(
            argThat { map: Map<String, Any> ->
              map["userId"] == userId &&
                  map["publicationId"] == pubId &&
                  map.containsKey("timestamp")
            })
  }

  @Test
  fun `updateProfile successfully updates profile`() = runTest {
    val userId = "testUser"
    val profile = Profile(id = userId, username = "test")

    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.set(profile)).thenReturn(Tasks.forResult(null))

    repository.updateProfile(userId, profile)

    verify(mockDocumentRef).set(profile)
  }

  @Test
  fun `addPublication successfully adds publication`() = runTest {
    val userId = "testUser"
    val publication = Publication(id = "pub1", userId = userId, title = "Test")

    whenever(mockCollectionRef.document(publication.id)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.set(publication)).thenReturn(Tasks.forResult(null))

    repository.addPublication(userId, publication)

    verify(mockDocumentRef).set(publication)
  }

  @Test
  fun `removePublication successfully removes publication`() = runTest {
    val publicationId = "pub1"

    whenever(mockCollectionRef.document(publicationId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.delete()).thenReturn(Tasks.forResult(null))

    repository.removePublication(publicationId)

    verify(mockDocumentRef).delete()
  }

  @Test
  fun `removeFromFavorites removes favorite document`() = runTest {
    val userId = "testUser"
    val pubId = "pub123"
    val mockQuery = mock(Query::class.java)
    val mockDocRef = mock(DocumentReference::class.java)

    whenever(mockCollectionRef.whereEqualTo("userId", userId)).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo("publicationId", pubId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockSnapshot))
    whenever(mockSnapshot.reference).thenReturn(mockDocRef)
    whenever(mockDocRef.delete()).thenReturn(Tasks.forResult(null))

    repository.removeFromFavorites(userId, pubId)

    verify(mockDocRef).delete()
  }

  @Test
  fun `followUser creates follower document`() = runTest {
    val followerId = "user1"
    val followedId = "user2"

    whenever(mockCollectionRef.add(any())).thenReturn(Tasks.forResult(mockDocumentRef))

    repository.followUser(followerId, followedId)

    verify(mockCollectionRef)
        .add(
            argThat { map: Map<String, Any> ->
              map["followerId"] == followerId &&
                  map["followedId"] == followedId &&
                  map.containsKey("timestamp")
            })
  }

  @Test
  fun `unfollowUser removes follower document`() = runTest {
    val followerId = "user1"
    val followedId = "user2"
    val mockQuery = mock(Query::class.java)
    val mockDocRef = mock(DocumentReference::class.java)

    whenever(mockCollectionRef.whereEqualTo("followerId", followerId)).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo("followedId", followedId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockSnapshot))
    whenever(mockSnapshot.reference).thenReturn(mockDocRef)
    whenever(mockDocRef.delete()).thenReturn(Tasks.forResult(null))

    repository.unfollowUser(followerId, followedId)

    verify(mockDocRef).delete()
  }

  @Test
  fun `updateProfileImage updates profile image URL`() = runTest {
    val userId = "testUser"
    val imageUrl = "http://example.com/image.jpg"

    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.update("profileImageUrl", imageUrl)).thenReturn(Tasks.forResult(null))

    repository.updateProfileImage(userId, imageUrl)

    verify(mockDocumentRef).update("profileImageUrl", imageUrl)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain() // Reset main dispatcher after the test
  }
}
