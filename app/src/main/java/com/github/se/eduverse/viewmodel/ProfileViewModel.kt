package com.github.se.eduverse.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
  private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
  open val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()
  private val _likedPublications = MutableStateFlow<List<Publication>>(emptyList())
  open val likedPublications: StateFlow<List<Publication>> = _likedPublications.asStateFlow()
  private val _imageUploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
  open val imageUploadState: StateFlow<ImageUploadState> = _imageUploadState.asStateFlow()
  private val _searchState = MutableStateFlow<SearchProfileState>(SearchProfileState.Idle)
  open val searchState: StateFlow<SearchProfileState> = _searchState.asStateFlow()
  private val _usernameState = MutableStateFlow<UsernameUpdateState>(UsernameUpdateState.Idle)
  open val usernameState: StateFlow<UsernameUpdateState> = _usernameState.asStateFlow()

  private var searchJob: Job? = null

  private val _error = MutableStateFlow<String?>(null)
  open val error: StateFlow<String?> = _error.asStateFlow()

  fun loadProfile(userId: String) {
    viewModelScope.launch {
      _profileState.value = ProfileUiState.Loading
      try {
        createProfileIfNotExists(userId)
        val profile = repository.getProfile(userId)
        _profileState.value =
            profile?.let { ProfileUiState.Success(it) } ?: ProfileUiState.Error("Profile not found")
      } catch (e: Exception) {
        _profileState.value = ProfileUiState.Error(e.message ?: "Unknown error")
      }
    }
  }

  fun loadLikedPublications(userId: String) {
    viewModelScope.launch {
      try {
        val likedIds = repository.getUserLikedPublicationsIds(userId)
        val allPublications = repository.getAllPublications()
        val likedPublicationsList = allPublications.filter { it.id in likedIds }
        _likedPublications.value = likedPublicationsList
      } catch (e: Exception) {
        _error.value = "Failed to load liked publications: ${e.message}"
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

  fun likeAndAddToFavorites(userId: String, publicationId: String) {
    viewModelScope.launch {
      try {
        repository.incrementLikes(publicationId, userId)
        addPublicationToUserCollection(userId, publicationId)
        Log.d("SUCCESS", "POST LIKE $publicationId ")
      } catch (e: Exception) {
        _error.value = "Failed to like and save publication"
        Log.d("DEBUG", "UserId: $userId, PublicationId: $publicationId")
        Log.d("ERROR", "POST NOT LIKED")
      }
    }
  }

  private suspend fun addPublicationToUserCollection(userId: String, publicationId: String) {
    repository.addToUserCollection(userId, "likedPublications", publicationId)
  }

  fun removeLike(userId: String, publicationId: String) {
    viewModelScope.launch {
      try {
        repository.removeFromLikedPublications(userId, publicationId)
        repository.decrementLikesAndRemoveUser(publicationId, userId)
      } catch (e: Exception) {
        _error.value = "Failed to remove like: ${e.message}"
      }
    }
  }

  open fun searchProfiles(query: String) {
    searchJob?.cancel()

    if (query.isBlank()) {
      _searchState.value = SearchProfileState.Idle
      return
    }

    searchJob =
        viewModelScope.launch {
          _searchState.value = SearchProfileState.Loading
          try {
            delay(300)
            val results = repository.searchProfiles(query)
            _searchState.value = SearchProfileState.Success(results)
          } catch (e: Exception) {
            _searchState.value = SearchProfileState.Error(e.message ?: "Search failed")
          }
        }
  }

  override fun onCleared() {
    super.onCleared()
    searchJob?.cancel()
  }

  suspend fun createProfileIfNotExists(userId: String) {
    try {
      val profile = repository.getProfile(userId)
      if (profile == null) {
        val user = FirebaseAuth.getInstance().currentUser
        val defaultUsername = user?.displayName ?: "User${userId.take(4)}"
        val photoUrl = user?.photoUrl?.toString() ?: ""

        repository.createProfile(
            userId = userId, defaultUsername = defaultUsername, photoUrl = photoUrl)
      }
    } catch (e: Exception) {
      _profileState.value = ProfileUiState.Error(e.message ?: "Failed to create profile")
    }
  }

  fun updateUsername(userId: String, newUsername: String) {
    viewModelScope.launch {
      _usernameState.value = UsernameUpdateState.Loading
      try {
        when {
          newUsername.isBlank() -> {
            _usernameState.value = UsernameUpdateState.Error("Username cannot be empty")
            return@launch
          }
          newUsername.length < 3 -> {
            _usernameState.value =
                UsernameUpdateState.Error("Username must be at least 3 characters")
            return@launch
          }
          !newUsername.matches(Regex("^[a-zA-Z0-9._]+$")) -> {
            _usernameState.value =
                UsernameUpdateState.Error(
                    "Username can only contain letters, numbers, dots and underscores")
            return@launch
          }
          repository.doesUsernameExist(newUsername) -> {
            _usernameState.value = UsernameUpdateState.Error("Username already taken")
            return@launch
          }
        }

        repository.updateUsername(userId, newUsername)
        loadProfile(userId)
        _usernameState.value = UsernameUpdateState.Success
      } catch (e: Exception) {
        _usernameState.value = UsernameUpdateState.Error(e.message ?: "Failed to update username")
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

sealed class SearchProfileState {
  object Idle : SearchProfileState()

  object Loading : SearchProfileState()

  data class Success(val profiles: List<Profile>) : SearchProfileState()

  data class Error(val message: String) : SearchProfileState()
}

sealed class UsernameUpdateState {
  object Idle : UsernameUpdateState()

  object Loading : UsernameUpdateState()

  object Success : UsernameUpdateState()

  data class Error(val message: String) : UsernameUpdateState()
}
