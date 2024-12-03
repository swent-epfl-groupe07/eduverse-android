package com.github.se.eduverse.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.repository.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class TodoListViewModelTest {

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var viewModel: TodoListViewModel
  private lateinit var mockRepository: TodoRepository
  private val testDispatcher = StandardTestDispatcher()

  private val todos =
      listOf(
          Todo(
              uid = "1",
              name = "Task 1",
              timeSpent = 60,
              status = TodoStatus.ACTUAL,
              ownerId = "3"),
          Todo(uid = "2", name = "Task 2", timeSpent = 60, status = TodoStatus.DONE, ownerId = "3"))

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mock()
    `when`(mockRepository.getActualTodos(any())).thenReturn(MutableStateFlow(listOf(todos[0])))
    `when`(mockRepository.getDoneTodos(any())).thenReturn(MutableStateFlow(listOf(todos[1])))
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun getNewUid() {
    `when`(mockRepository.getNewUid()).thenReturn("uid")
    assertEquals(viewModel.getNewUid(), "uid")
  }

  @Test
  fun addNewTodoCallsRepositoryWhenCurrentUidIsNotNull() {
    val todoName = "New Task"
    val uid = "newUid"
    `when`(mockRepository.getNewUid()).thenReturn(uid)

    viewModel.addNewTodo(todoName)

    verify(mockRepository)
        .addNewTodo(
            argThat { name == todoName && status == TodoStatus.ACTUAL && ownerId == "3" },
            any(),
            any())
  }

  @Test
  fun addNewTodoDoesNotCallRepositoryWhenCurrentUidIsNull() {
    viewModel.currentUid = null
    viewModel.addNewTodo("new Todo")
    verify(mockRepository, Mockito.never()).addNewTodo(any(), any(), any())
  }

  @Test
  fun setTodoDoneCallsRepositoryWithCorrectlyUpdatedTodo() {
    viewModel.setTodoDone(todos[0])
    val doneTodo = todos[0].copy(status = TodoStatus.DONE)
    verify(mockRepository).updateTodo(eq(doneTodo), any(), any())
  }

  @Test
  fun setTodoActualCallsRepository() {
    viewModel.setTodoActual(todos[1])
    val actualTodo = todos[1].copy(status = TodoStatus.ACTUAL)
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
    viewModel.renameTodo(todos[0], newTodoName)
    val renamedTodo = todos[0].copy(name = newTodoName)
    verify(mockRepository).updateTodo(eq(renamedTodo), any(), any())
  }

  @Test
  fun selectTodoUpdatesSelectedTodo() {
    val todo = todos[0]
    viewModel.selectTodo(todo)
    assertEquals(todo, viewModel.selectedTodo.value)
  }

  @Test
  fun unselectTodoSetsSelectedTodoToNull() {
    viewModel.selectTodo(todos[0])
    viewModel.unselectTodo()
    assertNull(viewModel.selectedTodo.value)
  }

  @Test
  fun updateTodoTimeSpentCallsRepositoryWithCorrectlyUpdatedTodo() {
    val newTimeSpent = 120L
    viewModel.updateTodoTimeSpent(todos[0], newTimeSpent)
    val updatedTodo = todos[0].copy(timeSpent = newTimeSpent)
    verify(mockRepository).updateTodo(eq(updatedTodo), any(), any())
  }
}
