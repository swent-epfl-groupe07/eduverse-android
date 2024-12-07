package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Comment
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class CommentsRepositoryImplTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var commentsRepository: CommentsRepository
  private val publicationId = "testPublicationId"
  private val commentId = "testCommentId"
  private val userId = "testUserId"

  @Before
  fun setUp() {
    db = mock(FirebaseFirestore::class.java)
    commentsRepository = CommentsRepositoryImpl(db)
  }

  @Test
  fun `test addComment successfully adds comment`(): Unit = runBlocking {
    // Arrange
    val comment =
        Comment(
            id = commentId,
            publicationId = publicationId,
            ownerId = userId,
            text = "Test comment",
            likes = 0,
            likedBy = emptyList(),
            profile = null)

    val documentReference = mock(DocumentReference::class.java)
    val publicationCommentsCollection = mock(CollectionReference::class.java)
    val commentsCollection = mock(CollectionReference::class.java)
    val documentTask = Tasks.forResult<Void>(null)

    `when`(db.collection("publicationsComments")).thenReturn(publicationCommentsCollection)
    `when`(publicationCommentsCollection.document(publicationId)).thenReturn(documentReference)
    `when`(documentReference.collection("comments")).thenReturn(commentsCollection)
    `when`(commentsCollection.document(comment.id)).thenReturn(documentReference)
    `when`(documentReference.set(any())).thenReturn(documentTask)

    // Act
    commentsRepository.addComment(publicationId, comment)

    // Assert
    verify(documentReference).set(comment.copy(profile = null))
  }

  @Test
  fun `test getComments successfully retrieves comments`() = runBlocking {
    // Arrange
    val publicationCommentsCollection = mock(CollectionReference::class.java)
    val documentReference = mock(DocumentReference::class.java)
    val commentsCollection = mock(CollectionReference::class.java)
    val querySnapshot = mock(QuerySnapshot::class.java)
    val documentSnapshot = mock(DocumentSnapshot::class.java)
    val comment =
        Comment(
            id = commentId,
            publicationId = publicationId,
            ownerId = userId,
            text = "Test comment",
            likes = 0,
            likedBy = emptyList(),
            profile = null)

    `when`(db.collection("publicationsComments")).thenReturn(publicationCommentsCollection)
    `when`(publicationCommentsCollection.document(publicationId)).thenReturn(documentReference)
    `when`(documentReference.collection("comments")).thenReturn(commentsCollection)
    `when`(commentsCollection.get()).thenReturn(Tasks.forResult(querySnapshot))
    `when`(querySnapshot.documents).thenReturn(listOf(documentSnapshot))
    `when`(documentSnapshot.toObject(Comment::class.java)).thenReturn(comment)

    // Act
    val comments = commentsRepository.getComments(publicationId)

    // Assert
    assertEquals(1, comments.size)
    assertEquals(comment, comments[0])
  }

  @Test
  fun `test likeComment toggles like when user has not liked before`(): Unit = runBlocking {
    // Arrange
    val documentReference = mock(DocumentReference::class.java)
    val publicationCommentsCollection = mock(CollectionReference::class.java)
    val commentsCollection = mock(CollectionReference::class.java)
    val transaction = mock(Transaction::class.java)
    val documentSnapshot = mock(DocumentSnapshot::class.java)

    val initialLikes = 0L
    val initialLikedBy = mutableListOf<String>()

    `when`(db.collection("publicationsComments")).thenReturn(publicationCommentsCollection)
    `when`(publicationCommentsCollection.document(publicationId)).thenReturn(documentReference)
    `when`(documentReference.collection("comments")).thenReturn(commentsCollection)
    `when`(commentsCollection.document(commentId)).thenReturn(documentReference)

    `when`(db.runTransaction<Void>(any())).thenAnswer { invocation ->
      val function = invocation.arguments[0] as Transaction.Function<Void>
      function.apply(transaction)
      Tasks.forResult(null)
    }

    `when`(transaction.get(documentReference)).thenReturn(documentSnapshot)
    `when`(documentSnapshot.getLong("likes")).thenReturn(initialLikes)
    `when`(documentSnapshot.get("likedBy")).thenReturn(initialLikedBy)

    // Act
    commentsRepository.likeComment(publicationId, commentId, userId)

    // Assert
    verify(transaction).update(eq(documentReference), anyMap<String, Any>())
  }

  @Test
  fun `test deleteComment successfully deletes comment`(): Unit = runBlocking {
    // Arrange
    val documentReference = mock(DocumentReference::class.java)
    val publicationCommentsCollection = mock(CollectionReference::class.java)
    val commentsCollection = mock(CollectionReference::class.java)
    val deleteTask = Tasks.forResult<Void>(null)

    `when`(db.collection("publicationsComments")).thenReturn(publicationCommentsCollection)
    `when`(publicationCommentsCollection.document(publicationId)).thenReturn(documentReference)
    `when`(documentReference.collection("comments")).thenReturn(commentsCollection)
    `when`(commentsCollection.document(commentId)).thenReturn(documentReference)
    `when`(documentReference.delete()).thenReturn(deleteTask)

    // Act
    commentsRepository.deleteComment(publicationId, commentId)

    // Assert
    verify(documentReference).delete()
  }
}
