package com.github.se.eduverse.repository

import android.net.Uri
import android.util.Log
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

interface ProfileRepository {
  suspend fun getProfile(userId: String): Profile?

  suspend fun updateProfile(userId: String, profile: Profile)

  suspend fun addPublication(userId: String, publication: Publication)

  suspend fun removePublication(publicationId: String)

  suspend fun removeFromFavorites(userId: String, publicationId: String)

  suspend fun followUser(followerId: String, followedId: String)

  suspend fun unfollowUser(followerId: String, followedId: String)

  suspend fun uploadProfileImage(userId: String, imageUri: Uri): String

  suspend fun updateProfileImage(userId: String, imageUrl: String)

  suspend fun searchProfiles(query: String, limit: Int = 20): List<Profile>

  suspend fun createProfile(userId: String, defaultUsername: String, photoUrl: String = ""): Profile

  suspend fun updateUsername(userId: String, newUsername: String)

  suspend fun doesUsernameExist(username: String): Boolean

  suspend fun addToUserCollection(userId: String, collectionName: String, publicationId: String)

  suspend fun incrementLikes(publicationId: String, userId: String)

  suspend fun removeFromLikedPublications(userId: String, publicationId: String)

  suspend fun decrementLikesAndRemoveUser(publicationId: String, userId: String)

  suspend fun getAllPublications(): List<Publication>

  suspend fun getUserLikedPublicationsIds(userId: String): List<String>

  suspend fun isFollowing(followerId: String, targetUserId: String): Boolean
  suspend fun toggleFollow(followerId: String, targetUserId: String): Boolean
  suspend fun updateFollowCounts(followerId: String, targetUserId: String, isFollowing: Boolean)

}

class ProfileRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ProfileRepository {
  private val profilesCollection = firestore.collection("profiles")
  private val publicationsCollection = firestore.collection("publications")
  private val favoritesCollection = firestore.collection("favorites")
  private val followersCollection = firestore.collection("followers")

  private val usersCollection = firestore.collection("users")

  override suspend fun getAllPublications(): List<Publication> {
    return try {
      publicationsCollection.get().await().documents.mapNotNull {
        it.toObject(Publication::class.java)
      }
    } catch (e: Exception) {
      Log.e("GET_ALL_PUBLICATIONS", "Failed to get all publications: ${e.message}")
      emptyList()
    }
  }

  override suspend fun getUserLikedPublicationsIds(userId: String): List<String> {
    return try {
      usersCollection
          .document(userId)
          .collection("likedPublications")
          .get()
          .await()
          .documents
          .mapNotNull { it.getString("publicationId") }
    } catch (e: Exception) {
      Log.e("GET_LIKED_PUBLICATIONS", "Failed to get liked publications: ${e.message}")
      emptyList()
    }
  }

