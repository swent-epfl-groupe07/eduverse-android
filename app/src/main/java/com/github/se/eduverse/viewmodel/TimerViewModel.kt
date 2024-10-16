package com.github.se.eduverse.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TimerViewModel : ViewModel() {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState = _timerState.asStateFlow()
    private var pomodoroTimer: CountDownTimer? = null

    init {
        resetTimer()
    }

    fun startTimer() {
        _timerState.update { it.copy(isPaused = false) }
        pomodoroTimer = object : CountDownTimer(
            _timerState.value.remainingSeconds * MILLISECONDS_IN_A_SECOND,
            MILLISECONDS_IN_A_SECOND
        ) {
            override fun onTick(millisUntilFinished: Long) {
                _timerState.update {
                    it.copy(remainingSeconds = millisUntilFinished / MILLISECONDS_IN_A_SECOND)
                }
            }

            override fun onFinish() {
                pomodoroTimer?.cancel()
                moveToNextTimer()
            }
        }
        pomodoroTimer?.start()
    }

    fun stopTimer() {
        _timerState.update { it.copy(isPaused = true) }
        pomodoroTimer?.cancel()
    }

    fun resetTimer() {
        pomodoroTimer?.cancel()
        _timerState.update {
            it.copy(
                isPaused = true,
                remainingSeconds = it.focusTime,
                currentTimerType = TimerType.POMODORO,
                currentCycle = 1
            )
        }
    }

    fun skipToNextTimer() {
        pomodoroTimer?.cancel()
        moveToNextTimer()
    }

    private fun moveToNextTimer() {
        val currentState = _timerState.value
        val newState = when (currentState.currentTimerType) {
            TimerType.POMODORO -> {
                if (currentState.currentCycle == currentState.cycles) {
                    // All cycles completed, move to long break and stop
                    currentState.copy(
                        currentTimerType = TimerType.LONG_BREAK,
                        remainingSeconds = currentState.longBreakTime,
                        isPaused = true // Stop the timer
                    )
                } else if (currentState.currentCycle % currentState.cycles == 0) {
                    currentState.copy(
                        currentTimerType = TimerType.LONG_BREAK,
                        remainingSeconds = currentState.longBreakTime
                    )
                } else {
                    currentState.copy(
                        currentTimerType = TimerType.SHORT_BREAK,
                        remainingSeconds = currentState.shortBreakTime
                    )
                }
            }
            TimerType.SHORT_BREAK, TimerType.LONG_BREAK -> {
                if (currentState.currentCycle == currentState.cycles) {
                    // All cycles completed, stop the timer
                    currentState.copy(
                        isPaused = true,
                        remainingSeconds = currentState.focusTime,
                        currentTimerType = TimerType.POMODORO
                    )
                } else {
                    currentState.copy(
                        currentTimerType = TimerType.POMODORO,
                        remainingSeconds = currentState.focusTime,
                        currentCycle = currentState.currentCycle + 1
                    )
                }
            }
        }
        _timerState.update { newState }
        if (!newState.isPaused) {
            startTimer()
        }
    }

    fun updateSettings(
        focusTime: Long,
        shortBreakTime: Long,
        longBreakTime: Long,
        cycles: Int
    ) {
        _timerState.update {
            it.copy(
                focusTime = focusTime,
                shortBreakTime = shortBreakTime,
                longBreakTime = longBreakTime,
                cycles = cycles
            )
        }
        resetTimer()
    }
}

private const val MILLISECONDS_IN_A_SECOND = 1000L