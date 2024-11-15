package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.repository.TodoRepository
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsNull.nullValue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

class TodoListViewModelTest {
  private lateinit var viewModel: TodoListViewModel
  private lateinit var mockRepository: TodoRepository

  private val todo =
      Todo(uid = "1", name = "Task", timeSpent = 60, status = TodoStatus.ACTUAL, ownerId = "3")

  @Before
  fun setUp() {
    mockRepository = mock(TodoRepository::class.java)
    `when`(mockRepository.init(any())).then { it.getArgument<(String?) -> Unit>(0)("3") }
    viewModel = TodoListViewModel(mockRepository)
  }

  @Test
  fun getNewUid() {
    `when`(mockRepository.getNewUid()).thenReturn("uid")
    assertThat(viewModel.getNewUid(), `is`("uid"))
  }

  @Test
  fun getActualTodosCallsRepository() {
    viewModel.getActualTodos()
    verify(mockRepository, times(2)).getActualTodos(eq("3"), any(), any())
  }

  @Test
  fun getDoneTodosCallsRepository() {
    viewModel.getDoneTodos()
    verify(mockRepository, times(2)).getDoneTodos(eq("3"), any(), any())
  }

  @Test
  fun addNewTodoCallsRepositoryWhenCurrentUidIsNotNull() {
    `when`(mockRepository.getNewUid()).thenReturn("uid")
    viewModel.addNewTodo("new Todo")
    val newTodo =
        Todo(
            uid = "uid",
            name = "new Todo",
            timeSpent = 0,
            status = TodoStatus.ACTUAL,
            ownerId = "3")
    verify(mockRepository).addNewTodo(eq(newTodo), any(), any())
  }

  @Test
  fun addNewTodoDoesNotCallRepositoryWhenCurrentUidIsNull() {
    viewModel.currentUid = null
    viewModel.addNewTodo("new Todo")
    verify(mockRepository, never()).addNewTodo(any(), any(), any())
  }

  @Test
  fun setTodoDoneCallsRepositoryWithCorrectlyUpdatedTodo() {
    viewModel.setTodoDone(todo)
    val doneTodo = todo.copy(status = TodoStatus.DONE)
    verify(mockRepository).updateTodo(eq(doneTodo), any(), any())
  }

  @Test
  fun setTodoActualCallsRepository() {
    viewModel.setTodoActual(todo)
    val actualTodo = todo.copy(status = TodoStatus.ACTUAL)
    verify(mockRepository).updateTodo(eq(actualTodo), any(), any())
  }

  @Test
  fun deleteTodoCallsRepository() {
    viewModel.deleteTodo("1")
    verify(mockRepository).deleteTodoById(eq("1"), any(), any())
  }

  @Test
  fun renameTodoCallsRepositoryWithCorrectlyUpdatedTodo() {
    val newTodoName = "new name"
    viewModel.renameTodo(todo, newTodoName)
    val renamedTodo = todo.copy(name = newTodoName)
    verify(mockRepository).updateTodo(eq(renamedTodo), any(), any())
  }

  @Test
  fun selectTodoUpdatesSelectedTodo() {
    viewModel.selectTodo(todo)
    assertThat(viewModel.selectedTodo.value, `is`(todo))
  }

  @Test
  fun unselectTodoSetsSelectedTodoToNull() {
    viewModel.selectTodo(todo)
    viewModel.unselectTodo()
    assertThat(viewModel.selectedTodo.value, `is`(nullValue()))
  }

  @Test
  fun updateTodoTimeSpentCallsRepositoryWithCorrectlyUpdatedTodo() {
    val newTimeSpent = 120L
    viewModel.updateTodoTimeSpent(todo, newTimeSpent)
    val updatedTodo = todo.copy(timeSpent = newTimeSpent)
    verify(mockRepository).updateTodo(eq(updatedTodo), any(), any())
  }
}
