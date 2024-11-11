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

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    profileViewModel = ProfileViewModel(mockRepository)
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

    `when`(mockRepository.getProfile("testUser")).thenReturn(testProfile)

    profileViewModel.loadProfile("testUser")
    advanceUntilIdle()

    val state = profileViewModel.profileState.first()
    assertTrue(state is ProfileUiState.Success)
    assertEquals(testProfile, (state as ProfileUiState.Success).profile)
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
    val imageUri = mock(Uri::class.java)
    val imageUrl = "http://example.com/image.jpg"

    `when`(mockRepository.uploadProfileImage("testUser", imageUri)).thenReturn(imageUrl)

    profileViewModel.updateProfileImage("testUser", imageUri)
    advanceUntilIdle()

    val state = profileViewModel.imageUploadState.first()
    assertTrue(state is ImageUploadState.Success)
    verify(mockRepository).updateProfileImage("testUser", imageUrl)
  }

  @Test
  fun `addPublication success updates profile`() = runTest {
    val userId = "testUser"
    val publication = Publication(id = "pub1", userId = userId, title = "Test")

    profileViewModel.addPublication(userId, publication)
    advanceUntilIdle()

    verify(mockRepository).addPublication(userId, publication)
    verify(mockRepository).getProfile(userId)
  }

  @Test
  fun `addPublication failure updates state with error`() = runTest {
    val userId = "testUser"
    val publication = Publication(id = "pub1", userId = userId, title = "Test")

    `when`(mockRepository.addPublication(userId, publication))
        .thenThrow(RuntimeException("Failed to add"))

    profileViewModel.addPublication(userId, publication)
    advanceUntilIdle()

    val state = profileViewModel.profileState.first()
    assertTrue(state is ProfileUiState.Error)
    assertEquals("Failed to add", (state as ProfileUiState.Error).message)
  }

  @Test
  fun `toggleFollow follows user when not following`() = runTest {
    val currentUserId = "user1"
    val targetUserId = "user2"

    profileViewModel.toggleFollow(currentUserId, targetUserId, false)
    advanceUntilIdle()

    verify(mockRepository).followUser(currentUserId, targetUserId)
    verify(mockRepository).getProfile(currentUserId)
  }

  @Test
  fun `toggleFollow unfollows user when following`() = runTest {
    val currentUserId = "user1"
    val targetUserId = "user2"

    profileViewModel.toggleFollow(currentUserId, targetUserId, true)
    advanceUntilIdle()

    verify(mockRepository).unfollowUser(currentUserId, targetUserId)
    verify(mockRepository).getProfile(currentUserId)
  }

  @Test
  fun `toggleFollow failure updates state with error`() = runTest {
    val currentUserId = "user1"
    val targetUserId = "user2"

    `when`(mockRepository.followUser(currentUserId, targetUserId))
        .thenThrow(RuntimeException("Failed to follow"))

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

    `when`(mockRepository.incrementLikes(publicationId, userId)).thenThrow(RuntimeException("Failed to like"))

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
