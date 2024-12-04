package com.github.se.eduverse.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.repository.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq

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
          Todo(uid = "2", name = "Task 2", timeSpent = 60, status = TodoStatus.DONE, ownerId = "3"),
          Todo(
              uid = "3",
              name = "Task 3",
              timeSpent = 60,
              status = TodoStatus.ACTUAL,
              ownerId = "3"))

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mock(TodoRepository::class.java)
    `when`(mockRepository.getActualTodos(any())).thenReturn(MutableStateFlow(listOf(todos[0])))
    `when`(mockRepository.getDoneTodos(any())).thenReturn(MutableStateFlow(listOf(todos[1])))
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun getNewUid() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    `when`(mockRepository.getNewUid()).thenReturn("uid")
    assertEquals(viewModel.getNewUid(), "uid")
  }

  @Test
  fun addNewTodoCallsRepositoryWhenCurrentUidIsNotNull() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
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
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke(null)
    }
    viewModel = TodoListViewModel(mockRepository)
    viewModel.addNewTodo("new Todo")
    verify(mockRepository, Mockito.never()).addNewTodo(any(), any(), any())
  }

  @Test
  fun setTodoDoneCallsRepositoryWithCorrectlyUpdatedTodo() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    viewModel.setTodoDone(todos[0])
    val doneTodo = todos[0].copy(status = TodoStatus.DONE)
    verify(mockRepository).updateTodo(eq(doneTodo), any(), any())
  }

  @Test
  fun setTodoDoneSetsSelectedTodoToNull() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    viewModel.selectTodo(todos[0])
    assertNotNull(viewModel.selectedTodo.value)
    viewModel.setTodoDone(todos[0])
    assertNull(viewModel.selectedTodo.value)
  }

  @Test
  fun setTodoActualCallsRepository() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    viewModel.setTodoActual(todos[1])
    val actualTodo = todos[1].copy(status = TodoStatus.ACTUAL)
    verify(mockRepository).updateTodo(eq(actualTodo), any(), any())
  }

  @Test
  fun deleteTodoCallsRepository() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    viewModel.deleteTodo("1")
    verify(mockRepository).deleteTodoById(eq("1"), any(), any())
  }

  @Test
  fun deleteTodoSetsSelectedTodoToNull() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    viewModel.selectTodo(todos[0])
    assertNotNull(viewModel.selectedTodo.value)
    viewModel.deleteTodo("1")
    assertNull(viewModel.selectedTodo.value)
  }

  @Test
  fun renameTodoCallsRepositoryWithCorrectlyUpdatedTodo() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    val newTodoName = "new name"
    viewModel.renameTodo(todos[0], newTodoName)
    val renamedTodo = todos[0].copy(name = newTodoName)
    verify(mockRepository).updateTodo(eq(renamedTodo), any(), any())
  }

  @Test
  fun renameTodoUpdatesSelectedTodo() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    val newTodoName = "new name"
    viewModel.selectTodo(todos[0])
    assertNotNull(viewModel.selectedTodo.value)
    viewModel.renameTodo(todos[0], newTodoName)
    assertEquals(newTodoName, viewModel.selectedTodo.value?.name)
  }

  @Test
  fun selectTodoUpdatesSelectedTodo() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    val todo = todos[0]
    viewModel.selectTodo(todo)
    assertEquals(todo, viewModel.selectedTodo.value)
  }

  @Test
  fun unselectTodoSetsSelectedTodoToNull() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    viewModel.selectTodo(todos[0])
    viewModel.unselectTodo()
    assertNull(viewModel.selectedTodo.value)
  }

  @Test
  fun updateTodoTimeSpentCallsRepositoryWithCorrectlyUpdatedTodo() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    val newTimeSpent = 120L
    viewModel.updateTodoTimeSpent(todos[0], newTimeSpent)
    val updatedTodo = todos[0].copy(timeSpent = newTimeSpent)
    verify(mockRepository).updateTodo(eq(updatedTodo), any(), any())
  }

  @Test
  fun todoListsAreCorrectlyInitializedWithUserLists() = runTest {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    advanceUntilIdle()
    assertEquals(listOf(todos[0]), viewModel.actualTodos.value)
    assertEquals(listOf(todos[1]), viewModel.doneTodos.value)
  }

  @Test
  fun todoListsAreEmptyWhenCurrentUidIsNull() = runTest {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke(null)
    }
    viewModel = TodoListViewModel(mockRepository)
    advanceUntilIdle()
    assert(viewModel.actualTodos.value.isEmpty())
    assert(viewModel.doneTodos.value.isEmpty())
  }

  @Test
  fun getTodoByIdReturnsActualTodo() = runTest {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    advanceUntilIdle()
    val todo = todos[0]
    assertEquals(todo, viewModel.getTodoById(todo.uid))
  }

  @Test
  fun getTodoByIdReturnsDoneTodo() = runTest {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    advanceUntilIdle()
    val todo = todos[1]
    assertEquals(todo, viewModel.getTodoById(todo.uid))
  }

  @Test
  fun selectedTodoIsNotModifiedWhenDeleteTodoIsCalledOnAnotherTodo() {
    `when`(mockRepository.init(any())).then { invocation ->
      (invocation.arguments[0] as (String?) -> Unit).invoke("3")
    }
    viewModel = TodoListViewModel(mockRepository)
    viewModel.selectTodo(todos[0])
    assertNotNull(viewModel.selectedTodo.value)
    viewModel.deleteTodo("3")
    assertNotNull(viewModel.selectedTodo.value)
  }
}
