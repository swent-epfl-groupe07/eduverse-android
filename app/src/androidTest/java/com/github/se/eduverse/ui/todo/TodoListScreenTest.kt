package com.github.se.eduverse.ui.todo

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.github.se.eduverse.BuildConfig
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.repository.TodoRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.TodoListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

  // StateFlow used to simulate real-time updates of the todos
  private val actualTodosFlow = MutableStateFlow(todos.filter { it.status == TodoStatus.ACTUAL })
  private val doneTodosFlow = MutableStateFlow(todos.filter { it.status == TodoStatus.DONE })

  // Helper function used to update the flows when todo lists are updated by repository functions
  // (mock the behavior of the snapshot listener)
  private fun updateFlows() {
    actualTodosFlow.value = todos.filter { todo -> todo.status == TodoStatus.ACTUAL }
    doneTodosFlow.value = todos.filter { todo -> todo.status == TodoStatus.DONE }
  }

  @Before
  fun setUp() {
    // Grant audio recording permission
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.uiAutomation
        .executeShellCommand(
            "pm grant ${BuildConfig.APPLICATION_ID} android.permission.RECORD_AUDIO")
        .close()

    mockRepository = mock(TodoRepository::class.java)
    mockNavigationActions = mock(NavigationActions::class.java)

    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.TODO_LIST)

    // Mock getActualTodos and getDoneTodos to return the flows defined above
    `when`(mockRepository.getActualTodos(any())).thenReturn(actualTodosFlow)
    `when`(mockRepository.getDoneTodos(any())).thenReturn(doneTodosFlow)

    `when`(mockRepository.getNewUid()).thenReturn("uid")
    `when`(mockRepository.addNewTodo(any(), any(), any())).then {
      val newTodo = it.getArgument<Todo>(0)
      todos = todos + newTodo
      updateFlows()
      it.getArgument<() -> Unit>(1)()
    }
    `when`(mockRepository.deleteTodoById(any(), any(), any())).then {
      val todoIdToDelete = it.getArgument<String>(0)
      todos = todos.filter { todo -> todo.uid != todoIdToDelete }
      updateFlows()
      it.getArgument<() -> Unit>(1)()
    }
    `when`(mockRepository.updateTodo(any(), any(), any())).then {
      val updatedTodo = it.getArgument<Todo>(0)
      todos = todos.map { todo -> if (todo.uid == updatedTodo.uid) updatedTodo else todo }
      updateFlows()
      it.getArgument<() -> Unit>(1)()
    }
    `when`(mockRepository.init(any())).then {
      todos = sampleTodos
      it.getArgument<(String?) -> Unit>(0)("3")
    }

    todoListViewModel = TodoListViewModel(mockRepository)
    composeTestRule.setContent {
      TodoListScreen(navigationActions = mockNavigationActions, todoListViewModel)
    }
  }

  @Test
  fun topNavigationBarIsCorrectlyDisplayed() {
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
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
    composeTestRule.onNodeWithTag("todoItem_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
  }

  @Test
  fun currentTodosAreCorrectlyDisplayed() {
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
    composeTestRule.onNodeWithTag("addTodoButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTodoButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("addTodoTextField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTodoTextField").performTextInput("new Todo")
    composeTestRule.onNodeWithTag("addTodoButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("addTodoButton").performClick()
    assert(todoListViewModel.actualTodos.value.any { it.name == "new Todo" })
    composeTestRule.onNodeWithTag("todoItem_uid").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoDoneButton_uid").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOptionsButton_uid").assertIsDisplayed()
    todos = sampleTodos
  }

  @Test
  fun doneButtonClickResultsInCorrectBehaviour() {
    composeTestRule.onNodeWithTag("todoDoneButton_2").performClick()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsDisplayed()
    todos = sampleTodos
  }

  @Test
  fun undoButtonClickResultsInCorrectBehaviour() {
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
    composeTestRule.onNodeWithTag("todoOptionsButton_2").performClick()
    composeTestRule.onNodeWithTag("renameTodoButton_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("deleteTodoButton_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOptionsButton_2").performClick()
    composeTestRule.onNodeWithTag("renameTodoButton_2").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("deleteTodoButton_2").assertIsNotDisplayed()
  }

  @Test
  fun todoOptionsButtonClickHasCorrectBehaviour_onDoneTodos() {
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
    composeTestRule.onNodeWithTag("todoOptionsButton_2").performClick()
    composeTestRule.onNodeWithTag("deleteTodoButton_2").performClick()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsNotDisplayed()
    todos = sampleTodos
  }

  @Test
  fun deleteTodoButtonClickResultsInCorrectBehaviour_onDoneTodos() {
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("todoOptionsButton_1").performClick()
    composeTestRule.onNodeWithTag("deleteTodoButton_1").performClick()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
    todos = sampleTodos
  }

  @Test
  fun renameTodoDialogIsCorrectlyDisplayed() {
    composeTestRule.onNodeWithTag("todoOptionsButton_2").performClick()
    composeTestRule.onNodeWithTag("renameTodoButton_2").performClick()
    composeTestRule.onNodeWithTag("renameTodoDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("renameTodoDialogTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("renameTodoDialogTitle").assertTextContains("Rename Todo")
    composeTestRule.onNodeWithTag("renameTodoTextField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("renameTodoConfirmButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("renameTodoConfirmButton").assertTextContains("Rename")
    composeTestRule.onNodeWithTag("renameTodoConfirmButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("renameTodoTextField").performTextClearance()
    composeTestRule.onNodeWithTag("renameTodoConfirmButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("renameTodoDismissButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("renameTodoDismissButton").assertTextContains("Cancel")
    composeTestRule.onNodeWithTag("renameTodoDismissButton").assertHasClickAction()
  }

  @Test
  fun renameTodoButtonClickResultsInCorrectBehaviour_onActualTodos() {
    val newName = "Renamed ToDo"
    composeTestRule.onNodeWithTag("todoOptionsButton_2").performClick()
    composeTestRule.onNodeWithTag("renameTodoButton_2").performClick()
    composeTestRule.onNodeWithTag("renameTodoTextField").performTextClearance()
    composeTestRule.onNodeWithTag("renameTodoTextField").performTextInput(newName)
    composeTestRule.onNodeWithTag("renameTodoConfirmButton").performClick()
    composeTestRule.onNodeWithTag("todoName_2").assertTextContains(newName)
    todos = sampleTodos
  }

  @Test
  fun renameTodoButtonClickResultsInCorrectBehaviour_onDoneTodos() {
    val newName = "Renamed ToDo"
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("todoOptionsButton_1").performClick()
    composeTestRule.onNodeWithTag("renameTodoButton_1").performClick()
    composeTestRule.onNodeWithTag("renameTodoTextField").performTextClearance()
    composeTestRule.onNodeWithTag("renameTodoTextField").performTextInput(newName)
    composeTestRule.onNodeWithTag("renameTodoConfirmButton").performClick()
    composeTestRule.onNodeWithTag("todoName_1").assertTextContains(newName)
    todos = sampleTodos
  }

  @Test
  fun newTodoIsDisplayedWhenAddedWhileCompletedTasksAreDisplayed() {
    val newTodoName = "New ToDo"
    composeTestRule.onNodeWithTag("completedTasksButton").performClick()
    composeTestRule.onNodeWithTag("todoItem_2").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTodoTextField").performTextInput(newTodoName)
    composeTestRule.onNodeWithTag("addTodoButton").performClick()
    // Check that the new todo is displayed
    composeTestRule.onNodeWithTag("todoItem_uid").assertIsDisplayed()
    // The lines below are there to check that the switch to the current tasks list is done
    // correctly
    composeTestRule.onNodeWithTag("todoItem_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("currentTasksButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("completedTasksButton").assertIsEnabled()
    todos = sampleTodos
  }

  @Test
  fun testClickingVoiceInputButton_correctlyDisplaysRecordNameDialog() {
    composeTestRule.onNodeWithTag("voiceInputButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("voiceInputButton").performClick()
    composeTestRule.onNodeWithTag("speechDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speechDialogTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speechDialogTitle").assertTextContains("Add a new task")
    composeTestRule.onNodeWithTag("speechDialogDescription").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("speechDialogDescription")
        .assertTextContains(
            "Press the bottom button to record the name of the new task to add. The recorded name will be displayed below. If you're happy with it, press the add button to add the new task to your todo list.\n\n")
    composeTestRule.onNodeWithTag("recordNameDialogAddButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recordNameDialogAddButton").assertTextEquals("Add")
    composeTestRule.onNodeWithTag("recordNameDialogAddButton").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("recordNameDialogAddButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("speechDialogDismissButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("speechDialogDismissButton").performClick()
    composeTestRule.onNodeWithTag("speechDialog").assertIsNotDisplayed()
  }
}
