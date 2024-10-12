package com.github.se.eduverse.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
  private val _widgetList =
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
