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

    // Add a comment to a publication's sub-collection
    open suspend fun addComment(publicationId: String, comment: Comment) {
        try {
            db.collection("publications")
                .document(publicationId)
                .collection("comments")
                .document(comment.id)
                .set(comment)
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to add comment: ${e.message}")
        }
    }

    // Retrieve comments for a specific publication
    open suspend fun getComments(publicationId: String): List<Comment> {
        return try {
            db.collection("publications")
                .document(publicationId)
                .collection("comments")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Comment::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Like a specific comment
    open suspend fun likeComment(publicationId: String, commentId: String) {
        try {
            val commentRef = db.collection("publications")
                .document(publicationId)
                .collection("comments")
                .document(commentId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)
                val currentLikes = snapshot.getLong("likes") ?: 0
                transaction.update(commentRef, "likes", currentLikes + 1)
            }.await()
        } catch (e: Exception) {
            throw Exception("Failed to like comment: ${e.message}")
        }
    }

    // Delete a specific comment
    open suspend fun deleteComment(publicationId: String, commentId: String) {
        try {
            db.collection("publications")
                .document(publicationId)
                .collection("comments")
                .document(commentId)
                .delete()
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to delete comment: ${e.message}")
        }
    }

}
