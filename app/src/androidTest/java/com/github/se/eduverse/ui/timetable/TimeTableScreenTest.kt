package com.github.se.eduverse.ui.timetable

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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

class TimeTableScreenTest {
  private lateinit var todoRepository: TodoRepository
  private lateinit var timeTableRepository: TimeTableRepository
  private lateinit var notificationRepository: NotificationRepository
  private lateinit var auth: FirebaseAuth
  private lateinit var user: FirebaseUser
  private lateinit var navigationActions: NavigationActions
  private lateinit var timeTableViewModel: TimeTableViewModel
  private lateinit var todoListViewModel: TodoListViewModel

  private lateinit var density: Density
  private val scheduled1 =
      Scheduled(
          "id1",
          ScheduledType.TASK,
          Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 3) },
          millisecInHour.toLong(),
          "taskId",
          "ownerId",
          "name1")
  private val scheduled2 =
      Scheduled(
          "id2",
          ScheduledType.EVENT,
          Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 2) },
          (2 * millisecInHour).toLong(),
          "eventId",
          "ownerId",
          "name2")
  private val scheduled3 =
      Scheduled(
          "id3",
          ScheduledType.EVENT,
          Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0) },
          (2.5 * millisecInHour).toLong(),
          "eventId",
          "ownerId",
          "name3")

  private val todo1 =
      Todo(uid = "1", name = "todo1", timeSpent = 60, status = TodoStatus.ACTUAL, ownerId = "owner")
  private val todo2 =
      Todo(uid = "2", name = "todo2", timeSpent = 60, status = TodoStatus.ACTUAL, ownerId = "owner")
  private val todo3 =
      Todo(uid = "3", name = "todo3", timeSpent = 60, status = TodoStatus.ACTUAL, ownerId = "owner")

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    todoRepository = mock(TodoRepository::class.java)
    timeTableRepository = mock(TimeTableRepository::class.java)
    notificationRepository = mock(NotificationRepository::class.java)
    auth = mock(FirebaseAuth::class.java)
    user = mock(FirebaseUser::class.java)
    navigationActions = mock(NavigationActions::class.java)

    `when`(auth.currentUser).thenReturn(user)
    `when`(user.uid).thenReturn("userId")
    `when`(timeTableRepository.getNewUid()).thenReturn("uid")
    `when`(timeTableRepository.getScheduled(any(), any(), any(), any())).then {
      val callback = it.getArgument<(List<Scheduled>) -> Unit>(2)
      callback(listOf(scheduled1, scheduled2, scheduled3))
    }
    `when`(timeTableRepository.addScheduled(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }
    `when`(timeTableRepository.updateScheduled(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }
    `when`(timeTableRepository.deleteScheduled(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }

    `when`(todoRepository.init(any())).then { it.getArgument<(String?) -> Unit>(0)("3") }
    `when`(todoRepository.getActualTodos(any(), any(), any())).then {
      val callback = it.getArgument<(List<Todo>) -> Unit>(1)
      callback(listOf(todo1, todo2, todo3))
    }

    todoListViewModel = TodoListViewModel(todoRepository)
    timeTableViewModel = TimeTableViewModel(timeTableRepository, notificationRepository, auth)

    composeTestRule.setContent {
      density = LocalDensity.current
      TimeTableScreen(timeTableViewModel, todoListViewModel, navigationActions)
    }
  }

  @Test
  fun displayElements() {
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bottomNavigationMenu").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTaskButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addEventButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lastWeekButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nextWeekButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("monthAndYear").assertIsDisplayed()
    composeTestRule.onNodeWithTag("days").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hours").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("tableColumn").assertCountEquals(7)

    composeTestRule.onNodeWithTag("buttonOfname1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("buttonOfname2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("buttonOfname3").assertIsDisplayed()
  }

  @Test
  fun nextAndLastWeekTest() {
    composeTestRule.onNodeWithTag("lastWeekButton").performClick()
    composeTestRule.onNodeWithTag("nextWeekButton").performClick()

    verify(3) { timeTableRepository.getScheduled(any(), any(), any(), any()) }
  }

  @Test
  fun buttonsHaveCorrectSize() {
    val node1 = composeTestRule.onNodeWithTag("buttonOfname1").fetchSemanticsNode()
    val bounds1 = node1.boundsInRoot
    val width1 = bounds1.width
    val height1 = bounds1.height

    val node2 = composeTestRule.onNodeWithTag("buttonOfname2").fetchSemanticsNode()
    val bounds2 = node2.boundsInRoot
    val width2 = bounds2.width
    val height2 = bounds2.height

    val node3 = composeTestRule.onNodeWithTag("buttonOfname3").fetchSemanticsNode()
    val bounds3 = node3.boundsInRoot
    val width3 = bounds3.width
    val height3 = bounds3.height

    val reference1 = with(density) { 50.dp.toPx() }
    val reference2 = with(density) { 100.dp.toPx() }
    val reference3 = with(density) { 125.dp.toPx() }

    // Check that the height are correct with max 5% error
    // Due to type conversion and dealing with float
    assertEquals("", reference1, height1, 5 * reference1 / 100)
    assertEquals("", reference2, height2, 5 * reference2 / 100)
    assertEquals("", reference3, height3, 5 * reference3 / 100)

    assert(width2 < width1)
    assert(width1 == width3)
  }

  @Test
  fun closeDialog() {
    composeTestRule.onNodeWithTag("dialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("addEventButton").performClick()
    composeTestRule.onNodeWithTag("dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancel").performClick()

    composeTestRule.onNodeWithTag("dialog").assertIsNotDisplayed()
  }

  @Test
  fun addEventTest() {
    composeTestRule.onNodeWithTag("dialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("addEventButton").performClick()
    composeTestRule.onNodeWithTag("dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addEventDialog").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nameTextField").performTextInput("name4")

    composeTestRule.onNodeWithTag("datePicker").assertHasClickAction()
    composeTestRule.onNodeWithTag("timePicker").assertHasClickAction()
    composeTestRule.onNodeWithTag("lengthPicker").assertHasClickAction()

    composeTestRule.onNodeWithTag("confirm").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("dialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("buttonOfname4").assertExists()
  }

  @Test
  fun addTaskTest() {
    composeTestRule.onNodeWithTag("dialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("addTaskButton").performClick()
    composeTestRule.onNodeWithTag("dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addTaskDialog").assertIsDisplayed()

    composeTestRule.onNodeWithTag("todo1").assertExists()
    composeTestRule.onNodeWithTag("todo2").assertExists()
    composeTestRule.onNodeWithTag("todo3").assertExists()

    composeTestRule.onNodeWithTag("todo1").performClick()
    composeTestRule.onNodeWithTag("confirm").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("dialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("buttonOftodo1").assertExists()
  }

  @Test
  fun openEventTest() {
    assert(timeTableViewModel.opened == null)

    composeTestRule.onNodeWithTag("buttonOfname2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("buttonOfname2").performClick()

    assert(timeTableViewModel.opened == scheduled2)
    verify(navigationActions).navigateTo(eq(Screen.DETAILS_EVENT))
  }
}
