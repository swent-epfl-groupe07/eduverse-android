package com.github.se.eduverse.model

data class Publication(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val thumbnailUrl: String = "",
    val mediaUrl: String = "",
    val mediaType: MediaType = MediaType.PHOTO,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList()
)

enum class MediaType {
  PHOTO,
  VIDEO
}
