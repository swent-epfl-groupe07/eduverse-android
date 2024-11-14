package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TodoRepositoryTest {

  private lateinit var repository: TodoRepository
  private val mockFirestore: FirebaseFirestore = mock(FirebaseFirestore::class.java)
  private val mockCollectionRef: CollectionReference = mock(CollectionReference::class.java)
  private val mockDocumentRef: DocumentReference = mock(DocumentReference::class.java)
  private val mockSnapshot: QuerySnapshot = mock(QuerySnapshot::class.java)

  @Before
  fun setUp() {
    repository = TodoRepository(mockFirestore)
  }

  @Test
  fun `getNewUid should return a unique id`() {
    whenever(mockFirestore.collection("todos")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document()).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.id).thenReturn("uniqueId")

    val result = repository.getNewUid()

    assertEquals("uniqueId", result)
  }

  @Test
  fun `getActualTodos should call documents`() {
    whenever(mockFirestore.collection("todos")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("ownerId", "userId")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("status", "ACTUAL")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockSnapshot.documents).thenReturn(listOf())

    repository.getActualTodos("userId", onSuccess = {}, onFailure = { throw it })
    verify(timeout(100)) { (mockSnapshot).documents }
  }

  @Test
  fun `getDoneTodos should call documents`() {
    whenever(mockFirestore.collection("todos")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("ownerId", "userId")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("status", "DONE")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockSnapshot.documents).thenReturn(listOf())

    repository.getDoneTodos("userId", onSuccess = {}, onFailure = { throw it })
    verify(timeout(100)) { (mockSnapshot).documents }
  }

  @Test
  fun `addNewTodo should add a todo to Firestore`() {
    val newTodo = Todo("1", "Task", 60, TodoStatus.ACTUAL, "3")

    whenever(mockFirestore.collection("todos")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(newTodo.uid)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.set(newTodo)).thenReturn(Tasks.forResult(null))

    repository.addNewTodo(newTodo, onSuccess = {}, onFailure = { throw it })

    verify(mockDocumentRef).set(newTodo)
  }

  @Test
  fun `updateTodo should update a todo in Firestore`() {
    val updatedTodo = Todo("1", "Updated Task", 120, TodoStatus.ACTUAL, "3")

    whenever(mockFirestore.collection("todos")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(updatedTodo.uid)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.set(updatedTodo)).thenReturn(Tasks.forResult(null))

    repository.updateTodo(updatedTodo, onSuccess = {}, onFailure = { throw it })

    verify(mockDocumentRef).set(updatedTodo)
  }

  @Test
  fun `deleteTodoById should delete a todo from Firestore`() {
    val todoId = "1"

    whenever(mockFirestore.collection("todos")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(todoId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.delete()).thenReturn(Tasks.forResult(null))

    repository.deleteTodoById(todoId, onSuccess = {}, onFailure = { throw it })

    verify(mockDocumentRef).delete()
  }
}
