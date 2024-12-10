package com.github.se.eduverse.repository

import android.net.Uri
import android.util.Log
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface ProfileRepository {
  suspend fun getProfile(userId: String): Profile?

  suspend fun updateProfile(userId: String, profile: Profile)

  suspend fun addPublication(userId: String, publication: Publication)

  suspend fun removePublication(publicationId: String)

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

  suspend fun getFollowers(userId: String): List<Profile>

  suspend fun getFollowing(userId: String): List<Profile>

  suspend fun deletePublication(publicationId: String, userId: String): Boolean

  suspend fun addToFavorites(userId: String, publicationId: String)

  suspend fun removeFromFavorites(userId: String, publicationId: String)

  suspend fun getFavoritePublicationsIds(userId: String): List<String>

  suspend fun isPublicationFavorited(userId: String, publicationId: String): Boolean
}

open class ProfileRepositoryImpl(
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

    // Check if the current user is following this profile
    val isFollowedByCurrentUser = currentUserId?.let { isFollowing(it, userId) } ?: false

    return profile?.copy(
        publications = publications,
        favoritePublications = favorites,
        followers = followersCount,
        following = followingCount,
        isFollowedByCurrentUser = isFollowedByCurrentUser)
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
    return try {
      profilesCollection
          .get()
          .await()
          .documents
          .mapNotNull { doc ->
            val profile = doc.toObject(Profile::class.java)
            profile?.let {
              // Get real-time follower and following counts for each profile
              val followersCount =
                  followersCollection.whereEqualTo("followedId", it.id).get().await().size()

              val followingCount =
                  followersCollection.whereEqualTo("followerId", it.id).get().await().size()

              // Get current user's follow status if logged in
              val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
              val isFollowedByCurrentUser =
                  currentUserId?.let { uid -> isFollowing(uid, it.id) } ?: false

              // Return profile with updated counts
              it.copy(
                  followers = followersCount,
                  following = followingCount,
                  isFollowedByCurrentUser = isFollowedByCurrentUser)
            }
          }
          .filter { profile -> profile.username.lowercase().contains(query.lowercase()) }
          .take(limit)
    } catch (e: Exception) {
      Log.e("SEARCH_PROFILES", "Failed to search profiles: ${e.message}")
      emptyList()
    }
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
    return try {
      val isCurrentlyFollowing = isFollowing(followerId, targetUserId)

      if (isCurrentlyFollowing) {
        unfollowUser(followerId, targetUserId)
      } else {
        followUser(followerId, targetUserId)
      }

      updateFollowCounts(followerId, targetUserId, !isCurrentlyFollowing)

      !isCurrentlyFollowing
    } catch (e: Exception) {
      Log.e("TOGGLE_FOLLOW", "Failed to toggle follow: ${e.message}")
      throw e
    }
  }

  override suspend fun updateFollowCounts(
      followerId: String,
      targetUserId: String,
      isFollowing: Boolean
  ) {
    firestore
        .runTransaction { transaction ->
          // First do all reads
          val targetUserRef = profilesCollection.document(targetUserId)
          val followerRef = profilesCollection.document(followerId)

          // Read both documents first
          val targetUserSnapshot = transaction.get(targetUserRef)
          val followerSnapshot = transaction.get(followerRef)

          // Get current counts
          val currentFollowers = targetUserSnapshot.getLong("followers") ?: 0
          val currentFollowing = followerSnapshot.getLong("following") ?: 0

          // Then do all writes
          transaction.update(
              targetUserRef,
              "followers",
              if (isFollowing) currentFollowers + 1 else currentFollowers - 1)

          transaction.update(
              followerRef,
              "following",
              if (isFollowing) currentFollowing + 1 else currentFollowing - 1)

          // Return a dummy value since we don't need to return anything
          null
        }
        .await()
  }

  override suspend fun getFollowers(userId: String): List<Profile> {
    return try {
      // Get all followers IDs
      val followerDocs = followersCollection.whereEqualTo("followedId", userId).get().await()

      // Get profile details for each follower
      val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
      followerDocs.documents.mapNotNull { doc ->
        val followerId = doc.getString("followerId") ?: return@mapNotNull null
        val profileDoc = profilesCollection.document(followerId).get().await()
        val profile = profileDoc.toObject(Profile::class.java) ?: return@mapNotNull null

        // Check if the current user is following this profile
        val isFollowed = currentUserId?.let { isFollowing(it, followerId) } ?: false

        profile.copy(isFollowedByCurrentUser = isFollowed)
      }
    } catch (e: Exception) {
      Log.e("GET_FOLLOWERS", "Failed to get followers: ${e.message}")
      emptyList()
    }
  }

  override suspend fun getFollowing(userId: String): List<Profile> {
    return try {
      // Get all following IDs
      val followingDocs = followersCollection.whereEqualTo("followerId", userId).get().await()

      // Get profile details for each following
      followingDocs.documents.mapNotNull { doc ->
        val followingId = doc.getString("followedId") ?: return@mapNotNull null
        val profileDoc = profilesCollection.document(followingId).get().await()
        profileDoc.toObject(Profile::class.java)?.let { profile ->
          // Since this is from following list, we know the current user is following them
          profile.copy(id = followingId, isFollowedByCurrentUser = true)
        }
      }
    } catch (e: Exception) {
      Log.e("GET_FOLLOWING", "Failed to get following: ${e.message}")
      emptyList()
    }
  }

  override suspend fun deletePublication(publicationId: String, userId: String): Boolean {
    return try {
      coroutineScope {
        val pubQuery =
            publicationsCollection
                .whereEqualTo("id", publicationId)
                .whereEqualTo("userId", userId)
                .get()
                .await()

        if (pubQuery.isEmpty) {
          throw Exception("Publication not found or user not authorized")
        }

        val pubDoc = pubQuery.documents[0]
        val mediaUrlToDelete = pubDoc.getString("mediaUrl")
        val thumbnailUrlToDelete = pubDoc.getString("thumbnailUrl")
        val likedByUsers = pubDoc.get("likedBy") as? List<String> ?: emptyList()

        // Delete Firestore document first
        pubDoc.reference.delete().await()

        // Use async for parallel deletion of liked publications
        val likedPubsDeletion = launch {
          likedByUsers.forEach { likedUserId ->
            val likedPubRef =
                usersCollection
                    .document(likedUserId)
                    .collection("likedPublications")
                    .document(publicationId)
            likedPubRef.delete().await()
          }
        }

        // Delete media files in parallel
        val mediaDeletion = launch {
          if (!mediaUrlToDelete.isNullOrEmpty()) {
            val mediaRef = storage.getReferenceFromUrl(mediaUrlToDelete)
            mediaRef.delete().await()
          }

          if (!thumbnailUrlToDelete.isNullOrEmpty() && thumbnailUrlToDelete != mediaUrlToDelete) {
            val thumbnailRef = storage.getReferenceFromUrl(thumbnailUrlToDelete)
            thumbnailRef.delete().await()
          }
        }

        // Wait for all deletions to complete
        likedPubsDeletion.join()
        mediaDeletion.join()
        true
      }
    } catch (e: Exception) {
      Log.e("DELETE_PUBLICATION", "Failed to delete publication: ${e.message}")
      false
    }
  }

  override suspend fun addToFavorites(userId: String, publicationId: String) {
    try {
      firestore
          .collection("users")
          .document(userId)
          .collection("favoritePublications")
          .document(publicationId)
          .set(
              hashMapOf(
                  "publicationId" to publicationId, "timestamp" to System.currentTimeMillis()))
          .await()
    } catch (e: Exception) {
      Log.e("ADD_TO_FAVORITES", "Failed to add to favorites: ${e.message}")
      throw e
    }
  }

  override suspend fun removeFromFavorites(userId: String, publicationId: String) {
    try {
      firestore
          .collection("users")
          .document(userId)
          .collection("favoritePublications")
          .document(publicationId)
          .delete()
          .await()
    } catch (e: Exception) {
      Log.e("REMOVE_FROM_FAVORITES", "Failed to remove from favorites: ${e.message}")
      throw e
    }
  }

  override suspend fun getFavoritePublicationsIds(userId: String): List<String> {
    return try {
      firestore
          .collection("users")
          .document(userId)
          .collection("favoritePublications")
          .get()
          .await()
          .documents
          .mapNotNull { it.getString("publicationId") }
    } catch (e: Exception) {
      Log.e("GET_FAVORITES", "Failed to get favorites: ${e.message}")
      emptyList()
    }
  }

  override suspend fun isPublicationFavorited(userId: String, publicationId: String): Boolean {
    return try {
      val doc =
          firestore
              .collection("users")
              .document(userId)
              .collection("favoritePublications")
              .document(publicationId)
              .get()
              .await()
      doc.exists()
    } catch (e: Exception) {
      Log.e("CHECK_FAVORITE", "Failed to check favorite status: ${e.message}")
      false
    }
  }
}
