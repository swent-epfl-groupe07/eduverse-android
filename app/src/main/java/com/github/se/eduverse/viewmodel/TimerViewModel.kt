package com.github.se.eduverse.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TimerViewModel() : ViewModel() {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState = _timerState.asStateFlow()
    private var pomodoroTimer: CountDownTimer? = null

    init {
        _timerState.update {
            it.copy(
                remainingSeconds = POMODORO_TIMER_SECONDS,
                lastTimer = TimerType.POMODORO
            )
        }
    }

    fun startTimer(seconds: Long) {
        _timerState.update {
            it.copy(
                isPaused = false
            )
        }
        pomodoroTimer = object : CountDownTimer(
            seconds * MILLISECONDS_IN_A_SECOND,
            MILLISECONDS_IN_A_SECOND
        ) {
            override fun onTick(millisUntilFinished: Long) {
                _timerState.update {
                    it.copy(
                        remainingSeconds = millisUntilFinished / MILLISECONDS_IN_A_SECOND
                    )
                }
            }

            override fun onFinish() {
                pomodoroTimer?.cancel()
                _timerState.update {
                    it.copy(
                        isPaused = true,
                        remainingSeconds =
                        if (it.lastTimer == TimerType.POMODORO)
                            REST_TIMER_SECONDS
                        else
                            POMODORO_TIMER_SECONDS,
                        lastTimer =
                        if (it.lastTimer == TimerType.POMODORO)
                            TimerType.REST
                        else
                            TimerType.POMODORO
                    )
                }
            }
        }
        pomodoroTimer?.start()
    }

    fun stopTimer() {
        _timerState.update {
            it.copy(
                isPaused = true
            )
        }
        pomodoroTimer?.cancel()
    }

    fun resetTimer(seconds: Long) {
        pomodoroTimer?.cancel()
        _timerState.update {
            it.copy(
                isPaused = true,
                remainingSeconds = seconds,
                lastTimer = TimerType.POMODORO
            )
        }
    }
}

const val MILLISECONDS_IN_A_SECOND = 1000L
const val POMODORO_TIMER_SECONDS = 1500L
const val REST_TIMER_SECONDS = 300L
const val SECONDS_IN_A_MINUTE = 60L