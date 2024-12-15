package com.github.se.eduverse.repository

import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.eq
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class PublicationRepositoryTest {

  private lateinit var firestoreMock: FirebaseFirestore
  private lateinit var repository: PublicationRepository

  private lateinit var mockCollectionReference: CollectionReference
  private lateinit var mockQuery: Query
  private lateinit var mockQuerySnapshot: QuerySnapshot
    @Mock private lateinit var mockDocumentSnapshot1: DocumentSnapshot

    @Mock private lateinit var mockDocumentSnapshot2: DocumentSnapshot

    @Before
    fun setUp() {
        firestoreMock = mock(FirebaseFirestore::class.java)
        repository = PublicationRepository(firestoreMock)

        mockCollectionReference = mock(CollectionReference::class.java)
        mockQuery = mock(Query::class.java)
        mockQuerySnapshot = mock(QuerySnapshot::class.java)

        `when`(firestoreMock.collection("publications")).thenReturn(mockCollectionReference)
        `when`(mockCollectionReference.orderBy("id")).thenReturn(mockQuery)
        `when`(mockQuery.startAt(anyString())).thenReturn(mockQuery)
        `when`(mockQuery.limit(20)).thenReturn(mockQuery)
        `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    MockitoAnnotations.openMocks(this)
    }

  /** Test Case 1: loadRandomPublications returns empty list on success */
  @Test(timeout = 3000)
  fun `loadRandomPublications returns empty list on success`() =
      runBlocking(Dispatchers.IO) {
        // Arrange
        val mockCollection = mock(CollectionReference::class.java)
        val mockQuery = mock(Query::class.java)
        val mockQuerySnapshot = mock(QuerySnapshot::class.java)

        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.orderBy("id")).thenReturn(mockQuery)
        `when`(mockQuery.startAt(anyString())).thenReturn(mockQuery)
        `when`(mockQuery.limit(20)).thenReturn(mockQuery)
        `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
        `when`(mockQuerySnapshot.documents).thenReturn(listOf())

        // Act
        val result = repository.loadRandomPublications()

        // Assert
        assertTrue(result.isEmpty())
      }

  /**
   * Test Case 2: loadRandomPublications returns list of publications when documents are available
   */
  @Test(timeout = 3000)
  fun `loadRandomPublications returns list of publications when documents are available`() =
      runBlocking(Dispatchers.IO) {
        // Arrange
        val mockCollection = mock(CollectionReference::class.java)
        val mockQuery = mock(Query::class.java)
        val mockQuerySnapshot = mock(QuerySnapshot::class.java)

        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.orderBy("id")).thenReturn(mockQuery)
        `when`(mockQuery.startAt(anyString())).thenReturn(mockQuery)
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
              mock(DocumentSnapshot::class.java).apply {
                `when`(toObject(Publication::class.java)).thenReturn(publication)
              }
            }

        `when`(mockQuerySnapshot.documents).thenReturn(documentSnapshots)

        // Act
        val result = repository.loadRandomPublications()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.containsAll(publicationList))
      }

  /** Test Case 3: loadRandomPublications returns empty list on exception */
  @Test(timeout = 3000)
  fun `loadRandomPublications returns empty list on exception`() =
      runBlocking(Dispatchers.IO) {
        // Arrange
        val mockCollection = mock(CollectionReference::class.java)
        val mockQuery = mock(Query::class.java)

        `when`(firestoreMock.collection("publications")).thenReturn(mockCollection)
        `when`(mockCollection.orderBy("id")).thenReturn(mockQuery)
        `when`(mockQuery.startAt(anyString())).thenReturn(mockQuery)
        `when`(mockQuery.limit(20)).thenReturn(mockQuery)
        `when`(mockQuery.get()).thenThrow(RuntimeException("Firestore error"))

        // Act
        val result = repository.loadRandomPublications()

        // Assert
        assertTrue(result.isEmpty())
      }

  @Test(timeout = 3000)
  fun `loadRandomPublications filter with userId if not null`() =
      runBlocking(Dispatchers.IO) {
        val userIds = listOf("userId")
        `when`(mockQuery.whereIn(eq("userId"), eq(userIds))).thenReturn(mockQuery)

        val publicationList =
            listOf(
                Publication(
                    id = "id1",
                    userId = "userId",
                    title = "title1",
                    thumbnailUrl = "thumbnail1",
                    mediaUrl = "media1",
                    mediaType = MediaType.PHOTO,
                    timestamp = 123456L),
                Publication(
                    id = "id2",
                    userId = "userId",
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
        val result = repository.loadRandomPublications(userIds)

        // Assert
        assertTrue(result.size == 2)
        assertTrue(result[0].title == "title1" || result[1].title == "title1")
        assertTrue(result[0].title == "title2" || result[1].title == "title2")
      }

  /** Test Case 1: loadCachePublications retrieves and caches publications */
  @Test
  fun `loadCachePublications retrieves and caches publications`(): Unit = runBlocking {
    // Arrange
    val publication1 =
        Publication(
            id = "id1",
            userId = "user1",
            title = "title1",
            thumbnailUrl = "thumbnail1",
            mediaUrl = "media1",
            mediaType = MediaType.PHOTO,
            timestamp = 123456L)
    val publication2 =
        Publication(
            id = "id2",
            userId = "user2",
            title = "title2",
            thumbnailUrl = "thumbnail2",
            mediaUrl = "media2",
            mediaType = MediaType.VIDEO,
            timestamp = 123457L)
    val limit = 50L

    // Mocking Firestore interactions for loadCachePublications
    `when`(firestoreMock.collection("publications")).thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.orderBy("id")).thenReturn(mockQuery)
    `when`(mockQuery.limit(limit)).thenReturn(mockQuery)
    `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    `when`(mockQuerySnapshot.documents)
        .thenReturn(listOf(mockDocumentSnapshot1, mockDocumentSnapshot2))
    `when`(mockDocumentSnapshot1.toObject(Publication::class.java)).thenReturn(publication1)
    `when`(mockDocumentSnapshot2.toObject(Publication::class.java)).thenReturn(publication2)

    // Act
    val result = repository.loadCachePublications()

    // Assert
    assertNotNull(result)
    assertEquals(2, result.size)
    assertTrue(result.containsAll(listOf(publication1, publication2)))

    // Verify Firestore interactions
    verify(firestoreMock).collection("publications")
    verify(mockCollectionReference).orderBy("id")
    verify(mockQuery).limit(limit)
    verify(mockQuery).get()

    // Optionally, verify that Log.d was called
    // This requires a logging framework that can be mocked or observed, which is not shown here
  }

  /** Test Case 2: loadCachePublications returns empty list on exception */
  @Test
  fun `loadCachePublications returns empty list on exception`(): Unit = runBlocking {
    // Arrange
    val limit = 50L

    // Mocking Firestore interactions to throw an exception
    `when`(firestoreMock.collection("publications")).thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.orderBy("id")).thenReturn(mockQuery)
    `when`(mockQuery.limit(limit)).thenReturn(mockQuery)
    `when`(mockQuery.get())
        .thenReturn(Tasks.forException<QuerySnapshot>(RuntimeException("Firestore error")))

    // Act
    val result = repository.loadCachePublications()

    // Assert
    assertNotNull(result)
    assertTrue(result.isEmpty())

    // Verify Firestore interactions
    verify(firestoreMock).collection("publications")
    verify(mockCollectionReference).orderBy("id")
    verify(mockQuery).limit(limit)
    verify(mockQuery).get()
  }
}
