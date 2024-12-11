package com.github.se.eduverse.viewmodel

import android.net.Uri
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.ProfileRepository
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var profileViewModel: ProfileViewModel
  private val mockRepository: ProfileRepository = mock(ProfileRepository::class.java)

  private val defaultProfile = Profile(id = "testId", username = "testUser")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    profileViewModel = ProfileViewModel(mockRepository)

    // Setup default profile without using matchers
    runTest { `when`(mockRepository.getProfile("testUser")).thenReturn(defaultProfile) }
  }

  @Test
  fun `loadProfile success updates state with profile data`() = runTest {
    val testProfile =
        Profile(
            id = "test123",
            username = "testuser",
            followers = 10,
            following = 20,
            publications = listOf(),
            favoritePublications = listOf())

    // Use specific value instead of matcher
    `when`(mockRepository.getProfile("testUser")).thenReturn(testProfile)

    profileViewModel.loadProfile("testUser")
    advanceUntilIdle()

    val state = profileViewModel.profileState.first()
    assertTrue(state is ProfileUiState.Success)
    assertEquals(testProfile, (state as ProfileUiState.Success).profile)
    verify(mockRepository, times(2)).getProfile("testUser")
  }

  @Test
  fun `loadProfile failure updates state with error`() = runTest {
    `when`(mockRepository.getProfile("testUser")).thenThrow(RuntimeException("Network error"))

    profileViewModel.loadProfile("testUser")
    advanceUntilIdle()

    val state = profileViewModel.profileState.first()
    assertTrue(state is ProfileUiState.Error)
    assertEquals("Network error", (state as ProfileUiState.Error).message)
  }

  @Test
  fun `updateProfileImage success updates state`() = runTest {
    clearInvocations(mockRepository)
    val imageUri = mock(Uri::class.java)
    val imageUrl = "http://example.com/image.jpg"
    val userId = "testUser"

    `when`(mockRepository.uploadProfileImage(userId, imageUri)).thenReturn(imageUrl)
    `when`(mockRepository.getProfile(userId)).thenReturn(defaultProfile)

    profileViewModel.updateProfileImage(userId, imageUri)
    advanceUntilIdle()

    val state = profileViewModel.imageUploadState.first()
    assertTrue(state is ImageUploadState.Success)
    verify(mockRepository).updateProfileImage(userId, imageUrl)
    verify(mockRepository, times(2)).getProfile(userId)
  }

  @Test
  fun `addPublication success updates profile`() = runTest {
    clearInvocations(mockRepository)
    val userId = "testUser"
    val publication = Publication(id = "pub1", userId = userId, title = "Test")

    `when`(mockRepository.getProfile(userId)).thenReturn(defaultProfile)

    profileViewModel.addPublication(userId, publication)
    advanceUntilIdle()

    verify(mockRepository).addPublication(userId, publication)
    verify(mockRepository, times(2)).getProfile(userId) // Changed from 1 to 2
  }

  @Test
  fun `addPublication failure updates state with error`() = runTest {
    val userId = "testUser"
    val publication = Publication(id = "pub1", userId = userId, title = "Test")

    `when`(mockRepository.addPublication(userId, publication))
        .thenThrow(RuntimeException("Failed to add"))
    `when`(mockRepository.getProfile(userId)).thenReturn(defaultProfile)

    profileViewModel.addPublication(userId, publication)
    advanceUntilIdle()

    val state = profileViewModel.profileState.first()
    assertTrue(state is ProfileUiState.Error)
    assertEquals("Failed to add", (state as ProfileUiState.Error).message)
  }

  @Test
  fun `toggleFollow updates UI state optimistically on follow action`() = runTest {
    val currentUserId = "user1"
    val targetUserId = "user2"
    val initialProfile =
        Profile(
            id = targetUserId,
            username = "testUser",
            followers = 5,
            isFollowedByCurrentUser = false)

    // Set initial state and mock successful follow
    `when`(mockRepository.getProfile(targetUserId)).thenReturn(initialProfile)
    `when`(mockRepository.toggleFollow(currentUserId, targetUserId)).thenReturn(true)

    profileViewModel.loadProfile(targetUserId)
    advanceUntilIdle()

    // Perform follow action
    profileViewModel.toggleFollow(currentUserId, targetUserId)
    advanceUntilIdle()

    // Verify UI state was updated optimistically
    val state = profileViewModel.profileState.first() as ProfileUiState.Success
    assertTrue(state.profile.isFollowedByCurrentUser)
    assertEquals(6, state.profile.followers)

    // Verify follow action state with new parameters
    val followState = profileViewModel.followActionState.first() as FollowActionState.Success
    assertEquals(currentUserId, followState.followerId)
    assertEquals(targetUserId, followState.targetUserId)
    assertTrue(followState.isNowFollowing)
  }

  @Test
  fun `toggleFollow updates UI state optimistically on unfollow action`() = runTest {
    val currentUserId = "user1"
    val targetUserId = "user2"
    val initialProfile =
        Profile(
            id = targetUserId, username = "testUser", followers = 5, isFollowedByCurrentUser = true)

    // Set initial state and mock successful unfollow
    `when`(mockRepository.getProfile(targetUserId)).thenReturn(initialProfile)
    `when`(mockRepository.toggleFollow(currentUserId, targetUserId)).thenReturn(false)

    profileViewModel.loadProfile(targetUserId)
    advanceUntilIdle()

    // Perform unfollow action
    profileViewModel.toggleFollow(currentUserId, targetUserId)
    advanceUntilIdle()

    // Verify UI state was updated optimistically
    val state = profileViewModel.profileState.first() as ProfileUiState.Success
    assertFalse(state.profile.isFollowedByCurrentUser)
    assertEquals(4, state.profile.followers)

    // Verify follow action state with new parameters
    val followState = profileViewModel.followActionState.first() as FollowActionState.Success
    assertEquals(currentUserId, followState.followerId)
    assertEquals(targetUserId, followState.targetUserId)
    assertFalse(followState.isNowFollowing)
  }

  @Test
  fun `toggleFollow shows error state on failure`() = runTest {
    val currentUserId = "user1"
    val targetUserId = "user2"
    val initialProfile =
        Profile(
            id = targetUserId,
            username = "testUser",
            followers = 5,
            isFollowedByCurrentUser = false)

    // Set initial state and mock error
    `when`(mockRepository.getProfile(targetUserId)).thenReturn(initialProfile)
    `when`(mockRepository.toggleFollow(currentUserId, targetUserId))
        .thenThrow(RuntimeException("Network error"))

    profileViewModel.loadProfile(targetUserId)
    advanceUntilIdle()

    // Perform follow action
    profileViewModel.toggleFollow(currentUserId, targetUserId)
    advanceUntilIdle()

    // Verify follow action error state
    val followState = profileViewModel.followActionState.first()
    assertTrue(followState is FollowActionState.Error)
    assertEquals("Network error", (followState as FollowActionState.Error).message)
  }

  @Test
  fun `toggleFollow handles transaction error gracefully`() = runTest {
    val currentUserId = "user1"
    val targetUserId = "user2"
    val initialProfile =
        Profile(
            id = targetUserId,
            username = "testUser",
            followers = 5,
            isFollowedByCurrentUser = false)

    // Set initial state and mock transaction error
    `when`(mockRepository.getProfile(targetUserId)).thenReturn(initialProfile)
    `when`(mockRepository.toggleFollow(currentUserId, targetUserId))
        .thenThrow(RuntimeException("Firestore transactions require all reads"))

    profileViewModel.loadProfile(targetUserId)
    advanceUntilIdle()

    // Perform follow action
    profileViewModel.toggleFollow(currentUserId, targetUserId)
    advanceUntilIdle()

    // Verify error is handled properly
    val followState = profileViewModel.followActionState.first()
    assertTrue(followState is FollowActionState.Error)
    assertEquals(
        "Firestore transactions require all reads",
        (followState as FollowActionState.Error).message)

    // Verify total number of getProfile calls:
    // 2 calls from initial loadProfile (1 in loadProfile, 1 in createProfileIfNotExists)
    // 2 calls from error handling loadProfile (1 in loadProfile, 1 in createProfileIfNotExists)
    verify(mockRepository, times(4)).getProfile(targetUserId)
  }

  @Test
  fun `updateProfileImage failure updates state with error`() = runTest {
    val userId = "testUser"
    val imageUri = mock(Uri::class.java)

    `when`(mockRepository.uploadProfileImage(userId, imageUri))
        .thenThrow(RuntimeException("Upload failed"))
    `when`(mockRepository.getProfile(userId)).thenReturn(defaultProfile)

    profileViewModel.updateProfileImage(userId, imageUri)
    advanceUntilIdle()

    val state = profileViewModel.imageUploadState.first()
    assertTrue(state is ImageUploadState.Error)
    assertEquals("Upload failed", (state as ImageUploadState.Error).message)
  }

  @Test
  fun `loadProfile returns null profile updates state with error`() = runTest {
    `when`(mockRepository.getProfile("testUser")).thenReturn(null)

    profileViewModel.loadProfile("testUser")
    advanceUntilIdle()

    val state = profileViewModel.profileState.first()
    assertTrue(state is ProfileUiState.Error)
    assertEquals("Profile not found", (state as ProfileUiState.Error).message)
  }

  @Test
  fun `updateUsername when username is blank returns error state`() = runTest {
    profileViewModel.updateUsername("testUser", "")
    advanceUntilIdle()

    val state = profileViewModel.usernameState.first()
    assertTrue(state is UsernameUpdateState.Error)
    assertEquals("Username cannot be empty", (state as UsernameUpdateState.Error).message)
    verify(mockRepository, never()).updateUsername("testUser", "")
  }

  @Test
  fun `updateUsername when username is too short returns error state`() = runTest {
    profileViewModel.updateUsername("testUser", "ab")
    advanceUntilIdle()

    val state = profileViewModel.usernameState.first()
    assertTrue(state is UsernameUpdateState.Error)
    assertEquals(
        "Username must be at least 3 characters", (state as UsernameUpdateState.Error).message)
    verify(mockRepository, never()).updateUsername("testUser", "ab")
  }

  @Test
  fun `updateUsername with invalid characters returns error state`() = runTest {
    val userId = "testUser"
    val invalidUsername = "user@name!"

    profileViewModel.updateUsername(userId, invalidUsername)
    advanceUntilIdle()

    val state = profileViewModel.usernameState.first()
    assertTrue(state is UsernameUpdateState.Error)
    assertEquals(
        "Username can only contain letters, numbers, dots and underscores",
        (state as UsernameUpdateState.Error).message)
    verify(mockRepository, never()).updateUsername(userId, invalidUsername)
  }

  @Test
  fun `updateUsername when username already exists returns error state`() = runTest {
    val userId = "testUser"
    val existingUsername = "existingUser"

    `when`(mockRepository.doesUsernameExist(existingUsername)).thenReturn(true)

    profileViewModel.updateUsername(userId, existingUsername)
    advanceUntilIdle()

    val state = profileViewModel.usernameState.first()
    assertTrue(state is UsernameUpdateState.Error)
    assertEquals("Username already taken", (state as UsernameUpdateState.Error).message)
    verify(mockRepository, never()).updateUsername(userId, existingUsername)
  }

  @Test
  fun `updateUsername with valid username updates successfully`() = runTest {
    val userId = "testUser"
    val newUsername = "validUser123"

    `when`(mockRepository.doesUsernameExist(newUsername)).thenReturn(false)
    `when`(mockRepository.getProfile(userId)).thenReturn(defaultProfile)

    profileViewModel.updateUsername(userId, newUsername)
    advanceUntilIdle()

    val state = profileViewModel.usernameState.first()
    assertTrue(state is UsernameUpdateState.Success)
    verify(mockRepository).updateUsername(userId, newUsername)
    verify(mockRepository, times(2)).getProfile(userId)
  }

  @Test
  fun `updateUsername when repository throws exception returns error state`() = runTest {
    val userId = "testUser"
    val newUsername = "validUser123"
    val errorMessage = "Database error"

    `when`(mockRepository.doesUsernameExist(newUsername)).thenReturn(false)
    `when`(mockRepository.updateUsername(userId, newUsername))
        .thenThrow(RuntimeException(errorMessage))

    profileViewModel.updateUsername(userId, newUsername)
    advanceUntilIdle()

    val state = profileViewModel.usernameState.first()
    assertTrue(state is UsernameUpdateState.Error)
    assertEquals(errorMessage, (state as UsernameUpdateState.Error).message)
  }

  @Test
  fun likeAndAddToFavorites_success() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"

    profileViewModel.likeAndAddToFavorites(userId, publicationId)
    advanceUntilIdle()

    verify(mockRepository).incrementLikes(publicationId, userId)
    verify(mockRepository).addToUserCollection(userId, "likedPublications", publicationId)
    assertNull(profileViewModel.error.value)
  }

  @Test
  fun likeAndAddToFavorites_failure() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"

    `when`(mockRepository.incrementLikes(publicationId, userId))
        .thenThrow(RuntimeException("Failed to like"))

    profileViewModel.likeAndAddToFavorites(userId, publicationId)
    advanceUntilIdle()

    verify(mockRepository).incrementLikes(publicationId, userId)
    verify(mockRepository, never()).addToUserCollection(userId, "likedPublications", publicationId)
    assertNotNull(profileViewModel.error.value)
    assertEquals("Failed to like and save publication", profileViewModel.error.value)
  }

  @Test
  fun removeLike_success() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"

    profileViewModel.removeLike(userId, publicationId)
    advanceUntilIdle()

    verify(mockRepository).removeFromLikedPublications(userId, publicationId)
    verify(mockRepository).decrementLikesAndRemoveUser(publicationId, userId)
    assertNull(profileViewModel.error.value)
  }

  @Test
  fun removeLike_failure() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"

    // Simulate an exception when removing the like
    `when`(mockRepository.removeFromLikedPublications(userId, publicationId))
        .thenThrow(RuntimeException("Failed to remove like"))

    profileViewModel.removeLike(userId, publicationId)
    advanceUntilIdle()

    verify(mockRepository).removeFromLikedPublications(userId, publicationId)
    assertNotNull(profileViewModel.error.value)
    assertEquals("Failed to remove like: Failed to remove like", profileViewModel.error.value)
  }

  @Test
  fun `loadLikedPublications success updates state with liked publications`() = runTest {
    val userId = "testUser"
    val likedPublicationIds = listOf("pub1", "pub2")
    val allPublications =
        listOf(
            Publication(id = "pub1", userId = userId, title = "Publication 1"),
            Publication(id = "pub2", userId = userId, title = "Publication 2"),
            Publication(id = "pub3", userId = "otherUser", title = "Publication 3"))
    val expectedLikedPublications = allPublications.filter { it.id in likedPublicationIds }

    // Mock repository responses
    `when`(mockRepository.getUserLikedPublicationsIds(userId)).thenReturn(likedPublicationIds)
    `when`(mockRepository.getAllPublications()).thenReturn(allPublications)

    // Call the method under test
    profileViewModel.loadLikedPublications(userId)
    advanceUntilIdle()

    // Verify the state is updated correctly
    val likedPublications = profileViewModel.likedPublications.first()
    assertEquals(expectedLikedPublications, likedPublications)
    assertNull(profileViewModel.error.value)

    verify(mockRepository).getUserLikedPublicationsIds(userId)
    verify(mockRepository).getAllPublications()
  }

  @Test
  fun `loadLikedPublications failure updates state with error`() = runTest {
    val userId = "testUser"

    // Simulate an exception when fetching liked publication IDs
    `when`(mockRepository.getUserLikedPublicationsIds(userId))
        .thenThrow(RuntimeException("Failed to fetch liked publications"))

    // Call the method under test
    profileViewModel.loadLikedPublications(userId)
    advanceUntilIdle()

    // Verify that an error message is set
    val error = profileViewModel.error.first()
    assertEquals("Failed to load liked publications: Failed to fetch liked publications", error)

    verify(mockRepository).getUserLikedPublicationsIds(userId)
    verify(mockRepository, never()).getAllPublications()
  }

  @Test
  fun `getFollowers success returns list of followers`() = runTest {
    val userId = "testUser"
    val followers =
        listOf(
            Profile(id = "follower1", username = "Follower1", isFollowedByCurrentUser = true),
            Profile(id = "follower2", username = "Follower2", isFollowedByCurrentUser = false))

    // Mock repository response
    `when`(mockRepository.getFollowers(userId)).thenReturn(followers)

    // Call the method
    val result = profileViewModel.getFollowers(userId)

    // Verify the result
    assertEquals(followers, result)
    assertNull(profileViewModel.error.value)
    verify(mockRepository).getFollowers(userId)
  }

  @Test
  fun `getFollowers failure returns empty list and sets error`() = runTest {
    val userId = "testUser"
    val errorMessage = "Failed to fetch followers"

    // Mock repository to throw exception
    `when`(mockRepository.getFollowers(userId)).thenThrow(RuntimeException(errorMessage))

    // Call the method
    val result = profileViewModel.getFollowers(userId)

    // Verify the result
    assertTrue(result.isEmpty())
    assertEquals("Failed to load followers: $errorMessage", profileViewModel.error.value)
    verify(mockRepository).getFollowers(userId)
  }

  @Test
  fun `getFollowing success returns list of following`() = runTest {
    val userId = "testUser"
    val following =
        listOf(
            Profile(id = "following1", username = "Following1", isFollowedByCurrentUser = true),
            Profile(id = "following2", username = "Following2", isFollowedByCurrentUser = true))

    // Mock repository response
    `when`(mockRepository.getFollowing(userId)).thenReturn(following)

    // Call the method
    val result = profileViewModel.getFollowing(userId)

    // Verify the result
    assertEquals(following, result)
    assertNull(profileViewModel.error.value)
    verify(mockRepository).getFollowing(userId)
  }

  @Test
  fun `getFollowing failure returns empty list and sets error`() = runTest {
    val userId = "testUser"
    val errorMessage = "Failed to fetch following"

    // Mock repository to throw exception
    `when`(mockRepository.getFollowing(userId)).thenThrow(RuntimeException(errorMessage))

    // Call the method
    val result = profileViewModel.getFollowing(userId)

    // Verify the result
    assertTrue(result.isEmpty())
    assertEquals("Failed to load following: $errorMessage", profileViewModel.error.value)
    verify(mockRepository).getFollowing(userId)
  }

  @Test
  fun `getFollowers returns followers with correct follow status`() = runTest {
    val userId = "testUser"
    val followers =
        listOf(
            Profile(
                id = "follower1",
                username = "Follower1",
                isFollowedByCurrentUser = true,
                followers = 10,
                following = 20),
            Profile(
                id = "follower2",
                username = "Follower2",
                isFollowedByCurrentUser = false,
                followers = 5,
                following = 15))

    `when`(mockRepository.getFollowers(userId)).thenReturn(followers)

    val result = profileViewModel.getFollowers(userId)

    assertEquals(2, result.size)
    assertTrue(result[0].isFollowedByCurrentUser)
    assertFalse(result[1].isFollowedByCurrentUser)
    assertEquals(10, result[0].followers)
    assertEquals(20, result[0].following)
    verify(mockRepository).getFollowers(userId)
  }

  @Test
  fun `getFollowing returns following with all followed status true`() = runTest {
    val userId = "testUser"
    val following =
        listOf(
            Profile(
                id = "following1",
                username = "Following1",
                isFollowedByCurrentUser = true,
                followers = 10,
                following = 20),
            Profile(
                id = "following2",
                username = "Following2",
                isFollowedByCurrentUser = true,
                followers = 5,
                following = 15))

    `when`(mockRepository.getFollowing(userId)).thenReturn(following)

    val result = profileViewModel.getFollowing(userId)

    assertEquals(2, result.size)
    assertTrue(result.all { it.isFollowedByCurrentUser })
    assertEquals(10, result[0].followers)
    assertEquals(20, result[0].following)
    verify(mockRepository).getFollowing(userId)
  }

  @Test
  fun `deletePublication success updates state and reloads profile`() = runTest {
    val userId = "user123"
    val publicationId = "pub123"

    `when`(mockRepository.deletePublication(publicationId, userId)).thenReturn(true)
    `when`(mockRepository.getProfile(userId)).thenReturn(defaultProfile)

    profileViewModel.deletePublication(publicationId, userId)
    advanceUntilIdle()

    val deleteState = profileViewModel.deletePublicationState.first()
    assertTrue(deleteState is DeletePublicationState.Success)
    verify(mockRepository).deletePublication(publicationId, userId)
    verify(mockRepository, times(2)).getProfile(userId)
  }

  @Test
  fun `deletePublication failure updates error state`() = runTest {
    val userId = "user123"
    val publicationId = "pub123"

    `when`(mockRepository.deletePublication(publicationId, userId)).thenReturn(false)

    profileViewModel.deletePublication(publicationId, userId)
    advanceUntilIdle()

    val deleteState = profileViewModel.deletePublicationState.first()
    assertTrue(deleteState is DeletePublicationState.Error)
    assertEquals(
        "Failed to delete publication", (deleteState as DeletePublicationState.Error).message)
  }

  @Test
  fun `resetDeleteState sets state to idle`() = runTest {
    profileViewModel.resetDeleteState()
    advanceUntilIdle()

    val state = profileViewModel.deletePublicationState.first()
    assertTrue(state is DeletePublicationState.Idle)
  }

  @Test
  fun `loadFavoritePublications success updates state with favorited publications`() = runTest {
    val userId = "testUser"
    val favoritePublicationIds = listOf("pub1", "pub2")
    val favoritePublications = listOf(
      Publication(id = "pub1", userId = userId, title = "Publication 1"),
      Publication(id = "pub2", userId = userId, title = "Publication 2")
    )

    // Mock the new method calls
    `when`(mockRepository.getFavoritePublicationsIds(userId)).thenReturn(favoritePublicationIds)
    `when`(mockRepository.getFavoritePublications(favoritePublicationIds)).thenReturn(favoritePublications)

    profileViewModel.loadFavoritePublications(userId)
    advanceUntilIdle()

    // Verify results
    val result = profileViewModel.favoritePublications.first()
    assertEquals(favoritePublications, result)
    assertNull(profileViewModel.error.value)

    // Verify the correct methods were called
    verify(mockRepository).getFavoritePublicationsIds(userId)
    verify(mockRepository).getFavoritePublications(favoritePublicationIds)
    verify(mockRepository, never()).getAllPublications()
  }

  @Test
  fun `loadFavoritePublications failure updates state with error`() = runTest {
    val userId = "testUser"
    `when`(mockRepository.getFavoritePublicationsIds(userId))
        .thenThrow(RuntimeException("Failed to fetch favorite publications"))

    profileViewModel.loadFavoritePublications(userId)
    advanceUntilIdle()

    val error = profileViewModel.error.first()
    assertEquals(
        "Failed to load favorite publications: Failed to fetch favorite publications", error)
    verify(mockRepository).getFavoritePublicationsIds(userId)
    verify(mockRepository, never()).getAllPublications()
  }

  @Test
  fun `toggleFavorite adds publication to favorites when not favorited`() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"

    `when`(mockRepository.isPublicationFavorited(userId, publicationId)).thenReturn(false)

    profileViewModel.toggleFavorite(userId, publicationId)
    advanceUntilIdle()

    verify(mockRepository).addToFavorites(userId, publicationId)
    verify(mockRepository, never()).removeFromFavorites(userId, publicationId)

    val state = profileViewModel.favoriteActionState.first()
    assertTrue(state is FavoriteActionState.Success)
    assertTrue((state as FavoriteActionState.Success).isNowFavorited)
  }

  @Test
  fun `toggleFavorite removes publication from favorites when already favorited`() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"

    `when`(mockRepository.isPublicationFavorited(userId, publicationId)).thenReturn(true)

    profileViewModel.toggleFavorite(userId, publicationId)
    advanceUntilIdle()

    verify(mockRepository).removeFromFavorites(userId, publicationId)
    verify(mockRepository, never()).addToFavorites(userId, publicationId)

    val state = profileViewModel.favoriteActionState.first()
    assertTrue(state is FavoriteActionState.Success)
    assertFalse((state as FavoriteActionState.Success).isNowFavorited)
  }

  @Test
  fun `toggleFavorite handles error gracefully`() = runTest {
    val userId = "testUser"
    val publicationId = "pub1"
    val errorMessage = "Network error"

    `when`(mockRepository.isPublicationFavorited(userId, publicationId))
        .thenThrow(RuntimeException(errorMessage))

    profileViewModel.toggleFavorite(userId, publicationId)
    advanceUntilIdle()

    val state = profileViewModel.favoriteActionState.first()
    assertTrue(state is FavoriteActionState.Error)
    assertEquals(errorMessage, (state as FavoriteActionState.Error).message)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }
}
