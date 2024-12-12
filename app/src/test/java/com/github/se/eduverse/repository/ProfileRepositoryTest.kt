package com.github.se.eduverse.repository

import android.net.Uri
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.fail
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
import org.mockito.MockedStatic
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
  private val mockTransaction: Transaction = mock(Transaction::class.java)
  private val mockQuery = mock(Query::class.java)
  private lateinit var firebaseAuthMock: MockedStatic<FirebaseAuth>
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher) // Set up test dispatcher

    // Setup Firebase Auth mock
    mockAuth = mock(FirebaseAuth::class.java)
    mockUser = mock(FirebaseUser::class.java)
    whenever(mockUser.uid).thenReturn("currentUser")
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    firebaseAuthMock = mockStatic(FirebaseAuth::class.java)
    firebaseAuthMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)

    whenever(mockFirestore.collection(any())).thenReturn(mockCollectionRef)
    whenever(mockStorage.reference).thenReturn(mockStorageRef)
    whenever(mockStorageRef.child(any())).thenReturn(mockStorageRef)

    repository = spy(TestableProfileRepositoryImpl(mockFirestore, mockStorage))
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

          override suspend fun getFavoritePublicationsIds(userId: String): List<String> {
            TODO("Not yet implemented")
          }

          override suspend fun isPublicationFavorited(
              userId: String,
              publicationId: String
          ): Boolean {
            TODO("Not yet implemented")
          }

          override suspend fun getFavoritePublications(
              favoriteIds: List<String>
          ): List<Publication> {
            TODO("Not yet implemented")
          }

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

          override suspend fun isFollowing(followerId: String, targetUserId: String): Boolean {
            TODO("Not yet implemented")
          }

          override suspend fun toggleFollow(followerId: String, targetUserId: String): Boolean {
            TODO("Not yet implemented")
          }

          override suspend fun updateFollowCounts(
              followerId: String,
              targetUserId: String,
              isFollowing: Boolean
          ) {
            TODO("Not yet implemented")
          }

          override suspend fun getFollowers(userId: String): List<Profile> {
            TODO("Not yet implemented")
          }

          override suspend fun getFollowing(userId: String): List<Profile> {
            TODO("Not yet implemented")
          }

          override suspend fun deletePublication(publicationId: String, userId: String): Boolean {
            TODO("Not yet implemented")
          }

          override suspend fun addToFavorites(userId: String, publicationId: String) {
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

  @Test
  fun `isFollowing returns true when user is following target`() = runTest {
    val followerId = "user1"
    val targetId = "user2"
    val mockQuery = mock(Query::class.java)

    // Mock the followers collection
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("followerId", followerId)).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo("followedId", targetId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockSnapshot))

    val result = repository.isFollowing(followerId, targetId)
    assertTrue(result)
  }

  @Test
  fun `isFollowing returns false when user is not following target`() = runTest {
    val followerId = "user1"
    val targetId = "user2"
    val mockQuery = mock(Query::class.java)

    // Mock the followers collection
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("followerId", followerId)).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo("followedId", targetId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(emptyList())

    val result = repository.isFollowing(followerId, targetId)
    assertFalse(result)
  }

  @Test
  fun `updateFollowCounts handles transaction correctly`() = runTest {
    val followerId = "user1"
    val targetId = "user2"
    val isFollowing = true

    // Mock the profiles collection
    whenever(mockFirestore.collection("profiles")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(targetId)).thenReturn(mockDocumentRef)
    whenever(mockCollectionRef.document(followerId)).thenReturn(mockDocumentRef)

    // Mock transaction
    val mockTransaction = mock(Transaction::class.java)
    whenever(mockFirestore.runTransaction<Void>(any())).thenAnswer { invocation ->
      val transactionFunction = invocation.arguments[0] as Transaction.Function<Void>
      transactionFunction.apply(mockTransaction)
      Tasks.forResult(null)
    }

    // Mock transaction.get() behavior
    whenever(mockTransaction.get(mockDocumentRef)).thenReturn(mockSnapshot)
    whenever(mockSnapshot.getLong("followers")).thenReturn(5L)
    whenever(mockSnapshot.getLong("following")).thenReturn(3L)

    repository.updateFollowCounts(followerId, targetId, isFollowing)

    // Verify the updates were called with correct values
    verify(mockTransaction).update(mockDocumentRef, "followers", 6L)
    verify(mockTransaction).update(mockDocumentRef, "following", 4L)
  }

  @Test
  fun `toggleFollow follows user when not currently following - using spy`() = runTest {
    val followerId = "user1"
    val targetId = "user2"

    // Mock isFollowing to return false (not currently following)
    doReturn(false).`when`(repository).isFollowing(followerId, targetId)

    // Mock add operation
    whenever(mockCollectionRef.add(any())).thenReturn(Tasks.forResult(mockDocumentRef))

    // Mock transaction for follow counts
    val mockTargetDocRef = mock(DocumentReference::class.java)
    val mockFollowerDocRef = mock(DocumentReference::class.java)
    val mockTargetSnapshot = mock(DocumentSnapshot::class.java)
    val mockFollowerSnapshot = mock(DocumentSnapshot::class.java)

    whenever(mockCollectionRef.document(targetId)).thenReturn(mockTargetDocRef)
    whenever(mockCollectionRef.document(followerId)).thenReturn(mockFollowerDocRef)
    whenever(mockTransaction.get(mockTargetDocRef)).thenReturn(mockTargetSnapshot)
    whenever(mockTransaction.get(mockFollowerDocRef)).thenReturn(mockFollowerSnapshot)
    whenever(mockTargetSnapshot.getLong("followers")).thenReturn(5L)
    whenever(mockFollowerSnapshot.getLong("following")).thenReturn(3L)

    whenever(mockFirestore.runTransaction<Void>(any())).thenAnswer { invocation ->
      val transactionFunction = invocation.arguments[0] as Transaction.Function<Void>
      transactionFunction.apply(mockTransaction)
      Tasks.forResult(null)
    }

    // Execute toggleFollow
    val result = repository.toggleFollow(followerId, targetId)

    // Verify the result is true (now following)
    assertTrue(result)

    // Verify follow document was added
    verify(mockCollectionRef)
        .add(
            argThat { map: Map<String, Any> ->
              map["followerId"] == followerId &&
                  map["followedId"] == targetId &&
                  map.containsKey("timestamp")
            })
  }

  @Test
  fun `toggleFollow unfollows user when currently following - simplified`() = runTest {
    val followerId = "user1"
    val targetId = "user2"

    // Mock isFollowing to return true (currently following)
    doReturn(true).`when`(repository).isFollowing(followerId, targetId)

    // Mock add operation for the collection reference that will be used in unfollowUser
    val mockQuery = mock(Query::class.java)
    whenever(mockCollectionRef.whereEqualTo("followerId", followerId)).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo("followedId", targetId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))

    // Mock document to be deleted
    val mockDocRef = mock(DocumentReference::class.java)
    whenever(mockSnapshot.reference).thenReturn(mockDocRef)
    whenever(mockDocRef.delete()).thenReturn(Tasks.forResult(null))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockSnapshot))

    // Mock transaction for follow counts
    val mockTargetDocRef = mock(DocumentReference::class.java)
    val mockFollowerDocRef = mock(DocumentReference::class.java)
    val mockTargetSnapshot = mock(DocumentSnapshot::class.java)
    val mockFollowerSnapshot = mock(DocumentSnapshot::class.java)

    whenever(mockCollectionRef.document(targetId)).thenReturn(mockTargetDocRef)
    whenever(mockCollectionRef.document(followerId)).thenReturn(mockFollowerDocRef)
    whenever(mockTransaction.get(mockTargetDocRef)).thenReturn(mockTargetSnapshot)
    whenever(mockTransaction.get(mockFollowerDocRef)).thenReturn(mockFollowerSnapshot)
    whenever(mockTargetSnapshot.getLong("followers")).thenReturn(6L)
    whenever(mockFollowerSnapshot.getLong("following")).thenReturn(4L)

    whenever(mockFirestore.runTransaction<Void>(any())).thenAnswer { invocation ->
      val transactionFunction = invocation.arguments[0] as Transaction.Function<Void>
      transactionFunction.apply(mockTransaction)
      Tasks.forResult(null)
    }

    // Execute toggleFollow
    val result = repository.toggleFollow(followerId, targetId)

    // Verify the result is false (no longer following)
    assertFalse(result)

    // Verify the unfollow operation
    verify(mockDocRef).delete()

    // Verify follow counts were updated correctly
    verify(mockTransaction).update(mockTargetDocRef, "followers", 5L)
    verify(mockTransaction).update(mockFollowerDocRef, "following", 3L)
  }

  @Test
  fun `toggleFollow handles errors gracefully`() = runTest {
    val followerId = "user1"
    val targetId = "user2"
    val mockQuery = mock(Query::class.java)

    // Mock isFollowing to throw an exception
    whenever(mockCollectionRef.whereEqualTo("followerId", followerId)).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo("followedId", targetId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenThrow(RuntimeException("Firestore error"))

    try {
      repository.toggleFollow(followerId, targetId)
      fail("Expected an exception to be thrown")
    } catch (e: Exception) {
      // Verify that the exception is propagated
      assertTrue(e is RuntimeException)
      assertEquals("Firestore error", e.message)
    }
  }

  @Test
  fun `getProfile handles errors gracefully`() = runTest {
    val userId = "testUser"

    // Mock profile document to throw exception
    val profileDocRef = mock(DocumentReference::class.java)
    whenever(mockCollectionRef.document(userId)).thenReturn(profileDocRef)
    whenever(profileDocRef.get()).thenThrow(RuntimeException("Firestore error"))

    try {
      repository.getProfile(userId)
      fail("Expected an exception to be thrown")
    } catch (e: Exception) {
      assertTrue(e is RuntimeException)
      assertEquals("Firestore error", e.message)
    }

    verify(profileDocRef).get()
  }

  @Test
  fun `getFollowers returns correct list of followers`() = runTest {
    val userId = "testUser"
    val currentUserId = "currentUser"

    // Mock followers collection query
    val mockQuery = mock(Query::class.java)
    val mockFollowerDoc = mock(DocumentSnapshot::class.java)
    val mockProfileDoc = mock(DocumentReference::class.java)
    val mockProfileSnapshot = mock(DocumentSnapshot::class.java)

    val followerProfile =
        Profile(id = "follower1", username = "Follower1", followers = 10, following = 5)

    // Set up followers collection mocks
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("followedId", userId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockFollowerDoc))
    whenever(mockFollowerDoc.getString("followerId")).thenReturn("follower1")

    // Set up profiles collection mocks
    whenever(mockFirestore.collection("profiles")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document("follower1")).thenReturn(mockProfileDoc)
    whenever(mockProfileDoc.get()).thenReturn(Tasks.forResult(mockProfileSnapshot))
    whenever(mockProfileSnapshot.toObject(Profile::class.java)).thenReturn(followerProfile)

    // Mock isFollowing check
    doReturn(true).`when`(repository).isFollowing(currentUserId, "follower1")

    val followers = repository.getFollowers(userId)

    assertEquals(1, followers.size)
    assertEquals("follower1", followers[0].id)
    assertEquals("Follower1", followers[0].username)
    assertTrue(followers[0].isFollowedByCurrentUser)
    verify(mockQuery).get()
  }

  @Test
  fun `getFollowers handles empty followers list`() = runTest {
    val userId = "testUser"

    // Mock followers collection query
    val mockFollowersQuery = mock(Query::class.java)
    val mockFollowerDocs = mock(QuerySnapshot::class.java)

    // Set up mocks
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("followedId", userId)).thenReturn(mockFollowersQuery)
    whenever(mockFollowersQuery.get()).thenReturn(Tasks.forResult(mockFollowerDocs))
    whenever(mockFollowerDocs.documents).thenReturn(emptyList())

    val followers = repository.getFollowers(userId)

    assertTrue(followers.isEmpty())
    verify(mockFollowersQuery).get()
  }

  @Test
  fun `getFollowers handles errors gracefully`() = runTest {
    val userId = "testUser"

    // Mock followers collection query to throw exception
    val mockFollowersQuery = mock(Query::class.java)
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("followedId", userId)).thenReturn(mockFollowersQuery)
    whenever(mockFollowersQuery.get()).thenThrow(RuntimeException("Firestore error"))

    val followers = repository.getFollowers(userId)

    assertTrue(followers.isEmpty())
    verify(mockFollowersQuery).get()
  }

  @Test
  fun `getFollowing returns correct list of following users`() = runTest {
    val userId = "testUser"

    // Mock following collection query
    val mockFollowingQuery = mock(Query::class.java)
    val mockFollowingDocs = mock(QuerySnapshot::class.java)
    val mockFollowingDoc = mock(DocumentSnapshot::class.java)

    // Mock profile document for the followed user
    val mockFollowedProfileDoc = mock(DocumentSnapshot::class.java)
    val followedProfile =
        Profile(id = "followed1", username = "Followed1", followers = 20, following = 15)

    // Set up mocks for following collection
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("followerId", userId)).thenReturn(mockFollowingQuery)
    whenever(mockFollowingQuery.get()).thenReturn(Tasks.forResult(mockFollowingDocs))
    whenever(mockFollowingDocs.documents).thenReturn(listOf(mockFollowingDoc))
    whenever(mockFollowingDoc.getString("followedId")).thenReturn("followed1")

    // Set up mocks for profile collection
    whenever(mockCollectionRef.document("followed1")).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.get()).thenReturn(Tasks.forResult(mockFollowedProfileDoc))
    whenever(mockFollowedProfileDoc.toObject(Profile::class.java)).thenReturn(followedProfile)

    val following = repository.getFollowing(userId)

    assertEquals(1, following.size)
    assertEquals("followed1", following[0].id)
    assertEquals("Followed1", following[0].username)
    assertTrue(following[0].isFollowedByCurrentUser) // Should always be true for following list
    verify(mockFollowingQuery).get()
  }

  @Test
  fun `getFollowing handles empty following list`() = runTest {
    val userId = "testUser"

    // Mock following collection query
    val mockFollowingQuery = mock(Query::class.java)
    val mockFollowingDocs = mock(QuerySnapshot::class.java)

    // Set up mocks
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("followerId", userId)).thenReturn(mockFollowingQuery)
    whenever(mockFollowingQuery.get()).thenReturn(Tasks.forResult(mockFollowingDocs))
    whenever(mockFollowingDocs.documents).thenReturn(emptyList())

    val following = repository.getFollowing(userId)

    assertTrue(following.isEmpty())
    verify(mockFollowingQuery).get()
  }

  @Test
  fun `getFollowing handles errors gracefully`() = runTest {
    val userId = "testUser"

    // Mock following collection query to throw exception
    val mockFollowingQuery = mock(Query::class.java)
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("followerId", userId)).thenReturn(mockFollowingQuery)
    whenever(mockFollowingQuery.get()).thenThrow(RuntimeException("Firestore error"))

    val following = repository.getFollowing(userId)

    assertTrue(following.isEmpty())
    verify(mockFollowingQuery).get()
  }

  @Test
  fun `getFollowing sets isFollowedByCurrentUser to true for all profiles`() = runTest {
    val userId = "testUser"

    // Mock following collection query
    val mockFollowingQuery = mock(Query::class.java)
    val mockFollowingDocs = mock(QuerySnapshot::class.java)
    val mockFollowingDoc1 = mock(DocumentSnapshot::class.java)
    val mockFollowingDoc2 = mock(DocumentSnapshot::class.java)

    // Mock profile documents
    val mockFollowedProfileDoc1 = mock(DocumentSnapshot::class.java)
    val mockFollowedProfileDoc2 = mock(DocumentSnapshot::class.java)

    val followedProfile1 = Profile(id = "followed1", username = "Followed1")
    val followedProfile2 = Profile(id = "followed2", username = "Followed2")

    // Set up mocks for following collection
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("followerId", userId)).thenReturn(mockFollowingQuery)
    whenever(mockFollowingQuery.get()).thenReturn(Tasks.forResult(mockFollowingDocs))
    whenever(mockFollowingDocs.documents).thenReturn(listOf(mockFollowingDoc1, mockFollowingDoc2))
    whenever(mockFollowingDoc1.getString("followedId")).thenReturn("followed1")
    whenever(mockFollowingDoc2.getString("followedId")).thenReturn("followed2")

    // Set up mocks for profile collection
    val mockDocRef1 = mock(DocumentReference::class.java)
    val mockDocRef2 = mock(DocumentReference::class.java)
    whenever(mockCollectionRef.document("followed1")).thenReturn(mockDocRef1)
    whenever(mockCollectionRef.document("followed2")).thenReturn(mockDocRef2)
    whenever(mockDocRef1.get()).thenReturn(Tasks.forResult(mockFollowedProfileDoc1))
    whenever(mockDocRef2.get()).thenReturn(Tasks.forResult(mockFollowedProfileDoc2))
    whenever(mockFollowedProfileDoc1.toObject(Profile::class.java)).thenReturn(followedProfile1)
    whenever(mockFollowedProfileDoc2.toObject(Profile::class.java)).thenReturn(followedProfile2)

    val following = repository.getFollowing(userId)

    assertEquals(2, following.size)
    assertTrue(following.all { it.isFollowedByCurrentUser })
  }

  @Test
  fun `deletePublication successfully deletes publication and associated data`() = runTest {
    val publicationId = "pub123"
    val userId = "user123"
    val mockMediaUrl = "http://example.com/media.jpg"
    val mockThumbnailUrl = "http://example.com/thumb.jpg"
    val mockLikedBy = listOf("user1", "user2")

    // Mock document reference
    val mockDocRef = mock(DocumentReference::class.java)
    whenever(mockDocRef.delete()).thenReturn(Tasks.forResult(null))

    // Mock publication query
    whenever(mockCollectionRef.whereEqualTo("id", publicationId)).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo("userId", userId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.isEmpty).thenReturn(false)
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockSnapshot))
    whenever(mockSnapshot.reference).thenReturn(mockDocRef)
    whenever(mockSnapshot.getString("mediaUrl")).thenReturn(mockMediaUrl)
    whenever(mockSnapshot.getString("thumbnailUrl")).thenReturn(mockThumbnailUrl)
    whenever(mockSnapshot.get("likedBy")).thenReturn(mockLikedBy)

    // Mock storage operations
    val mockStorageRef = mock(StorageReference::class.java)
    whenever(mockStorage.getReferenceFromUrl(any())).thenReturn(mockStorageRef)
    whenever(mockStorageRef.delete()).thenReturn(Tasks.forResult(null))

    // Mock users collection
    val mockLikedPubRef = mock(DocumentReference::class.java)
    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(any())).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("likedPublications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(publicationId)).thenReturn(mockLikedPubRef)
    whenever(mockLikedPubRef.delete()).thenReturn(Tasks.forResult(null))

    val result = repository.deletePublication(publicationId, userId)
    advanceUntilIdle()

    assertTrue(result)
    verify(mockDocRef).delete()
    verify(mockStorageRef, times(2)).delete()
  }

  @Test
  fun `deletePublication returns false when publication not found`() = runTest {
    val publicationId = "pub123"
    val userId = "user123"

    val mockQuery = mock(Query::class.java)
    whenever(mockFirestore.collection("publications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("id", publicationId)).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo("userId", userId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.isEmpty).thenReturn(true)

    val result = repository.deletePublication(publicationId, userId)

    assertFalse(result)
  }

  @Test
  fun `addToFavorites successfully adds publication to favorites`() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"
    val mockUsersCollectionRef = mock(CollectionReference::class.java)
    val mockUserDocumentRef = mock(DocumentReference::class.java)
    val mockFavoritePublicationsCollectionRef = mock(CollectionReference::class.java)
    val mockPublicationDocumentRef = mock(DocumentReference::class.java)

    whenever(mockFirestore.collection("users")).thenReturn(mockUsersCollectionRef)
    whenever(mockUsersCollectionRef.document(userId)).thenReturn(mockUserDocumentRef)
    whenever(mockUserDocumentRef.collection("favoritePublications"))
        .thenReturn(mockFavoritePublicationsCollectionRef)
    whenever(mockFavoritePublicationsCollectionRef.document(publicationId))
        .thenReturn(mockPublicationDocumentRef)
    whenever(mockPublicationDocumentRef.set(any())).thenReturn(Tasks.forResult(null))

    repository.addToFavorites(userId, publicationId)

    verify(mockPublicationDocumentRef)
        .set(
            argThat { map: Map<String, Any> ->
              map["publicationId"] == publicationId && map.containsKey("timestamp")
            })
  }

  @Test
  fun `getFavoritePublicationsIds returns list of favorited publication IDs`() = runTest {
    val userId = "testUser"
    val mockQuerySnapshot = mock(QuerySnapshot::class.java)
    val mockDocumentSnapshot1 = mock(DocumentSnapshot::class.java)
    val mockDocumentSnapshot2 = mock(DocumentSnapshot::class.java)

    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("favoritePublications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents)
        .thenReturn(listOf(mockDocumentSnapshot1, mockDocumentSnapshot2))
    whenever(mockDocumentSnapshot1.getString("publicationId")).thenReturn("pub1")
    whenever(mockDocumentSnapshot2.getString("publicationId")).thenReturn("pub2")

    val result = repository.getFavoritePublicationsIds(userId)

    assertEquals(listOf("pub1", "pub2"), result)
    verify(mockCollectionRef).get()
  }

  @Test
  fun `isPublicationFavorited returns true when publication is favorited`() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"
    val mockDocSnapshot = mock(DocumentSnapshot::class.java)

    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("favoritePublications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(publicationId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.get()).thenReturn(Tasks.forResult(mockDocSnapshot))
    whenever(mockDocSnapshot.exists()).thenReturn(true)

    val result = repository.isPublicationFavorited(userId, publicationId)

    assertTrue(result)
    verify(mockDocumentRef).get()
  }

  @Test
  fun `isPublicationFavorited returns false when publication is not favorited`() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"
    val mockDocSnapshot = mock(DocumentSnapshot::class.java)

    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("favoritePublications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(publicationId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.get()).thenReturn(Tasks.forResult(mockDocSnapshot))
    whenever(mockDocSnapshot.exists()).thenReturn(false)

    val result = repository.isPublicationFavorited(userId, publicationId)

    assertFalse(result)
    verify(mockDocumentRef).get()
  }

  @Test
  fun `getFavoritePublicationsIds returns empty list on error`() = runTest {
    val userId = "testUser"

    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("favoritePublications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.get()).thenThrow(RuntimeException("Firestore error"))

    val result = repository.getFavoritePublicationsIds(userId)

    assertTrue(result.isEmpty())
    verify(mockCollectionRef).get()
  }

  @Test
  fun `incrementLikes handles empty query result`() = runTest {
    val publicationId = "pub123"
    val userId = "user456"

    // Mock empty query result
    whenever(mockFirestore.collection("publications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("id", publicationId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.isEmpty).thenReturn(true)

    repository.incrementLikes(publicationId, userId)
    // Verify no transaction was attempted
    verify(mockFirestore, never()).runTransaction<Any>(any())
  }

  @Test
  fun `getFavoritePublications handles invalid publication reference`() = runTest {
    val favoriteIds = listOf("pub1", "pub2")
    val mockPublication = mock(Publication::class.java)

    whenever(mockFirestore.collection("publications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereIn("id", favoriteIds)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockSnapshot))
    whenever(mockSnapshot.toObject(Publication::class.java)).thenReturn(null)

    val result = repository.getFavoritePublications(favoriteIds)
    assertTrue(result.isEmpty())
  }

  @Test
  fun `removeFromLikedPublications throws exception with error message on failure`() = runTest {
    val userId = "testUser"
    val publicationId = "pub123"

    // Mock the collections and document references
    val mockUsersCollection = mock(CollectionReference::class.java)
    val mockUserDoc = mock(DocumentReference::class.java)
    val mockLikedPubsCollection = mock(CollectionReference::class.java)
    val mockPubDoc = mock(DocumentReference::class.java)

    whenever(mockFirestore.collection("users")).thenReturn(mockUsersCollection)
    whenever(mockUsersCollection.document(userId)).thenReturn(mockUserDoc)
    whenever(mockUserDoc.collection("likedPublications")).thenReturn(mockLikedPubsCollection)
    whenever(mockLikedPubsCollection.document(publicationId)).thenReturn(mockPubDoc)

    // Mock the delete operation to throw an exception
    val errorMessage = "Network error during delete"
    whenever(mockPubDoc.delete()).thenThrow(RuntimeException(errorMessage))

    try {
      repository.removeFromLikedPublications(userId, publicationId)
      fail("Expected an exception to be thrown")
    } catch (e: Exception) {
      // Verify only the actual exception message
      assertEquals(errorMessage, e.message)
    }
  }

  @Test
  fun `decrementLikesAndRemoveUser handles transaction failure and throws exception`() = runTest {
    val publicationId = "pub123"
    val userId = "user456"
    val documentRef = mock(DocumentReference::class.java)
    val errorMessage = "Transaction failed due to network error"

    // Mock the publication query
    whenever(mockFirestore.collection("publications")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("id", publicationId)).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.isEmpty).thenReturn(false)
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockSnapshot))
    whenever(mockSnapshot.reference).thenReturn(documentRef)

    // Mock transaction to throw exception
    whenever(mockFirestore.runTransaction<Void>(any())).thenThrow(RuntimeException(errorMessage))

    try {
      repository.decrementLikesAndRemoveUser(publicationId, userId)
      fail("Expected an exception to be thrown")
    } catch (e: Exception) {
      // Verify only the actual exception message
      assertEquals(errorMessage, e.message)
    }

    // Verify that the transaction was attempted
    verify(mockFirestore).runTransaction<Void>(any())
  }

  @Test
  fun `getProfile handles favorites retrieval correctly`() = runTest {
    val userId = "testUser"
    val favoritePublicationId = "favPub1"

    // Mock base profile document
    val profileSnapshot = mock(DocumentSnapshot::class.java)
    val initialProfile = Profile(id = userId, username = "testUser")
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.get()).thenReturn(Tasks.forResult(profileSnapshot))
    whenever(profileSnapshot.toObject(Profile::class.java)).thenReturn(initialProfile)

    // Mock publications query
    val pubQuery = mock(Query::class.java)
    val pubSnapshot = mock(QuerySnapshot::class.java)
    whenever(mockCollectionRef.whereEqualTo("userId", userId)).thenReturn(pubQuery)
    whenever(pubQuery.get()).thenReturn(Tasks.forResult(pubSnapshot))
    whenever(pubSnapshot.documents).thenReturn(emptyList())

    // Mock publication document for favorite
    val favPubSnapshot = mock(DocumentSnapshot::class.java)
    val favPubDoc = mock(DocumentReference::class.java)
    whenever(mockCollectionRef.document(favoritePublicationId)).thenReturn(favPubDoc)
    whenever(favPubDoc.get()).thenReturn(Tasks.forResult(favPubSnapshot))
    whenever(favPubSnapshot.toObject(Publication::class.java))
        .thenReturn(Publication(id = favoritePublicationId, title = "Favorite Publication"))

    // Mock favorites query
    val favQuery = mock(Query::class.java)
    val favSnapshot = mock(QuerySnapshot::class.java)
    val favDoc = mock(DocumentSnapshot::class.java)
    whenever(mockCollectionRef.whereEqualTo("userId", userId)).thenReturn(favQuery)
    whenever(favQuery.get()).thenReturn(Tasks.forResult(favSnapshot))
    whenever(favSnapshot.documents).thenReturn(listOf(favDoc))
    whenever(favDoc.getString("publicationId")).thenReturn(favoritePublicationId)

    // Mock followers/following queries
    val followersQuery = mock(Query::class.java)
    val followingQuery = mock(Query::class.java)
    val followersSnapshot = mock(QuerySnapshot::class.java)
    val followingSnapshot = mock(QuerySnapshot::class.java)
    whenever(mockCollectionRef.whereEqualTo("followedId", userId)).thenReturn(followersQuery)
    whenever(mockCollectionRef.whereEqualTo("followerId", userId)).thenReturn(followingQuery)
    whenever(followersQuery.get()).thenReturn(Tasks.forResult(followersSnapshot))
    whenever(followingQuery.get()).thenReturn(Tasks.forResult(followingSnapshot))
    whenever(followersSnapshot.size()).thenReturn(5)
    whenever(followingSnapshot.size()).thenReturn(3)

    // Override the collection returns for different collections
    whenever(mockFirestore.collection("profiles")).thenReturn(mockCollectionRef)
    whenever(mockFirestore.collection("publications")).thenReturn(mockCollectionRef)
    whenever(mockFirestore.collection("favorites")).thenReturn(mockCollectionRef)
    whenever(mockFirestore.collection("followers")).thenReturn(mockCollectionRef)

    // Mock isFollowing check
    doReturn(true).`when`(repository).isFollowing("currentUser", userId)

    // Execute getProfile
    val result = repository.getProfile(userId)

    // Verify result
    assertNotNull(result)
    assertEquals(1, result?.favoritePublications?.size)
    assertEquals(favoritePublicationId, result?.favoritePublications?.first()?.id)
    assertEquals("Favorite Publication", result?.favoritePublications?.first()?.title)
    assertEquals(5, result?.followers)
    assertEquals(3, result?.following)
    assertTrue(result?.isFollowedByCurrentUser ?: false)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
    firebaseAuthMock.close()
  }
}

class TestableProfileRepositoryImpl(firestore: FirebaseFirestore, storage: FirebaseStorage) :
    ProfileRepositoryImpl(firestore, storage) {
  // Make isFollowing public so we can override it in tests
  public override suspend fun isFollowing(followerId: String, targetUserId: String): Boolean {
    return super.isFollowing(followerId, targetUserId)
  }
}
