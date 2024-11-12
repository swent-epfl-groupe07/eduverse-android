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
import junit.framework.TestCase.assertFalse
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

          override suspend fun removeFromFavorites(userId: String, publicationId: String) {}

          override suspend fun followUser(followerId: String, followedId: String) {}

          override suspend fun unfollowUser(followerId: String, followedId: String) {}

          override suspend fun uploadProfileImage(userId: String, imageUri: Uri): String = ""

          override suspend fun updateProfileImage(userId: String, imageUrl: String) {}

          override suspend fun searchProfiles(query: String, limit: Int): List<Profile> {
            return emptyList()
          }

          override suspend fun createProfile(
              userId: String,
              defaultUsername: String,
              photoUrl: String
          ): Profile {
            return Profile(id = userId, username = defaultUsername, profileImageUrl = photoUrl)
          }

          override suspend fun updateUsername(userId: String, newUsername: String) {
            TODO()
          }

          override suspend fun doesUsernameExist(username: String): Boolean {
            return false
          }

          override suspend fun addToUserCollection(
              userId: String,
              collectionName: String,
              publicationId: String
          ) {
            TODO("Not yet implemented")
          }

          override suspend fun incrementLikes(publicationId: String, userId: String) {
            TODO("Not yet implemented")
          }

          override suspend fun removeFromLikedPublications(userId: String, publicationId: String) {
            TODO("Not yet implemented")
          }

          override suspend fun decrementLikesAndRemoveUser(publicationId: String, userId: String) {
            TODO("Not yet implemented")
          }

          override suspend fun getAllPublications(): List<Publication> {
            TODO("Not yet implemented")
          }

          override suspend fun getUserLikedPublicationsIds(userId: String): List<String> {
            TODO("Not yet implemented")
          }
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

  @Test
  fun `createProfile creates new profile with correct data`() = runTest {
    val userId = "testUser"
    val username = "testUsername"
    val photoUrl = "http://example.com/photo.jpg"

    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.set(any())).thenReturn(Tasks.forResult(null))

    val result = repository.createProfile(userId, username, photoUrl)

    verify(mockDocumentRef)
        .set(
            argThat { profile: Profile ->
              profile.id == userId &&
                  profile.username == username &&
                  profile.profileImageUrl == photoUrl &&
                  profile.followers == 0 &&
                  profile.following == 0 &&
                  profile.publications.isEmpty() &&
                  profile.favoritePublications.isEmpty()
            })

    assertEquals(userId, result.id)
    assertEquals(username, result.username)
    assertEquals(photoUrl, result.profileImageUrl)
  }

  @Test
  fun `updateUsername updates only username field`() = runTest {
    val userId = "testUser"
    val newUsername = "newUsername"

    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.update("username", newUsername)).thenReturn(Tasks.forResult(null))

    repository.updateUsername(userId, newUsername)

    verify(mockDocumentRef).update("username", newUsername)
  }

  @Test
  fun `doesUsernameExist returns true for existing username`() = runTest {
    val username = "existingUsername"
    val mockQuery = mock(Query::class.java)

    whenever(mockCollectionRef.whereEqualTo("username", username)).thenReturn(mockQuery)
    whenever(mockQuery.limit(1)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.isEmpty).thenReturn(false)

    val result = repository.doesUsernameExist(username)

    assertTrue(result)
  }

  @Test
  fun `doesUsernameExist returns false for non-existing username`() = runTest {
    val username = "newUsername"
    val mockQuery = mock(Query::class.java)

    whenever(mockCollectionRef.whereEqualTo("username", username)).thenReturn(mockQuery)
    whenever(mockQuery.limit(1)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.isEmpty).thenReturn(true)

    val result = repository.doesUsernameExist(username)

    assertFalse(result)
  }

  @Test
  fun `addToUserCollection successfully adds publication to user collection`() = runTest {
    val userId = "testUser"
    val collectionName = "testCollection"
    val publicationId = "pub123"
    val mockUsersCollectionRef = mock(CollectionReference::class.java)
    val mockUserDocumentRef = mock(DocumentReference::class.java)
    val mockCollectionRef = mock(CollectionReference::class.java)
    val mockPublicationDocumentRef = mock(DocumentReference::class.java)

    // Mocking the Firestore calls
    whenever(mockFirestore.collection("users")).thenReturn(mockUsersCollectionRef)
    whenever(mockUsersCollectionRef.document(userId)).thenReturn(mockUserDocumentRef)
    whenever(mockUserDocumentRef.collection(collectionName)).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(publicationId)).thenReturn(mockPublicationDocumentRef)
    whenever(mockPublicationDocumentRef.set(any())).thenReturn(Tasks.forResult(null))

    // Call the method
    repository.addToUserCollection(userId, collectionName, publicationId)

    // Verify that the correct methods were called with the correct arguments
    verify(mockFirestore, atLeastOnce()).collection("users")
    verify(mockUsersCollectionRef).document(userId)
    verify(mockUserDocumentRef).collection(collectionName)
    verify(mockCollectionRef).document(publicationId)
    verify(mockPublicationDocumentRef)
        .set(
            argThat { map: Map<String, Any> ->
              map["publicationId"] == publicationId && map.containsKey("timestamp")
            })
  }

  @Test
  fun `incrementLikes successfully increments likes and adds user to likedBy`() = runTest {
    val publicationId = "pub123"
    val userId = "user456"

    // Mock Firestore references
    val mockPublicationsCollection = mock(CollectionReference::class.java)
    val mockQuery = mock(Query::class.java)
    val mockQuerySnapshot = mock(QuerySnapshot::class.java)
    val mockDocumentSnapshot = mock(DocumentSnapshot::class.java)
    val mockDocumentReference = mock(DocumentReference::class.java)

    // Mock Transaction
    val mockTransaction = mock(Transaction::class.java)

    // Mock Firestore collection and query behavior
    whenever(mockFirestore.collection("publications")).thenReturn(mockPublicationsCollection)
    whenever(mockPublicationsCollection.whereEqualTo("id", publicationId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.isEmpty).thenReturn(false)
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockDocumentSnapshot))
    whenever(mockDocumentSnapshot.reference).thenReturn(mockDocumentReference)

    // Mock transaction behavior
    // When runTransaction is called, execute the transaction function with our mockTransaction
    whenever(mockFirestore.runTransaction<Transaction>(any())).thenAnswer { invocation ->
      val transactionFunction = invocation.arguments[0] as Transaction.Function<Transaction>
      transactionFunction.apply(mockTransaction)
      Tasks.forResult(null)
    }

    // Mock transaction.get() to return our mockDocumentSnapshot
    whenever(mockTransaction.get(mockDocumentReference)).thenReturn(mockDocumentSnapshot)

    // Simulate that the user hasn't liked the publication yet
    whenever(mockDocumentSnapshot.get("likedBy")).thenReturn(emptyList<String>())
    whenever(mockDocumentSnapshot.getLong("likes")).thenReturn(0L)

    // Call the method under test
    repository.incrementLikes(publicationId, userId)

    // Verify that transaction.update was called with the correct arguments
    verify(mockTransaction).update(mockDocumentReference, "likes", 1L)
    verify(mockTransaction).update(mockDocumentReference, "likedBy", listOf(userId))
  }

  @Test
  fun `removeFromLikedPublications successfully removes publication from likedPublications`() =
      runTest {
        val userId = "testUser"
        val publicationId = "pub123"

        val mockUsersCollectionRef = mock(CollectionReference::class.java)
        val mockUserDocumentRef = mock(DocumentReference::class.java)
        val mockLikedPublicationsCollectionRef = mock(CollectionReference::class.java)
        val mockPublicationDocumentRef = mock(DocumentReference::class.java)

        // Mocking the Firestore calls
        whenever(mockFirestore.collection("users")).thenReturn(mockUsersCollectionRef)
        whenever(mockUsersCollectionRef.document(userId)).thenReturn(mockUserDocumentRef)
        whenever(mockUserDocumentRef.collection("likedPublications"))
            .thenReturn(mockLikedPublicationsCollectionRef)
        whenever(mockLikedPublicationsCollectionRef.document(publicationId))
            .thenReturn(mockPublicationDocumentRef)
        whenever(mockPublicationDocumentRef.delete()).thenReturn(Tasks.forResult(null))

        // Call the method
        repository.removeFromLikedPublications(userId, publicationId)

        // Verify that delete was called
        verify(mockPublicationDocumentRef).delete()
      }

  @Test
  fun `decrementLikesAndRemoveUser successfully decrements likes and removes user from likedBy`() =
      runTest {
        val publicationId = "pub123"
        val userId = "user456"

        // Mock Firestore references
        val mockPublicationsCollection = mock(CollectionReference::class.java)
        val mockQuery = mock(Query::class.java)
        val mockQuerySnapshot = mock(QuerySnapshot::class.java)
        val mockDocumentSnapshot = mock(DocumentSnapshot::class.java)
        val mockDocumentReference = mock(DocumentReference::class.java)

        // Mock Transaction
        val mockTransaction = mock(Transaction::class.java)

        // Mock Firestore collection and query behavior
        whenever(mockFirestore.collection("publications")).thenReturn(mockPublicationsCollection)
        whenever(mockPublicationsCollection.whereEqualTo("id", publicationId)).thenReturn(mockQuery)
        whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
        whenever(mockQuerySnapshot.isEmpty).thenReturn(false)
        whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockDocumentSnapshot))
        whenever(mockDocumentSnapshot.reference).thenReturn(mockDocumentReference)

        // Mock transaction behavior
        whenever(mockFirestore.runTransaction<Void>(any())).thenAnswer { invocation ->
          val transactionFunction = invocation.arguments[0] as Transaction.Function<Void>
          transactionFunction.apply(mockTransaction)
          Tasks.forResult(null)
        }

        // Mock transaction.get() to return our mockDocumentSnapshot
        whenever(mockTransaction.get(mockDocumentReference)).thenReturn(mockDocumentSnapshot)

        // Simulate that the user has liked the publication
        val likedBy = mutableListOf(userId)
        whenever(mockDocumentSnapshot.get("likedBy")).thenReturn(likedBy)
        whenever(mockDocumentSnapshot.getLong("likes")).thenReturn(1L)

        // Call the method under test
        repository.decrementLikesAndRemoveUser(publicationId, userId)

        // Verify that likedBy list no longer contains the user
        verify(mockTransaction)
            .update(
                eq(mockDocumentReference),
                argThat { map ->
                  val updatedLikedBy = map["likedBy"] as? List<*>
                  val updatedLikes = map["likes"] as? Long
                  updatedLikedBy != null && !updatedLikedBy.contains(userId) && updatedLikes == 0L
                })
      }

  @Test
  fun `getAllPublications returns list of publications`() = runTest {
    // Arrange
    val mockQuerySnapshot = mock(QuerySnapshot::class.java)
    val mockDocumentSnapshot1 = mock(DocumentSnapshot::class.java)
    val mockDocumentSnapshot2 = mock(DocumentSnapshot::class.java)

    val publication1 = Publication(id = "pub1", userId = "user1", title = "Test Pub 1")
    val publication2 = Publication(id = "pub2", userId = "user2", title = "Test Pub 2")

    whenever(mockFirestore.collection("publications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents)
        .thenReturn(listOf(mockDocumentSnapshot1, mockDocumentSnapshot2))
    whenever(mockDocumentSnapshot1.toObject(Publication::class.java)).thenReturn(publication1)
    whenever(mockDocumentSnapshot2.toObject(Publication::class.java)).thenReturn(publication2)

    // Act
    val result = repository.getAllPublications()

    // Assert
    assertEquals(2, result.size)
    assertTrue(result.containsAll(listOf(publication1, publication2)))
    verify(mockCollectionRef).get()
  }

  @Test
  fun `getAllPublications returns empty list on exception`() = runTest {
    // Arrange
    whenever(mockFirestore.collection("publications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.get()).thenThrow(RuntimeException("Firestore error"))

    // Act
    val result = repository.getAllPublications()

    // Assert
    assertTrue(result.isEmpty())
    verify(mockCollectionRef).get()
  }

  @Test
  fun `getUserLikedPublicationsIds returns list of liked publication IDs`() = runTest {
    // Arrange
    val userId = "testUser"
    val mockQuerySnapshot = mock(QuerySnapshot::class.java)
    val mockDocumentSnapshot1 = mock(DocumentSnapshot::class.java)
    val mockDocumentSnapshot2 = mock(DocumentSnapshot::class.java)

    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("likedPublications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents)
        .thenReturn(listOf(mockDocumentSnapshot1, mockDocumentSnapshot2))
    whenever(mockDocumentSnapshot1.getString("publicationId")).thenReturn("pub1")
    whenever(mockDocumentSnapshot2.getString("publicationId")).thenReturn("pub2")

    // Act
    val result = repository.getUserLikedPublicationsIds(userId)

    // Assert
    assertEquals(2, result.size)
    assertTrue(result.containsAll(listOf("pub1", "pub2")))
    verify(mockCollectionRef).get()
  }

  @Test
  fun `getUserLikedPublicationsIds returns empty list on exception`() = runTest {
    // Arrange
    val userId = "testUser"
    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("likedPublications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.get()).thenThrow(RuntimeException("Firestore error"))

    // Act
    val result = repository.getUserLikedPublicationsIds(userId)

    // Assert
    assertTrue(result.isEmpty())
    verify(mockCollectionRef).get()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain() // Reset main dispatcher after the test
  }
}
