package com.github.se.eduverse.model

import java.util.Calendar

val daysInWeek = 7
val hoursInDay = 24

data class Scheduled(
    val id: String,
    val type: ScheduledType,
    val start: Calendar,
    val length: Long,
    val taskOrEventId: String,
    val ownerId: String
)


enum class ScheduledType {
    TASK,
    EVENT
}


typealias WeeklyTable = List<List<MutableList<Scheduled>>> // mutable list to have a setter

fun emptyWeeklyTable(): WeeklyTable {
    return List(daysInWeek) { List(hoursInDay) { emptyList<Scheduled>().toMutableList() } }
}