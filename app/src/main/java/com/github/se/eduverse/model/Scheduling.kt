package com.github.se.eduverse.model

import java.util.Calendar

typealias WeeklyTable = List<List<List<Scheduled>>>

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