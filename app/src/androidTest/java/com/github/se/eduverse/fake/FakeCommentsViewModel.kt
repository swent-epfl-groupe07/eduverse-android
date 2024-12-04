package com.github.se.eduverse.fake

import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.viewmodel.CommentsUiState
import com.github.se.eduverse.viewmodel.CommentsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCommentsViewModel : CommentsViewModel(FakeCommentsRepository(), FakeProfileRepository()) {
  private val _commentsState = MutableStateFlow<CommentsUiState>(CommentsUiState.Loading)
  override val commentsState: StateFlow<CommentsUiState> = _commentsState.asStateFlow()

  private val commentsMap = mutableMapOf<String, MutableList<Comment>>()
  var currentUserId: String = "user2" // Pour les tests

  fun setCommentsState(state: CommentsUiState) {
    _commentsState.value = state
  }

  fun setComments(publicationId: String, comments: List<Comment>) {
    commentsMap[publicationId] = comments.toMutableList()
    _commentsState.value = CommentsUiState.Success(comments)
  }

  override fun loadComments(publicationId: String) {}

  override fun addComment(publicationId: String, ownerId: String, text: String) {
    val newComment =
        Comment(
            id = "new_comment_id",
            ownerId = ownerId,
            text = text,
            likes = 0,
            likedBy = emptyList(),
            profile = Profile(username = "CurrentUser", profileImageUrl = ""))
    val comments = commentsMap.getOrPut(publicationId) { mutableListOf() }
    comments.add(newComment)
    _commentsState.value = CommentsUiState.Success(comments)
  }

  override fun likeComment(publicationId: String, commentId: String, userId: String) {}

  override fun deleteComment(publicationId: String, commentId: String) {
    val comments = commentsMap[publicationId]
    comments?.removeIf { it.id == commentId }
    _commentsState.value = CommentsUiState.Success(comments ?: emptyList())
  }
}
