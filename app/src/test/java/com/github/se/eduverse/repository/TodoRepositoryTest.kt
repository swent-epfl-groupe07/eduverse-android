package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TodoRepositoryTest {

  private lateinit var repository: TodoRepository
  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockCollectionRef: CollectionReference
  private lateinit var mockDocumentRef: DocumentReference
  private lateinit var mockSnapshot: QuerySnapshot
  private lateinit var mockDocument1: DocumentSnapshot
  private lateinit var mockDocument2: DocumentSnapshot
  private lateinit var mockListenerRegistration: ListenerRegistration
  val userId = "user1"
  var todo1 = Todo("1", "Task 1", 60, TodoStatus.DONE, userId)
  var todo2 = Todo("2", "Task 2", 120, TodoStatus.DONE, userId)

  @Before
  fun setUp() {
    mockFirestore = mock(FirebaseFirestore::class.java)
    mockCollectionRef = mock(CollectionReference::class.java)
    mockDocumentRef = mock(DocumentReference::class.java)
    mockSnapshot = mock(QuerySnapshot::class.java)
    mockDocument1 = mock(DocumentSnapshot::class.java)
    mockDocument2 = mock(DocumentSnapshot::class.java)
    mockListenerRegistration = mock(ListenerRegistration::class.java)
    repository = TodoRepository(mockFirestore)

    whenever(mockFirestore.collection("todos")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("ownerId", userId)).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("status", "ACTUAL")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.whereEqualTo("status", "DONE")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.orderBy("creationTime", Query.Direction.ASCENDING))
        .thenReturn(mockCollectionRef)
    whenever(mockSnapshot.documents).thenReturn(listOf(mockDocument1, mockDocument2))
    whenever(mockDocument1.id).thenReturn(todo1.uid)
    whenever(mockDocument1.getString("name")).thenReturn(todo1.name)
    whenever(mockDocument1.getLong("timeSpent")).thenReturn(todo1.timeSpent)
    whenever(mockDocument1.getString("ownerId")).thenReturn(todo1.ownerId)
    whenever(mockDocument1.getDate("creationTime")).thenReturn(todo1.creationTime)
    whenever(mockDocument2.id).thenReturn(todo2.uid)
    whenever(mockDocument2.getString("name")).thenReturn(todo2.name)
    whenever(mockDocument2.getLong("timeSpent")).thenReturn(todo2.timeSpent)
    whenever(mockDocument2.getString("ownerId")).thenReturn(todo2.ownerId)
    whenever(mockDocument2.getDate("creationTime")).thenReturn(todo2.creationTime)
  }

  @Test
  fun `getNewUid should return a unique id`() {
    whenever(mockCollectionRef.document()).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.id).thenReturn("uniqueId")

    val result = repository.getNewUid()

    assertEquals("uniqueId", result)
  }

  @Test
  fun `addNewTodo should add a todo to Firestore`() {
    val newTodo = Todo("1", "Task", 60, TodoStatus.ACTUAL, "3")

    whenever(mockCollectionRef.document(newTodo.uid)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.set(newTodo)).thenReturn(Tasks.forResult(null))

    repository.addNewTodo(newTodo, onSuccess = {}, onFailure = { throw it })

    verify(mockDocumentRef).set(newTodo)
  }

  @Test
  fun `updateTodo should update a todo in Firestore`() {
    val updatedTodo = Todo("1", "Updated Task", 120, TodoStatus.ACTUAL, "3")

    whenever(mockCollectionRef.document(updatedTodo.uid)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.set(updatedTodo)).thenReturn(Tasks.forResult(null))

    repository.updateTodo(updatedTodo, onSuccess = {}, onFailure = { throw it })

    verify(mockDocumentRef).set(updatedTodo)
  }

  @Test
  fun `deleteTodoById should delete a todo from Firestore`() {
    val todoId = "1"

    whenever(mockCollectionRef.document(todoId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.delete()).thenReturn(Tasks.forResult(null))

    repository.deleteTodoById(todoId, onSuccess = {}, onFailure = { throw it })

    verify(mockDocumentRef).delete()
  }

  @Test
  fun `getActualTodos should return actual todos of user`() = runBlocking {
    todo1 = todo1.copy(status = TodoStatus.ACTUAL)
    todo2 = todo2.copy(status = TodoStatus.ACTUAL)
    whenever(mockDocument1.getString("status")).thenReturn(todo1.status.name)
    whenever(mockDocument2.getString("status")).thenReturn(todo2.status.name)
    whenever(mockCollectionRef.addSnapshotListener(any())).thenAnswer { invocation ->
      val listener = invocation.arguments[0] as EventListener<QuerySnapshot>
      listener.onEvent(mockSnapshot, null)
      mockListenerRegistration
    }

    val result = repository.getActualTodos(userId).first()

    assertEquals(listOf(todo1, todo2), result)
  }

  @Test
  fun `getActualTodos should return empty list in case of error`() = runBlocking {
    whenever(mockCollectionRef.addSnapshotListener(any())).thenAnswer { invocation ->
      val listener = invocation.arguments[0] as EventListener<QuerySnapshot>
      listener.onEvent(
          mockSnapshot,
          FirebaseFirestoreException("Error", FirebaseFirestoreException.Code.CANCELLED))
      mockListenerRegistration
    }

    val result = repository.getActualTodos(userId).first()

    assertTrue(result.isEmpty())
  }

  @Test
  fun `getActualTodos should return empty list in case of null snapshots`() = runBlocking {
    whenever(mockCollectionRef.addSnapshotListener(any())).thenAnswer { invocation ->
      val listener = invocation.arguments[0] as EventListener<QuerySnapshot>
      listener.onEvent(null, null)
      mockListenerRegistration
    }

    val result = repository.getActualTodos(userId).first()

    assertTrue(result.isEmpty())
  }

  @Test
  fun `getDoneTodos should return done todos of user`() = runBlocking {
    todo1 = todo1.copy(status = TodoStatus.DONE)
    todo2 = todo2.copy(status = TodoStatus.DONE)
    whenever(mockDocument1.getString("status")).thenReturn(todo1.status.name)
    whenever(mockDocument2.getString("status")).thenReturn(todo2.status.name)
    whenever(mockCollectionRef.addSnapshotListener(any())).thenAnswer { invocation ->
      val listener = invocation.arguments[0] as EventListener<QuerySnapshot>
      listener.onEvent(mockSnapshot, null)
      mockListenerRegistration
    }

    val result = repository.getDoneTodos(userId).first()

    assertEquals(listOf(todo1, todo2), result)
  }

  @Test
  fun `getDoneTodos should return empty list in case of error`() = runBlocking {
    whenever(mockCollectionRef.addSnapshotListener(any())).thenAnswer { invocation ->
      val listener = invocation.arguments[0] as EventListener<QuerySnapshot>
      listener.onEvent(
          mockSnapshot,
          FirebaseFirestoreException("Error", FirebaseFirestoreException.Code.CANCELLED))
      mockListenerRegistration
    }

    val result = repository.getDoneTodos(userId).first()

    assertTrue(result.isEmpty())
  }

  @Test
  fun `getDoneTodos should return empty list in case of null snapshots`() = runBlocking {
    whenever(mockCollectionRef.addSnapshotListener(any())).thenAnswer { invocation ->
      val listener = invocation.arguments[0] as EventListener<QuerySnapshot>
      listener.onEvent(null, null)
      mockListenerRegistration
    }

    val result = repository.getDoneTodos(userId).first()

    assertTrue(result.isEmpty())
  }
}
