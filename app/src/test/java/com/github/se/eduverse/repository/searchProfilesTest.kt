package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Profile
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.lenient
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class) // Changed to Silent mode
class SearchProfilesTests {
  @Mock private lateinit var firestore: FirebaseFirestore

  @Mock private lateinit var storage: FirebaseStorage

  @Mock private lateinit var profilesCollection: CollectionReference

  @Mock private lateinit var followersCollection: CollectionReference

  @Mock private lateinit var currentUser: FirebaseUser

  @Mock private lateinit var auth: FirebaseAuth

  private lateinit var repository: ProfileRepositoryImpl
  private lateinit var mockFirebaseAuth: MockedStatic<FirebaseAuth>

  @Before
  fun setup() {
    // Mock collections
    lenient().`when`(firestore.collection("profiles")).thenReturn(profilesCollection)
    lenient().`when`(firestore.collection("followers")).thenReturn(followersCollection)
    lenient().`when`(firestore.collection("favorites")).thenReturn(mock())
    lenient().`when`(firestore.collection("publications")).thenReturn(mock())
    lenient().`when`(firestore.collection("users")).thenReturn(mock())

    setupFirebaseAuth()

    repository = ProfileRepositoryImpl(firestore, storage)
  }

  @After
  fun tearDown() {
    if (::mockFirebaseAuth.isInitialized) {
      mockFirebaseAuth.close()
    }
  }

  private fun setupFirebaseAuth() {
    mockFirebaseAuth = Mockito.mockStatic(FirebaseAuth::class.java)
    mockFirebaseAuth.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(auth)
    lenient().`when`(auth.currentUser).thenReturn(currentUser)
    lenient().`when`(currentUser.uid).thenReturn("test_user_id")
  }

  @Test
  fun `searchProfiles returns filtered profiles matching query`() = runTest {
    // Given
    val query = "john"
    val limit = 20

    val profiles =
        listOf(
            Profile(
                id = "1",
                username = "john_doe",
                followers = 0,
                following = 0,
                publications = emptyList(),
                favoritePublications = emptyList(),
                profileImageUrl = "",
                isFollowedByCurrentUser = false),
            Profile(
                id = "2",
                username = "johnny_smith",
                followers = 0,
                following = 0,
                publications = emptyList(),
                favoritePublications = emptyList(),
                profileImageUrl = "",
                isFollowedByCurrentUser = false),
            Profile(
                id = "3",
                username = "alice_jones",
                followers = 0,
                following = 0,
                publications = emptyList(),
                favoritePublications = emptyList(),
                profileImageUrl = "",
                isFollowedByCurrentUser = false))

    // Mock profile query results
    val querySnapshot = mock<QuerySnapshot>()
    val documents =
        profiles.map { profile ->
          mock<DocumentSnapshot>().apply {
            lenient().`when`(toObject(Profile::class.java)).thenReturn(profile)
          }
        }
    lenient().`when`(querySnapshot.documents).thenReturn(documents)
    lenient().`when`(profilesCollection.get()).thenReturn(Tasks.forResult(querySnapshot))

    // Mock the followers collection queries
    val followersQuery = mock<Query>()
    val followingQuery = mock<Query>()
    val followersSnapshot = mock<QuerySnapshot>()

    // Mock followers count query
    lenient().`when`(followersCollection.whereEqualTo("followedId", "1")).thenReturn(followersQuery)
    lenient().`when`(followersCollection.whereEqualTo("followedId", "2")).thenReturn(followersQuery)
    lenient().`when`(followersCollection.whereEqualTo("followedId", "3")).thenReturn(followersQuery)
    lenient().`when`(followersQuery.get()).thenReturn(Tasks.forResult(followersSnapshot))
    lenient().`when`(followersSnapshot.size()).thenReturn(0)

    // Mock following count query
    lenient().`when`(followersCollection.whereEqualTo("followerId", "1")).thenReturn(followingQuery)
    lenient().`when`(followersCollection.whereEqualTo("followerId", "2")).thenReturn(followingQuery)
    lenient().`when`(followersCollection.whereEqualTo("followerId", "3")).thenReturn(followingQuery)
    lenient().`when`(followingQuery.get()).thenReturn(Tasks.forResult(followersSnapshot))

    // Mock isFollowing queries
    val isFollowingFirstQuery = mock<Query>()
    val isFollowingSecondQuery = mock<Query>()
    val isFollowingSnapshot = mock<QuerySnapshot>()

    lenient()
        .`when`(followersCollection.whereEqualTo("followerId", "test_user_id"))
        .thenReturn(isFollowingFirstQuery)
    lenient()
        .`when`(isFollowingFirstQuery.whereEqualTo("followedId", "1"))
        .thenReturn(isFollowingSecondQuery)
    lenient()
        .`when`(isFollowingFirstQuery.whereEqualTo("followedId", "2"))
        .thenReturn(isFollowingSecondQuery)
    lenient()
        .`when`(isFollowingFirstQuery.whereEqualTo("followedId", "3"))
        .thenReturn(isFollowingSecondQuery)
    lenient().`when`(isFollowingSecondQuery.get()).thenReturn(Tasks.forResult(isFollowingSnapshot))
    lenient().`when`(isFollowingSnapshot.documents).thenReturn(emptyList())

    // Mock FirebaseAuth
    lenient().`when`(auth.currentUser).thenReturn(currentUser)
    lenient().`when`(currentUser.uid).thenReturn("test_user_id")

    // When
    val result = repository.searchProfiles(query, limit)

    // Then
    assertEquals(2, result.size)
    assertTrue(result.all { it.username.lowercase().contains(query) })
  }

