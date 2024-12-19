package com.github.se.eduverse.E2E

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
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
  fun testHappyPathCalculatorWidgetFlow() {
    composeTestRule.apply {
      // Initial setup verification
      verifyInitialDashboardState()

      // Add calculator widget
      addCalculatorWidget()

      // Verify calculator widget is added
      verifyCalculatorWidgetAdded()

      // Open calculator and perform basic calculation
      performBasicCalculation()

      // Return to dashboard and verify widget persistence
      returnToDashboardAndVerify()

      // Delete widget and verify empty state
      deleteWidgetAndVerifyEmptyState()
    }
  }

  @Test
  fun testEdgeCaseInvalidInputs() {
    composeTestRule.apply {
      // Add calculator widget and open calculator
      addCalculatorWidget()
      onNodeWithText("Calculator").performClick()
      navigationActions.navigateToCalculator()
      waitForIdle()

      // Test division by zero
      onNodeWithTag("button_5").performClick()
      onNodeWithTag("button_/").performClick()
      onNodeWithTag("button_0").performClick()
      onNodeWithTag("button_=").performClick()
      onNodeWithTag("resultText").assertTextContains("Undefined")

      // Test invalid trigonometric input
      onNodeWithTag("clearButton").performClick()
      // Switch to Functions menu
      onNodeWithText("Functions").performClick()
      // Try arcsin with invalid input
      onNodeWithText("arcsin").performClick()
      onNodeWithText("Basic").performClick()
      onNodeWithText("2").performClick()
      onNodeWithText("=").performClick()
      onNodeWithTag("resultText").assertTextContains("Undefined")

      // Verify navigation state
      assert(navigationActions.currentRoute() == "CALCULATOR") { "Should still be on calculator screen" }
    }
  }

  @Test
  fun testNavigationStateChanges() {
    composeTestRule.apply {
      // Initial state should be dashboard
      assert(navigationActions.currentRoute() == "DASHBOARD") { "Initial route should be dashboard" }

      // Add calculator and navigate to it
      addCalculatorWidget()
      onNodeWithText("Calculator").performClick()
      navigationActions.navigateToCalculator()
      waitForIdle()

      // Verify navigation to calculator
      assert(navigationActions.currentRoute() == "CALCULATOR") { "Route should be calculator after navigation" }

      // Navigate back to dashboard
      onNodeWithTag("goBackButton").performClick()
      navigationActions.navigateToDashboard()
      waitForIdle()

      // Verify navigation back to dashboard
      assert(navigationActions.currentRoute() == "DASHBOARD") { "Route should return to dashboard" }
    }
  }

  @Test
  fun testComplexMathematicalOperations() {
    composeTestRule.apply {
      addCalculatorWidget()
      onNodeWithText("Calculator").performClick()
      navigationActions.navigateToCalculator()
      waitForIdle()

      // Test nested parentheses
      onNodeWithTag("button_(").performClick()
      onNodeWithTag("button_2").performClick()
      onNodeWithTag("button_+").performClick()
      onNodeWithTag("button_3").performClick()
      onNodeWithTag("button_)").performClick()
      onNodeWithTag("button_×").performClick()
      onNodeWithTag("button_(").performClick()
      onNodeWithTag("button_4").performClick()
      onNodeWithTag("button_-").performClick()
      onNodeWithTag("button_1").performClick()
      onNodeWithTag("button_)").performClick()
      onNodeWithTag("button_=").performClick()
      onNodeWithTag("resultText").assertTextContains("15")

      // Test scientific notation result
      onNodeWithTag("clearButton").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_×").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_9").performClick()
      onNodeWithTag("button_=").performClick()
      // Result should be in scientific notation
      onNodeWithTag("resultText").assertExists()
    }
  }

  // Helper functions
  private fun ComposeTestRule.verifyInitialDashboardState() {
    waitForIdle()
    onNodeWithTag("empty_dashboard_message").assertIsDisplayed()
    onNodeWithTag("empty_state_add_button").assertIsDisplayed()
    onNodeWithTag("add_widget_button").assertDoesNotExist()
    assert(navigationActions.currentRoute() == "DASHBOARD") { "Initial route should be dashboard" }
  }

  private fun ComposeTestRule.addCalculatorWidget() {
    onNodeWithTag("empty_state_add_button").performClick()
    val calculatorWidget = CommonWidgetType.CALCULATOR
    onNodeWithText(calculatorWidget.title).performClick()
  }

  private fun ComposeTestRule.verifyCalculatorWidgetAdded() {
    onNodeWithTag("empty_dashboard_message").assertDoesNotExist()
    onNodeWithTag("widget_list").assertIsDisplayed()
    onNodeWithTag("add_widget_button").assertIsDisplayed()
    onNodeWithText("Calculator").assertIsDisplayed()
  }

  private fun ComposeTestRule.performBasicCalculation() {
    onNodeWithText("Calculator").performClick()
    navigationActions.navigateToCalculator()
    waitForIdle()
    assert(navigationActions.currentRoute() == "CALCULATOR") { "Should be on calculator screen" }

    onNodeWithTag("display").assertExists()
    onNodeWithTag("displayText").assertExists()

    onNodeWithTag("button_7").performClick()
    onNodeWithTag("button_+").performClick()
    onNodeWithTag("button_3").performClick()
    onNodeWithTag("button_=").performClick()
    onNodeWithTag("resultText").assertTextContains("10")
  }

  private fun ComposeTestRule.returnToDashboardAndVerify() {
    onNodeWithTag("goBackButton").performClick()
    navigationActions.navigateToDashboard()
    waitForIdle()
    assert(navigationActions.currentRoute() == "DASHBOARD") { "Should return to dashboard" }
    onNodeWithText("Calculator").assertIsDisplayed()
  }

  private fun ComposeTestRule.deleteWidgetAndVerifyEmptyState() {
    onAllNodesWithTag("widget_card").onFirst().assertExists().performScrollTo()
    onAllNodesWithTag("delete_icon").onFirst().assertExists().performClick()
    onNodeWithTag("empty_dashboard_message").assertIsDisplayed()
    onNodeWithTag("empty_state_add_button").assertIsDisplayed()
    onNodeWithTag("add_widget_button").assertDoesNotExist()
    onNodeWithText("Calculator").assertDoesNotExist()
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
    var mockAuth: FirebaseAuth? = null
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
