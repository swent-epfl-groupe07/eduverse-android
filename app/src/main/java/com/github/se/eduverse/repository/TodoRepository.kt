package com.github.se.eduverse.repository

import android.util.Log
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

open class TodoRepository(private val db: FirebaseFirestore) {
  private val collectionPath = "todos"

  open fun init(onAuthStateChanged: (userId: String?) -> Unit) {
    Firebase.auth.addAuthStateListener {
      if (it.currentUser != null) {
        onAuthStateChanged(it.currentUser!!.uid)
      } else {
        onAuthStateChanged(null)
      }
    }
  }

  /**
   * Get a new unique id for a todo
   *
   * @return a unique id in the todo collection of the database
   */
  open fun getNewUid(): String {
    return db.collection(collectionPath).document().id
  }

  /**
   * Get the list pending todos from the database
   *
   * @param onSuccess code executed if the todos are successfully accessed
   * @param onFailure code executed if the todos can't be accessed
   */
  open fun getActualTodos(
      userId: String,
      onSuccess: (List<Todo>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    db.collection(collectionPath)
        .whereEqualTo("ownerId", userId)
        .whereEqualTo("status", "ACTUAL")
        .orderBy(
            "creationTime",
            Query.Direction
                .ASCENDING) // makes sure the newly added todos are displayed at the bottom
        .get()
        .addOnSuccessListener { result ->
          val todos = result.documents.mapNotNull { document -> decodeTodo(document) }
          onSuccess(todos)
        }
        .addOnFailureListener { onFailure(it) }
  }

  /**
   * Get the list of done todos from the database
   *
   * @param onSuccess code executed if the todos are successfully accessed
   * @param onFailure code executed if the todos can't be accessed
   */
  open fun getDoneTodos(
      userId: String,
      onSuccess: (List<Todo>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    db.collection(collectionPath)
        .whereEqualTo("ownerId", userId)
        .whereEqualTo("status", "DONE")
        .orderBy("creationTime", Query.Direction.ASCENDING)
        .get()
        .addOnSuccessListener { result ->
          val todos = result.documents.mapNotNull { document -> decodeTodo(document) }
          onSuccess(todos)
        }
        .addOnFailureListener { onFailure(it) }
  }

  /**
   * Add a new todo to the database
   *
   * @param todo the todo to add
   * @param onSuccess code executed if the todo is successfully added
   * @param onFailure code executed if the todo can't be added
   */
  open fun addNewTodo(todo: Todo, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    db.collection(collectionPath)
        .document(todo.uid)
        .set(todo)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
  }

  /**
   * Update a todo in the database
   *
   * @param todo the todo to update
   * @param onSuccess code executed if the todo is successfully updated
   * @param onFailure code executed if the todo can't be updated
   */
  open fun updateTodo(todo: Todo, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    db.collection(collectionPath)
        .document(todo.uid)
        .set(todo)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
  }

  /**
   * Delete a todo from the database
   *
   * @param todoId the id of the todo to delete
   * @param onSuccess code executed if the todo is successfully deleted
   * @param onFailure code executed if the todo can't be deleted
   */
  open fun deleteTodoById(todoId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    db.collection(collectionPath)
        .document(todoId)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
  }

  /**
   * Decodes a Firestore document into a ToDo object.
   *
   * @param document The Firestore document to decode.
   * @return The ToDo object.
   */
  private fun decodeTodo(document: DocumentSnapshot): Todo? {
    return try {
      val uid = document.id
      val name = document.getString("name") ?: return null
      val timeSpent = document.getLong("timeSpent") ?: return null
      val statusString = document.getString("status") ?: return null
      val status = TodoStatus.valueOf(statusString)
      val ownerId = document.getString("ownerId") ?: return null
      val creationTime = document.getDate("creationTime") ?: return null

      Todo(
          uid = uid,
          name = name,
          timeSpent = timeSpent,
          status = status,
          ownerId = ownerId,
          creationTime = creationTime)
    } catch (e: Exception) {
      Log.e("TodosRepositoryFirestore", "Error converting document to ToDo", e)
      null
    }
  }
}
