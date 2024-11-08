package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import com.github.se.eduverse.repository.TimeTableRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class TimeTableViewModelTest {
  private lateinit var timeTableRepository: TimeTableRepository
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var user: FirebaseUser
  private lateinit var timeTableViewModel: TimeTableViewModel

  private val scheduled1 =
      Scheduled(
          "id1",
          ScheduledType.TASK,
          Calendar.getInstance().apply { timeInMillis = 12 },
          7,
          "taskId",
          "ownerId",
          "name1")
  private val scheduled2 =
      Scheduled(
          "id2",
          ScheduledType.EVENT,
          Calendar.getInstance().apply { timeInMillis = 12 },
          11,
          "eventId",
          "ownerId",
          "name2")
  private val scheduled3 =
      Scheduled(
          "id3",
          ScheduledType.EVENT,
          Calendar.getInstance().apply { timeInMillis = 12 },
          9,
          "eventId",
          "ownerId",
          "name3")
  private val weekWithScheduled = MutableList(7) { emptyList<Scheduled>() }
  private lateinit var initialWeek: Calendar

  @Before
  fun setUp() {
    timeTableRepository = mock(TimeTableRepository::class.java)
    firebaseAuth = mock(FirebaseAuth::class.java)
    user = mock(FirebaseUser::class.java)

    `when`(firebaseAuth.currentUser).thenReturn(user)
    `when`(user.uid).thenReturn("ownerId")
    `when`(timeTableRepository.getNewUid()).thenReturn("uid")
    `when`(timeTableRepository.getScheduled(any(), any(), any(), any())).then {
      initialWeek =
          Calendar.getInstance().apply { timeInMillis = it.getArgument<Calendar>(0).timeInMillis }
      val callback = it.getArgument<(List<Scheduled>) -> Unit>(2)
      callback(listOf(scheduled1, scheduled2))
    }

    timeTableViewModel = TimeTableViewModel(timeTableRepository, firebaseAuth)
    timeTableViewModel.getWeek()

    // 3 because scheduled1 and scheduled2 are tuesday, represented by 3
    weekWithScheduled[
        (Calendar.THURSDAY - Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 7) % 7] =
        listOf(scheduled1, scheduled2)
  }

  @Test
  fun getNewUidTest() {
    assertEquals("uid", timeTableViewModel.getNewUid())
    verify(timeTableRepository).getNewUid()
  }

  @Test
  fun getWeek() {
    timeTableViewModel.getWeek()
    assertEquals(weekWithScheduled, timeTableViewModel.table.value)
  }

  @Test
  fun getPreviousWeek() {
    `when`(timeTableRepository.getScheduled(any(), any(), any(), any())).then {
      val callback = it.getArgument<(List<Scheduled>) -> Unit>(2)
      callback(listOf(scheduled1, scheduled2))
      assertEquals(
          initialWeek.get(Calendar.WEEK_OF_YEAR) - 1,
          it.getArgument<Calendar>(0).get(Calendar.WEEK_OF_YEAR))
    }

    timeTableViewModel.getPreviousWeek()
    assertEquals(weekWithScheduled, timeTableViewModel.table.value)
  }

  @Test
  fun getNextWeek() {
    `when`(timeTableRepository.getScheduled(any(), any(), any(), any())).then {
      val callback = it.getArgument<(List<Scheduled>) -> Unit>(2)
      callback(listOf(scheduled1, scheduled2))

      assertEquals(
          initialWeek.get(Calendar.WEEK_OF_YEAR) + 1,
          it.getArgument<Calendar>(0).get(Calendar.WEEK_OF_YEAR))
    }

    timeTableViewModel.getNextWeek()
    assertEquals(weekWithScheduled, timeTableViewModel.table.value)
  }

  @Test
  fun addScheduledTest() {
    `when`(timeTableRepository.getScheduled(any(), any(), any(), any())).then {
      val callback = it.getArgument<(List<Scheduled>) -> Unit>(2)
      callback(listOf(scheduled1))
    }
    `when`(timeTableRepository.addScheduled(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }

    timeTableViewModel.getWeek()
    scheduled2.start.apply { timeInMillis = Calendar.getInstance().timeInMillis }
    timeTableViewModel.addScheduled(scheduled2)
    assertEquals(weekWithScheduled, timeTableViewModel.table.value)
  }

  @Test
  fun updateScheduledTest() {
    `when`(timeTableRepository.updateScheduled(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }

    timeTableViewModel.updateScheduled(scheduled2.apply { name = "newName" })
    assertEquals("newName", scheduled2.name)
    assertEquals(weekWithScheduled, timeTableViewModel.table.value)
  }

  @Test
  fun deleteScheduledTest() {
    `when`(timeTableRepository.getScheduled(any(), any(), any(), any())).then {
      val callback = it.getArgument<(List<Scheduled>) -> Unit>(2)
      callback(listOf(scheduled1, scheduled2, scheduled3))
    }
    `when`(timeTableRepository.deleteScheduled(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }

    timeTableViewModel.deleteScheduled(scheduled3)
    assertEquals(weekWithScheduled, timeTableViewModel.table.value)
  }

  @Test
  fun getDateAtDayTest() {
    @Suppress("DEPRECATION") val cal = Calendar.getInstance().apply { time = Date(124, 10, 7) }

    Locale.setDefault(Locale.ENGLISH) // To get the result in english
    assertEquals("Thu\n7", timeTableViewModel.getDateAtDay(0, cal))
    assertEquals("Fri\n8", timeTableViewModel.getDateAtDay(1, cal))
    assertEquals("Wed\n13", timeTableViewModel.getDateAtDay(6, cal))

    Locale.setDefault(Locale.FRENCH) // To get the result in french
    assertEquals("jeu.\n7", timeTableViewModel.getDateAtDay(0, cal))
    assertEquals("ven.\n8", timeTableViewModel.getDateAtDay(1, cal))
    assertEquals("mer.\n13", timeTableViewModel.getDateAtDay(6, cal))
  }
}
