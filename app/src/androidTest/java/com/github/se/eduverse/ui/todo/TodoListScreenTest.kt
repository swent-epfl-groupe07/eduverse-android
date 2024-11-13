package com.github.se.eduverse.ui.todo

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
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
  private lateinit var mockRepository: TodoRepository
  private lateinit var todoListViewModel: TodoListViewModel

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleTodos =
      listOf(
          Todo("2", "Second ToDo", 60, TodoStatus.ACTUAL, "3"),
          Todo("1", "First ToDo", 60, TodoStatus.DONE, "3"))
  private var todos = sampleTodos

  @Before
  fun setUp() {
    mockRepository = mock(TodoRepository::class.java)
    mockNavigationActions = mock(NavigationActions::class.java)
    todoListViewModel = TodoListViewModel(mockRepository)
    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.TODO_LIST)
    `when`(mockRepository.getActualTodos(any(), any(), any())).then {
      it.getArgument<(List<Todo>) -> Unit>(1)(
          todos.filter { todo -> todo.status == TodoStatus.ACTUAL })
    }
    `when`(mockRepository.getDoneTodos(any(), any(), any())).then {
      it.getArgument<(List<Todo>) -> Unit>(1)(
          todos.filter { todo -> todo.status == TodoStatus.DONE })
    }
    `when`(mockRepository.getNewUid()).thenReturn("uid")

    `when`(mockRepository.addNewTodo(any(), any(), any())).then {
      todos += it.getArgument<Todo>(0)
      it.getArgument<() -> Unit>(1)()
    }
    `when`(mockRepository.deleteTodoById(any(), any(), any())).then {
      val todoToDeleteId = it.getArgument<String>(0)
      todos = todos.filter { todo -> todo.uid != todoToDeleteId }
      it.getArgument<() -> Unit>(1)()
    }
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
    todoListViewModel.currentUid = "3"
    todoListViewModel.getActualTodos()
    todoListViewModel.getDoneTodos()
    composeTestRule.onNodeWithTag("todoListScreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTodoEntry").assertIsDisplayed()
    composeTestRule.onNodeWithTag("currentTasksButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("currentTasksButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
  }

  @Test
  fun currentTodosAreCorrectlyDisplayed() {
    todoListViewModel.currentUid = "3"
    todoListViewModel.getActualTodos()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoDoneButton_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOptionsButton_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoName_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoName_2").assertTextContains("Second ToDo")
    composeTestRule.onNodeWithTag("todoOptionsButton_2").assertIsEnabled()
    composeTestRule.onNodeWithTag("todoDoneButton_2").assertIsEnabled()
  }

  @Test
  fun completedTodosAreCorrectlyDisplayed() {
    todoListViewModel.currentUid = "3"
    todoListViewModel.getActualTodos()
    todoListViewModel.getDoneTodos()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("currentTasksButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoDoneButton_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoName_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoName_1").assertTextContains("First ToDo")
    composeTestRule.onNodeWithTag("todoOptionsButton_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOptionsButton_1").assertIsEnabled()
    composeTestRule.onNodeWithTag("todoDoneButton_1").assertIsEnabled()
  }

  @Test
  fun addNewTodoEntry() {
    todoListViewModel.currentUid = "3"
    composeTestRule.onNodeWithTag("addTodoButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTodoButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("addTodoTextField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTodoTextField").performTextInput("new Todo")
    composeTestRule.onNodeWithTag("addTodoButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("addTodoButton").performClick()
    val newTodo =
        Todo(
            uid = "uid",
            name = "new Todo",
            timeSpent = 0,
            status = TodoStatus.ACTUAL,
            ownerId = "3")
    assert(todos.contains(newTodo))
    composeTestRule.onNodeWithTag("todoItem_uid").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoDoneButton_uid").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOptionsButton_uid").assertIsDisplayed()
    todos = sampleTodos
  }

  @Test
  fun doneButtonClickResultsInCorrectBehaviour() {
    todoListViewModel.currentUid = "3"
    todoListViewModel.getActualTodos()
    `when`(mockRepository.updateTodo(any(), any(), any())).then {
      todos -= it.getArgument<Todo>(0).copy(status = TodoStatus.ACTUAL)
      todos += it.getArgument<Todo>(0)
      it.getArgument<() -> Unit>(1)()
    }
    composeTestRule.onNodeWithTag("todoDoneButton_2").performClick()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsDisplayed()
    todos = sampleTodos
  }

  @Test
  fun undoButtonClickResultsInCorrectBehaviour() {
    todoListViewModel.currentUid = "3"
    todoListViewModel.getActualTodos()
    todoListViewModel.getDoneTodos()
    `when`(mockRepository.updateTodo(any(), any(), any())).then {
      todos -= it.getArgument<Todo>(0).copy(status = TodoStatus.DONE)
      todos += it.getArgument<Todo>(0)
      it.getArgument<() -> Unit>(1)()
    }
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoDoneButton_1").performClick()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("currentTasksButton").performClick()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsDisplayed()
    todos = sampleTodos
  }

  @Test
  fun todoOptionsButtonClickHasCorrectBehaviour_onActualTodos() {
    todoListViewModel.currentUid = "3"
    todoListViewModel.getActualTodos()
    composeTestRule.onNodeWithTag("todoOptionsButton_2").performClick()
    composeTestRule.onNodeWithTag("renameTodoButton_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("deleteTodoButton_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOptionsButton_2").performClick()
    composeTestRule.onNodeWithTag("renameTodoButton_2").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("deleteTodoButton_2").assertIsNotDisplayed()
  }

  @Test
  fun todoOptionsButtonClickHasCorrectBehaviour_onDoneTodos() {
    todoListViewModel.currentUid = "3"
    todoListViewModel.getDoneTodos()
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("todoOptionsButton_1").performClick()
    composeTestRule.onNodeWithTag("renameTodoButton_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("deleteTodoButton_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOptionsButton_1").performClick()
    composeTestRule.onNodeWithTag("renameTodoButton_1").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("deleteTodoButton_1").assertIsNotDisplayed()
  }

  @Test
  fun deleteTodoButtonClickResultsInCorrectBehaviour_onActualTodos() {
    todoListViewModel.currentUid = "3"
    todoListViewModel.getActualTodos()
    composeTestRule.onNodeWithTag("todoOptionsButton_2").performClick()
    composeTestRule.onNodeWithTag("deleteTodoButton_2").performClick()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsNotDisplayed()
    todos = sampleTodos
  }

  @Test
  fun deleteTodoButtonClickResultsInCorrectBehaviour_onDoneTodos() {
    todoListViewModel.currentUid = "3"
    todoListViewModel.getDoneTodos()
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("todoOptionsButton_1").performClick()
    composeTestRule.onNodeWithTag("deleteTodoButton_1").performClick()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
    todos = sampleTodos
  }
}
