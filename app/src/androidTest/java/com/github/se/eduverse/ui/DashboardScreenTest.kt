package com.github.se.eduverse.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

// Creating a simplified fake ViewModel for testing purposes
class FakeDashboardViewModel : DashboardViewModel(mock()) {
  private val _widgetList =
      MutableStateFlow(
          listOf(
              Widget("1", "Type", "Title", "Content", "Owner"),
              Widget("2", "Type 2", "Title 2", "Content 2", "Owner 2")))
  override val widgetList: StateFlow<List<Widget>> = _widgetList.asStateFlow()

  private val _availableWidgets =
      MutableStateFlow(listOf(Widget("3", "Type 3", "Title 3", "Content 3", "Owner 3")))
  override val availableWidgets: StateFlow<List<Widget>> = _availableWidgets.asStateFlow()

  override fun addWidget(userId: String, widget: Widget) {
    // Simulate adding the widget to the list
    _widgetList.value = _widgetList.value + widget
  }

  override fun removeWidget(userId: String, widgetId: String) {
    // Simulate removing the widget from the list by filtering it out
    _widgetList.value = _widgetList.value.filter { it.widgetId != widgetId }
  }

  // Override fetch methods to do nothing, as the test uses predefined data
  override fun fetchWidgets(userId: String) {}

  override fun fetchAvailableWidgets() {}
}

@RunWith(AndroidJUnit4::class)
class DashboardScreenUiTest : TestCase() {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeViewModel = FakeDashboardViewModel()

  @Test
  fun testDashboardScreenDisplaysWidgets() = run {
    step("Launch Dashboard Screen") {
      composeTestRule.setContent { DashboardScreen(viewModel = fakeViewModel, userId = "userId") }
    }

    step("Check if Add Widget Button is displayed") {
      composeTestRule.onNodeWithTag("add_widget_button").assertExists()
    }

    step("Check if Widgets are displayed") {
      composeTestRule.onNodeWithTag("widget_list").assertExists()
      composeTestRule.onAllNodesWithTag("widget_card").assertCountEquals(2)
    }
  }

  @Test
  fun testAddWidget() = run {
    step("Launch Dashboard Screen") {
      composeTestRule.setContent { DashboardScreen(viewModel = fakeViewModel, userId = "userId") }
    }

    step("Click on Add Widget Button and check Dropdown visibility") {
      // Simulate clicking on the add widget button
      composeTestRule.onNodeWithTag("add_widget_button").performClick()

      // Check if the dropdown appears by verifying the presence of available widgets
      composeTestRule.onAllNodesWithTag("available_widget_card").assertCountEquals(1)
    }

    step("Click on Available Widget and add it to the list") {
      // Simulate clicking on an available widget from the dropdown
      composeTestRule.onAllNodesWithTag("available_widget_card").onFirst().performClick()

      // Verify if the widget count increases (as one is added from availableWidgets)
      composeTestRule.onAllNodesWithTag("widget_card").assertCountEquals(3)
    }
  }

  @Test
  fun testRemoveWidget() = run {
    step("Launch Dashboard Screen") {
      composeTestRule.setContent { DashboardScreen(viewModel = fakeViewModel, userId = "userId") }
    }

    step("Long press on a Widget and remove it") {
      // Simulate long press to show delete icon
      composeTestRule.onAllNodesWithTag("widget_card").onFirst().performTouchInput {
        longClick(durationMillis = 500L) // Increase long press duration to 500ms
      }

      composeTestRule.waitForIdle()

      // Simulate clicking on the delete icon
      composeTestRule.onNodeWithContentDescription("Delete Widget").performClick()

      // Wait for the UI to reflect the removal
      composeTestRule.waitForIdle()

      // Verify the widget count decreases
      composeTestRule.onAllNodesWithTag("widget_card").assertCountEquals(1)
    }
  }

  @Test
  fun testDropdownVisibility() = run {
    step("Launch Dashboard Screen") {
      composeTestRule.setContent { DashboardScreen(viewModel = fakeViewModel, userId = "userId") }
    }

    step("Check if Dropdown is not visible initially") {
      // Ensure that dropdown is not visible on launch
      composeTestRule.onAllNodesWithTag("available_widget_card").assertCountEquals(0)
    }

    step("Click Add Widget Button and check Dropdown visibility") {
      // Simulate clicking the add widget button
      composeTestRule.onNodeWithTag("add_widget_button").performClick()

      // Check if the dropdown appears by verifying available widgets
      composeTestRule.onAllNodesWithTag("available_widget_card").assertCountEquals(1)
    }

    step("Click Add Widget Button again and check Dropdown disappearance") {
      // Simulate clicking the add widget button again to close the dropdown
      composeTestRule.onNodeWithTag("add_widget_button").performClick()

      // Ensure the dropdown is no longer visible
      composeTestRule.onAllNodesWithTag("available_widget_card").assertCountEquals(0)
    }
  }
}
