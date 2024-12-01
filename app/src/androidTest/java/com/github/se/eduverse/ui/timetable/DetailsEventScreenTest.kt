package com.github.se.eduverse.ui.timetable

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
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
import com.github.se.eduverse.model.millisecInHour
import com.github.se.eduverse.repository.NotificationRepository
import com.github.se.eduverse.repository.TimeTableRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.TimeTableViewModel
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

class DetailsEventScreenTest {
  private lateinit var timeTableRepository: TimeTableRepository
  private lateinit var notificationRepository: NotificationRepository
  private lateinit var timeTableViewModel: TimeTableViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var auth: FirebaseAuth

  private val before =
      Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -1)
        add(Calendar.HOUR_OF_DAY, -1)
        set(Calendar.MINUTE, -30)
      }
  private val event =
      Scheduled(
          "id",
          ScheduledType.EVENT,
          before,
          2 * millisecInHour.toLong(),
          "description",
          "ownerId",
          "name")

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
  }

  @Test
  fun goesBackOnNullOpenedEvent() {
    timeTableViewModel.opened = null
    composeTestRule.setContent { DetailsEventScreen(timeTableViewModel, navigationActions) }

    verify(navigationActions).goBack()
  }

  @Test
  fun displaysElements() {
    launch()

    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("deleteButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nameTextField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("descTextField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("datePicker").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timePicker").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lengthPicker").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("saveIcon").assertCountEquals(5)
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
    verify(timeTableRepository).deleteScheduled(eq(event), any(), any())
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
    assertEquals("++ name", event.name)

    composeTestRule.onNodeWithTag("nameTextField").performTextClearance()
    composeTestRule.onNodeWithTag("nameTextField").performTextInput("new name")

    composeTestRule.onNodeWithTag("descTextField").performTextClearance()
    composeTestRule.onNodeWithTag("descTextField").performTextInput("new description")

    composeTestRule.onNodeWithTag("datePicker").assertHasClickAction()
    composeTestRule.onNodeWithTag("timePicker").assertHasClickAction()

    clickAllSaveIcons()

    assertEquals("new name", event.name)
    assertEquals("new description", event.content)
  }

  private fun launch() {
    timeTableViewModel.opened = event
    composeTestRule.setContent { DetailsEventScreen(timeTableViewModel, navigationActions) }
  }

  private fun clickAllSaveIcons() {
    for (i in 0..4) {
      composeTestRule.onAllNodesWithTag("saveIcon")[i].performClick()
    }
  }
}
