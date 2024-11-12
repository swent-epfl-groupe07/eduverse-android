package com.github.se.eduverse.repository

import android.util.Log
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.model.repository.IVideoRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class VideoRepository(private val db: FirebaseFirestore, private val storage: FirebaseStorage) :
    IVideoRepository {

  override suspend fun saveVideo(video: Video): Boolean {
    return try {
      // Définir le chemin dans Firebase Storage pour chaque utilisateur
      val storageRef = storage.reference.child(video.path)
      storageRef.putBytes(video.video).await()

      // Obtenir l'URL de téléchargement
      val downloadUrl = storageRef.downloadUrl.await().toString()

      // Créer un document pour la vidéo avec ses métadonnées
      val videoData =
          hashMapOf("ownerId" to video.ownerId, "videoUrl" to downloadUrl, "path" to video.path)

      // Ajouter les métadonnées dans Firestore
      db.collection("videos").add(videoData).await()
      Log.d("VideoRepository", "Video saved successfully")
      true
    } catch (e: Exception) {
      Log.e("VideoRepository", "Error saving video", e)
      false
    }
  }

  override suspend fun updateVideo(videoId: String, video: Video): Boolean {
    return try {
      val storageRef = storage.reference.child(video.path)
      storageRef.putBytes(video.video).await()

      val downloadUrl = storageRef.downloadUrl.await().toString()

      val videoData =
          hashMapOf("ownerId" to video.ownerId, "videoUrl" to downloadUrl, "path" to video.path)

      db.collection("videos").document(videoId).set(videoData).await()
      Log.d("VideoRepository", "Video updated successfully")
      true
    } catch (e: Exception) {
      Log.e("VideoRepository", "Error updating video", e)
      false
    }
  }

  override suspend fun deleteVideo(videoId: String): Boolean {
    return try {
      db.collection("videos").document(videoId).delete().await()
      Log.d("VideoRepository", "Video deleted successfully")
      true
    } catch (e: Exception) {
      Log.e("VideoRepository", "Error deleting video", e)
      false
    }
  }

  override suspend fun getVideosByOwner(ownerId: String): List<Video> {
    val storageRef = storage.reference

    return try {
      // Récupérer les documents dans la collection "videos" où l'ownerId correspond
      val snapshot = db.collection("videos").whereEqualTo("ownerId", ownerId).get().await()

      snapshot.documents.map { document ->
        val path = document.getString("path") ?: ""
        val videoRef = storageRef.child(path)

        // Récupérer l'URL de téléchargement
        val downloadUrl = videoRef.downloadUrl.await()
        Log.d("GalleryScreen", "Download URL: $downloadUrl")

        Video(
            ownerId = document.getString("ownerId") ?: "",
            video = ByteArray(0), // Tu peux ignorer ceci si tu utilises uniquement l'URL
            path = downloadUrl.toString() // Utilise l'URL ici
            )
      }
    } catch (e: Exception) {
      Log.e("VideoRepository", "Error retrieving videos", e)
      emptyList()
    }
  }
}
