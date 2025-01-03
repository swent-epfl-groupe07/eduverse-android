package com.github.se.eduverse.model

import java.util.Calendar

val daysInWeek = 7
val millisecInDay = 86400000 // The number of milliseconds in a day
val millisecInHour = 3600000
val millisecInMin = 60000

data class Scheduled(
    val id: String,
    val type: ScheduledType,
    val start: Calendar,
    var length: Long,
    var content: String, // The id of the task or the description of the event
    val ownerId: String,
    var name: String
)

enum class ScheduledType {
  TASK,
  EVENT
}

typealias WeeklyTable = List<List<Scheduled>>

fun emptyWeeklyTable(): WeeklyTable {
  return List(daysInWeek) { emptyList<Scheduled>().toMutableList() }
}
