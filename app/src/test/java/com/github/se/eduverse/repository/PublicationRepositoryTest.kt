package com.github.se.eduverse.repository

import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

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
        `when`(mockCollection.orderBy("id")).thenReturn(mockQuery)
        `when`(mockQuery.startAt(anyString())).thenReturn(mockQuery)
        `when`(mockQuery.limit(20)).thenReturn(mockQuery)
        `when`(mockQuery.get()).thenThrow(RuntimeException("Firestore error"))

        // Act
        val result = repository.loadRandomPublications()

        // Assert
        assertTrue(result.isEmpty())
      }
}
