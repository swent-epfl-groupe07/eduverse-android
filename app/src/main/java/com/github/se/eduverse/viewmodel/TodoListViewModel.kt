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
    getActualTodos()
    getDoneTodos()
  }

  fun getNewUid(): String {
    return todoRepository.getNewUid()
  }

  fun getActualTodos() {
    todoRepository.getActualTodos(
        onSuccess = { todos -> _actualTodos.value = todos },
        onFailure = { exception -> Log.e("getActualTodos", "Error getting todos", exception) })
  }

  fun getDoneTodos() {
    todoRepository.getDoneTodos(
        onSuccess = { todos -> _doneTodos.value = todos },
        onFailure = { exception -> Log.e("getDoneTodos", "Error getting todos", exception) })
  }

  fun addNewTodo(todo: Todo) {
    todoRepository.addNewTodo(
        todo = todo,
        onSuccess = { getActualTodos() },
        onFailure = { exception -> Log.e("addNewTodo", "Error adding todo", exception) })
  }

  /** Sets the todo state to done */
  fun setTodoDone(todo: Todo) {
    val doneTodo =
        Todo(uid = todo.uid, name = todo.name, timeSpent = todo.timeSpent, status = TodoStatus.DONE)
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
    val actualTodo =
        Todo(
            uid = todo.uid,
            name = todo.name,
            timeSpent = todo.timeSpent,
            status = TodoStatus.ACTUAL)
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
}
