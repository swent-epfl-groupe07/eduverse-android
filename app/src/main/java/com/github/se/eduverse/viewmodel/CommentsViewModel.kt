// CommentsViewModel.kt
package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.repository.CommentsRepository
import com.github.se.eduverse.repository.ProfileRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CommentsUiState {
  object Loading : CommentsUiState()

  data class Success(val comments: List<Comment>) : CommentsUiState()

  data class Error(val message: String) : CommentsUiState()
}

open class CommentsViewModel(
    private val commentsRepository: CommentsRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

  private val _commentsState = MutableStateFlow<CommentsUiState>(CommentsUiState.Loading)
  open val commentsState: StateFlow<CommentsUiState>
    get() = _commentsState

  open fun loadComments(publicationId: String) {
    viewModelScope.launch {
      Log.d("CommentsViewModel", "Loading comments for publication: $publicationId")
      _commentsState.value = CommentsUiState.Loading
      try {
        val commentsList = commentsRepository.getComments(publicationId)
        Log.d("CommentsViewModel", "Fetched ${commentsList.size} comments")

        // Retrieve the profiles of the comment authors
        val userIds = commentsList.map { it.ownerId }.distinct()
        val profilesDeferred =
            userIds.associateWith { userId -> async { profileRepository.getProfile(userId) } }
        val profiles = profilesDeferred.mapValues { it.value.await() }

        val commentsWithProfile =
            commentsList.map { comment ->
              val profile = profiles[comment.ownerId]
              comment.copy(profile = profile)
            }

        _commentsState.value = CommentsUiState.Success(commentsWithProfile)
        Log.d("CommentsViewModel", "Comments loaded successfully")
      } catch (e: Exception) {
        Log.e("CommentsViewModel", "Failed to load comments: ${e.message}")
        _commentsState.value = CommentsUiState.Error("Failed to load comments: ${e.message}")
      }
    }
  }

  open fun addComment(publicationId: String, ownerId: String, text: String) {
    viewModelScope.launch {
      try {
        Log.d("CommentsViewModel", "Adding comment to publication: $publicationId")
        val profile = profileRepository.getProfile(ownerId)
        val newComment =
            Comment(
                id = java.util.UUID.randomUUID().toString(),
                publicationId = publicationId,
                ownerId = ownerId,
                text = text,
                likes = 0,
                likedBy = emptyList(),
                profile = profile)
        commentsRepository.addComment(publicationId, newComment)
        // Update the state locally
        val currentState = _commentsState.value
        if (currentState is CommentsUiState.Success) {
          val updatedComments = currentState.comments + newComment
          _commentsState.value = CommentsUiState.Success(updatedComments)
        } else {
          // If the state is not Success, load the comments
          loadComments(publicationId)
        }
        Log.d("CommentsViewModel", "Comment added successfully and state updated")
      } catch (e: Exception) {
        Log.e("CommentsViewModel", "Failed to add comment: ${e.message}")
        _commentsState.value = CommentsUiState.Error("Failed to add comment: ${e.message}")
      }
    }
  }

  open fun likeComment(publicationId: String, commentId: String, userId: String) {
    viewModelScope.launch {
      try {
        commentsRepository.likeComment(publicationId, commentId, userId)
        // Update the state locally
        val currentState = _commentsState.value
        if (currentState is CommentsUiState.Success) {
          val updatedComments =
              currentState.comments.map { comment ->
                if (comment.id == commentId) {
                  val updatedLikedBy = comment.likedBy.toMutableList()
                  val updatedLikes =
                      if (updatedLikedBy.contains(userId)) {
                        updatedLikedBy.remove(userId)
                        comment.likes - 1
                      } else {
                        updatedLikedBy.add(userId)
                        comment.likes + 1
                      }
                  comment.copy(likes = updatedLikes, likedBy = updatedLikedBy)
                } else {
                  comment
                }
              }
          _commentsState.value = CommentsUiState.Success(updatedComments)
        } else {
          loadComments(publicationId)
        }
      } catch (e: Exception) {
        Log.e("CommentsViewModel", "Failed to like/unlike comment: ${e.message}")
        _commentsState.value = CommentsUiState.Error("Failed to like/unlike comment: ${e.message}")
      }
    }
  }

  open fun deleteComment(publicationId: String, commentId: String) {
    viewModelScope.launch {
      try {
        commentsRepository.deleteComment(publicationId, commentId)
        // Update the state locally
        val currentState = _commentsState.value
        if (currentState is CommentsUiState.Success) {
          val updatedComments = currentState.comments.filter { it.id != commentId }
          _commentsState.value = CommentsUiState.Success(updatedComments)
        } else {
          loadComments(publicationId)
        }
        Log.d("CommentsViewModel", "Comment deleted successfully and state updated")
      } catch (e: Exception) {
        Log.e("CommentsViewModel", "Failed to delete comment: ${e.message}")
        _commentsState.value = CommentsUiState.Error("Failed to delete comment: ${e.message}")
      }
    }
  }
}
