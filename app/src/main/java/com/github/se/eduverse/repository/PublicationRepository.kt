package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.Publication
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

open class PublicationRepository(private val db: FirebaseFirestore) {

  open suspend fun loadRandomPublications(limit: Long = 20): List<Publication> {
    return try {
      db.collection("publications")
          .orderBy("timestamp")
          .limit(limit)
          .get()
          .await()
          .documents
          .mapNotNull { it.toObject(Publication::class.java) }
          .shuffled()
    } catch (e: Exception) {
      emptyList()
    }
  }

    open suspend fun addComment(publicationId: String, comment: Comment) {
        try {
            // Créer une copie du commentaire sans le champ profile pour le stockage
            val commentData = comment.copy(profile = null)

            db.collection("publicationsComments") // Changement de la collection principale
                .document(publicationId)
                .collection("comments")
                .document(comment.id)
                .set(commentData)
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to add comment: ${e.message}")
        }
    }

    /**
     * Récupère les commentaires pour une publication spécifique depuis "publicationsComments/{publicationId}/comments".
     */
    open suspend fun getComments(publicationId: String): List<Comment> {
        return try {
            db.collection("publicationsComments") // Changement de la collection principale
                .document(publicationId)
                .collection("comments")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val comment = doc.toObject(Comment::class.java)
                    comment
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Like un commentaire spécifique dans "publicationsComments/{publicationId}/comments/{commentId}".
     * Note : Cette méthode est déjà redondante avec toggleLikeComment et pourrait être supprimée.
     */
    open suspend fun likeComment(publicationId: String, commentId: String) {
        try {
            val commentRef =
                db.collection("publicationsComments") // Changement de la collection principale
                    .document(publicationId)
                    .collection("comments")
                    .document(commentId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)
                val currentLikes = snapshot.getLong("likes") ?: 0
                transaction.update(commentRef, "likes", currentLikes + 1)
            }
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to like comment: ${e.message}")
        }
    }

    /**
     * Toggle le like d'un commentaire spécifique dans "publicationsComments/{publicationId}/comments/{commentId}".
     */
    open suspend fun toggleLikeComment(publicationId: String, commentId: String, userId: String) {
        try {
            val commentRef =
                db.collection("publicationsComments") // Changement de la collection principale
                    .document(publicationId)
                    .collection("comments")
                    .document(commentId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)
                val likedBy = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()
                val currentLikes = snapshot.getLong("likes") ?: 0

                if (likedBy.contains(userId)) {
                    likedBy.remove(userId)
                    transaction.update(commentRef, "likes", currentLikes - 1)
                } else {
                    likedBy.add(userId)
                    transaction.update(commentRef, "likes", currentLikes + 1)
                }
                transaction.update(commentRef, "likedBy", likedBy)
            }.await()
        } catch (e: Exception) {
            throw Exception("Failed to toggle like on comment: ${e.message}")
        }
    }

    /**
     * Supprime un commentaire spécifique de "publicationsComments/{publicationId}/comments/{commentId}".
     * Seul l'auteur du commentaire peut le supprimer.
     */
    open suspend fun deleteComment(
        publicationId: String,
        commentId: String,
        currentUserId: String
    ) {
        try {
            val commentRef =
                db.collection("publicationsComments") // Changement de la collection principale
                    .document(publicationId)
                    .collection("comments")
                    .document(commentId)

            val commentSnapshot = commentRef.get().await()
            val ownerId = commentSnapshot.getString("ownerId")

            if (ownerId == currentUserId) {
                commentRef.delete().await()
            } else {
                throw Exception("Unauthorized to delete this comment")
            }
        } catch (e: Exception) {
            throw Exception("Failed to delete comment: ${e.message}")
        }
    }
}
