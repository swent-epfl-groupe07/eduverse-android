package com.github.se.eduverse.model

data class Comment(
    val id: String = "",
    val publicationId: String = "",
    val ownerId: String = "",
    val text: String = "",
    val likes: Int = 0
)