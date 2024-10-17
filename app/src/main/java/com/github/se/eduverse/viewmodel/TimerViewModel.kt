package com.github.se.eduverse.viewmodel

import androidx.lifecycle.ViewModel
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class TimerViewModel(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
  private val _timerState =
      MutableStateFlow(
          TimerState(
              isPaused = true,
              remainingSeconds = DEFAULT_FOCUS_TIME,
              currentTimerType = TimerType.POMODORO,
              focusTime = DEFAULT_FOCUS_TIME,
              shortBreakTime = DEFAULT_SHORT_BREAK_TIME,
              longBreakTime = DEFAULT_LONG_BREAK_TIME,
              cycles = DEFAULT_CYCLES,
              currentCycle = 1))
  open val timerState = _timerState.asStateFlow()

  private var timerJob: Job? = null

  open fun startTimer() {
    if (timerJob?.isActive == true) return

    _timerState.update { it.copy(isPaused = false) }
    timerJob =
        coroutineScope.launch(dispatcher) {
          while (_timerState.value.remainingSeconds > 0) {
            delay(1000)
            _timerState.update { currentState ->
              currentState.copy(
                  remainingSeconds = (currentState.remainingSeconds - 1).coerceAtLeast(0))
            }
          }
          delay(100)
          // Only proceed with transition if we actually reached zero
          if (_timerState.value.remainingSeconds == 0L) {
            skipToNextTimer()
          }
        }
  }

  open fun stopTimer() {
    _timerState.update { it.copy(isPaused = true) }
    timerJob?.cancel()
  }

  open fun resetTimer() {
    timerJob?.cancel()
    _timerState.update {
      it.copy(
          isPaused = true,
          remainingSeconds = it.focusTime,
          currentTimerType = TimerType.POMODORO,
          currentCycle = 1)
    }
  }

  open fun skipToNextTimer() {
    timerJob?.cancel()
    moveToNextTimer()
  }

  private fun moveToNextTimer() {
    val currentState = _timerState.value
    val newState =
        when (currentState.currentTimerType) {
          TimerType.POMODORO -> {
            if (currentState.currentCycle >= currentState.cycles) {
              currentState.copy(
                  currentTimerType = TimerType.LONG_BREAK,
                  remainingSeconds = currentState.longBreakTime,
                  isPaused = true,
                  currentCycle = 1)
            } else {
              currentState.copy(
                  currentTimerType = TimerType.SHORT_BREAK,
                  remainingSeconds = currentState.shortBreakTime,
                  isPaused = false,
                  currentCycle = currentState.currentCycle)
            }
          }
          TimerType.SHORT_BREAK -> {
            currentState.copy(
                currentTimerType = TimerType.POMODORO,
                remainingSeconds = currentState.focusTime,
                isPaused = false,
                currentCycle = currentState.currentCycle + 1)
          }
          TimerType.LONG_BREAK -> {
            currentState.copy(
                currentTimerType = TimerType.POMODORO,
                remainingSeconds = currentState.focusTime,
                isPaused = true,
                currentCycle = 1)
          }
        }
    _timerState.update { newState }

    // Only start the timer if the new state isn't paused
    if (!newState.isPaused) {
      startTimer()
    }
  }

  open fun updateSettings(focusTime: Long, shortBreakTime: Long, longBreakTime: Long, cycles: Int) {
    _timerState.update {
      it.copy(
          focusTime = focusTime,
          shortBreakTime = shortBreakTime,
          longBreakTime = longBreakTime,
          cycles = cycles)
    }
    resetTimer()
  }
}

private const val DEFAULT_FOCUS_TIME = 1500L // Default 25 minutes
private const val DEFAULT_SHORT_BREAK_TIME = 300L // Default 5 minutes
private const val DEFAULT_LONG_BREAK_TIME = 900L // Default 15 minutes
private const val DEFAULT_CYCLES = 4
