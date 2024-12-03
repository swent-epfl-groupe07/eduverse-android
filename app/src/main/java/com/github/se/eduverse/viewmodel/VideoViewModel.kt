package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.model.repository.IVideoRepository
import com.github.se.eduverse.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class VideoViewModel(
    private val videoRepository: IVideoRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

  // MutableStateFlow pour suivre l'état de sauvegarde
  private val _saveVideoState = MutableStateFlow(false)
  open val saveVideoState: StateFlow<Boolean>
    get() = _saveVideoState

  // MutableStateFlow pour suivre les vidéos
  open val _videos = MutableStateFlow<List<Video>>(emptyList())
  open val videos: StateFlow<List<Video>>
    get() = _videos

  // Fonction pour sauvegarder une vidéo
  fun saveVideo(
      video: Video,
      folder: Folder? = null,
      onSuccess: () -> Unit = {},
      addToFolder: (String, String, Folder) -> Unit = { _, _, _ -> }
  ) {
    viewModelScope.launch {
      val success = videoRepository.saveVideo(video)
      _saveVideoState.value = success
      if (folder != null) {
        val uid = fileRepository.getNewUid()
        fileRepository.savePathToFirestore(video.path, ".mp4", uid) {
          addToFolder(
              uid,
              uid,
              folder) // The second uid is the name, might be replaced with a better one in future
          onSuccess()
        }
      }
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
class VideoViewModelFactory(
    private val videoRepository: IVideoRepository,
    private val fileRepository: FileRepository
) : ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
      return VideoViewModel(videoRepository, fileRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
