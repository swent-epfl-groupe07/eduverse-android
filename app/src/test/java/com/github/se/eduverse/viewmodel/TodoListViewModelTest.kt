package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.repository.TodoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class TodoListViewModelTest {
  private lateinit var viewModel: TodoListViewModel
  private lateinit var mockRepository: TodoRepository

  private val todo = Todo(uid = "1", name = "Task", timeSpent = 60, status = TodoStatus.ACTUAL)

  @Before
  fun setUp() {
    mockRepository = mock(TodoRepository::class.java)
    viewModel = TodoListViewModel(mockRepository)
  }

  @Test
  fun getNewUid() {
    `when`(mockRepository.getNewUid()).thenReturn("uid")
    assertThat(viewModel.getNewUid(), `is`("uid"))
  }

  @Test
  fun addNewTodoCallsRepository() {
    viewModel.addNewTodo(todo)
    verify(mockRepository).addNewTodo(eq(todo), any(), any())
  }

  @Test
  fun setTodoDoneCallsRepository() {
    viewModel.setTodoDone(todo)
    verify(mockRepository).updateTodo(any(), any(), any())
  }

  @Test
  fun setTodoActualCallsRepository() {
    viewModel.setTodoActual(todo)
    verify(mockRepository).updateTodo(any(), any(), any())
  }

  @Test
  fun deleteTodoCallsRepository() {
    viewModel.deleteTodo("1")
    verify(mockRepository).deleteTodoById(eq("1"), any(), any())
  }
}
