import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class PublicationRepositoryTest {

  private lateinit var firestoreMock: FirebaseFirestore
  private lateinit var repository: PublicationRepository

  @Before
  fun setUp() {
    firestoreMock = mock(FirebaseFirestore::class.java)
    repository = PublicationRepository(firestoreMock)
  }

  @Test(timeout = 3000)
  fun `loadRandomPublications returns empty list on success`() =
      runBlocking(Dispatchers.IO) {
        // Arrange
        val mockCollection = mock(CollectionReference::class.java)
        val mockQuery = mock(Query::class.java)
        val mockQuerySnapshot = mock(QuerySnapshot::class.java)

        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.orderBy("timestamp")).thenReturn(mockQuery)
        `when`(mockQuery.limit(20)).thenReturn(mockQuery)
        `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
        `when`(mockQuerySnapshot.documents).thenReturn(listOf())

        // Act
        val result = repository.loadRandomPublications()

        // Assert
        assertTrue(result.isEmpty())
      }

  @Test(timeout = 3000)
  fun `loadRandomPublications returns list of publications when documents are available`() =
      runBlocking(Dispatchers.IO) {
        // Arrange
        val mockCollection = mock(CollectionReference::class.java)
        val mockQuery = mock(Query::class.java)
        val mockQuerySnapshot = mock(QuerySnapshot::class.java)

        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.orderBy("timestamp")).thenReturn(mockQuery)
        `when`(mockQuery.limit(20)).thenReturn(mockQuery)
        `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))

        // Créons des publications simulées sous forme de DocumentSnapshot
        val publicationList =
            listOf(
                Publication(
                    id = "id1",
                    userId = "user1",
                    title = "title1",
                    thumbnailUrl = "thumbnail1",
                    mediaUrl = "media1",
                    mediaType = MediaType.PHOTO,
                    timestamp = 123456L),
                Publication(
                    id = "id2",
                    userId = "user2",
                    title = "title2",
                    thumbnailUrl = "thumbnail2",
                    mediaUrl = "media2",
                    mediaType = MediaType.VIDEO,
                    timestamp = 123457L))

        val documentSnapshots =
            publicationList.map { publication ->
              mock(com.google.firebase.firestore.DocumentSnapshot::class.java).apply {
                `when`(toObject(Publication::class.java)).thenReturn(publication)
              }
            }

        `when`(mockQuerySnapshot.documents).thenReturn(documentSnapshots)

        // Act
        val result = repository.loadRandomPublications()

        // Assert
        assertTrue(result.size == 2)
        assertTrue(result[0].title == "title1" || result[1].title == "title1")
        assertTrue(result[0].title == "title2" || result[1].title == "title2")
      }

  @Test(timeout = 3000)
  fun `loadRandomPublications returns empty list on exception`() =
      runBlocking(Dispatchers.IO) {
        // Arrange
        val mockCollection = mock(CollectionReference::class.java)
        val mockQuery = mock(Query::class.java)

        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.orderBy("timestamp")).thenReturn(mockQuery)
        `when`(mockQuery.limit(20)).thenReturn(mockQuery)
        `when`(mockQuery.get()).thenThrow(RuntimeException("Firestore error"))

        // Act
        val result = repository.loadRandomPublications()

        // Assert
        assertTrue(result.isEmpty())
      }

    @Test(timeout = 3000)
    fun `addComment adds a comment successfully`(): Unit = runBlocking(Dispatchers.IO) {
        // Arrange
        val publicationId = "testPublication"
        val comment = Comment(
            id = "comment1",
            publicationId = publicationId,
            ownerId = "user1",
            text = "Nice post!",
            likes = 0
        )

        val mockCollection = mock(CollectionReference::class.java)
        val mockDocument = mock(DocumentReference::class.java)

        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.document(publicationId)).thenReturn(mockDocument)
        `when`(mockDocument.collection("comments")).thenReturn(mockCollection)
        `when`(mockCollection.document(comment.id)).thenReturn(mockDocument)
        `when`(mockDocument.set(comment)).thenReturn(Tasks.forResult(null))

        // Act
        repository.addComment(publicationId, comment)

        // Assert
        verify(mockDocument).set(comment)
    }

    @Test(timeout = 3000)
    fun `getComments retrieves a list of comments`() = runBlocking(Dispatchers.IO) {
        // Arrange
        val publicationId = "testPublication"

        val mockCollection = mock(CollectionReference::class.java)
        val mockDocument = mock(com.google.firebase.firestore.DocumentReference::class.java)
        val mockCommentsCollection = mock(CollectionReference::class.java)
        val mockQuerySnapshot = mock(QuerySnapshot::class.java)

        val commentList = listOf(
            Comment(
                id = "comment1",
                publicationId = publicationId,
                ownerId = "user1",
                text = "Great!",
                likes = 10
            ),
            Comment(
                id = "comment2",
                publicationId = publicationId,
                ownerId = "user2",
                text = "Amazing!",
                likes = 5
            )
        )

        val documentSnapshots = commentList.map { comment ->
            mock(com.google.firebase.firestore.DocumentSnapshot::class.java).apply {
                `when`(toObject(Comment::class.java)).thenReturn(comment)
            }
        }

        // Mocking Firestore interactions
        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.document(publicationId)).thenReturn(mockDocument)
        `when`(mockDocument.collection("comments")).thenReturn(mockCommentsCollection)
        `when`(mockCommentsCollection.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
        `when`(mockQuerySnapshot.documents).thenReturn(documentSnapshots)

        // Act
        val result = repository.getComments(publicationId)

        // Assert
        assertTrue(result.size == 2)
        assertTrue(result.any { it.text == "Great!" })
        assertTrue(result.any { it.text == "Amazing!" })
    }

    @Test(timeout = 3000)
    fun `likeComment increments the like count of a comment`(): Unit = runBlocking(Dispatchers.IO) {
        // Arrange
        val publicationId = "testPublication"
        val commentId = "comment1"

        val mockCollection = mock(CollectionReference::class.java)
        val mockDocument = mock(DocumentReference::class.java)
        val mockCommentsCollection = mock(CollectionReference::class.java)
        val mockCommentDocument = mock(DocumentReference::class.java)
        val mockSnapshot = mock(DocumentSnapshot::class.java)

        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.document(publicationId)).thenReturn(mockDocument)
        `when`(mockDocument.collection("comments")).thenReturn(mockCommentsCollection)
        `when`(mockCommentsCollection.document(commentId)).thenReturn(mockCommentDocument)
        `when`(mockCommentDocument.get()).thenReturn(Tasks.forResult(mockSnapshot))
        `when`(mockSnapshot.getLong("likes")).thenReturn(5)

        // Mocking Firestore transaction
        `when`(firestoreMock.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
            val transactionFunction = invocation.arguments[0] as Transaction.Function<Void>
            transactionFunction.apply {
                // Simulating the transaction function execution
                mock(Transaction::class.java)
            }
            Tasks.forResult<Void>(null)
        }

        // Act
        repository.likeComment(publicationId, commentId)

        // Assert
        verify(firestoreMock).runTransaction(any<Transaction.Function<Void>>())
    }

    @Test(timeout = 3000)
    fun `deleteComment removes a comment successfully`(): Unit = runBlocking(Dispatchers.IO) {
        // Arrange
        val publicationId = "testPublication"
        val commentId = "comment1"

        val mockCollection = mock(CollectionReference::class.java)
        val mockDocument = mock(DocumentReference::class.java)
        val mockCommentsCollection = mock(CollectionReference::class.java)
        val mockCommentDocument = mock(DocumentReference::class.java)

        // Mocking Firestore structure
        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.document(publicationId)).thenReturn(mockDocument)
        `when`(mockDocument.collection("comments")).thenReturn(mockCommentsCollection)
        `when`(mockCommentsCollection.document(commentId)).thenReturn(mockCommentDocument)
        `when`(mockCommentDocument.delete()).thenReturn(Tasks.forResult(null))

        // Act
        repository.deleteComment(publicationId, commentId)

        // Assert
        verify(mockCommentDocument).delete()
    }
}
