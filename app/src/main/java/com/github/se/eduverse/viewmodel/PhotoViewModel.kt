package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.repository.IPhotoRepository
import com.github.se.eduverse.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class PhotoViewModel(
    private val photoRepository: IPhotoRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

  private val _savePhotoState = MutableStateFlow(false)
  open val savePhotoState: StateFlow<Boolean>
    get() = _savePhotoState

  open val _photos = MutableStateFlow<List<Photo>>(emptyList())
  open val photos: StateFlow<List<Photo>>
    get() = _photos

  fun savePhoto(
      photo: Photo,
      folder: Folder? = null,
      onSuccess: () -> Unit = {},
      addToFolder: (String, String, Folder) -> Unit = { _, _, _ -> }
  ) {
    viewModelScope.launch {
      val success = photoRepository.savePhoto(photo)
      _savePhotoState.value = success
      if (folder != null) {
        val uid = fileRepository.getNewUid()
        fileRepository.savePathToFirestore(photo.path, ".jpg", uid) {
          addToFolder(
              uid,
              uid,
              folder) // The second uid is the name, might be replaced with a better one in future
          onSuccess()
        }
      }
    }
  }

  open fun getPhotosByOwner(ownerId: String) {
    viewModelScope.launch {
      val photos = photoRepository.getPhotosByOwner(ownerId)
      Log.d("GalleryScreen", "Photos fetched: ${photos.size}")
      _photos.value = photos
    }
  }

  open fun makeFileFromPhoto(photo: Photo, onSuccess: (String) -> Unit) {
    viewModelScope.launch {
      val fileId = fileRepository.getNewUid()
      fileRepository.savePathToFirestore(photo.path, ".jpg", fileId) { onSuccess(fileId) }
    }
  }

  /*fun updatePhoto(photoId: String, photo: Photo) {
    viewModelScope.launch { photoRepository.updatePhoto(photoId, photo) }
  }

  fun deletePhoto(photoId: String) {
    viewModelScope.launch { photoRepository.deletePhoto(photoId) }
  }*/
}

class PhotoViewModelFactory(
    private val photoRepository: IPhotoRepository,
    private val fileRepository: FileRepository
) : ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
      return PhotoViewModel(photoRepository, fileRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
