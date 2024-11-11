package com.github.se.eduverse.viewmodel

import android.net.Uri
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.ProfileRepository
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
  fun `toggleFollow follows user when not following`() = runTest {
    clearInvocations(mockRepository)
    val currentUserId = "user1"
    val targetUserId = "user2"

    `when`(mockRepository.getProfile(currentUserId)).thenReturn(defaultProfile)

    profileViewModel.toggleFollow(currentUserId, targetUserId, false)
    advanceUntilIdle()

    verify(mockRepository).followUser(currentUserId, targetUserId)
    verify(mockRepository, times(2)).getProfile(currentUserId) // Changed from 1 to 2
  }

  @Test
  fun `toggleFollow unfollows user when following`() = runTest {
    clearInvocations(mockRepository)
    val currentUserId = "user1"
    val targetUserId = "user2"

    `when`(mockRepository.getProfile(currentUserId)).thenReturn(defaultProfile)

    profileViewModel.toggleFollow(currentUserId, targetUserId, true)
    advanceUntilIdle()

    verify(mockRepository).unfollowUser(currentUserId, targetUserId)
    verify(mockRepository, times(2)).getProfile(currentUserId) // Changed from 1 to 2
  }

  @Test
  fun `toggleFollow failure updates state with error`() = runTest {
    val currentUserId = "user1"
    val targetUserId = "user2"

    `when`(mockRepository.followUser(currentUserId, targetUserId))
        .thenThrow(RuntimeException("Failed to follow"))
    `when`(mockRepository.getProfile(currentUserId)).thenReturn(defaultProfile)

    profileViewModel.toggleFollow(currentUserId, targetUserId, false)
    advanceUntilIdle()

    val state = profileViewModel.profileState.first()
    assertTrue(state is ProfileUiState.Error)
    assertEquals("Failed to follow", (state as ProfileUiState.Error).message)
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

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }
}
