package com.github.se.eduverse.model

data class Profile(
    val id: String = "",
    val username: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val publications: List<Publication> = emptyList(),
    val favoritePublications: List<Publication> = emptyList(),
    val profileImageUrl: String = ""
)

data class Publication(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val thumbnailUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
