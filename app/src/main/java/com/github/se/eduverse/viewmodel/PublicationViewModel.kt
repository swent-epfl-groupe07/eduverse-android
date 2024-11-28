package com.github.se.eduverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

open class PublicationViewModel(private val repository: PublicationRepository) : ViewModel() {

  private val _publications = MutableStateFlow<List<Publication>>(emptyList())
  open val publications: StateFlow<List<Publication>>
    get() = _publications

  private val _comments = MutableStateFlow<List<Comment>>(emptyList())
  val comments: StateFlow<List<Comment>> = _comments

  private val _error = MutableStateFlow<String?>(null)
  open val error: StateFlow<String?>
    get() = _error

  init {
    loadPublications()
  }

  private fun loadPublications() {
    viewModelScope.launch {
      try {
        val newPublications = repository.loadRandomPublications()
        _publications.value = newPublications
        _error.value = null
      } catch (e: Exception) {
        _error.value = "Échec du chargement des publications"
      }
    }
  }

  open suspend fun loadMorePublications() {
    viewModelScope.launch {
      try {
        if (_publications.value.isEmpty()) {
          val newPublications = repository.loadRandomPublications()
          _publications.value = newPublications
        } else {
          val morePublications = repository.loadRandomPublications()
          _publications.value = (_publications.value + morePublications).shuffled()
        }
        _error.value = null
      } catch (e: Exception) {
        _error.value = "Échec du chargement des publications"
      }
    }
  }

  // Load comments for a specific publication
  open fun loadComments(publicationId: String) {
    viewModelScope.launch {
      try {
        val commentsList = repository.getComments(publicationId)
        _comments.value = commentsList
      } catch (e: Exception) {
        _error.value = "Failed to load comments: ${e.message}"
      }
    }
  }

  // Add a new comment to a publication
  open fun addComment(publicationId: String, ownerId: String, text: String) {
    viewModelScope.launch {
      try {
        val newComment = Comment(
          id = UUID.randomUUID().toString(),
          publicationId = publicationId,
          ownerId = ownerId,
          text = text
        )
        repository.addComment(publicationId, newComment)
        loadComments(publicationId) // Reload comments after adding a new one
      } catch (e: Exception) {
        _error.value = "Failed to add comment: ${e.message}"
      }
    }
  }

  // Like a comment
  open fun likeComment(publicationId: String, commentId: String) {
    viewModelScope.launch {
      try {
        repository.likeComment(publicationId, commentId)
        // Optional: Reload comments to reflect updated likes
        loadComments(publicationId)
      } catch (e: Exception) {
        _error.value = "Failed to like comment: ${e.message}"
      }
    }
  }

  // Delete a comment
  open fun deleteComment(publicationId: String, commentId: String) {
    viewModelScope.launch {
      try {
        repository.deleteComment(publicationId, commentId)
        loadComments(publicationId) // Reload comments after deletion
      } catch (e: Exception) {
        _error.value = "Failed to delete comment: ${e.message}"
      }
    }
  }
}
