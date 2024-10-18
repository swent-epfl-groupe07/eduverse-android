package com.github.se.eduverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.repository.IPhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PhotoViewModel(private val photoRepository: IPhotoRepository) : ViewModel() {

  private val _savePhotoState = MutableStateFlow(false)
  val savePhotoState: StateFlow<Boolean>
    get() = _savePhotoState

  fun savePhoto(photo: Photo) {
    viewModelScope.launch {
      val success = photoRepository.savePhoto(photo)
      _savePhotoState.value = success
    }
  }

  /*fun updatePhoto(photoId: String, photo: Photo) {
    viewModelScope.launch { photoRepository.updatePhoto(photoId, photo) }
  }

  fun deletePhoto(photoId: String) {
    viewModelScope.launch { photoRepository.deletePhoto(photoId) }
  }*/
}

class PhotoViewModelFactory(private val photoRepository: IPhotoRepository) :
    ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
      return PhotoViewModel(photoRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
