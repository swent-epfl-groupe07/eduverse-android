package com.github.se.eduverse.fake

import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeProfileViewModel : ProfileViewModel(FakeProfileRepository()) {
  private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
  override val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

  fun setState(state: ProfileUiState) {
    _profileState.value = state
  }

  override fun likeAndAddToFavorites(userId: String, publicationId: String) {}

  override fun removeLike(userId: String, publicationId: String) {}
}
