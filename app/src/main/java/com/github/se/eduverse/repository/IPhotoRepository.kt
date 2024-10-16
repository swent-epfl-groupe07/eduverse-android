package com.github.se.eduverse.model.repository

import com.github.se.eduverse.model.Photo

interface IPhotoRepository {
  suspend fun savePhoto(photo: Photo): Boolean

  suspend fun updatePhoto(photoId: String, photo: Photo): Boolean

  suspend fun deletePhoto(photoId: String): Boolean
}
