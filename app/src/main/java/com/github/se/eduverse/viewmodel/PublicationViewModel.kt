package com.github.se.eduverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class PublicationViewModel(
    val repository: PublicationRepository,
) : ViewModel() {

  private val _publications = MutableStateFlow<List<Publication>>(emptyList())
  open val publications: StateFlow<List<Publication>>
    get() = _publications

  private val _followedPublications = MutableStateFlow<List<Publication>>(emptyList())
  open val followedPublications: StateFlow<List<Publication>>
    get() = _followedPublications

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
        _error.value = "fail to load publications"
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
        _error.value = "fail to load publications"
      }
    }
  }

  open suspend fun loadFollowedPublications(userIds: List<String> = emptyList()) {
    viewModelScope.launch {
      try {
        if (_followedPublications.value.isEmpty()) {
          val newPublications = repository.loadRandomPublications(userIds)
          _followedPublications.value = newPublications
        } else {
          val morePublications = repository.loadRandomPublications(userIds)
          _followedPublications.value = (_followedPublications.value + morePublications).shuffled()
        }
        _error.value = null
      } catch (e: Exception) {
        _error.value = "fail to load publications"
      }
    }
  }
}
