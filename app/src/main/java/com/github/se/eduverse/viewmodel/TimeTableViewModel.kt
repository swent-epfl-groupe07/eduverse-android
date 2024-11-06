package com.github.se.eduverse.viewmodel

import android.util.Log
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.WeeklyTable
import com.github.se.eduverse.model.daysInWeek
import com.github.se.eduverse.model.emptyWeeklyTable
import com.github.se.eduverse.model.millisecInDay
import com.github.se.eduverse.repository.EventRepository
import com.github.se.eduverse.repository.TimeTableRepository
import com.github.se.eduverse.repository.TodoRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class TimeTableViewModel(
    val timeTableRepository: TimeTableRepository,
    val todoRepository: TodoRepository,
    val eventRepository: EventRepository,
    val auth: FirebaseAuth
) {
    private val currentWeek = Calendar.getInstance()

    // A 3-dimensional table with dimension corresponding to day, hour and events
    private val _table: MutableStateFlow<WeeklyTable> =
        MutableStateFlow(emptyWeeklyTable())
    val table: StateFlow<WeeklyTable> = _table


    /**
     * Access all scheduled events and tasks in the current week for rhe active user
     */
    fun getWeek() {
        timeTableRepository.getScheduled(
            currentWeek,
            auth.currentUser!!.uid,
            {
                _table.value = buildWeekTable(it)
            },
            { Log.e("TimeTableViewModel",
                "Exception $it while trying to load data for the week") }
        )
    }

    /**
     * Change the active week to the next one, and access the scheduled events and tasks
     * in that week
     */
    fun getNextWeek() {
        currentWeek.add(Calendar.WEEK_OF_YEAR, 1)
        getWeek()
    }

    /**
     * Change the active week to the previous one, and access the scheduled events and tasks
     * in that week
     */
    fun getPreviousWeek() {
        currentWeek.add(Calendar.WEEK_OF_YEAR, -1)
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
                    var currentTime = currentWeek.timeInMillis
                    val addedTime = scheduled.start.timeInMillis
                    for (i in 0..<daysInWeek) {
                        if (currentTime <= addedTime && addedTime < currentTime + millisecInDay) {
                            _table.value[i][scheduled.start.get(Calendar.HOUR_OF_DAY)].add(scheduled)
                        }
                        currentTime += millisecInDay
                    }
                }
            },
            { Log.e("TimeTableViewModel",
                "Exception $it while trying to schedule an event") }
        )
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
                for (day in _table.value) {
                    for (hour in day) {
                        hour.map {
                            return@map if (it.id == scheduled.id) scheduled else it
                        }
                    }
                }
            },
            { Log.e("TimeTableViewModel",
                "Exception $it while trying to modify a scheduled event") }
        )
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
                for (day in _table.value) {
                    for (hour in day) {
                        hour.removeIf { it.id == scheduled.id }
                    }
                }
            },
            { Log.e("TimeTableViewModel",
                "Exception $it while trying to delete a scheduled event") }
        )
    }


    /**
     * Check that an event is entirely on 1 day so that it can be displayed efficiently
     *
     * @param time the time of the beginning of the event
     * @param length the length of the event (in millisecond)
     */
    private fun isValidTime(time: Calendar, length: Long): Boolean {
        return length <= millisecInDay &&
        time.get(Calendar.DAY_OF_YEAR) == time.apply { timeInMillis += length }.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Fill the weekly table with some elements
     *
     * @param list the list of elements to add the the table
     * @return a WeeklyTable containing required elements
     */
    private fun buildWeekTable(list: List<Scheduled>): WeeklyTable {
        val ret = emptyWeeklyTable()

        list.forEach {
            if (isValidTime(it.start, it.length)) {
                val day = (it.start.get(Calendar.DAY_OF_WEEK) - currentWeek.get(Calendar.DAY_OF_WEEK)) % daysInWeek
                val hour = it.start.get(Calendar.HOUR_OF_DAY)
                ret[day][hour].add(it)
            } else Log.d("TimeTableViewModel", "A scheduled event occurring on more than one day was ignored")
        }
        return ret
    }

}