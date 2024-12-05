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
import com.github.se.eduverse.repository.NotificationRepository
import com.github.se.eduverse.repository.TimeTableRepository
import com.github.se.eduverse.repository.TodoRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.profile.auth
import com.github.se.eduverse.viewmodel.TimeTableViewModel
import com.github.se.eduverse.viewmodel.TodoListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
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
  private lateinit var notificationRepository: NotificationRepository
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
  private val sampleTodo = Todo("id_todo", "name", 0, TodoStatus.ACTUAL, "owner")
  private var todo = sampleTodo

  // StateFlow used to simulate real-time updates of the todos
  private val actualTodosFlow = MutableStateFlow(listOf(todo))
  private val doneTodosFlow = MutableStateFlow(emptyList<Todo>())

  // Helper function used to update the flows when todo lists are updated by repository functions
  // (mock the behavior of the snapshot listener)
  private fun updateFlows() {
    actualTodosFlow.value = if (todo.status == TodoStatus.ACTUAL) listOf(todo) else emptyList()
    doneTodosFlow.value = if (todo.status == TodoStatus.DONE) listOf(todo) else emptyList()
  }

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    timeTableRepository = mock(TimeTableRepository::class.java)
    notificationRepository = mock(NotificationRepository::class.java)
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

    timeTableViewModel = TimeTableViewModel(timeTableRepository, notificationRepository, auth)

    todoRepository = mock(TodoRepository::class.java)

    // Mock getActualTodos and getDoneTodos to return the flows defined above
    `when`(todoRepository.getActualTodos(any())).thenReturn(actualTodosFlow)
    `when`(todoRepository.getDoneTodos(any())).thenReturn(doneTodosFlow)

    `when`(todoRepository.updateTodo(any(), any(), any())).then {
      val updatedTodo = it.getArgument<Todo>(0)
      todo = updatedTodo
      updateFlows()
      it.getArgument<() -> Unit>(1)()
    }
    `when`(todoRepository.init(any())).then {
      todo = sampleTodo
      it.getArgument<(String?) -> Unit>(0)("owner")
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
    actualTodosFlow.value = emptyList()
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

    assert(todoListViewModel.doneTodos.value.contains(todo))
    assert(!todoListViewModel.actualTodos.value.contains(todo))
    composeTestRule.onNodeWithTag("markAsDone").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("markAsCurrent").assertIsDisplayed()

    composeTestRule.onNodeWithTag("markAsCurrent").performClick()

    assert(!todoListViewModel.doneTodos.value.contains(todo))
    assert(todoListViewModel.actualTodos.value.contains(todo))
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
