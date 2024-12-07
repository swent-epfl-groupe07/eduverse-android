package com.github.se.eduverse.E2E

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
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
  fun testPomodoroTimerFlow() {
    composeTestRule.apply {
      waitForIdle()

      // 1. Verify Empty State
      onNodeWithTag("empty_dashboard_message").assertIsDisplayed()
      onNodeWithTag("empty_state_add_button").assertIsDisplayed()
      onNodeWithTag("add_widget_button").assertDoesNotExist()

      // 2. Add Timer Widget from Empty State
      onNodeWithTag("empty_state_add_button").performClick()
      val timerWidget = CommonWidgetType.TIMER
      onNodeWithText(timerWidget.title).performClick()

      // Verify transition from empty state to widget list
      onNodeWithTag("empty_dashboard_message").assertDoesNotExist()
      onNodeWithTag("widget_list").assertIsDisplayed()
      onNodeWithTag("add_widget_button").assertIsDisplayed()

      // Verify timer widget appears on dashboard
      onNodeWithText(timerWidget.title).assertIsDisplayed()
      onNodeWithText(timerWidget.content).assertIsDisplayed()

      // 3. Open Timer
      onNodeWithText("Study Timer").performClick()
      navigationActions.navigateToPomodoro()
      waitForIdle()
      waitForIdle()
      waitForIdle()

      // Verify initial state
      onNodeWithTag("timerText").assertTextContains("25:00")
      onNodeWithTag("cycleText").assertTextContains("Cycle: 1/4")
      onNodeWithTag("focusIcon").assertExists()

      // 4. Test timer controls
      val initialTime =
          onNodeWithTag("timerText")
              .fetchSemanticsNode()
              .config
              .first { it.key.name == "Text" }
              .value
              .toString()

      // Start timer
      onNodeWithTag("playPauseButton").performClick()
      waitForIdle()

      // Verify timer started
      val afterStartTime =
          onNodeWithTag("timerText")
              .fetchSemanticsNode()
              .config
              .first { it.key.name == "Text" }
              .value
              .toString()
      assert(initialTime != afterStartTime) { "Timer value should change after starting" }

      // Pause timer
      onNodeWithTag("playPauseButton").performClick()
      waitForIdle()

      // 5. Test timer settings
      onNodeWithTag("settingsButton").performClick()
      waitForIdle()

      onNode(hasTestTag("Focus Time Slider"), useUnmergedTree = true).performSemanticsAction(
          SemanticsActions.SetProgress) {
            it(1.8f)
          }
      waitForIdle()

      onNode(hasTestTag("settingsSaveButton"), useUnmergedTree = true).performClick()
      waitForIdle()

      // 6. Test timer type changes
      onNodeWithTag("skipButton").performClick()
      waitForIdle()

      // Verify short break mode
      onNodeWithTag("shortBreakIcon").assertExists()
      onNodeWithTag("timerText").assertTextContains("5:00")

      // 7. Test reset
      onNodeWithTag("resetButton").performClick()
      waitForIdle()

      // Verify reset state
      onNodeWithTag("timerText").assertTextContains("2:00")
      onNodeWithTag("focusIcon").assertExists()

      // 8. Return to dashboard
      onNodeWithTag("goBackButton").performClick()
      navigationActions.goBack()
      waitForIdle()

      // 9. Delete widget and verify empty state
      onAllNodesWithTag("delete_icon").onFirst().performScrollTo().performClick()

      // Verify transition back to empty state
      onNodeWithTag("empty_dashboard_message").assertIsDisplayed()
      onNodeWithTag("empty_state_add_button").assertIsDisplayed()
      onNodeWithTag("add_widget_button").assertDoesNotExist()
      onNodeWithTag("widget_card").assertDoesNotExist()
    }
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
