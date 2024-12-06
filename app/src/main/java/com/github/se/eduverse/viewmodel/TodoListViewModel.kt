package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.repository.TodoRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodoListViewModel(private val todoRepository: TodoRepository) : ViewModel() {

  private val _actualTodos = MutableStateFlow<List<Todo>>(emptyList())
  val actualTodos: StateFlow<List<Todo>> = _actualTodos.asStateFlow()

  private val _doneTodos = MutableStateFlow<List<Todo>>(emptyList())
  val doneTodos: StateFlow<List<Todo>> = _doneTodos.asStateFlow()

  private val _selectedTodo = MutableStateFlow<Todo?>(null)
  val selectedTodo: StateFlow<Todo?> = _selectedTodo.asStateFlow()

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
      setTodoListsObservation() // observe the user's todos for real-time updates
    }
  }

  fun getNewUid(): String {
    return todoRepository.getNewUid()
  }

  /**
   * Adds a new todo with the given name
   *
   * @param name the name of the new todo
   */
  fun addNewTodo(name: String) {
    currentUid?.let { uid ->
      val newTodo = Todo(uid = getNewUid(), name = name, status = TodoStatus.ACTUAL, ownerId = uid)
      todoRepository.addNewTodo(
          newTodo,
          onSuccess = { Log.d("addNewTodo", "Todo added successfully to the database") },
          onFailure = { exception ->
            Log.e("addNewTodo", "Error adding todo to the database", exception)
          })
    } ?: { Log.e("addNewTodo", "Error adding todo: currentUid is null") }
  }

  /**
   * Sets the todo state to done
   *
   * @param todo the todo to set to done
   */
  fun setTodoDone(todo: Todo) {
    val doneTodo = todo.copy(status = TodoStatus.DONE)
    todoRepository.updateTodo(
        todo = doneTodo,
        onSuccess = { Log.d("setTodoDone", "Todo updated successfully to the database") },
        onFailure = { exception ->
          Log.e("setTodoDone", "Error updating todo in the database", exception)
        })
    if (!checkSelectedTodoValidity(todo.uid)) {
      _selectedTodo.value = null
    }
  }

  /**
   * Sets the todo state to done from the pomodoro screen (since setTodoDone set selectedTodo to
   * null and pomodoro screen needs to update the todo time spent before unselecting it)
   *
   * @param todo the todo to set to done
   */
  fun setTodoDoneFromPomodoro(todo: Todo) {
    val doneTodo = todo.copy(status = TodoStatus.DONE)
    todoRepository.updateTodo(
        todo = doneTodo,
        onSuccess = {
          Log.d("setTodoDoneFromPomodoro", "Todo updated successfully to the database")
        },
        onFailure = { exception ->
          Log.e("setTodoDone", "Error updating todo in the database", exception)
        })
  }

  /**
   * Sets the todo state to actual
   *
   * @param todo the todo to set to actual
   */
  fun setTodoActual(todo: Todo) {
    val actualTodo = todo.copy(status = TodoStatus.ACTUAL)
    todoRepository.updateTodo(
        todo = actualTodo,
        onSuccess = { Log.d("setTodoActual", "Todo updated successfully to the database") },
        onFailure = { exception ->
          Log.e("setTodoActual", "Error updating todo in the database", exception)
        })
  }

  /**
   * Deletes the todo that has the given id
   *
   * @param todoId the id of the todo to delete
   */
  fun deleteTodo(todoId: String) {
    todoRepository.deleteTodoById(
        todoId = todoId,
        onSuccess = { Log.d("deleteTodo", "Todo deleted successfully from the database") },
        onFailure = { exception ->
          Log.e("deleteTodo", "Error deleting todo from the database", exception)
        })
    if (!checkSelectedTodoValidity(todoId)) {
      _selectedTodo.value = null
    }
  }

  /**
   * Renames a todo to the given name
   *
   * @param todo the todo to rename
   * @param newName the new name of the todo
   */
  fun renameTodo(todo: Todo, newName: String) {
    val newTodo = todo.copy(name = newName)
    todoRepository.updateTodo(
        todo = newTodo,
        onSuccess = { Log.d("renameTodo", "Todo updated successfully to the database") },
        onFailure = { exception ->
          Log.e("renameTodo", "Error updating todo in the database", exception)
        })
    if (!checkSelectedTodoValidity(todo.uid)) {
      _selectedTodo.value = newTodo
    }
  }

  /**
   * Gets a todo given its id
   *
   * @param id the id of the todo to get
   * @return the todo with the given id, or null if no todo with that id is found
   */
  fun getTodoById(id: String): Todo? {
    return _actualTodos.value.find { it.uid == id } ?: _doneTodos.value.find { it.uid == id }
  }

  /**
   * Used to select a todo from the list of todos
   *
   * @param todo the selected todo
   */
  fun selectTodo(todo: Todo) {
    _selectedTodo.value = todo
  }

  /** Used to unselect the todo that is currently selected */
  fun unselectTodo() {
    _selectedTodo.value = null
  }

  /**
   * Updates of the time spent attribute of the given todo
   *
   * @param todo the todo to update the time spent
   * @param time the new time spent value
   */
  fun updateTodoTimeSpent(todo: Todo, time: Long) {
    val newTodo = todo.copy(timeSpent = time)
    todoRepository.updateTodo(
        todo = newTodo,
        onSuccess = { Log.d("updateTodoTimeSpent", "Todo updated successfully to the database") },
        onFailure = { exception ->
          Log.e("updateTodoTimeSpent", "Error updating todo in the database", exception)
        })
  }

  /** Observes the user's "actual" and "done" todo lists for real-time updates. */
  private fun setTodoListsObservation() {
    viewModelScope.launch {
      currentUid?.let { uid ->
        launch { todoRepository.getActualTodos(uid).collect { _actualTodos.value = it } }
        launch { todoRepository.getDoneTodos(uid).collect { _doneTodos.value = it } }
      }
          ?: run {
            _actualTodos.value = emptyList()
            _doneTodos.value = emptyList()
            Log.e("setTodoListsListenesr", "Error setting todo lists listeners: currentUid is null")
          }
    }
  }

  /**
   * Helper function to check if the selected todo is still valid when the deleteTodo function is
   * called or the setTodoDone function is called, it checks whether the selected todo is the same
   * as the todo that is being deleted or set to done, and if it is, it sets the selected todo to
   * null as in both cases the todo is no longer available to work on
   */
  private fun checkSelectedTodoValidity(todoId: String): Boolean {
    _selectedTodo.value?.let { selectedTodo ->
      return selectedTodo.uid != todoId
    } ?: return true
  }
}
