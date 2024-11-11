package com.github.se.eduverse.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.ProfileRepository
import com.github.se.eduverse.repository.PublicationRepository
import com.google.android.play.core.assetpacks.db
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

open class ProfileViewModel(
  private val profileRepository: ProfileRepository,
) : ViewModel() {
  private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
  open val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

  private val _imageUploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
  val imageUploadState: StateFlow<ImageUploadState> = _imageUploadState.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun loadProfile(userId: String) {
    viewModelScope.launch {
      _profileState.value = ProfileUiState.Loading
      try {
        val profile = profileRepository.getProfile(userId)
        _profileState.value =
          profile?.let { ProfileUiState.Success(it) } ?: ProfileUiState.Error("Profile not found")
      } catch (e: Exception) {
        _profileState.value = ProfileUiState.Error(e.message ?: "Unknown error")
      }
    }
  }


  fun addPublication(userId: String, publication: Publication) {
    viewModelScope.launch {
      try {
        profileRepository.addPublication(userId, publication)
        loadProfile(userId)
      } catch (e: Exception) {
        _profileState.value = ProfileUiState.Error(e.message ?: "Failed to add publication")
      }
    }
  }

  fun toggleFollow(currentUserId: String, targetUserId: String, isFollowing: Boolean) {
    viewModelScope.launch {
      try {
        if (isFollowing) {
          profileRepository.unfollowUser(currentUserId, targetUserId)
        } else {
          profileRepository.followUser(currentUserId, targetUserId)
        }
        loadProfile(currentUserId)
      } catch (e: Exception) {
        _profileState.value = ProfileUiState.Error(e.message ?: "Failed to update follow status")
      }
    }
  }

  fun updateProfileImage(userId: String, imageUri: Uri) {
    viewModelScope.launch {
      _imageUploadState.value = ImageUploadState.Loading
      try {
        val imageUrl = profileRepository.uploadProfileImage(userId, imageUri)
        profileRepository.updateProfileImage(userId, imageUrl)
        loadProfile(userId)
        _imageUploadState.value = ImageUploadState.Success
      } catch (e: Exception) {
        _imageUploadState.value = ImageUploadState.Error(e.message ?: "Upload failed")
      }
    }
  }

  fun likeAndAddToFavorites(userId: String, publicationId: String) {
    viewModelScope.launch {
      try {
        profileRepository.incrementLikes(publicationId, userId)
        addPublicationToUserCollection(userId, publicationId)
        Log.d("REUSSIII", "POST LIKEEE $publicationId ")
      } catch (e: Exception) {
        _error.value = "Failed to like and save publication"
        Log.d("DEBUG", "UserId: $userId, PublicationId: $publicationId")
        Log.d("MAKAYENCHH", "POST NON LIKEEE")
      }
    }
  }



  private suspend fun addPublicationToUserCollection(userId: String, publicationId: String) {
    profileRepository.addToUserCollection(userId, "likedPublications", publicationId)
  }

  fun removeLike(userId: String, publicationId: String) {
    viewModelScope.launch {
      try {
        profileRepository.removeFromLikedPublications(userId, publicationId)
        profileRepository.decrementLikesAndRemoveUser(publicationId, userId)
      } catch (e: Exception) {
        _error.value = "Failed to remove like: ${e.message}"
      }
    }
  }

}


sealed class ProfileUiState {
  object Loading : ProfileUiState()
  data class Success(val profile: Profile) : ProfileUiState()
  data class Error(val message: String) : ProfileUiState()
}

sealed class ImageUploadState {
  object Idle : ImageUploadState()
  object Loading : ImageUploadState()
  object Success : ImageUploadState()
  data class Error(val message: String) : ImageUploadState()
}
