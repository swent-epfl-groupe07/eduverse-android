import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import com.github.se.eduverse.ui.Pomodoro.PomodoroScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.TimerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class PomodoroScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeViewModel =
      FakeTimerViewModel(
          TimerState(
              remainingSeconds = 1500,
              isPaused = true,
              currentTimerType = TimerType.POMODORO,
              focusTime = 1500,
              shortBreakTime = 300,
              longBreakTime = 900,
              cycles = 4,
              currentCycle = 1))
  private val mockNavigationActions = FakeNavigationActions(navController = mock())

  @Test
  fun testPomodoroScreenInitialState() {
    setupPomodoroScreen()

    composeTestRule.onNodeWithTag("topBarTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("backButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timerDisplay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timerText").assertTextEquals("25:00")
    composeTestRule.onNodeWithTag("cycleText").assertTextEquals("Cycle: 1/4")
    composeTestRule.onNodeWithTag("playPauseButton").assertIsDisplayed()
  }

  @Test
  fun testPlayPauseButton() {
    setupPomodoroScreen()

    // Simulate button click to start the timer
    composeTestRule.onNodeWithTag("playPauseButton").performClick()
    assert(!fakeViewModel.timerState.value.isPaused)

    // Simulate button click to stop the timer
    composeTestRule.onNodeWithTag("playPauseButton").performClick()
    assert(fakeViewModel.timerState.value.isPaused)
  }

  @Test
  fun testOpenAndCloseSettingsDialog() {
    setupPomodoroScreen()

    // Open the settings dialog
    composeTestRule.onNodeWithTag("settingsButton").performClick()
    composeTestRule.onNodeWithTag("settingsDialog").assertIsDisplayed()

    // Close the dialog
    composeTestRule.onNodeWithTag("settingsCancelButton").performClick()
    composeTestRule.onNodeWithTag("settingsDialog").assertDoesNotExist()
  }

  @Test
  fun testUpdateSettingsInDialog() {
    setupPomodoroScreen()

    // Open settings dialog
    composeTestRule.onNodeWithTag("settingsButton").performClick()

    // Interact with sliders
    composeTestRule.onNodeWithTag("Focus Time Slider").performGesture {
      this.swipeRight()
    } // 60 minutes
    composeTestRule.onNodeWithTag("Short Break Slider").performGesture {
      this.swipeRight()
    } // 30 minutes

    // Save settings
    composeTestRule.onNodeWithTag("settingsSaveButton").performClick()

    // Verify if ViewModel was updated
    assert(fakeViewModel.timerState.value.focusTime == 3600L)
    assert(fakeViewModel.timerState.value.shortBreakTime == 1800L)
  }

  @Test
  fun testResetAndSkipButtons() {
    setupPomodoroScreen()

    // Reset Timer
    composeTestRule.onNodeWithTag("resetButton").performClick()
    assert(fakeViewModel.timerState.value.remainingSeconds.toInt() == 1500)

    // Skip Timer
    composeTestRule.onNodeWithTag("skipButton").performClick()
    assert(fakeViewModel.timerState.value.currentTimerType == TimerType.SHORT_BREAK)
  }

  private fun setupPomodoroScreen() {
    composeTestRule.setContent {
      PomodoroScreen(navigationActions = mockNavigationActions, timerViewModel = fakeViewModel)
    }
  }
}

class FakeTimerViewModel(initialState: TimerState) : TimerViewModel() {
  private val _timerState = MutableStateFlow(initialState)
  override val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

  override fun startTimer() {
    _timerState.value = _timerState.value.copy(isPaused = false)
  }

  override fun stopTimer() {
    _timerState.value = _timerState.value.copy(isPaused = true)
  }

  override fun resetTimer() {
    _timerState.value = _timerState.value.copy(remainingSeconds = _timerState.value.focusTime)
  }

  override fun skipToNextTimer() {
    when (_timerState.value.currentTimerType) {
      TimerType.POMODORO ->
          _timerState.value = _timerState.value.copy(currentTimerType = TimerType.SHORT_BREAK)
      TimerType.SHORT_BREAK ->
          if (_timerState.value.currentCycle < _timerState.value.cycles) {
            _timerState.value =
                _timerState.value.copy(
                    currentTimerType = TimerType.POMODORO,
                    currentCycle = _timerState.value.currentCycle + 1)
          } else {
            _timerState.value =
                _timerState.value.copy(currentTimerType = TimerType.LONG_BREAK, currentCycle = 1)
          }
      TimerType.LONG_BREAK ->
          _timerState.value = _timerState.value.copy(currentTimerType = TimerType.POMODORO)
    }
  }

  override fun updateSettings(
      focusTime: Long,
      shortBreakTime: Long,
      longBreakTime: Long,
      cycles: Int
  ) {
    _timerState.value =
        _timerState.value.copy(
            focusTime = focusTime,
            shortBreakTime = shortBreakTime,
            longBreakTime = longBreakTime,
            cycles = cycles)
  }
}

class FakeNavigationActions(navController: NavHostController) : NavigationActions(navController) {
  fun navigate(route: String) {
    // No-op for testing
  }
}
