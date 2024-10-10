package com.github.se.eduverse.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.github.se.eduverse.model.Widget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mockito.Mockito.mock

// Creating a simplified fake ViewModel for testing purposes
class FakeDashboardViewModel : DashboardViewModel(mock()) {
    private val _widgetList = MutableStateFlow(
        listOf(
            Widget("1", "Type", "Title", "Content", "Owner"),
            Widget("2", "Type 2", "Title 2", "Content 2", "Owner 2")
        )
    )
    override val widgetList: StateFlow<List<Widget>> = _widgetList.asStateFlow()

    // No repository interaction during tests
    override fun fetchWidgets(userId: String) {
        // This is intentionally left empty, as the widgetList is pre-filled for testing
    }
}

@RunWith(AndroidJUnit4::class)
class DashboardScreenUiTest : TestCase() {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeViewModel = FakeDashboardViewModel()

    @Test
    fun testDashboardScreenDisplaysWidgets() = run {
        step("Launch Dashboard Screen") {
            composeTestRule.setContent {
                DashboardScreen(viewModel = fakeViewModel, userId = "userId")
            }
        }

        step("Check if Add Widget Button is displayed") {
            composeTestRule.onNodeWithTag("add_widget_button").assertExists()
        }

        step("Check if Widgets are displayed") {
            composeTestRule.onNodeWithTag("widget_list").assertExists()
            composeTestRule.onAllNodesWithTag("widget_card")
                .assertCountEquals(2)
        }
    }
}
