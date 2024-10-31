package com.github.se.eduverse.repository

import android.net.Uri
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

interface ProfileRepository {
  suspend fun getProfile(userId: String): Profile?

  suspend fun updateProfile(userId: String, profile: Profile)

  suspend fun addPublication(userId: String, publication: Publication)

  suspend fun removePublication(publicationId: String)

  suspend fun addToFavorites(userId: String, publicationId: String)

  suspend fun removeFromFavorites(userId: String, publicationId: String)

  suspend fun followUser(followerId: String, followedId: String)

  suspend fun unfollowUser(followerId: String, followedId: String)

  suspend fun uploadProfileImage(userId: String, imageUri: Uri): String

  suspend fun updateProfileImage(userId: String, imageUrl: String)
}

class ProfileRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ProfileRepository {
  private val profilesCollection = firestore.collection("profiles")
  private val publicationsCollection = firestore.collection("publications")
  private val favoritesCollection = firestore.collection("favorites")
  private val followersCollection = firestore.collection("followers")

  override suspend fun getProfile(userId: String): Profile? {
    val profileDoc = profilesCollection.document(userId).get().await()
    val profile = profileDoc.toObject(Profile::class.java)

    // Get publications
    val publications =
        publicationsCollection.whereEqualTo("userId", userId).get().await().documents.mapNotNull {
          it.toObject(Publication::class.java)
        }

    // Get favorites
    val favorites =
        favoritesCollection.whereEqualTo("userId", userId).get().await().documents.mapNotNull {
          publicationsCollection
              .document(it.getString("publicationId") ?: "")
              .get()
              .await()
              .toObject(Publication::class.java)
        }

    // Get followers/following count
    val followersCount = followersCollection.whereEqualTo("followedId", userId).get().await().size()

    val followingCount = followersCollection.whereEqualTo("followerId", userId).get().await().size()

    return profile?.copy(
        publications = publications,
        favoritePublications = favorites,
        followers = followersCount,
        following = followingCount)
  }

  override suspend fun updateProfile(userId: String, profile: Profile) {
    profilesCollection.document(userId).set(profile).await()
  }

  override suspend fun addPublication(userId: String, publication: Publication) {
    publicationsCollection.document(publication.id).set(publication).await()
  }

  override suspend fun removePublication(publicationId: String) {
    publicationsCollection.document(publicationId).delete().await()
  }

  override suspend fun addToFavorites(userId: String, publicationId: String) {
    favoritesCollection
        .add(
            hashMapOf(
                "userId" to userId,
                "publicationId" to publicationId,
                "timestamp" to System.currentTimeMillis()))
        .await()
  }

  override suspend fun removeFromFavorites(userId: String, publicationId: String) {
    favoritesCollection
        .whereEqualTo("userId", userId)
        .whereEqualTo("publicationId", publicationId)
        .get()
        .await()
        .documents
        .forEach { it.reference.delete().await() }
  }

  override suspend fun followUser(followerId: String, followedId: String) {
    followersCollection
        .add(
            hashMapOf(
                "followerId" to followerId,
                "followedId" to followedId,
                "timestamp" to System.currentTimeMillis()))
        .await()
  }

  override suspend fun unfollowUser(followerId: String, followedId: String) {
    followersCollection
        .whereEqualTo("followerId", followerId)
        .whereEqualTo("followedId", followedId)
        .get()
        .await()
        .documents
        .forEach { it.reference.delete().await() }
  }

  override suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
    val storageRef = storage.reference.child("profile_images/$userId")
    storageRef.putFile(imageUri).await()
    return storageRef.downloadUrl.await().toString()
  }

  override suspend fun updateProfileImage(userId: String, imageUrl: String) {
    profilesCollection.document(userId).update("profileImageUrl", imageUrl).await()
  }
}
