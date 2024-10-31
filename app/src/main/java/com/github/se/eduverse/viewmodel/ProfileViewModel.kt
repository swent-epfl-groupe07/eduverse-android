package com.github.se.eduverse.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
  private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
  open val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()
  private val _imageUploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
  val imageUploadState: StateFlow<ImageUploadState> = _imageUploadState.asStateFlow()

  fun loadProfile(userId: String) {
    viewModelScope.launch {
      _profileState.value = ProfileUiState.Loading
      try {
        val profile = repository.getProfile(userId)
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
        repository.addPublication(userId, publication)
        loadProfile(userId)
      } catch (e: Exception) {
        _profileState.value = ProfileUiState.Error(e.message ?: "Failed to add publication")
      }
    }
  }

  fun toggleFavorite(userId: String, publicationId: String, isFavorite: Boolean) {
    viewModelScope.launch {
      try {
        if (isFavorite) {
          repository.removeFromFavorites(userId, publicationId)
        } else {
          repository.addToFavorites(userId, publicationId)
        }
        loadProfile(userId)
      } catch (e: Exception) {
        _profileState.value = ProfileUiState.Error(e.message ?: "Failed to update favorites")
      }
    }
  }

  fun toggleFollow(currentUserId: String, targetUserId: String, isFollowing: Boolean) {
    viewModelScope.launch {
      try {
        if (isFollowing) {
          repository.unfollowUser(currentUserId, targetUserId)
        } else {
          repository.followUser(currentUserId, targetUserId)
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
        val imageUrl = repository.uploadProfileImage(userId, imageUri)
        repository.updateProfileImage(userId, imageUrl)
        loadProfile(userId)
        _imageUploadState.value = ImageUploadState.Success
      } catch (e: Exception) {
        _imageUploadState.value = ImageUploadState.Error(e.message ?: "Upload failed")
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
