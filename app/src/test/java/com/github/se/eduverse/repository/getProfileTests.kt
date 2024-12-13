package com.github.se.eduverse.repository

import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

private class TestProfileRepositoryForGetProfile(
    firestore: FirebaseFirestore,
    storage: FirebaseStorage
) : ProfileRepositoryImpl(firestore, storage) {
  override suspend fun isFollowing(followerId: String, targetUserId: String): Boolean = true
}

class GetProfileTests {
  private val mockFirestore: FirebaseFirestore = mock()
  private val mockStorage: FirebaseStorage = mock()
  private val mockFirebaseAuth: FirebaseAuth = mock()
  private val mockFirebaseUser: FirebaseUser = mock()

  // Collections
  private val mockProfilesCollection: CollectionReference = mock()
  private val mockPublicationsCollection: CollectionReference = mock()
  private val mockFavoritesCollection: CollectionReference = mock()
  private val mockFollowersCollection: CollectionReference = mock()
  private val mockUsersCollection: CollectionReference = mock()

  private lateinit var repository: TestProfileRepositoryForGetProfile
  private val testUserId = "testUserId"
  private val currentUserId = "currentUserId"
  private val testTimestamp = System.currentTimeMillis()

  @Before
  fun setUp() {
    // Mock FirebaseAuth singleton
    Mockito.mockStatic(FirebaseAuth::class.java).use { mockedFirebaseAuth ->
      mockedFirebaseAuth
          .`when`<FirebaseAuth> { FirebaseAuth.getInstance() }
          .thenReturn(mockFirebaseAuth)

      // Mock current user
      whenever(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
      whenever(mockFirebaseUser.uid).thenReturn(currentUserId)

      // Mock Firestore collections
      doReturn(mockProfilesCollection).`when`(mockFirestore).collection("profiles")
      doReturn(mockPublicationsCollection).`when`(mockFirestore).collection("publications")
      doReturn(mockFavoritesCollection).`when`(mockFirestore).collection("favorites")
      doReturn(mockFollowersCollection).`when`(mockFirestore).collection("followers")
      doReturn(mockUsersCollection).`when`(mockFirestore).collection("users")

      repository = TestProfileRepositoryForGetProfile(mockFirestore, mockStorage)
    }
  }

  @Test
  fun `getProfile returns complete profile with publications and favorites`() = runTest {
    Mockito.mockStatic(FirebaseAuth::class.java).use { mockedFirebaseAuth ->
      mockedFirebaseAuth
          .`when`<FirebaseAuth> { FirebaseAuth.getInstance() }
          .thenReturn(mockFirebaseAuth)

      // Given
      val testProfile =
          Profile(
              id = testUserId,
              username = "testUser",
              profileImageUrl = "test.jpg",
              followers = 0,
              following = 0,
              publications = emptyList()
          )

      val testPublication =
          Publication(
              id = "pub1",
              userId = testUserId,
              title = "Test Publication",
              thumbnailUrl = "thumbnail.jpg",
              mediaUrl = "media.jpg",
              mediaType = MediaType.PHOTO,
              timestamp = testTimestamp,
              likes = 10,
              likedBy = listOf(currentUserId))

      // Mock Profile document chain
      val mockProfileDoc: DocumentReference = mock()
      val mockProfileSnapshot: DocumentSnapshot = mock()
      doReturn(mockProfileDoc).`when`(mockProfilesCollection).document(testUserId)
      doReturn(Tasks.forResult(mockProfileSnapshot)).`when`(mockProfileDoc).get()
      doReturn(testProfile).`when`(mockProfileSnapshot).toObject(Profile::class.java)

      // Mock Publications query chain
      val mockPublicationsQuery: Query = mock()
      val mockPublicationsSnapshot: QuerySnapshot = mock()
      val mockPublicationDoc: DocumentSnapshot = mock()
      doReturn(mockPublicationsQuery)
          .`when`(mockPublicationsCollection)
          .whereEqualTo("userId", testUserId)
      doReturn(Tasks.forResult(mockPublicationsSnapshot)).`when`(mockPublicationsQuery).get()
      doReturn(listOf(mockPublicationDoc)).`when`(mockPublicationsSnapshot).documents
      doReturn(testPublication).`when`(mockPublicationDoc).toObject(Publication::class.java)

      // Mock Followers/Following counts
      val mockFollowersQuery: Query = mock()
      val mockFollowingQuery: Query = mock()
      val mockFollowersSnapshot: QuerySnapshot = mock()
      val mockFollowingSnapshot: QuerySnapshot = mock()

      doReturn(mockFollowersQuery)
          .`when`(mockFollowersCollection)
          .whereEqualTo("followedId", testUserId)
      doReturn(Tasks.forResult(mockFollowersSnapshot)).`when`(mockFollowersQuery).get()
      doReturn(5).`when`(mockFollowersSnapshot).size()

      doReturn(mockFollowingQuery)
          .`when`(mockFollowersCollection)
          .whereEqualTo("followerId", testUserId)
      doReturn(Tasks.forResult(mockFollowingSnapshot)).`when`(mockFollowingQuery).get()
      doReturn(3).`when`(mockFollowingSnapshot).size()

      // Mock empty favorites
      val mockFavoritesQuery: Query = mock()
      val mockFavoritesSnapshot: QuerySnapshot = mock()
      doReturn(mockFavoritesQuery)
          .`when`(mockFavoritesCollection)
          .whereEqualTo("userId", testUserId)
      doReturn(Tasks.forResult(mockFavoritesSnapshot)).`when`(mockFavoritesQuery).get()
      doReturn(emptyList<DocumentSnapshot>()).`when`(mockFavoritesSnapshot).documents

      // When
      val result = repository.getProfile(testUserId)

      // Then
      assertNotNull(result)
      assertEquals(testUserId, result?.id)
      assertEquals("testUser", result?.username)
      assertEquals(5, result?.followers)
      assertEquals(3, result?.following)
      assertEquals(true, result?.isFollowedByCurrentUser)

      // Verify publications
      assertEquals(1, result?.publications?.size)
      result?.publications?.firstOrNull()?.let { pub ->
        assertEquals("pub1", pub.id)
        assertEquals(testUserId, pub.userId)
        assertEquals("Test Publication", pub.title)
        assertEquals("thumbnail.jpg", pub.thumbnailUrl)
        assertEquals("media.jpg", pub.mediaUrl)
        assertEquals(MediaType.PHOTO, pub.mediaType)
        assertEquals(testTimestamp, pub.timestamp)
        assertEquals(10, pub.likes)
        assertEquals(listOf(currentUserId), pub.likedBy)
      }
    }
  }

  @Test
  fun `getProfile returns null when profile doesn't exist`() = runTest {
    Mockito.mockStatic(FirebaseAuth::class.java).use { mockedFirebaseAuth ->
      mockedFirebaseAuth
          .`when`<FirebaseAuth> { FirebaseAuth.getInstance() }
          .thenReturn(mockFirebaseAuth)

      // Given
      val mockProfileDoc: DocumentReference = mock()
      val mockProfileSnapshot: DocumentSnapshot = mock()
      doReturn(mockProfileDoc).`when`(mockProfilesCollection).document(testUserId)
      doReturn(Tasks.forResult(mockProfileSnapshot)).`when`(mockProfileDoc).get()
      doReturn(null).`when`(mockProfileSnapshot).toObject(Profile::class.java)

      // Mock Publications query
      val mockPublicationsQuery: Query = mock()
      val mockPublicationsSnapshot: QuerySnapshot = mock()
      doReturn(mockPublicationsQuery)
          .`when`(mockPublicationsCollection)
          .whereEqualTo("userId", testUserId)
      doReturn(Tasks.forResult(mockPublicationsSnapshot)).`when`(mockPublicationsQuery).get()
      doReturn(emptyList<DocumentSnapshot>()).`when`(mockPublicationsSnapshot).documents

      // Mock Favorites query
      val mockFavoritesQuery: Query = mock()
      val mockFavoritesSnapshot: QuerySnapshot = mock()
      doReturn(mockFavoritesQuery)
          .`when`(mockFavoritesCollection)
          .whereEqualTo("userId", testUserId)
      doReturn(Tasks.forResult(mockFavoritesSnapshot)).`when`(mockFavoritesQuery).get()
      doReturn(emptyList<DocumentSnapshot>()).`when`(mockFavoritesSnapshot).documents

      // Mock Followers query
      val mockFollowersQuery: Query = mock()
      val mockFollowersSnapshot: QuerySnapshot = mock()
      doReturn(mockFollowersQuery)
          .`when`(mockFollowersCollection)
          .whereEqualTo("followedId", testUserId)
      doReturn(Tasks.forResult(mockFollowersSnapshot)).`when`(mockFollowersQuery).get()
      doReturn(0).`when`(mockFollowersSnapshot).size()

      // Mock Following query
      val mockFollowingQuery: Query = mock()
      val mockFollowingSnapshot: QuerySnapshot = mock()
      doReturn(mockFollowingQuery)
          .`when`(mockFollowersCollection)
          .whereEqualTo("followerId", testUserId)
      doReturn(Tasks.forResult(mockFollowingSnapshot)).`when`(mockFollowingQuery).get()
      doReturn(0).`when`(mockFollowingSnapshot).size()

      // When
      val result = repository.getProfile(testUserId)

      // Then
      assertNull(result)
    }
  }
}
