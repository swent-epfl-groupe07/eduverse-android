package com.github.se.eduverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.repository.Profile
import com.github.se.eduverse.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

  private val _profileState = MutableStateFlow(Profile())
  open val profileState: StateFlow<Profile> = _profileState

  open fun loadProfile(userId: String) {
    viewModelScope.launch {
      val profile = repository.getProfile(userId)
      profile?.let { _profileState.value = it }
    }
  }
  // Updated saveProfile to accept individual fields as parameters
  open fun saveProfile(
      userId: String,
      name: String,
      school: String,
      coursesSelected: String,
      videosWatched: String, // Changed to Int
      quizzesCompleted: String, // Changed to Int
      studyTime: String, // Changed to Double
      studyGoals: String
  ) {
    viewModelScope.launch {
      // Create a new Profile instance or update the existing one
      val updatedProfile =
          _profileState.value.copy(
              name = name,
              school = school,
              coursesSelected = coursesSelected,
              videosWatched = videosWatched,
              quizzesCompleted = quizzesCompleted,
              studyTime = studyTime,
              studyGoals = studyGoals)

      // Update the profileState to the new profile data
      _profileState.value = updatedProfile
      // Save the updated profile using the repository
      repository.saveProfile(userId, updatedProfile)
    }
  }
}
