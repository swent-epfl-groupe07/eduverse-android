package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Widget
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DashboardRepositoryTest {

  private lateinit var repository: DashboardRepositoryImpl
  private val mockFirestore: FirebaseFirestore = mock(FirebaseFirestore::class.java)
  private val mockCollectionRef: CollectionReference = mock(CollectionReference::class.java)
  private val mockDocumentRef: DocumentReference = mock(DocumentReference::class.java)
  private val mockSnapshot: QuerySnapshot = mock(QuerySnapshot::class.java)

  @Before
  fun setUp() {
    repository = DashboardRepositoryImpl(mockFirestore)
  }

  @Test
  fun `getWidgets should return a flow of widgets`() = runBlocking {
    val widgetList =
        listOf(
            Widget("1", "Type 1", "Title 1", "Content 1", "owner1"),
            Widget("2", "Type 2", "Title 2", "Content 2", "owner2"))

    // Mock Firestore collection and document references
    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document("userId")).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("widgets")).thenReturn(mockCollectionRef)

    // Mock the Firestore snapshot documents
    val mockDocumentSnapshots =
        widgetList.map { widget ->
          val mockSnapshot = mock(DocumentSnapshot::class.java)
          whenever(mockSnapshot.toObject(Widget::class.java)).thenReturn(widget)
          mockSnapshot
        }

    // Mock the Firestore snapshot result
    whenever(mockSnapshot.documents).thenReturn(mockDocumentSnapshots)

    // Mock the SnapshotListener to simulate the callback
    whenever(mockCollectionRef.addSnapshotListener(any())).thenAnswer { invocation ->
      val listener = invocation.getArgument(0) as EventListener<QuerySnapshot>
      listener.onEvent(mockSnapshot, null) // Pass the mock snapshot
      mock(ListenerRegistration::class.java)
    }

    // Test the method
    val result = repository.getWidgets("userId").first()

    // Verify that the correct Firestore methods were called
    verify(mockFirestore).collection("users")
    verify(mockCollectionRef).document("userId")
    verify(mockDocumentRef).collection("widgets")

    // Assert the result matches the mocked widget list
    assertEquals(widgetList, result)
  }

  @Test
  fun `addWidget should add widget to Firestore`(): Unit = runBlocking {
    val newWidget = Widget("1", "Type", "Title", "Content", "ownerId")

    // Mock Firestore interactions
    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document("userId")).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("widgets")).thenReturn(mockCollectionRef)

    // Call the addWidget method
    repository.addWidget("userId", newWidget)

    // Verify that the Firestore add method is called
    verify(mockCollectionRef).add(newWidget)
  }

  @Test
  fun `removeWidget should remove widget from Firestore`(): Unit = runBlocking {
    val widgetId = "widgetId"

    // Mock Firestore interactions
    whenever(mockFirestore.collection("users")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document("userId")).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.collection("widgets")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(widgetId)).thenReturn(mockDocumentRef)

    // Call the removeWidget method
    repository.removeWidget("userId", widgetId)

    // Verify that the Firestore delete method is called
    verify(mockDocumentRef).delete()
  }
}
