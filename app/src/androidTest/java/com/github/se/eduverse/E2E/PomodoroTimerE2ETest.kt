package com.github.se.eduverse.E2E

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.github.se.eduverse.model.CommonWidgetType
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.ui.pomodoro.PomodoroScreen
import com.github.se.eduverse.viewmodel.TimerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.mockk.mockk
import io.mockk.unmockkAll
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class PomodoroTimerE2ETest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: FakeDashboardViewModel
  private lateinit var timerViewModel: FakeTimerViewModel
  private lateinit var navigationActions: FakePomodoroNavigationActions

  @Before
  fun setup() {
    MockFirebaseAuth.setup()
    viewModel = FakeDashboardViewModel()
    timerViewModel = FakeTimerViewModel()
    navigationActions = FakePomodoroNavigationActions()

    composeTestRule.setContent {
      TestPomodoroNavigation(
          dashboardViewModel = viewModel,
          timerViewModel = timerViewModel,
          navigationActions = navigationActions)
    }
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

    @Test
    fun testBasicPomodoroFlow() {
        composeTestRule.apply {
            // Add timer widget and navigate to timer screen
            addTimerWidgetAndNavigate()

            // Verify initial state
            verifyInitialTimerState()

            // Basic timer control test
            testBasicTimerControls()

            // Return to dashboard and verify widget exists
            returnToDashboardAndVerify()

            // Clean up
            deleteWidgetAndVerifyEmptyState()
        }
    }

    @Test
    fun testTimerInterruptionsAndTransitions() {
        composeTestRule.apply {
            addTimerWidgetAndNavigate()

            // Start timer and immediately pause
            onNodeWithTag("playPauseButton").performClick()
            waitForIdle()
            onNodeWithTag("playPauseButton").performClick()
            waitForIdle()

            // Verify timer is paused
            verifyTimerState(isPaused = true)

            // Try to skip while paused
            onNodeWithTag("skipButton").performClick()
            waitForIdle()

            // Verify transition to short break
            onNodeWithTag("shortBreakIcon").assertExists()
            onNodeWithTag("timerText").assertTextContains("5:00")

            // Start timer and try to navigate away
            onNodeWithTag("playPauseButton").performClick()
            waitForIdle()

            // Navigate to dashboard and back
            onNodeWithTag("goBackButton").performClick()
            navigationActions.goBack()
            waitForIdle()

            onNodeWithText("Study Timer").performClick()
            navigationActions.navigateToPomodoro()
            waitForIdle()

            // Verify timer state persisted
            onNodeWithTag("shortBreakIcon").assertExists()
            verifyTimerState(isPaused = false)
        }
    }

    @Test
    fun testSettingsInteractionDuringActiveTimer() {
        composeTestRule.apply {
            addTimerWidgetAndNavigate()

            // Start the timer
            onNodeWithTag("playPauseButton").performClick()
            waitForIdle()

            // Open settings during active timer
            onNodeWithTag("settingsButton").performClick()
            waitForIdle()

            // Change focus time while timer is running
            onNode(hasTestTag("Focus Time Slider"), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(2.0f) }
            waitForIdle()

            // Save settings
            onNode(hasTestTag("settingsSaveButton"), useUnmergedTree = true).performClick()
            waitForIdle()

            // Verify timer reset to new settings
            onNodeWithTag("timerText").assertTextContains("2:00")
            verifyTimerState(isPaused = true)
        }
    }

    @Test
    fun testCompletePomodoroSessionCycle() {
        composeTestRule.apply {
            addTimerWidgetAndNavigate()

            // Set shorter times for testing
            adjustTimerSettings(focusTime = 1, shortBreak = 1, longBreak = 2, cycles = 2)

            // Complete first pomodoro
            startAndCompleteTimer()
            onNodeWithTag("shortBreakIcon").assertExists()

            // Complete short break
            startAndCompleteTimer()
            onNodeWithTag("focusIcon").assertExists()

            // Complete second pomodoro
            startAndCompleteTimer()
            onNodeWithTag("longBreakIcon").assertExists()

            // Verify cycle count
            onNodeWithTag("cycleText").assertTextContains("Cycle: 1/2")        }
    }

    // Helper functions
    private fun ComposeTestRule.addTimerWidgetAndNavigate() {
        waitForIdle()
        onNodeWithTag("empty_state_add_button").performClick()
        val timerWidget = CommonWidgetType.TIMER
        onNodeWithText(timerWidget.title).performClick()
        onNodeWithText("Study Timer").performClick()
        navigationActions.navigateToPomodoro()
        waitForIdle()
    }

    private fun ComposeTestRule.verifyInitialTimerState() {
        onNodeWithTag("timerText").assertTextContains("25:00")
        onNodeWithTag("cycleText").assertTextContains("Cycle: 1/4")
        onNodeWithTag("focusIcon").assertExists()
        verifyTimerState(isPaused = true)
    }

    private fun ComposeTestRule.verifyTimerState(isPaused: Boolean) {
        onNodeWithTag("playPauseButton")
            .assertContentDescriptionContains(if (isPaused) "Start" else "Pause")
    }

    private fun ComposeTestRule.testBasicTimerControls() {
        // Start timer
        onNodeWithTag("playPauseButton").performClick()
        waitForIdle()
        verifyTimerState(isPaused = false)

        // Pause timer
        onNodeWithTag("playPauseButton").performClick()
        waitForIdle()
        verifyTimerState(isPaused = true)

        // Reset timer
        onNodeWithTag("resetButton").performClick()
        waitForIdle()
        onNodeWithTag("timerText").assertTextContains("25:00")
    }

    private fun ComposeTestRule.returnToDashboardAndVerify() {
        onNodeWithTag("goBackButton").performClick()
        navigationActions.goBack()
        waitForIdle()
        onNodeWithText("Study Timer").assertIsDisplayed()
    }

    private fun ComposeTestRule.deleteWidgetAndVerifyEmptyState() {
        onAllNodesWithTag("delete_icon").onFirst().performScrollTo().performClick()
        onNodeWithTag("empty_dashboard_message").assertIsDisplayed()
        onNodeWithTag("empty_state_add_button").assertIsDisplayed()
    }

    private fun ComposeTestRule.adjustTimerSettings(
        focusTime: Long,
        shortBreak: Long,
        longBreak: Long,
        cycles: Int
    ) {
        onNodeWithTag("settingsButton").performClick()
        waitForIdle()

        onNode(hasTestTag("Focus Time Slider"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(focusTime.toFloat()) }
        onNode(hasTestTag("Short Break Slider"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(shortBreak.toFloat()) }
        onNode(hasTestTag("Long Break Slider"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(longBreak.toFloat()) }
        onNode(hasTestTag("Cycles Slider"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(cycles.toFloat()) }

        onNode(hasTestTag("settingsSaveButton"), useUnmergedTree = true).performClick()
        waitForIdle()
    }

    private fun ComposeTestRule.startAndCompleteTimer() {
        onNodeWithTag("playPauseButton").performClick()
        waitForIdle()
        // Wait for timer completion (handled by FakeTimerViewModel)
        waitForIdle()
    }
}

@HiltViewModel
class FakeTimerViewModel @Inject constructor() : TimerViewModel(mock()) {
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
  override val timerState: StateFlow<TimerState> = _timerState

  override fun startTimer() {
    _timerState.value = _timerState.value.copy(isPaused = false)
    // Simulate time passing
    _timerState.value =
        _timerState.value.copy(
            remainingSeconds =
                _timerState.value.remainingSeconds - 60 // Decrease by 1 minute for clear testing
            )
  }

  override fun stopTimer() {
    _timerState.value = _timerState.value.copy(isPaused = true)
  }

  override fun resetTimer() {
    val currentState = _timerState.value
    _timerState.value =
        currentState.copy(
            remainingSeconds =
                currentState.focusTime, // Use focus time as we're resetting to POMODORO
            isPaused = true,
            currentTimerType = TimerType.POMODORO, // Always reset to POMODORO
            currentCycle = 1)
  }

  override fun skipToNextTimer() {
    val currentState = _timerState.value
    val (newType, newTime) =
        when (currentState.currentTimerType) {
          TimerType.POMODORO -> TimerType.SHORT_BREAK to currentState.shortBreakTime
          TimerType.SHORT_BREAK -> TimerType.LONG_BREAK to currentState.longBreakTime
          TimerType.LONG_BREAK -> TimerType.POMODORO to currentState.focusTime
        }
    _timerState.value =
        currentState.copy(currentTimerType = newType, remainingSeconds = newTime, isPaused = true)
  }

  override fun updateSettings(focusTime: Long, shortBreak: Long, longBreak: Long, cycles: Int) {
    _timerState.value =
        _timerState.value.copy(
            focusTime = focusTime,
            shortBreakTime = shortBreak,
            longBreakTime = longBreak,
            cycles = cycles,
            remainingSeconds = focusTime, // Set remaining seconds to new focus time
            isPaused = true,
            currentTimerType = TimerType.POMODORO)
  }

  companion object {
    private const val DEFAULT_FOCUS_TIME = 1500L // 25 minutes
    private const val DEFAULT_SHORT_BREAK_TIME = 300L // 5 minutes
    private const val DEFAULT_LONG_BREAK_TIME = 900L // 15 minutes
    private const val DEFAULT_CYCLES = 4
  }
}

class FakePomodoroNavigationActions : NavigationActions(mockk(relaxed = true)) {
  private var _currentRoute = mutableStateOf("DASHBOARD")

  fun navigateToPomodoro() {
    _currentRoute.value = "POMODORO"
  }

  override fun navigateTo(destination: TopLevelDestination) {
    _currentRoute.value = destination.route
  }

  override fun navigateTo(route: String) {
    _currentRoute.value = route
  }

  override fun goBack() {
    _currentRoute.value = "DASHBOARD"
  }

  override fun currentRoute(): String = _currentRoute.value
}

@Composable
fun TestPomodoroNavigation(
    dashboardViewModel: FakeDashboardViewModel,
    timerViewModel: FakeTimerViewModel,
    navigationActions: FakePomodoroNavigationActions
) {
  var currentScreen by remember { mutableStateOf("DASHBOARD") }

  if (navigationActions is FakePomodoroNavigationActions) {
    currentScreen = navigationActions.currentRoute()
  }

  when (currentScreen) {
    "POMODORO" ->
        PomodoroScreen(navigationActions = navigationActions, timerViewModel = timerViewModel)
    else -> DashboardScreen(viewModel = dashboardViewModel, navigationActions = navigationActions)
  }
}
