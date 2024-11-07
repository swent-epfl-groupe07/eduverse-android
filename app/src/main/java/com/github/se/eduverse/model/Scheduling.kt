package com.github.se.eduverse.model

import java.util.Calendar

val daysInWeek = 7
val millisecInDay = 86400000 // The number of milliseconds in a day
val millisecInHour = 3600000

data class Scheduled(
    val id: String,
    val type: ScheduledType,
    val start: Calendar,
    val length: Long,
    val taskOrEventId: String,
    val ownerId: String,
    val name: String
)


enum class ScheduledType {
    TASK,
    EVENT
}


typealias WeeklyTable = List<List<Scheduled>>

fun emptyWeeklyTable(): WeeklyTable {
    return List(daysInWeek) { emptyList<Scheduled>().toMutableList() }
}