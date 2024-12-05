package com.github.se.eduverse.fake

import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.repository.CommentsRepository

class FakeCommentsRepository : CommentsRepository {
  override suspend fun addComment(publicationId: String, comment: Comment) {}

  override suspend fun getComments(publicationId: String): List<Comment> {
    return emptyList()
  }

  override suspend fun likeComment(publicationId: String, commentId: String, userId: String) {}

  override suspend fun deleteComment(publicationId: String, commentId: String) {}
}
