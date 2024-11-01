package com.github.se.eduverse.model

data class Publication(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val thumbnailUrl: String = "",  // URL pour l'aperçu ou la vignette
    val mediaUrl: String = "",      // URL du média (photo ou vidéo)
    val mediaType: MediaType = MediaType.PHOTO,  // Type de média (photo ou vidéo)
    val timestamp: Long = System.currentTimeMillis()
)

enum class MediaType {
    PHOTO, VIDEO
}