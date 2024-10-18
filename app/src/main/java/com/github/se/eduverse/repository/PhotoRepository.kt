package com.github.se.eduverse.repository

import android.util.Log
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.repository.IPhotoRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class PhotoRepository(private val db: FirebaseFirestore, private val storage: FirebaseStorage) :
    IPhotoRepository {

  override suspend fun savePhoto(photo: Photo): Boolean {
    return try {
      val storageRef = storage.reference.child(photo.path)
      storageRef.putBytes(photo.photo).await()

      val downloadUrl = storageRef.downloadUrl.await().toString()

      val photoData =
          hashMapOf("ownerId" to photo.ownerId, "photoUrl" to downloadUrl, "path" to photo.path)

      db.collection("photos").add(photoData).await()
      Log.d("PhotoRepository", "Photo saved successfully")
      true
    } catch (e: Exception) {
      Log.e("PhotoRepository", "Error saving photo", e)
      false
    }
  }

  override suspend fun updatePhoto(photoId: String, photo: Photo): Boolean {
    return true
  }

  override suspend fun deletePhoto(photoId: String): Boolean {
    return true
  }
}

/*override suspend fun updatePhoto(photoId: String, photo: Photo): Boolean {
  return try {
    val storageRef = storage.reference.child(photo.path)
    storageRef.putBytes(photo.photo).await()

    val downloadUrl = storageRef.downloadUrl.await().toString()

    val photoData = hashMapOf(
      "ownerId" to photo.ownerId,
      "photoUrl" to downloadUrl,
      "path" to photo.path
    )

    db.collection("photos").document(photoId).set(photoData).await()
    Log.d("PhotoRepository", "Photo updated successfully")
    true
  } catch (e: Exception) {
    Log.e("PhotoRepository", "Error updating photo", e)
    false
  }
}

override suspend fun deletePhoto(photoId: String): Boolean {
  return try {
    db.collection("photos").document(photoId).delete().await()
    Log.d("PhotoRepository", "Photo deleted successfully")
    true
  } catch (e: Exception) {
    Log.e("PhotoRepository", "Error deleting photo", e)
    false
  }
}
}*/
