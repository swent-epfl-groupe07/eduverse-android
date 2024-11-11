package com.github.se.eduverse.repository

import android.net.Uri
import android.util.Log
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.google.firebase.firestore.DocumentReference
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

    suspend fun addToUserCollection(userId: String, collectionName: String, publicationId: String)

    suspend fun incrementLikes(publicationId: String, userId: String)
}

class ProfileRepositoryImpl(
    private val db: FirebaseFirestore,
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
            .document("$userId-$publicationId")
            .set(
                hashMapOf(
                    "userId" to userId,
                    "publicationId" to publicationId,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
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

    override suspend fun addToUserCollection(
        userId: String,
        collectionName: String,
        publicationId: String
    ) {
        firestore.collection("users")
            .document(userId)
            .collection(collectionName)
            .document(publicationId)
            .set(
                hashMapOf(
                    "publicationId" to publicationId,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
    }

    override suspend fun incrementLikes(publicationId: String, userId: String) {
        try {
            // Récupérer tous les documents de la collection `publications`
            val querySnapshot = db.collection("publications").get().await()

            var documentRefToUpdate: DocumentReference? = null

            // Parcourir les documents pour trouver celui avec l'attribut `id` correspondant
            for (document in querySnapshot.documents) {
                val docId = document.getString("id")
                if (docId == publicationId) {
                    documentRefToUpdate = document.reference
                    break
                }
            }

            // Si le document correspondant est trouvé, procéder à la mise à jour
            if (documentRefToUpdate != null) {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(documentRefToUpdate)
                    val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

                    // Vérifier si l'utilisateur a déjà liké
                    if (!likedBy.contains(userId)) {
                        val currentLikes = snapshot.getLong("likes") ?: 0
                        transaction.update(documentRefToUpdate, "likes", currentLikes + 1)
                        transaction.update(documentRefToUpdate, "likedBy", likedBy + userId)
                    } else {
                        Log.d("INCREMENTCHECK", "User $userId has already liked this publication.")
                    }
                }.await()
            } else {
                throw Exception("Publication not found with ID: $publicationId")
            }
        } catch (e: Exception) {
            Log.d("INCREMENTFAIIIL", "FAIIIL: ${e.message}")
        }
    }



}
