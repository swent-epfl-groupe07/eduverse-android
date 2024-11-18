package com.github.se.eduverse.ui.timetable

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.model.millisecInHour
import com.github.se.eduverse.repository.TimeTableRepository
import com.github.se.eduverse.repository.TodoRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.TimeTableViewModel
import com.github.se.eduverse.viewmodel.TodoListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

class DetailsTasksScreenTest {
  private lateinit var timeTableRepository: TimeTableRepository
  private lateinit var todoRepository: TodoRepository
  private lateinit var timeTableViewModel: TimeTableViewModel
  private lateinit var todoListViewModel: TodoListViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var auth: FirebaseAuth

  private val before =
      Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -1)
        add(Calendar.HOUR_OF_DAY, -1)
        set(Calendar.MINUTE, -30)
      }
  private val task =
      Scheduled(
          "id",
          ScheduledType.TASK,
          before,
          2 * millisecInHour.toLong(),
          "id_todo",
          "ownerId",
          "name")
  private var todo = Todo("id_todo", "name", 0, TodoStatus.ACTUAL, "owner")

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    timeTableRepository = mock(TimeTableRepository::class.java)
    auth = mock(FirebaseAuth::class.java)
    val user = mock(FirebaseUser::class.java)
    navigationActions = mock(NavigationActions::class.java)

    `when`(auth.currentUser).thenReturn(user)
    `when`(user.uid).thenReturn("userId")
    `when`(timeTableRepository.updateScheduled(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }
    `when`(timeTableRepository.deleteScheduled(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }

    timeTableViewModel = TimeTableViewModel(timeTableRepository, auth)

    todoRepository = mock(TodoRepository::class.java)

    `when`(todoRepository.init(any())).then { it.getArgument<(String) -> Unit>(0)("owner") }
    `when`(todoRepository.getActualTodos(any(), any(), any())).then {
      it.getArgument<(List<Todo>) -> Unit>(1)(
          if (todo.status == TodoStatus.ACTUAL) listOf(todo) else emptyList())
    }
    `when`(todoRepository.getDoneTodos(any(), any(), any())).then {
      it.getArgument<(List<Todo>) -> Unit>(1)(
          if (todo.status == TodoStatus.DONE) listOf(todo) else emptyList())
    }
    `when`(todoRepository.updateTodo(any(), any(), any())).then {
      todo =
          todo.copy(
              status = if (todo.status == TodoStatus.DONE) TodoStatus.ACTUAL else TodoStatus.DONE)
      it.getArgument<() -> Unit>(1)()
    }

    todoListViewModel = TodoListViewModel(todoRepository)
  }

  @Test
  fun goesBackOnNullOpenedEvent() {
    timeTableViewModel.opened = null
    composeTestRule.setContent {
      DetailsTasksScreen(timeTableViewModel, todoListViewModel, navigationActions)
    }

    verify(navigationActions).goBack()
  }

  @Test
  fun goesBackOnNullLinkedTodo() {
    `when`(todoRepository.getActualTodos(any(), any(), any())).then {
      it.getArgument<(List<Todo>) -> Unit>(1)(emptyList())
    }
    todoListViewModel.getActualTodos()
    todoListViewModel.getDoneTodos()
    launch()

    verify(navigationActions).goBack()
  }

  @Test
  fun displaysElements() {
    launch()

    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("deleteButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nameTextField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("datePicker").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timePicker").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lengthPicker").assertIsDisplayed()
    composeTestRule.onNodeWithTag("status").assertIsDisplayed()
    composeTestRule.onNodeWithTag("seeTodo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("markAsDone").assertIsDisplayed()
    composeTestRule.onNodeWithTag("markAsCurrent").assertIsNotDisplayed()
    composeTestRule.onAllNodesWithTag("saveIcon").assertCountEquals(4)
  }

  @Test
  fun goBackTest() {
    launch()

    composeTestRule.onNodeWithTag("goBackButton").performClick()
    verify(navigationActions).goBack()
  }

  @Test
  fun deleteTest() {
    launch()

    composeTestRule.onNodeWithTag("deleteButton").performClick()
    verify(timeTableRepository).deleteScheduled(eq(task), any(), any())
    verify(navigationActions).goBack()
  }

  @Test
  fun fieldsAndSaveTest() {
    val now = Calendar.getInstance()
    launch()

    composeTestRule.onAllNodesWithTag("saveIcon").assertAll(isNotEnabled())

    composeTestRule.onNodeWithTag("nameTextField").performTextInput("++ ")
    composeTestRule.onAllNodesWithTag("saveIcon").assertAny(isEnabled())
    clickAllSaveIcons()
    assertEquals("++ name", task.name)

    composeTestRule.onNodeWithTag("nameTextField").performTextClearance()
    composeTestRule.onNodeWithTag("nameTextField").performTextInput("new name")

    composeTestRule.onNodeWithTag("datePicker").assertHasClickAction()
    composeTestRule.onNodeWithTag("timePicker").assertHasClickAction()

    clickAllSaveIcons()

    assertEquals("new name", task.name)
  }

  @Test
  fun seeTodoTest() {
    launch()

    composeTestRule.onNodeWithTag("seeTodo").performClick()
    verify(navigationActions).navigateTo(eq(Screen.TODO_LIST))
  }

  @Test
  fun changeTodoStatusTest() {
    launch()

    composeTestRule.onNodeWithTag("markAsDone").performClick()

    assertEquals(TodoStatus.DONE, todo.status)
    composeTestRule.onNodeWithTag("markAsDone").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("markAsCurrent").assertIsDisplayed()

    composeTestRule.onNodeWithTag("markAsCurrent").performClick()

    assertEquals(TodoStatus.ACTUAL, todo.status)
    composeTestRule.onNodeWithTag("markAsCurrent").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("markAsDone").assertIsDisplayed()
  }

  private fun launch() {
    timeTableViewModel.opened = task
    composeTestRule.setContent {
      DetailsTasksScreen(timeTableViewModel, todoListViewModel, navigationActions)
    }
  }

  private fun clickAllSaveIcons() {
    for (i in 0..3) {
      composeTestRule.onAllNodesWithTag("saveIcon")[i].performClick()
    }
  }
}
