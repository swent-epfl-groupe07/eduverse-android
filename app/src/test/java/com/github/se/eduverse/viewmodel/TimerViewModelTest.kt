import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import com.github.se.eduverse.viewmodel.TimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TimerViewModelTest {

  private lateinit var viewModel: TimerViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    viewModel = TimerViewModel(testScope, testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    testScope.cancel()
  }

  @Test
  fun `initial state is correct`() =
      testScope.runTest {
        val initialState = viewModel.timerState.first()
        assertEquals(TimerType.POMODORO, initialState.currentTimerType)
        assertEquals(1500L, initialState.remainingSeconds)
        assertEquals(true, initialState.isPaused)
        assertEquals(1, initialState.currentCycle)
      }

  @Test
  fun `startTimer updates state correctly`() =
      testScope.runTest {
        viewModel.startTimer()
        val stateAfterStart = viewModel.timerState.first()
        assertEquals(false, stateAfterStart.isPaused)
      }

  @Test
  fun `stopTimer updates state correctly`() =
      testScope.runTest {
        viewModel.startTimer()
        viewModel.stopTimer()
        val stateAfterStop = viewModel.timerState.first()
        assertEquals(true, stateAfterStop.isPaused)
      }

  @Test
  fun `resetTimer resets state to initial values`() =
      testScope.runTest {
        viewModel.updateSettings(1200, 300, 600, 3)
        viewModel.startTimer()
        viewModel.resetTimer()

        val resetState = viewModel.timerState.first()
        assertEquals(TimerType.POMODORO, resetState.currentTimerType)
        assertEquals(1200L, resetState.remainingSeconds)
        assertEquals(true, resetState.isPaused)
        assertEquals(1, resetState.currentCycle)
      }

  @Test
  fun `skipToNextTimer moves to short break after first pomodoro`() =
      testScope.runTest {
        viewModel.skipToNextTimer()
        val stateAfterSkip = viewModel.timerState.first()
        assertEquals(TimerType.SHORT_BREAK, stateAfterSkip.currentTimerType)
        assertEquals(300L, stateAfterSkip.remainingSeconds) // Default short break time
      }

  @Test
  fun `skipToNextTimer moves to pomodoro after short break`() =
      testScope.runTest {
        viewModel.skipToNextTimer() // Move to short break
        viewModel.skipToNextTimer() // Move back to pomodoro
        val stateAfterSecondSkip = viewModel.timerState.first()
        assertEquals(TimerType.POMODORO, stateAfterSecondSkip.currentTimerType)
        assertEquals(1500L, stateAfterSecondSkip.remainingSeconds) // Default focus time
        assertEquals(2, stateAfterSecondSkip.currentCycle)
      }

  @Test
  fun `skipToNextTimer moves to long break after completing all cycles`() =
      testScope.runTest {
        repeat(7) { viewModel.skipToNextTimer() } // 4 pomodoros and 3 short breaks
        val stateAfterCycles = viewModel.timerState.first()
        assertEquals(TimerType.LONG_BREAK, stateAfterCycles.currentTimerType)
        assertEquals(900L, stateAfterCycles.remainingSeconds) // Default long break time
      }

  @Test
  fun `timer counts down correctly`() =
      testScope.runTest {
        // Set up short duration for testing
        viewModel.updateSettings(5L, 3L, 10L, 4)

        // Collect states in background
        val states = mutableListOf<TimerState>()
        val job = launch {
          viewModel.timerState
              .take(8) // Increased to capture the transition state
              .collect {
                println(
                    "Collecting state: type=${it.currentTimerType}, seconds=${it.remainingSeconds}")
                states.add(it)
              }
        }

        // Start timer
        viewModel.startTimer()

        // Advance time with small increments to catch all states
        repeat(7) {
          advanceTimeBy(1000)
          advanceUntilIdle()
        }

        // Cancel collection
        job.cancel()

        // Print collected states for debugging
        println("\nCollected states:")
        states.forEachIndexed { index, state ->
          println(
              "State $index: type=${state.currentTimerType}, " +
                  "seconds=${state.remainingSeconds}, " +
                  "isPaused=${state.isPaused}")
        }

        // Verify the countdown sequence
        val countdownStates =
            states.filter { !it.isPaused && it.currentTimerType == TimerType.POMODORO }

        println("\nVerifying sequence:")
        countdownStates.forEachIndexed { index, state ->
          println("$index: ${state.remainingSeconds}")
        }

        // Should have exactly 6 states (5 down to 0)
        assertEquals("Should have 6 states", 6, countdownStates.size)

        // Verify each state in the sequence
        assertEquals(5L, countdownStates[0].remainingSeconds)
        assertEquals(4L, countdownStates[1].remainingSeconds)
        assertEquals(3L, countdownStates[2].remainingSeconds)
        assertEquals(2L, countdownStates[3].remainingSeconds)
        assertEquals(1L, countdownStates[4].remainingSeconds)
        assertEquals(0L, countdownStates[5].remainingSeconds)

        // Verify we transition to SHORT_BREAK after reaching 0
        val finalState = states.last()
        assertEquals(TimerType.SHORT_BREAK, finalState.currentTimerType)
      }

  @Test
  fun `timer finishes and moves to next state automatically`() =
      testScope.runTest {
        // Set up shorter duration
        viewModel.updateSettings(2L, 3L, 10L, 4)

        // Collect states
        val states = mutableListOf<TimerState>()
        val job = launch { viewModel.timerState.collect { states.add(it) } }

        // Start timer
        viewModel.startTimer()

        // Advance enough time to complete the countdown and transition
        repeat(3) { // Advance 3 seconds to ensure completion
          advanceTimeBy(1000)
          advanceUntilIdle()
        }

        // Cancel collection
        job.cancel()

        // Print states for debugging
        states.forEachIndexed { index, state ->
          println(
              "State $index: type=${state.currentTimerType}, " +
                  "remainingSeconds=${state.remainingSeconds}, " +
                  "isPaused=${state.isPaused}")
        }

        // Verify initial state
        val initialState = states.first()
        assertEquals(TimerType.POMODORO, initialState.currentTimerType)
        assertEquals(2L, initialState.remainingSeconds)

        // Find transition state (first SHORT_BREAK state)
        val transitionState = states.first { it.currentTimerType == TimerType.SHORT_BREAK }
        assertEquals(3L, transitionState.remainingSeconds)
        assertEquals(false, transitionState.isPaused)
      }

  @Test
  fun `updateSettings changes timer durations`() =
      testScope.runTest {
        viewModel.updateSettings(1800, 400, 1200, 3)
        val updatedState = viewModel.timerState.first()
        assertEquals(1800L, updatedState.focusTime)
        assertEquals(400L, updatedState.shortBreakTime)
        assertEquals(1200L, updatedState.longBreakTime)
        assertEquals(3, updatedState.cycles)
      }

  @Test
  fun `unbindTodoFromTimer sets currentTodoElapsedTime to null`() =
      testScope.runTest {
        viewModel.bindTodoToTimer(100L)
        viewModel.unbindTodoFromTimer()
        val elapsedTime = viewModel.currentTodoElapsedTime.first()
        assertEquals(null, elapsedTime)
      }

  @Test
  fun `bindTodoToTimer sets currentTodoElapsedTime correctly`() =
      testScope.runTest {
        viewModel.bindTodoToTimer(100L)
        val elapsedTime = viewModel.currentTodoElapsedTime.first()
        assertEquals(100L, elapsedTime)
      }

  @Test
  fun `updateCurrentTodoElapsedTime increments elapsed time correctly`() =
      testScope.runTest {
        viewModel.bindTodoToTimer(100L)
        viewModel.startTimer()
        advanceTimeBy(1001)
        val elapsedTime = viewModel.currentTodoElapsedTime.first()
        assertEquals(101L, elapsedTime)
      }

  @Test
  fun `pomodoroSessionActive returns true when pomodoro is active`() =
      testScope.runTest {
        viewModel.startTimer()
        val isActive = viewModel.pomodoroSessionActive()
        assertEquals(true, isActive)
      }
}
