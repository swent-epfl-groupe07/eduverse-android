package com.github.se.eduverse.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.query
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

open class ProfileViewModel(open val repository: ProfileRepository) : ViewModel() {
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
  private val _followActionState = MutableStateFlow<FollowActionState>(FollowActionState.Idle)
  open val followActionState: StateFlow<FollowActionState> = _followActionState.asStateFlow()
  private val _deletePublicationState =
      MutableStateFlow<DeletePublicationState>(DeletePublicationState.Idle)
  open val deletePublicationState: StateFlow<DeletePublicationState> =
      _deletePublicationState.asStateFlow()

  private val _favoritePublications = MutableStateFlow<List<Publication>>(emptyList())
  open val favoritePublications: StateFlow<List<Publication>> = _favoritePublications.asStateFlow()

  private val _favoriteActionState = MutableStateFlow<FavoriteActionState>(FavoriteActionState.Idle)
  val favoriteActionState: StateFlow<FavoriteActionState> = _favoriteActionState.asStateFlow()

  private var searchJob: Job? = null

  private val _error = MutableStateFlow<String?>(null)
  open val error: StateFlow<String?> = _error.asStateFlow()

  open fun loadProfile(userId: String) {
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

  open fun loadLikedPublications(userId: String) {
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

  open fun toggleFollow(currentUserId: String, targetUserId: String) {
    viewModelScope.launch {
      try {
        _followActionState.value = FollowActionState.Loading

        // Get current state
        val currentState = _profileState.value
        if (currentState is ProfileUiState.Success) {
          // Update UI optimistically
          val updatedProfile =
              currentState.profile.copy(
                  isFollowedByCurrentUser = !currentState.profile.isFollowedByCurrentUser,
                  followers =
                      if (currentState.profile.isFollowedByCurrentUser)
                          currentState.profile.followers - 1
                      else currentState.profile.followers + 1)
          _profileState.value = ProfileUiState.Success(updatedProfile)
        }

        // Perform database update
        val isNowFollowing = repository.toggleFollow(currentUserId, targetUserId)

        // Update success state with all relevant information
        _followActionState.value =
            FollowActionState.Success(
                followerId = currentUserId,
                targetUserId = targetUserId,
                isNowFollowing = isNowFollowing)
      } catch (e: Exception) {
        _followActionState.value =
            FollowActionState.Error(e.message ?: "Failed to update follow status")

        // Revert optimistic update if there was an error
        loadProfile(targetUserId)
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

  open fun likeAndAddToFavorites(userId: String, publicationId: String) {
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

  open fun removeLike(userId: String, publicationId: String) {
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

  open fun loadSearchHistory(userId: String? = FirebaseAuth.getInstance().currentUser?.uid) {
    if (userId == null) return

    searchJob?.cancel()
    searchJob =
        viewModelScope.launch {
          _searchState.value = SearchProfileState.Loading
          try {
            val results = repository.loadSearchHistory(userId)
            if (results.isEmpty()) {
              _searchState.value = SearchProfileState.Idle
            } else {
              _searchState.value = SearchProfileState.Success(results)
            }
          } catch (e: Exception) {
            _searchState.value = SearchProfileState.Error(e.message ?: "Load history failed")
          }
        }
  }

  open fun addProfileToHistory(userId: String, searchedProfileId: String) {
    viewModelScope.launch {
      try {
        repository.addProfileToHistory(userId, searchedProfileId)
      } catch (e: Exception) {
        _error.value = "Failed to add profile to history: ${e.message}"
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

  open fun resetUsernameState() {
    _usernameState.value = UsernameUpdateState.Idle
  }

  open suspend fun getFollowers(userId: String): List<Profile> {
    return try {
      repository.getFollowers(userId)
    } catch (e: Exception) {
      _error.value = "Failed to load followers: ${e.message}"
      emptyList()
    }
  }

  open suspend fun getFollowing(userId: String): List<Profile> {
    return try {
      repository.getFollowing(userId)
    } catch (e: Exception) {
      _error.value = "Failed to load following: ${e.message}"
      emptyList()
    }
  }

  open fun deletePublication(publicationId: String, userId: String) {
    viewModelScope.launch {
      _deletePublicationState.value = DeletePublicationState.Loading
      try {
        val success = repository.deletePublication(publicationId, userId)
        if (success) {
          _deletePublicationState.value = DeletePublicationState.Success
          // Reload profile to refresh the publications list
          loadProfile(userId)
        } else {
          _deletePublicationState.value =
              DeletePublicationState.Error("Failed to delete publication")
        }
      } catch (e: Exception) {
        _deletePublicationState.value =
            DeletePublicationState.Error(e.message ?: "Unknown error occurred")
      }
    }
  }

  open fun resetDeleteState() {
    _deletePublicationState.value = DeletePublicationState.Idle
  }

  /**
   * Loads all publications that a user has favorited. Combines favorite IDs with full publication
   * details.
   *
   * @param userId The ID of the user whose favorites should be loaded
   */
  fun loadFavoritePublications(userId: String) {
    viewModelScope.launch {
      try {
        val favoriteIds = repository.getFavoritePublicationsIds(userId)
        val favoritePublications = repository.getFavoritePublications(favoriteIds)
        _favoritePublications.value = favoritePublications
      } catch (e: Exception) {
        _error.value = "Failed to load favorite publications: ${e.message}"
      }
    }
  }

  /**
   * Toggles the favorite status of a publication for a user. If the publication is already
   * favorited, it will be unfavorited and vice versa.
   *
   * @param userId The ID of the user toggling the favorite
   * @param publicationId The ID of the publication being toggled
   */
  fun toggleFavorite(userId: String, publicationId: String) {
    viewModelScope.launch {
      _favoriteActionState.value = FavoriteActionState.Loading
      try {
        // Check current favorite status
        val isFavorited = repository.isPublicationFavorited(userId, publicationId)

        // Toggle the favorite status
        if (isFavorited) {
          repository.removeFromFavorites(userId, publicationId)
        } else {
          repository.addToFavorites(userId, publicationId)
        }

        // Reload favorites to update the UI
        loadFavoritePublications(userId)

        // Update action state with new favorite status
        _favoriteActionState.value = FavoriteActionState.Success(!isFavorited)
      } catch (e: Exception) {
        _favoriteActionState.value =
            FavoriteActionState.Error(e.message ?: "Failed to update favorite status")
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

sealed class FollowActionState {
  object Idle : FollowActionState()

  object Loading : FollowActionState()

  data class Success(
      val followerId: String,
      val targetUserId: String,
      val isNowFollowing: Boolean
  ) : FollowActionState()

  data class Error(val message: String) : FollowActionState()
}

sealed class DeletePublicationState {
  object Idle : DeletePublicationState()

  object Loading : DeletePublicationState()

  object Success : DeletePublicationState()

  data class Error(val message: String) : DeletePublicationState()
}

sealed class FavoriteActionState {
  object Idle : FavoriteActionState()

  object Loading : FavoriteActionState()

  data class Success(val isNowFavorited: Boolean) : FavoriteActionState()

  data class Error(val message: String) : FavoriteActionState()
}