  @Test
  fun `searchProfiles returns empty list when no matches found`() = runTest {
    // Given
    val query = "xyz"
    val limit = 20

    val profiles =
        listOf(
            Profile(id = "1", username = "john_doe", followers = 0, following = 0),
            Profile(id = "2", username = "alice_smith", followers = 0, following = 0))

    // Mock profile query results
    val querySnapshot = mock<QuerySnapshot>()
    val documents =
        profiles.map { profile ->
          mock<DocumentSnapshot>().apply {
            Mockito.`when`(toObject(Profile::class.java)).thenReturn(profile)
          }
        }
    lenient().`when`(querySnapshot.documents).thenReturn(documents)
    lenient().`when`(profilesCollection.get()).thenReturn(Tasks.forResult(querySnapshot))

    // Mock followers queries with non-null results
    val followersQuery = mock<Query>()
    val followersSnapshot = mock<QuerySnapshot>()
    lenient()
        .`when`(followersCollection.whereEqualTo(anyString(), anyString()))
        .thenReturn(followersQuery)
    lenient().`when`(followersQuery.get()).thenReturn(Tasks.forResult(followersSnapshot))
    lenient().`when`(followersSnapshot.size()).thenReturn(0)
    lenient().`when`(followersSnapshot.documents).thenReturn(emptyList())

    // When
    val result = repository.searchProfiles(query, limit)

    // Then
    assertTrue(result.isEmpty())
  }

  @Test
  fun `searchProfiles respects limit parameter`() = runTest {
    // Given
    val query = "user"
    val limit = 2

    val profiles =
        listOf(
            Profile(id = "1", username = "user_one", followers = 0, following = 0),
            Profile(id = "2", username = "user_two", followers = 0, following = 0),
            Profile(id = "3", username = "user_three", followers = 0, following = 0))

    // Mock profile query results
    val querySnapshot = mock<QuerySnapshot>()
    val documents =
        profiles.map { profile ->
          mock<DocumentSnapshot>().apply {
            Mockito.`when`(toObject(Profile::class.java)).thenReturn(profile)
          }
        }
    lenient().`when`(querySnapshot.documents).thenReturn(documents)
    lenient().`when`(profilesCollection.get()).thenReturn(Tasks.forResult(querySnapshot))

    // Mock followers and following queries
    val followersQuery = mock<Query>()
    val followersSnapshot = mock<QuerySnapshot>()

    // Use eq() matcher consistently
    lenient()
        .`when`(followersCollection.whereEqualTo(eq("followedId"), anyString()))
        .thenReturn(followersQuery)
    lenient()
        .`when`(followersCollection.whereEqualTo(eq("followerId"), anyString()))
        .thenReturn(followersQuery)
    lenient().`when`(followersQuery.get()).thenReturn(Tasks.forResult(followersSnapshot))
    lenient().`when`(followersSnapshot.size()).thenReturn(0)
    lenient().`when`(followersSnapshot.documents).thenReturn(emptyList())

    // Mock isFollowing queries
    val isFollowingQuery = mock<Query>()
    val isFollowingSnapshot = mock<QuerySnapshot>()
    lenient()
        .`when`(followersCollection.whereEqualTo(eq("followerId"), eq("test_user_id")))
        .thenReturn(isFollowingQuery)
    lenient()
        .`when`(isFollowingQuery.whereEqualTo(eq("followedId"), anyString()))
        .thenReturn(isFollowingQuery)
    lenient().`when`(isFollowingQuery.get()).thenReturn(Tasks.forResult(isFollowingSnapshot))
    lenient().`when`(isFollowingSnapshot.documents).thenReturn(emptyList())

    // When
    val result = repository.searchProfiles(query, limit)

    // Then
    assertEquals(limit, result.size)
    assertTrue(result.all { it.username.lowercase().contains(query) })
  }

  @Test
  fun `searchProfiles includes follower counts and current user follow status`() = runTest {
    // Given
    val query = "test"
    val limit = 20
    val currentUserId = "test_user_id"

    val profile = Profile(id = "1", username = "test_user", followers = 0, following = 0)

    // Mock profile query results
    val querySnapshot = mock<QuerySnapshot>()
    val document =
        mock<DocumentSnapshot>().apply {
          Mockito.`when`(toObject(Profile::class.java)).thenReturn(profile)
        }
    lenient().`when`(querySnapshot.documents).thenReturn(listOf(document))
    lenient().`when`(profilesCollection.get()).thenReturn(Tasks.forResult(querySnapshot))

    // Mock followers queries with non-null results
    val followersQuery = mock<Query>()
    val followersSnapshot = mock<QuerySnapshot>()
    lenient()
        .`when`(followersCollection.whereEqualTo(anyString(), anyString()))
        .thenReturn(followersQuery)
    lenient()
        .`when`(followersQuery.whereEqualTo(anyString(), anyString()))
        .thenReturn(followersQuery)
    lenient().`when`(followersSnapshot.size()).thenReturn(5)
    lenient().`when`(followersSnapshot.documents).thenReturn(listOf(mock()))
    lenient().`when`(followersQuery.get()).thenReturn(Tasks.forResult(followersSnapshot))

    // When
    val result = repository.searchProfiles(query, limit)

    // Then
    assertEquals(1, result.size)
    val resultProfile = result.first()
    assertEquals(5, resultProfile.followers)
    assertEquals(5, resultProfile.following)
    assertTrue(resultProfile.isFollowedByCurrentUser)
  }

  private inline fun <reified T> mock(): T = Mockito.mock(T::class.java)
}
