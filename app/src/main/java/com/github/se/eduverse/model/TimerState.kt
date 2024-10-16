package com.github.se.eduverse.model

data class TimerState(
    val remainingSeconds: Long = 0L,
    val isPaused: Boolean = true,
    val currentTimerType: TimerType = TimerType.POMODORO,
    val focusTime: Long = 25 * 60, // 25 minutes
    val shortBreakTime: Long = 5 * 60, // 5 minutes
    val longBreakTime: Long = 15 * 60, // 15 minutes
    val cycles: Int = 4,
    val currentCycle: Int = 1
)

enum class TimerType {
    POMODORO,
    SHORT_BREAK,
    LONG_BREAK
}