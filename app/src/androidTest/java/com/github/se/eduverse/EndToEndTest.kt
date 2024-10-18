package com.github.se.eduverse.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.ui.Pomodoro.PomodoroScreen
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestinations
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.github.se.eduverse.viewmodel.TimerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var dashboardViewModel: DashboardViewModel
  private lateinit var timerViewModel: TimerViewModel
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser

  @Before
  fun setup() {
    // Initialize Mockito
    MockitoAnnotations.openMocks(this)

    // Mock Firebase Auth
    mockUser = mock(FirebaseUser::class.java)
    `when`(mockUser.uid).thenReturn("test_user_id")

    firebaseAuth = mock(FirebaseAuth::class.java)
    `when`(firebaseAuth.currentUser).thenReturn(mockUser)

    // Mock Navigation Actions
    navigationActions = mock(NavigationActions::class.java)

    // Mock Dashboard ViewModel
    val widgetListFlow =
        MutableStateFlow(
            listOf(
                Widget(
                    widgetId = "1",
                    widgetTitle = "Calculator",
                    widgetContent = "Basic Calculator",
                    order = 0,
                    ownerUid = "test_user_id"),
                Widget(
                    widgetId = "2",
                    widgetTitle = "Pomodoro Timer",
                    widgetContent = "Focus Timer",
                    order = 1,
                    ownerUid = "test_user_id")))
    dashboardViewModel = mock(DashboardViewModel::class.java)
    `when`(dashboardViewModel.widgetList).thenReturn(widgetListFlow)

    // Mock Timer ViewModel
    val timerStateFlow =
        MutableStateFlow(
            TimerState(
                isPaused = true,
                remainingSeconds = 1500,
                focusTime = 1500,
                shortBreakTime = 300,
                longBreakTime = 900,
                cycles = 4))
    timerViewModel = mock(TimerViewModel::class.java)
    `when`(timerViewModel.timerState).thenReturn(timerStateFlow)
  }

  @Test
  fun navigateToPomodoroScreen_CheckTimerFunctionality() {
    // Start with Pomodoro Screen
    composeTestRule.setContent {
      PomodoroScreen(navigationActions = navigationActions, timerViewModel = timerViewModel)
    }

    // Verify Pomodoro screen elements
    composeTestRule.onNodeWithTag("pomodoroScreenContent").assertExists()
    composeTestRule.onNodeWithTag("timerDisplay").assertExists()
    composeTestRule.onNodeWithTag("timerText").assertExists()
    composeTestRule.onNodeWithTag("playPauseButton").assertExists()
    composeTestRule.onNodeWithTag("resetButton").assertExists()
    composeTestRule.onNodeWithTag("skipButton").assertExists()

    // Test timer controls
    composeTestRule.onNodeWithTag("playPauseButton").performClick()
    verify(timerViewModel).startTimer()

    composeTestRule.onNodeWithTag("resetButton").performClick()
    verify(timerViewModel).resetTimer()

    // Test settings dialog
    composeTestRule.onNodeWithTag("settingsButton").performClick()
    composeTestRule.onNodeWithTag("settingsDialog").assertExists()
    composeTestRule.onNodeWithTag("Focus Time Slider").assertExists()
    composeTestRule.onNodeWithTag("settingsSaveButton").performClick()
  }

  @Test
  fun testBottomNavigation() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          onTabSelect = { navigationActions.navigateTo(it) },
          tabList = LIST_TOP_LEVEL_DESTINATION,
          selectedItem = "Dashboard")
    }

    // Verify bottom navigation elements
    composeTestRule.onNodeWithTag("bottomNavigationMenu").assertExists()
    composeTestRule.onNodeWithText("Home").assertExists()
    composeTestRule.onNodeWithText("Videos").assertExists()
    composeTestRule.onNodeWithText("Camera").assertExists()
    composeTestRule.onNodeWithText("Others").assertExists()

    // Make sure to click and test the actual behavior
    composeTestRule.onNodeWithText("Others").performClick()

    // Ensure the navigation action is mocked and verify it
    verify(navigationActions).navigateTo(TopLevelDestinations.OTHERS)
  }
}
