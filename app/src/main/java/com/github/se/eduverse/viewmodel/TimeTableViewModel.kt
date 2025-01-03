package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.WeeklyTable
import com.github.se.eduverse.model.daysInWeek
import com.github.se.eduverse.model.emptyWeeklyTable
import com.github.se.eduverse.model.millisecInDay
import com.github.se.eduverse.repository.NotificationRepository
import com.github.se.eduverse.repository.TimeTableRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TimeTableViewModel(
    val timeTableRepository: TimeTableRepository,
    val notificationRepository: NotificationRepository,
    val auth: FirebaseAuth
) : ViewModel() {
  private val _currentWeek =
      MutableStateFlow(
          Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
          })
  val currentWeek: StateFlow<Calendar> = _currentWeek

  // A 3-dimensional table with dimension corresponding to day, hour and events
  private val _table: MutableStateFlow<WeeklyTable> = MutableStateFlow(emptyWeeklyTable())
  val table: StateFlow<WeeklyTable> = _table

  var opened: Scheduled? = null

  /** Create a new document in the database and returns its id */
  fun getNewUid(): String {
    return timeTableRepository.getNewUid()
  }

  /** Access all scheduled events and tasks in the current week for the active user */
  fun getWeek() {
    timeTableRepository.getScheduled(
        _currentWeek.value,
        auth.currentUser!!.uid,
        { _table.value = buildWeekTable(it) },
        { Log.e("TimeTableViewModel", "Exception $it while trying to load data for the week") })
  }

  /**
   * Return the scheduled with given id
   *
   * @param id the id of the scheduled
   */
  suspend fun getScheduledById(id: String): Scheduled {
    return timeTableRepository.getScheduledById(id)
  }

  /**
   * Change the active week to the next one, and access the scheduled events and tasks in that week
   */
  fun getNextWeek() {
    _currentWeek.value =
        Calendar.getInstance().apply {
          timeInMillis = _currentWeek.value.timeInMillis
          add(Calendar.WEEK_OF_YEAR, 1)
        }
    getWeek()
  }

  /**
   * Change the active week to the previous one, and access the scheduled events and tasks in that
   * week
   */
  fun getPreviousWeek() {
    _currentWeek.value =
        Calendar.getInstance().apply {
          timeInMillis = _currentWeek.value.timeInMillis
          add(Calendar.WEEK_OF_YEAR, -1)
        }
    getWeek()
  }

  /**
   * Schedule a new event
   *
   * @param scheduled the event to schedule
   */
  fun addScheduled(scheduled: Scheduled) {
    timeTableRepository.addScheduled(
        scheduled,
        {
          if (isValidTime(scheduled.start, scheduled.length)) {
            var currentTime = _currentWeek.value.timeInMillis
            val addedTime = scheduled.start.timeInMillis
            for (i in 0 ..< daysInWeek) {
              if (currentTime <= addedTime && addedTime < currentTime + millisecInDay) {
                _table.value =
                    _table.value.mapIndexed { index, innerList ->
                      if (index == i) {
                        (innerList + scheduled).sortedBy { it.start.timeInMillis }
                      } else innerList
                    }
              }
              currentTime += millisecInDay
            }
          }

          notificationRepository.scheduleNotification(scheduled)
        },
        { Log.e("TimeTableViewModel", "Exception $it while trying to schedule an event") })
  }

  /**
   * Modify an event
   *
   * @param scheduled the new event, with the same id that the one it replace
   */
  fun updateScheduled(scheduled: Scheduled) {
    timeTableRepository.updateScheduled(
        scheduled,
        {
          _table.value =
              _table.value.map { innerList ->
                innerList
                    .map { if (it.id == scheduled.id) scheduled else it }
                    .sortedBy { it.start.timeInMillis }
              }

          notificationRepository.scheduleNotification(scheduled)
        },
        { Log.e("TimeTableViewModel", "Exception $it while trying to modify a scheduled event") })
  }

  /**
   * Delete a scheduled event
   *
   * @param scheduled the event to delete
   */
  fun deleteScheduled(scheduled: Scheduled) {
    timeTableRepository.deleteScheduled(
        scheduled,
        {
          _table.value = _table.value.map { innerList -> innerList - scheduled }
          notificationRepository.cancelNotification(scheduled)
        },
        { Log.e("TimeTableViewModel", "Exception $it while trying to delete a scheduled event") })
  }

  /**
   * Get a meaningful string representing the month and year
   *
   * @param calendar the calendar of the moment we consider, typically the beginning of the week
   * @return a 2-lines string with the month (Sep, Oct, ...) on the first one and the year (2024,
   *   2027, ...) on the second
   */
  fun getMonthAndYear(calendar: Calendar): String {
    val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH)
    val year = calendar.get(Calendar.YEAR)
    return "$month\n$year"
  }

  /**
   * Get a meaningful string representing a date
   *
   * @param day the day of the week, starting at the current one (0 = today, 1 = tomorrow, ...)
   * @param calendar the time of the beginning of the week
   * @return a 2-lines string with the day (Mon., Tue., ...) on the first one and the date (17, 18,
   *   ...) on the second
   */
  fun getDateAtDay(day: Int, calendar: Calendar): String {
    val week =
        Calendar.getInstance().apply {
          timeInMillis = calendar.timeInMillis
          add(Calendar.DAY_OF_MONTH, day)
        }
    val dayLetter = SimpleDateFormat("E", Locale.ENGLISH).format(week.time)
    return "$dayLetter\n${week.get(Calendar.DAY_OF_MONTH)}"
  }

  /**
   * Check that an event is entirely on 1 day so that it can be displayed efficiently
   *
   * @param time the time of the beginning of the event
   * @param length the length of the event (in millisecond)
   */
  private fun isValidTime(time: Calendar, length: Long): Boolean {
    val comp = Calendar.getInstance()
    return length <= millisecInDay &&
        time.get(Calendar.DAY_OF_YEAR) ==
            comp.apply { timeInMillis = time.timeInMillis + length }.get(Calendar.DAY_OF_YEAR)
  }

  /**
   * Fill the weekly table with some elements
   *
   * @param list the list of elements to add the the table
   * @return a WeeklyTable containing required elements
   */
  private fun buildWeekTable(list: List<Scheduled>): WeeklyTable {
    val ret = MutableList(daysInWeek) { emptyList<Scheduled>() }

    val map =
        list
            .filter { isValidTime(it.start, it.length) }
            .groupBy {
              (it.start.get(Calendar.DAY_OF_WEEK) - _currentWeek.value.get(Calendar.DAY_OF_WEEK) +
                  daysInWeek) % daysInWeek
            }

    for ((key, value) in map) {
      ret[key] = value.sortedBy { it.start.timeInMillis }
    }

    return ret
  }
}
