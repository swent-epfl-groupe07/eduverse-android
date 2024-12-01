package com.github.se.eduverse.model

data class Publication(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val thumbnailUrl: String = "", // URL pour l'aperçu ou la vignette
    val mediaUrl: String = "", // URL du média (photo ou vidéo)
    val mediaType: MediaType = MediaType.PHOTO, // Type de média (photo ou vidéo)
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList() // Ajout de la liste des commentaires
)

enum class MediaType {
  PHOTO,
  VIDEO
}
