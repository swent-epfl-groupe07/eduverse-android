package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.repository.TodoRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TodoListViewModel(private val todoRepository: TodoRepository) : ViewModel() {
  private val _actualTodos = MutableStateFlow<List<Todo>>(emptyList())
  val actualTodos: StateFlow<List<Todo>> = _actualTodos.asStateFlow()

  private val _doneTodos = MutableStateFlow<List<Todo>>(emptyList())
  val doneTodos: StateFlow<List<Todo>> = _doneTodos.asStateFlow()

  var currentUid: String? = null

  // create viewmodel factory
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodoListViewModel(TodoRepository(Firebase.firestore)) as T
          }
        }
  }

  init {
    todoRepository.init {
      currentUid = it
      getActualTodos()
      getDoneTodos()
    }
  }

  fun getNewUid(): String {
    return todoRepository.getNewUid()
  }

  fun getActualTodos() {
    currentUid?.let { uid ->
      todoRepository.getActualTodos(
          uid,
          onSuccess = { todos -> _actualTodos.value = todos },
          onFailure = { exception -> Log.e("getActualTodos", "Error getting todos", exception) })
    } ?: { _actualTodos.value = emptyList() }
  }

  fun getDoneTodos() {
    currentUid?.let { uid ->
      todoRepository.getDoneTodos(
          uid,
          onSuccess = { todos -> _doneTodos.value = todos },
          onFailure = { exception -> Log.e("getDoneTodos", "Error getting todos", exception) })
    } ?: { _doneTodos.value = emptyList() }
  }

  fun addNewTodo(name: String) {
    currentUid?.let { uid ->
      val newTodo = Todo(uid = getNewUid(), name = name, status = TodoStatus.ACTUAL, ownerId = uid)
      todoRepository.addNewTodo(
          newTodo,
          onSuccess = { getActualTodos() },
          onFailure = { exception -> Log.e("addNewTodo", "Error adding todo", exception) })
    } ?: { Log.e("addNewTodo", "Error adding todo: currentUid is null") }
  }

  /** Sets the todo state to done */
  fun setTodoDone(todo: Todo) {
    val doneTodo = todo.copy(status = TodoStatus.DONE)
    todoRepository.updateTodo(
        todo = doneTodo,
        onSuccess = {
          getActualTodos()
          getDoneTodos()
        },
        onFailure = { exception -> Log.e("setTodoDone", "Error updating todo", exception) })
  }

  /** Sets the todo state to actual */
  fun setTodoActual(todo: Todo) {
    val actualTodo = todo.copy(status = TodoStatus.ACTUAL)
    todoRepository.updateTodo(
        todo = actualTodo,
        onSuccess = {
          getActualTodos()
          getDoneTodos()
        },
        onFailure = { exception -> Log.e("setTodoActual", "Error updating todo", exception) })
  }

  fun deleteTodo(todoId: String) {
    todoRepository.deleteTodoById(
        todoId = todoId,
        onSuccess = {
          getActualTodos()
          getDoneTodos()
        },
        onFailure = { exception -> Log.e("deleteTodo", "Error deleting todo", exception) })
  }

  fun renameTodo(todo: Todo, newName: String) {
    val newTodo = todo.copy(name = newName)
    todoRepository.updateTodo(
        todo = newTodo,
        onSuccess = {
          getActualTodos()
          getDoneTodos()
        },
        onFailure = { exception -> Log.e("renameTodo", "Error renaming todo", exception) })
  }

  fun getTodoById(id: String): Todo? {
    return _actualTodos.value.find { it.uid == id } ?: _doneTodos.value.find { it.uid == id }
  }
}
