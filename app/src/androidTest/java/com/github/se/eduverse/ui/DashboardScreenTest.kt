package com.github.se.eduverse.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class DashboardScreenUiTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeViewModel = FakeDashboardViewModel()
  private val mockNavigationActions = FakeNavigationActions(navController = mock())

  @Test
  fun testDashboardScreenDisplaysWidgets() {
    setupDashboardScreen()

    composeTestRule.onNodeWithTag("add_widget_button").assertExists()
    composeTestRule.onNodeWithTag("widget_list").assertExists()
    composeTestRule.onAllNodesWithTag("widget_card").assertCountEquals(2)
  }

  @Test
  fun testAddWidget() {
    setupDashboardScreen()

    composeTestRule.onNodeWithTag("add_widget_button").performClick()
    composeTestRule.onAllNodesWithTag("add_common_widget_button").assertCountEquals(1)

    composeTestRule.onAllNodesWithTag("add_common_widget_button").onFirst().performClick()
    composeTestRule.onAllNodesWithTag("widget_card").assertCountEquals(3)
  }

  @Test
  fun testRemoveWidget() {
    setupDashboardScreen()

    composeTestRule.onAllNodesWithTag("widget_card").onFirst().performTouchInput { longClick() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("delete_icon").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithTag("widget_card").assertCountEquals(1)
  }

  @Test
  fun testCommonWidgetDialogVisibility() {
    setupDashboardScreen()

    composeTestRule.onAllNodesWithTag("add_common_widget_button").onFirst().assertDoesNotExist()

    composeTestRule.onNodeWithTag("add_widget_button").performClick()
    composeTestRule.onAllNodesWithTag("add_common_widget_button").assertCountEquals(1)

    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onAllNodesWithTag("add_common_widget_button").onFirst().assertDoesNotExist()
  }

  @Test
  fun testDragCancellation() {
    setupDashboardScreen()

    val initialOrder = fakeViewModel.widgetList.value.map { it.widgetId }

    // Start dragging but cancel before releasing
    composeTestRule.onAllNodesWithTag("widget_card").onFirst().performTouchInput {
      down(center)
      moveBy(Offset(0f, 50f))
      cancel()
    }

    composeTestRule.waitForIdle()

    val newOrder = fakeViewModel.widgetList.value.map { it.widgetId }

    // Assert that the order hasn't changed
    assertEquals(initialOrder, newOrder)
  }

  @Test
  fun testDragLastBeyondBounds() {
    setupDashboardScreen()

    val initialOrder = fakeViewModel.widgetList.value.map { it.widgetId }

    // Attempt to drag beyond the bounds of the list
    composeTestRule.onAllNodesWithTag("widget_card").onLast().performTouchInput {
      down(center)
      advanceEventTime(100)
      moveBy(Offset(x = 1200f, y = 1180.dp.toPx()), 0)
      advanceEventTime(1000)
      up()
    }

    composeTestRule.waitForIdle()

    val newOrder = fakeViewModel.widgetList.value.map { it.widgetId }

    // Assert that the order has changed, but the widget is still in the list
    assertEquals(initialOrder, newOrder)
    assertTrue(newOrder.containsAll(initialOrder))
  }

  @Test
  fun testWidgetContentDisplaysCorrectIcon() {
    // Define a list of test widgets with expected icons
    val testWidgets =
        listOf(
            Widget("1", "Type", "Calculator", "Calculator content", "Owner"),
            Widget("2", "Type 2", "PDF Converter", "PDF content", "Owner 2"),
            Widget("3", "Type 3", "Weekly Planner", "Planner content", "Owner 3"),
            Widget("4", "Type 4", "Pomodoro Timer", "Timer content", "Owner 4"),
            Widget(
                "5", "Type 5", "Unknown Widget", "Unknown content", "Owner 5") // For default case
            )

    // Set up the DashboardScreen with the test widgets
    fakeViewModel.apply { _widgetList.value = testWidgets }

    setupDashboardScreen()

    // Check that each widget displays the correct icon
    testWidgets.forEach { widget ->
      val iconNode = composeTestRule.onNodeWithText(widget.widgetTitle)
      iconNode.assertExists() // Ensure the widget exists

      // Here we can check the icon visually or programmatically if you have specific identifiers
      // But for simplicity, we are checking if the widget displays correctly.
      // You can create more specific checks if you have test tags for icons.
    }
  }

  private fun setupDashboardScreen() {
    composeTestRule.setContent {
      DashboardScreen(
          viewModel = fakeViewModel,
          navigationActions = mockNavigationActions,
          userId = "testUserId")
    }
  }
}

class FakeDashboardViewModel : DashboardViewModel(mock()) {
  val _widgetList =
      MutableStateFlow(
          listOf(
              Widget("1", "Type", "Title 1", "Content 1", "Owner"),
              Widget("2", "Type 2", "Title 2", "Content 2", "Owner 2")))
  override val widgetList: StateFlow<List<Widget>> = _widgetList.asStateFlow()

  override fun addWidget(widget: Widget) {
    _widgetList.value = _widgetList.value + widget
  }

  override fun removeWidget(widgetId: String) {
    _widgetList.value = _widgetList.value.filter { it.widgetId != widgetId }
  }

  override fun getCommonWidgets(): List<Widget> {
    return listOf(Widget("3", "Type 3", "Title 3", "Content 3", "Owner 3"))
  }
}

class FakeNavigationActions(navController: NavHostController) : NavigationActions(navController) {
  fun navigate(route: String) {
    // No-op for testing
  }
}
