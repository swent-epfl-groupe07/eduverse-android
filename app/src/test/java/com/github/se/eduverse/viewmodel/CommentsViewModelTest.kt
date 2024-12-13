// CommentsViewModelTest.kt
package com.github.se.eduverse.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.repository.CommentsRepository
import com.github.se.eduverse.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class MainDispatcherRule(val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    TestWatcher() {

  override fun starting(description: Description) {
    super.starting(description)
    Dispatchers.setMain(testDispatcher)
  }

  override fun finished(description: Description) {
    super.finished(description)
    Dispatchers.resetMain()
  }
}

@ExperimentalCoroutinesApi
class CommentsViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  @Mock private lateinit var commentsRepository: CommentsRepository

  @Mock private lateinit var profileRepository: ProfileRepository

  private lateinit var commentsViewModel: CommentsViewModel

  private lateinit var closeable: AutoCloseable

  @Before
  fun setUp() {
    closeable = MockitoAnnotations.openMocks(this)
    commentsViewModel = CommentsViewModel(commentsRepository, profileRepository)
  }

  @After
  fun tearDown() {
    closeable.close()
  }

  @Test
  fun `loadComments - success`() = runTest {
    // Arrange
    val publicationId = "testPublicationId"
    val comment =
        Comment(
            id = "commentId",
            publicationId = publicationId,
            ownerId = "ownerId",
            text = "Test comment",
            likes = 10,
            likedBy = listOf("user1", "user2"),
            profile = null)
    val profile =
        Profile(
            id = "ownerId",
            username = "TestUser",
            followers = 100,
            following = 50,
            publications = emptyList(),
            profileImageUrl = "",
            isFollowedByCurrentUser = false)

    whenever(commentsRepository.getComments(publicationId)).thenReturn(listOf(comment))
    whenever(profileRepository.getProfile("ownerId")).thenReturn(profile)

    // Act
    commentsViewModel.loadComments(publicationId)

    // Advance until all coroutines are completed
    advanceUntilIdle()

    // Assert
    val state = commentsViewModel.commentsState.value
    assertTrue(state is CommentsUiState.Success)
    val comments = (state as CommentsUiState.Success).comments
    assertEquals(1, comments.size)
    assertEquals(comment.copy(profile = profile), comments[0])
  }

  @Test
  fun `addComment - success when current state is Success`() = runTest {
    // Arrange
    val publicationId = "testPublicationId"
    val ownerId = "ownerId"
    val text = "New Comment"
    val existingComment =
        Comment(
            id = "existingCommentId",
            publicationId = publicationId,
            ownerId = "ownerId2",
            text = "Existing comment",
            likes = 5,
            likedBy = listOf("user1"),
            profile = null)
    val profile =
        Profile(
            id = ownerId,
            username = "TestUser",
            followers = 100,
            following = 50,
            publications = emptyList(),
            profileImageUrl = "",
            isFollowedByCurrentUser = false)

    // Mock initial comments and profiles
    whenever(commentsRepository.getComments(publicationId)).thenReturn(listOf(existingComment))
    whenever(profileRepository.getProfile("ownerId2")).thenReturn(profile)
    whenever(profileRepository.getProfile(ownerId)).thenReturn(profile)

    // Load initial comments to set the state to Success
    commentsViewModel.loadComments(publicationId)
    advanceUntilIdle()

    // Act
    commentsViewModel.addComment(publicationId, ownerId, text)
    advanceUntilIdle()

    // Assert
    val state = commentsViewModel.commentsState.value
    assertTrue(state is CommentsUiState.Success)
    val comments = (state as CommentsUiState.Success).comments
    assertEquals(2, comments.size)
    val newComment = comments.find { it.text == text }
    assertNotNull(newComment)
    assertEquals(ownerId, newComment?.ownerId)
    assertEquals(publicationId, newComment?.publicationId)
    assertEquals(profile, newComment?.profile)

    // Verify interactions
    verify(commentsRepository).addComment(eq(publicationId), any())
  }

  @Test
  fun `addComment - success when current state is not Success`() = runTest {
    // Arrange
    val publicationId = "testPublicationId"
    val ownerId = "ownerId"
    val text = "New Comment"
    val profile =
        Profile(
            id = ownerId,
            username = "TestUser",
            followers = 100,
            following = 50,
            publications = emptyList(),
            profileImageUrl = "",
            isFollowedByCurrentUser = false)
    val newComment =
        Comment(
            id = "newCommentId",
            publicationId = publicationId,
            ownerId = ownerId,
            text = text,
            likes = 0,
            likedBy = emptyList(),
            profile = profile)

    whenever(profileRepository.getProfile(ownerId)).thenReturn(profile)
    whenever(commentsRepository.getComments(publicationId)).thenReturn(listOf(newComment))

    // Act
    commentsViewModel.addComment(publicationId, ownerId, text)
    advanceUntilIdle()

    // Assert
    val state = commentsViewModel.commentsState.value
    assertTrue(state is CommentsUiState.Success)
    val comments = (state as CommentsUiState.Success).comments
    assertEquals(1, comments.size)
    assertEquals(newComment, comments[0])

    verify(commentsRepository).addComment(eq(publicationId), any())
    verify(commentsRepository).getComments(publicationId)
  }

  @Test
  fun `likeComment - success when current state is Success`() = runTest {
    // Arrange
    val publicationId = "testPublicationId"
    val commentId = "commentId"
    val userId = "userId"
    val existingComment =
        Comment(
            id = commentId,
            publicationId = publicationId,
            ownerId = "ownerId",
            text = "Test comment",
            likes = 1,
            likedBy = listOf("anotherUser"),
            profile = null)

    whenever(commentsRepository.likeComment(publicationId, commentId, userId)).thenReturn(Unit)
    whenever(commentsRepository.getComments(publicationId)).thenReturn(listOf(existingComment))
    whenever(profileRepository.getProfile("ownerId")).thenReturn(null)

    // Load initial comments
    commentsViewModel.loadComments(publicationId)
    advanceUntilIdle()

    // Act
    commentsViewModel.likeComment(publicationId, commentId, userId)
    advanceUntilIdle()

    // Assert
    val state = commentsViewModel.commentsState.value
    assertTrue(state is CommentsUiState.Success)
    val comments = (state as CommentsUiState.Success).comments
    val likedComment = comments.find { it.id == commentId }
    assertNotNull(likedComment)
    assertEquals(2, likedComment?.likes)
    assertTrue(likedComment?.likedBy?.contains(userId) ?: false)

    verify(commentsRepository).likeComment(publicationId, commentId, userId)
  }

  @Test
  fun `deleteComment - success when current state is Success`() = runTest {
    // Arrange
    val publicationId = "testPublicationId"
    val commentId = "commentId"
    val existingComment =
        Comment(
            id = commentId,
            publicationId = publicationId,
            ownerId = "ownerId",
            text = "Test comment",
            likes = 1,
            likedBy = listOf("user1"),
            profile = null)
    val otherComment =
        Comment(
            id = "otherCommentId",
            publicationId = publicationId,
            ownerId = "ownerId2",
            text = "Another comment",
            likes = 0,
            likedBy = emptyList(),
            profile = null)

    whenever(commentsRepository.deleteComment(publicationId, commentId)).thenReturn(Unit)
    whenever(commentsRepository.getComments(publicationId))
        .thenReturn(listOf(existingComment, otherComment))
    whenever(profileRepository.getProfile("ownerId")).thenReturn(null)
    whenever(profileRepository.getProfile("ownerId2")).thenReturn(null)

    // Load initial comments
    commentsViewModel.loadComments(publicationId)
    advanceUntilIdle()

    // Act
    commentsViewModel.deleteComment(publicationId, commentId)
    advanceUntilIdle()

    // Assert
    val state = commentsViewModel.commentsState.value
    assertTrue(state is CommentsUiState.Success)
    val comments = (state as CommentsUiState.Success).comments
    assertEquals(1, comments.size)
    assertEquals("otherCommentId", comments[0].id)

    verify(commentsRepository).deleteComment(publicationId, commentId)
  }

  @Test
  fun `addComment - failure`() = runTest {
    // Arrange
    val publicationId = "testPublicationId"
    val ownerId = "ownerId"
    val text = "New Comment"
    val exception = RuntimeException("Network error")

    whenever(profileRepository.getProfile(ownerId)).thenReturn(null)
    whenever(commentsRepository.addComment(any(), any())).thenThrow(exception)

    // Act
    commentsViewModel.addComment(publicationId, ownerId, text)
    advanceUntilIdle()

    // Assert
    val state = commentsViewModel.commentsState.value
    assertTrue(state is CommentsUiState.Error)
    assertEquals(
        "Failed to add comment: ${exception.message}", (state as CommentsUiState.Error).message)
  }

  @Test
  fun `likeComment - failure`() = runTest {
    // Arrange
    val publicationId = "testPublicationId"
    val commentId = "commentId"
    val userId = "userId"
    val exception = RuntimeException("Network error")

    whenever(commentsRepository.likeComment(publicationId, commentId, userId)).thenThrow(exception)

    // Act
    commentsViewModel.likeComment(publicationId, commentId, userId)
    advanceUntilIdle()

    // Assert
    val state = commentsViewModel.commentsState.value
    assertTrue(state is CommentsUiState.Error)
    assertEquals(
        "Failed to like/unlike comment: ${exception.message}",
        (state as CommentsUiState.Error).message)
  }

  @Test
  fun `deleteComment - failure`() = runTest {
    // Arrange
    val publicationId = "testPublicationId"
    val commentId = "commentId"
    val exception = RuntimeException("Network error")

    whenever(commentsRepository.deleteComment(publicationId, commentId)).thenThrow(exception)

    // Act
    commentsViewModel.deleteComment(publicationId, commentId)
    advanceUntilIdle()

    // Assert
    val state = commentsViewModel.commentsState.value
    assertTrue(state is CommentsUiState.Error)
    assertEquals(
        "Failed to delete comment: ${exception.message}", (state as CommentsUiState.Error).message)
  }
}
