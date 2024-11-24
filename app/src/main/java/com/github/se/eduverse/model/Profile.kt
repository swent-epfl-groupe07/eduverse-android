package com.github.se.eduverse.model

data class Profile(
    val id: String = "",
    val username: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val publications: List<Publication> = emptyList(),
    val favoritePublications: List<Publication> = emptyList(),
    val profileImageUrl: String = "",
    val isFollowedByCurrentUser: Boolean = false,
    val profileType: ProfileType = ProfileType.STUDENT,
    val address: String? = null,
    val phoneNumber: String? = null,
    val subjects: List<String>? = null
)

enum class ProfileType {
    STUDENT,
    TEACHER,
    SCHOOL
}