    override suspend fun getProfile(userId: String): Profile? {
        val profileDoc = profilesCollection.document(userId).get().await()
        val profile = profileDoc.toObject(Profile::class.java)

        // Get current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Get publications
        val publications = publicationsCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Publication::class.java) }

        // Get favorites
        val favorites = favoritesCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull {
                publicationsCollection
                    .document(it.getString("publicationId") ?: "")
                    .get()
                    .await()
                    .toObject(Publication::class.java)
            }

        // Get followers/following count
        val followersCount = followersCollection
            .whereEqualTo("followedId", userId)
            .get()
            .await()
            .size()

        val followingCount = followersCollection
            .whereEqualTo("followerId", userId)
            .get()
            .await()
            .size()

        // Check if the current user is following this profile
        val isFollowedByCurrentUser = currentUserId?.let {
            isFollowing(it, userId)
        } ?: false

        return profile?.copy(
            publications = publications,
            favoritePublications = favorites,
            followers = followersCount,
            following = followingCount,
            isFollowedByCurrentUser = isFollowedByCurrentUser
        )
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

  override suspend fun searchProfiles(query: String, limit: Int): List<Profile> {
    return profilesCollection
        .get()
        .await()
        .documents
        .mapNotNull { it.toObject(Profile::class.java) }
        .filter { profile -> profile.username.lowercase().contains(query.lowercase()) }
        .take(limit)
  }

  override suspend fun createProfile(
      userId: String,
      defaultUsername: String,
      photoUrl: String
  ): Profile {
    val profile =
        Profile(
            id = userId,
            username = defaultUsername,
            profileImageUrl = photoUrl,
            followers = 0,
            following = 0,
            publications = emptyList(),
            favoritePublications = emptyList())

    profilesCollection.document(userId).set(profile).await()
    return profile
  }

  override suspend fun updateUsername(userId: String, newUsername: String) {
    profilesCollection.document(userId).update("username", newUsername).await()
  }

  override suspend fun doesUsernameExist(username: String): Boolean {
    val snapshot = profilesCollection.whereEqualTo("username", username).limit(1).get().await()
    return !snapshot.isEmpty
  }

  override suspend fun addToUserCollection(
      userId: String,
      collectionName: String,
      publicationId: String
  ) {
    firestore
        .collection("users")
        .document(userId)
        .collection(collectionName)
        .document(publicationId)
        .set(hashMapOf("publicationId" to publicationId, "timestamp" to System.currentTimeMillis()))
        .await()
  }

  override suspend fun incrementLikes(publicationId: String, userId: String) {
    try {
      // Targeted query to retrieve the document with a matching `id` field

      val querySnapshot =
          firestore.collection("publications").whereEqualTo("id", publicationId).get().await()

      if (!querySnapshot.isEmpty) {
        val documentRef = querySnapshot.documents[0].reference

        firestore
            .runTransaction { transaction ->
              val snapshot = transaction.get(documentRef)
              val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

              // Check if the user has already liked

              if (!likedBy.contains(userId)) {
                val currentLikes = snapshot.getLong("likes") ?: 0
                transaction.update(documentRef, "likes", currentLikes + 1)
                transaction.update(documentRef, "likedBy", likedBy + userId)
              } else {
                Log.d("INCREMENTCHECK", "User $userId has already liked this publication.")
              }
            }
            .await()
      } else {
        throw Exception("Publication not found with ID: $publicationId")
      }
    } catch (e: Exception) {
      Log.d("INCREMENTFAIIIL", "FAIIIL: ${e.message}")
    }
  }

  override suspend fun removeFromLikedPublications(userId: String, publicationId: String) {
    try {
      val documentRef =
          firestore
              .collection("users")
              .document(userId)
              .collection("likedPublications")
              .document(publicationId)

      documentRef.delete().await()
      Log.d(
          "REMOVE_LIKE",
          "Publication $publicationId removed from likedPublications for user $userId.")
    } catch (e: Exception) {
      Log.d("REMOVE_LIKE", "Failed to remove publication from likedPublications: ${e.message}")
      throw e
    }
  }

  override suspend fun decrementLikesAndRemoveUser(publicationId: String, userId: String) {
    try {
      // Query to find the document with a matching `id` field

      val querySnapshot =
          firestore.collection("publications").whereEqualTo("id", publicationId).get().await()

      if (!querySnapshot.isEmpty) {
        val documentRef = querySnapshot.documents[0].reference

        firestore
            .runTransaction { transaction ->
              val snapshot = transaction.get(documentRef)
              val likedBy = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()

              if (likedBy.contains(userId)) {
                likedBy.remove(userId)
                val currentLikes = snapshot.getLong("likes") ?: 0
                val newLikes = if (currentLikes > 0) currentLikes - 1 else 0

                // Update the number of likes and the `likedBy` list

                transaction.update(documentRef, mapOf("likedBy" to likedBy, "likes" to newLikes))
              }
            }
            .await()
      } else {
        throw Exception("Publication not found with ID: $publicationId")
      }
    } catch (e: Exception) {
      Log.d("REMOVE_LIKE", "Failed to decrement likes and remove user from likedBy: ${e.message}")
      throw e
    }
  }

    override suspend fun isFollowing(followerId: String, targetUserId: String): Boolean {
        return followersCollection
            .whereEqualTo("followerId", followerId)
            .whereEqualTo("followedId", targetUserId)
            .get()
            .await()
            .documents
            .isNotEmpty()
    }

    override suspend fun toggleFollow(followerId: String, targetUserId: String): Boolean {
        val isCurrentlyFollowing = isFollowing(followerId, targetUserId)

        if (isCurrentlyFollowing) {
            // Unfollow
            unfollowUser(followerId, targetUserId)
        } else {
            // Follow
            followUser(followerId, targetUserId)
        }

        // Update the follower counts for both users
        updateFollowCounts(followerId, targetUserId, !isCurrentlyFollowing)

        return !isCurrentlyFollowing
    }

    override suspend fun updateFollowCounts(followerId: String, targetUserId: String, isFollowing: Boolean) {
        firestore.runTransaction { transaction ->
            // Update target user's followers count
            val targetUserRef = profilesCollection.document(targetUserId)
            val targetUserSnapshot = transaction.get(targetUserRef)
            val currentFollowers = targetUserSnapshot.getLong("followers") ?: 0
            transaction.update(targetUserRef, "followers",
                if (isFollowing) currentFollowers + 1 else currentFollowers - 1)

            // Update current user's following count
            val followerRef = profilesCollection.document(followerId)
            val followerSnapshot = transaction.get(followerRef)
            val currentFollowing = followerSnapshot.getLong("following") ?: 0
            transaction.update(followerRef, "following",
                if (isFollowing) currentFollowing + 1 else currentFollowing - 1)
        }.await()
    }

}
