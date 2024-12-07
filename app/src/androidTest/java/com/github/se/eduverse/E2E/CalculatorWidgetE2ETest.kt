package com.github.se.eduverse.E2E

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.se.eduverse.model.CommonWidgetType
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.ui.calculator.CalculatorScreen
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class CalculatorWidgetE2ETest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: FakeDashboardViewModel
  private lateinit var navigationActions: FakeNavigationActions

  @Before
  fun setup() {
    MockFirebaseAuth.setup()
    viewModel = FakeDashboardViewModel()
    navigationActions = FakeNavigationActions()

    composeTestRule.setContent {
      TestNavigation(viewModel = viewModel, navigationActions = navigationActions)
    }
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun testCalculatorWidgetFlow() {
    composeTestRule.apply {
      waitForIdle()

      // 1. Verify Empty State
      onNodeWithTag("empty_dashboard_message").assertIsDisplayed()
      onNodeWithTag("empty_state_add_button").assertIsDisplayed()
      onNodeWithTag("add_widget_button").assertDoesNotExist()

      // 2. Add Calculator Widget from Empty State
      onNodeWithTag("empty_state_add_button").performClick()

      // Get button with Calculator text from CommonWidgetType
      val calculatorWidget = CommonWidgetType.CALCULATOR
      onNodeWithText(calculatorWidget.title).performClick()

      // Verify transition from empty state to widget list
      onNodeWithTag("empty_dashboard_message").assertDoesNotExist()
      onNodeWithTag("widget_list").assertIsDisplayed()
      onNodeWithTag("add_widget_button").assertIsDisplayed()

      // Verify calculator widget appears on dashboard
      onNodeWithText(calculatorWidget.title).assertIsDisplayed()
      onNodeWithText(calculatorWidget.content).assertIsDisplayed()

      // 3. Open Calculator
      onNodeWithText("Calculator").performClick()
      navigationActions.navigateToCalculator()
      waitForIdle()

      // Verify calculator screen elements
      onNodeWithTag("display").assertExists()
      onNodeWithTag("displayText").assertExists()

      // 4. Perform Basic Calculation
      onNodeWithTag("button_7").performClick()
      onNodeWithTag("button_+").performClick()
      onNodeWithTag("button_3").performClick()
      onNodeWithTag("button_=").performClick()
      onNodeWithTag("resultText").assertTextContains("10")

      // 5. Return to Dashboard
      onNodeWithTag("goBackButton").performClick()
      navigationActions.navigateToDashboard()
      waitForIdle()

      // Re-verify the calculator widget is still there
      onNodeWithText("Calculator").assertIsDisplayed()

      // 6. Delete the Calculator widget
      onAllNodesWithTag("widget_card").onFirst().assertExists().performScrollTo()
      onAllNodesWithTag("delete_icon").onFirst().assertExists().performClick()

      // 7. Verify return to empty state
      onNodeWithTag("empty_dashboard_message").assertIsDisplayed()
      onNodeWithTag("empty_state_add_button").assertIsDisplayed()
      onNodeWithTag("add_widget_button").assertDoesNotExist()
      onNodeWithText("Calculator").assertDoesNotExist()
    }
  }
}

@HiltViewModel
class FakeDashboardViewModel @Inject constructor() : DashboardViewModel(mock()) {
  // Keep track of widgets persistently
  private var widgets = mutableListOf<Widget>()

  override fun fetchWidgets(userId: String) {
    _widgetList.value = widgets
  }

  override fun addWidget(widget: Widget) {
    widgets.add(widget.copy(order = widgets.size))
    _widgetList.value = widgets.toList()
  }

  override fun removeWidgetAndUpdateOrder(widgetId: String, updatedWidgets: List<Widget>) {
    widgets = updatedWidgets.toMutableList()
    _widgetList.value = widgets.toList()
  }

  override fun updateWidgetOrder(reorderedWidgets: List<Widget>) {
    widgets =
        reorderedWidgets.mapIndexed { index, widget -> widget.copy(order = index) }.toMutableList()
    _widgetList.value = widgets.toList()
  }

  override fun getCommonWidgets(): List<Widget> {
    return CommonWidgetType.values().map { commonWidget ->
      Widget(
          widgetId = commonWidget.name,
          widgetType = commonWidget.name,
          widgetTitle = commonWidget.title,
          widgetContent = commonWidget.content,
          ownerUid = "",
          order = 0)
    }
  }
}

@Composable
fun TestNavigation(viewModel: FakeDashboardViewModel, navigationActions: FakeNavigationActions) {
  var currentScreen by remember { mutableStateOf("DASHBOARD") }

  // Listen for navigation changes
  if (navigationActions is FakeNavigationActions) {
    currentScreen = navigationActions.currentRoute()
  }

  when (currentScreen) {
    "CALCULATOR" -> CalculatorScreen(navigationActions = navigationActions)
    else -> DashboardScreen(viewModel = viewModel, navigationActions = navigationActions)
  }
}

class FakeNavigationActions : NavigationActions(mockk(relaxed = true)) {
  private var _currentRoute = mutableStateOf("DASHBOARD")

  fun navigateToCalculator() {
    _currentRoute.value = "CALCULATOR"
  }

  fun navigateToDashboard() {
    _currentRoute.value = "DASHBOARD"
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

// Update MockFirebaseAuth to use MockK consistently
class MockFirebaseAuth {
  companion object {
    private var mockAuth: FirebaseAuth? = null
    private var mockUser: FirebaseUser? = null

    fun setup(isAuthenticated: Boolean = true) {
      cleanup() // Clean up previous mocks

      mockkStatic(FirebaseAuth::class)
      mockAuth = mock(FirebaseAuth::class.java)
      mockUser =
          mock(FirebaseUser::class.java).apply {
            `when`(getUid()).thenReturn("test_user_id")
            `when`(getEmail()).thenReturn("test@example.com")
            `when`(getDisplayName()).thenReturn("Test User")
            `when`(getPhoneNumber()).thenReturn(null)
            `when`(getPhotoUrl()).thenReturn(null)
            `when`(getProviderId()).thenReturn("firebase")
            `when`(isEmailVerified).thenReturn(true)
            `when`(isAnonymous).thenReturn(false)
            `when`(getMetadata()).thenReturn(null)
            `when`(getProviderData()).thenReturn(mutableListOf())
            `when`(getTenantId()).thenReturn(null)
          }

      every { FirebaseAuth.getInstance() } returns mockAuth!!
      `when`(mockAuth!!.currentUser).thenReturn(if (isAuthenticated) mockUser else null)
    }

    fun cleanup() {
      unmockkAll()
      mockAuth = null
      mockUser = null
    }
  }
}
