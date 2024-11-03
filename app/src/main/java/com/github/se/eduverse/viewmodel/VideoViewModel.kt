package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.model.repository.IVideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class VideoViewModel(private val videoRepository: IVideoRepository) : ViewModel() {

  // MutableStateFlow pour suivre l'état de sauvegarde
  private val _saveVideoState = MutableStateFlow(false)
  open val saveVideoState: StateFlow<Boolean>
    get() = _saveVideoState

  // MutableStateFlow pour suivre les vidéos
  open val _videos = MutableStateFlow<List<Video>>(emptyList())
  open val videos: StateFlow<List<Video>>
    get() = _videos

  // Fonction pour sauvegarder une vidéo
  fun saveVideo(video: Video) {
    viewModelScope.launch {
      val success = videoRepository.saveVideo(video)
      _saveVideoState.value = success
    }
  }

  // Fonction pour récupérer les vidéos par ownerId
  open fun getVideosByOwner(ownerId: String) {
    viewModelScope.launch {
      val videos = videoRepository.getVideosByOwner(ownerId)
      Log.d("GalleryScreen", "Videos fetched: ${videos.size}")
      _videos.value = videos
    }
  }

  /* Fonctions supplémentaires pour mettre à jour ou supprimer une vidéo si nécessaire
  fun updateVideo(videoId: String, video: Video) {
      viewModelScope.launch { videoRepository.updateVideo(videoId, video) }
  }

  fun deleteVideo(videoId: String) {
      viewModelScope.launch { videoRepository.deleteVideo(videoId) }
  }*/
}

// Factory pour créer une instance de VideoViewModel avec un IVideoRepository
class VideoViewModelFactory(private val videoRepository: IVideoRepository) :
    ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
      return VideoViewModel(videoRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
