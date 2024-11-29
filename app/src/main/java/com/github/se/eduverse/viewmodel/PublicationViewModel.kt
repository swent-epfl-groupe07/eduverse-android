package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.ProfileRepository
import com.github.se.eduverse.repository.PublicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class PublicationViewModel(
  private val repository: PublicationRepository,
  private val profileRepository: ProfileRepository
) : ViewModel() {

  private val _publications = MutableStateFlow<List<Publication>>(emptyList())
  open val publications: StateFlow<List<Publication>>
    get() = _publications

  private val _comments = MutableStateFlow<List<Comment>>(emptyList())
  val comments: StateFlow<List<Comment>> = _comments

  private val _error = MutableStateFlow<String?>(null)
  open val error: StateFlow<String?>
    get() = _error

  private val userProfileCache = mutableMapOf<String, String?>()

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

  // Charger les commentaires pour une publication spécifique
  open fun loadComments(publicationId: String) {
    viewModelScope.launch {
      try {
        val commentsList = repository.getComments(publicationId)

        // Récupérer les ownerId uniques des commentaires
        val userIds = commentsList.map { it.ownerId }.distinct()

        // Récupérer les profils des utilisateurs
        val profiles = userIds.associateWith { userId ->
          profileRepository.getProfile(userId)
        }

        // Inclure le profil dans chaque commentaire
        val commentsWithProfile = commentsList.map { comment ->
          val profile = profiles[comment.ownerId]
          comment.copy(profile = profile)
        }

        _comments.value = commentsWithProfile
      } catch (e: Exception) {
        _error.value = "Failed to load comments: ${e.message}"
      }
    }
  }


  open fun addComment(publicationId: String, ownerId: String, text: String) {
    viewModelScope.launch {
      try {
        val newComment = Comment(
          id = java.util.UUID.randomUUID().toString(),
          publicationId = publicationId,
          ownerId = ownerId,
          text = text
        )
        repository.addComment(publicationId, newComment)
        loadComments(publicationId) // Recharger les commentaires après en avoir ajouté un nouveau
      } catch (e: Exception) {
        _error.value = "Failed to add comment: ${e.message}"
      }
    }
  }


  // Méthode pour aimer un commentaire (si nécessaire)
  open fun likeComment(publicationId: String, commentId: String) {
    viewModelScope.launch {
      try {
        repository.likeComment(publicationId, commentId)
        // Optionnel : Recharger les commentaires pour refléter les likes mis à jour
        loadComments(publicationId)
      } catch (e: Exception) {
        _error.value = "Failed to like comment: ${e.message}"
      }
    }
  }

  open fun toggleLikeComment(publicationId: String, commentId: String, userId: String) {
    viewModelScope.launch {
      try {
        repository.toggleLikeComment(publicationId, commentId, userId)
        // Mettre à jour localement le commentaire
        _comments.value = _comments.value.map { comment ->
          if (comment.id == commentId) {
            val hasLiked = comment.likedBy.contains(userId)
            val updatedLikes = if (hasLiked) comment.likes - 1 else comment.likes + 1
            val updatedLikedBy = if (hasLiked) {
              comment.likedBy - userId
            } else {
              comment.likedBy + userId
            }
            comment.copy(
              likes = updatedLikes,
              likedBy = updatedLikedBy
            )
          } else {
            comment
          }
        }
      } catch (e: Exception) {
        _error.value = "Failed to toggle like on comment: ${e.message}"
      }
    }
  }




  // Supprimer un commentaire
  open fun deleteComment(publicationId: String, commentId: String, currentUserId: String) {
    viewModelScope.launch {
      try {
        repository.deleteComment(publicationId, commentId, currentUserId)
        // Mettre à jour localement la liste des commentaires
        _comments.value = _comments.value.filterNot { it.id == commentId }
      } catch (e: Exception) {
        _error.value = "Failed to delete comment: ${e.message}"
      }
    }
  }

}
