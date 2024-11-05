package com.github.se.eduverse.ui.todo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.repository.TodoRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.TodoListViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class TodoListScreenTest {

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var mockTodoRepository: TodoRepository
  private lateinit var todoListViewModel: TodoListViewModel

  @get:Rule val composeTestRule = createComposeRule()

  private val actualTodo = Todo("2", "Second ToDo", 60, TodoStatus.ACTUAL)
  private val doneTodo = Todo("1", "First ToDo", 60, TodoStatus.DONE)

  @Before
  fun setUp() {
    mockTodoRepository = mock(TodoRepository::class.java)
    mockNavigationActions = mock(NavigationActions::class.java)
    todoListViewModel = TodoListViewModel(mockTodoRepository)
    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.TODO_LIST)
    composeTestRule.setContent {
      TodoListScreen(navigationActions = mockNavigationActions, todoListViewModel)
    }
  }

  @Test
  fun topNavigationBarIsCorrectlyDisplayed() {
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertTextContains("Todo List")
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
  }

  @Test
  fun screenIsCorrectlyDisplayed() {
    composeTestRule.onNodeWithTag("todoListScreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTodoEntry").assertIsDisplayed()
    composeTestRule.onNodeWithTag("currentTasksButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("currentTasksButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsEnabled()
  }

  @Test
  fun currentTodosAreCorrectlyDisplayed() {
    `when`(mockTodoRepository.getActualTodos(any(), any())).then {
      it.getArgument<(List<Todo>) -> Unit>(0)(listOf(actualTodo))
    }
    todoListViewModel.getActualTodos()
    composeTestRule.onNodeWithTag("currentTasksButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("currentTasksButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoDoneButton_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOptionsButton_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoDoneButton_2").assertIsEnabled()
  }

  @Test
  fun completedTodosAreCorrectlyDisplayed() {
    `when`(mockTodoRepository.getDoneTodos(any(), any())).then {
      it.getArgument<(List<Todo>) -> Unit>(0)(listOf(doneTodo))
    }
    todoListViewModel.getDoneTodos()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("currentTasksButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoDoneButton_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOptionsButton_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoDoneButton_1").assertIsEnabled()
  }

  @Test
  fun addNewTodoEntry() {
    composeTestRule.onNodeWithTag("addTodoButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTodoButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("addTodoTextField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTodoTextField").performTextInput("New Todo")
    composeTestRule.onNodeWithTag("addTodoButton").assertIsEnabled()
  }
}
