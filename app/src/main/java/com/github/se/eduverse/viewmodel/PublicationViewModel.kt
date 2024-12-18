package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.MediaCacheManager
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class PublicationViewModel(
    val repository: PublicationRepository,
    private val mediaCacheManager: MediaCacheManager
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

  init {}

  fun initializePublications() {
    loadPublications()
  }

  private fun loadPublications() {
    viewModelScope.launch {
      try {
        val newPublications = repository.loadRandomPublications()
        _publications.value = newPublications
        loadAndCachePublications()
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

  open fun loadFollowedPublications(userIds: List<String> = emptyList()) {
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

  private fun loadAndCachePublications() {
    viewModelScope.launch {
      try {
        mediaCacheManager.cleanCache()
        Log.d("CACHE", "Cache cleaned. Checking for cached files...")

        if (mediaCacheManager.hasCachedFiles()) {
          Log.d("CACHE", "Cached files still exist after cleaning, skipping caching")
          return@launch
        }

        val cachePublications = repository.loadCachePublications(limit = 50)
        cachePublications.forEach { publication ->
          val timestampedMediaFileName =
              "${System.currentTimeMillis()}_${publication.id}_media.${if (publication.mediaType == MediaType.VIDEO) "mp4" else "jpg"}"

          val success =
              mediaCacheManager.savePublicationToCache(
                  publication = publication,
                  mediaUrl = publication.mediaUrl,
                  mediaFileName = timestampedMediaFileName,
                  metadataFileName = "${publication.id}_metadata.json")

          if (success) {
            Log.d("CACHE_SAVE", "Successfully cached: $timestampedMediaFileName")
          } else {
            Log.e("CACHE_ERROR", "Failed to cache: $timestampedMediaFileName")
          }
        }

        Log.d("CACHE", "Successfully cached ${cachePublications.size} publications")
      } catch (e: Exception) {
        Log.e("CACHE_ERROR", "Failed to cache publications: ${e.message}")
      }
    }
  }
}
