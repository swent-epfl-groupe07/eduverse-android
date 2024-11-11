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

  private val defaultProfile = Profile(
    id = "testId",
    username = "testUser"
  )

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    profileViewModel = ProfileViewModel(mockRepository)

    // Setup default profile without using matchers
    runTest {
      `when`(mockRepository.getProfile("testUser")).thenReturn(defaultProfile)
    }
  }

  @Test
  fun `loadProfile success updates state with profile data`() = runTest {
    val testProfile = Profile(
      id = "test123",
      username = "testuser",
      followers = 10,
      following = 20,
      publications = listOf(),
      favoritePublications = listOf()
    )

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
  fun `toggleFavorite adds publication to favorites`() = runTest {
    clearInvocations(mockRepository)
    val userId = "testUser"

    `when`(mockRepository.getProfile(userId)).thenReturn(defaultProfile)

    profileViewModel.toggleFavorite(userId, "pub123", false)
    advanceUntilIdle()

    verify(mockRepository).addToFavorites(userId, "pub123")
    verify(mockRepository, times(2)).getProfile(userId)  // Changed from 1 to 2
  }

  @Test
  fun `toggleFavorite removes publication from favorites`() = runTest {
    clearInvocations(mockRepository)
    val userId = "testUser"

    `when`(mockRepository.getProfile(userId)).thenReturn(defaultProfile)

    profileViewModel.toggleFavorite(userId, "pub123", true)
    advanceUntilIdle()

    verify(mockRepository).removeFromFavorites(userId, "pub123")
    verify(mockRepository, times(2)).getProfile(userId)  // Changed from 1 to 2
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
    verify(mockRepository, times(2)).getProfile(userId)  // Changed from 1 to 2
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
    verify(mockRepository, times(2)).getProfile(currentUserId)  // Changed from 1 to 2
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
    verify(mockRepository, times(2)).getProfile(currentUserId)  // Changed from 1 to 2
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

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }
}