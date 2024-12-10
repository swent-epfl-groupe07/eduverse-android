package com.github.se.eduverse.fake

import android.net.Uri
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.ProfileRepository

class FakeProfileRepository : ProfileRepository {
  override suspend fun getProfile(userId: String): Profile? {
    return null
  }

  override suspend fun updateProfile(userId: String, profile: Profile) {
    TODO("Not yet implemented")
  }

  override suspend fun addPublication(userId: String, publication: Publication) {
    TODO("Not yet implemented")
  }

  override suspend fun removePublication(publicationId: String) {
    TODO("Not yet implemented")
  }

  override suspend fun removeFromFavorites(userId: String, publicationId: String) {
    TODO("Not yet implemented")
  }

  override suspend fun getFavoritePublicationsIds(userId: String): List<String> {
    TODO("Not yet implemented")
  }

  override suspend fun isPublicationFavorited(userId: String, publicationId: String): Boolean {
    TODO("Not yet implemented")
  }

  override suspend fun followUser(followerId: String, followedId: String) {
    TODO("Not yet implemented")
  }

  override suspend fun unfollowUser(followerId: String, followedId: String) {
    TODO("Not yet implemented")
  }

  override suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
    TODO("Not yet implemented")
  }

  override suspend fun updateProfileImage(userId: String, imageUrl: String) {
    TODO("Not yet implemented")
  }

  override suspend fun searchProfiles(query: String, limit: Int): List<Profile> {
    TODO("Not yet implemented")
  }

  override suspend fun createProfile(
      userId: String,
      defaultUsername: String,
      photoUrl: String
  ): Profile {
    TODO("Not yet implemented")
  }

  override suspend fun updateUsername(userId: String, newUsername: String) {
    TODO("Not yet implemented")
  }

  override suspend fun doesUsernameExist(username: String): Boolean {
    TODO("Not yet implemented")
  }

  override suspend fun addToUserCollection(
      userId: String,
      collectionName: String,
      publicationId: String
  ) {
    TODO("Not yet implemented")
  }

  override suspend fun incrementLikes(publicationId: String, userId: String) {
    TODO("Not yet implemented")
  }

  override suspend fun removeFromLikedPublications(userId: String, publicationId: String) {
    TODO("Not yet implemented")
  }

  override suspend fun decrementLikesAndRemoveUser(publicationId: String, userId: String) {
    TODO("Not yet implemented")
  }

  override suspend fun getAllPublications(): List<Publication> {
    TODO("Not yet implemented")
  }

  override suspend fun getUserLikedPublicationsIds(userId: String): List<String> {
    TODO("Not yet implemented")
  }

  override suspend fun isFollowing(followerId: String, targetUserId: String): Boolean {
    TODO("Not yet implemented")
  }

  override suspend fun toggleFollow(followerId: String, targetUserId: String): Boolean {
    TODO("Not yet implemented")
  }

  override suspend fun updateFollowCounts(
      followerId: String,
      targetUserId: String,
      isFollowing: Boolean
  ) {
    TODO("Not yet implemented")
  }

  override suspend fun getFollowers(userId: String): List<Profile> {
    TODO("Not yet implemented")
  }

  override suspend fun getFollowing(userId: String): List<Profile> {
    TODO("Not yet implemented")
  }

  override suspend fun deletePublication(publicationId: String, userId: String): Boolean {
    TODO("Not yet implemented")
  }

  override suspend fun addToFavorites(userId: String, publicationId: String) {
    TODO("Not yet implemented")
  }
}
