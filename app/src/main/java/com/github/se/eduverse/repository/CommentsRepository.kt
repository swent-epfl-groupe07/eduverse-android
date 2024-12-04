// Package: com.github.se.eduverse.repository

package com.github.se.eduverse.repository

import android.util.Log
import com.github.se.eduverse.model.Comment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface CommentsRepository {
  suspend fun addComment(publicationId: String, comment: Comment)

  suspend fun getComments(publicationId: String): List<Comment>

  suspend fun likeComment(publicationId: String, commentId: String, userId: String)

  suspend fun deleteComment(publicationId: String, commentId: String)
}

class CommentsRepositoryImpl(private val db: FirebaseFirestore) : CommentsRepository {

  override suspend fun addComment(publicationId: String, comment: Comment) {
    try {
      Log.d("CommentsRepository", "Attempting to add comment: $comment")
      val commentData = comment.copy(profile = null)
      db.collection("publicationsComments")
          .document(publicationId)
          .collection("comments")
          .document(comment.id)
          .set(commentData)
          .await()
      Log.d("CommentsRepository", "Successfully added comment: ${comment.id}")
    } catch (e: Exception) {
      Log.e("CommentsRepository", "Failed to add comment: ${e.message}")
      throw Exception("Failed to add comment: ${e.message}")
    }
  }

  override suspend fun getComments(publicationId: String): List<Comment> {
    return try {
      Log.d("CommentsRepository", "Fetching comments for publicationId: $publicationId")
      val snapshot =
          db.collection("publicationsComments")
              .document(publicationId)
              .collection("comments")
              .get()
              .await()

      val comments = snapshot.documents.mapNotNull { it.toObject(Comment::class.java) }
      Log.d("CommentsRepository", "Fetched ${comments.size} comments from Firestore")
      comments
    } catch (e: Exception) {
      Log.e("CommentsRepository", "Error fetching comments: ${e.message}")
      emptyList()
    }
  }

  override suspend fun likeComment(publicationId: String, commentId: String, userId: String) {
    try {
      val commentRef =
          db.collection("publicationsComments")
              .document(publicationId)
              .collection("comments")
              .document(commentId)

      db.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val likedBy =
                (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                    ?: mutableListOf()
            val currentLikes = snapshot.getLong("likes") ?: 0

            if (likedBy.contains(userId)) {
              // The user has already liked the comment, removing the like
              likedBy.remove(userId)
              transaction.update(
                  commentRef, mapOf("likes" to currentLikes - 1, "likedBy" to likedBy))
            } else {
              // The user hasn't liked the comment yet
              likedBy.add(userId)
              transaction.update(
                  commentRef, mapOf("likes" to currentLikes + 1, "likedBy" to likedBy))
            }
          }
          .await()
      Log.d("CommentsRepository", "Successfully toggled like for comment: $commentId")
    } catch (e: Exception) {
      Log.e("CommentsRepository", "Failed to toggle like on comment: ${e.message}")
      throw Exception("Failed to like/unlike comment: ${e.message}")
    }
  }

  override suspend fun deleteComment(publicationId: String, commentId: String) {
    try {
      db.collection("publicationsComments")
          .document(publicationId)
          .collection("comments")
          .document(commentId)
          .delete()
          .await()
      Log.d("CommentsRepository", "Successfully deleted comment: $commentId")
    } catch (e: Exception) {
      Log.e("CommentsRepository", "Failed to delete comment: ${e.message}")
      throw Exception("Failed to delete comment: ${e.message}")
    }
  }
}
