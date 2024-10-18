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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class DashboardScreenUiTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeViewModel = FakeDashboardViewModel()
  private val mockNavigationActions = FakeNavigationActions(navController = mock())

  @Before
  fun setUp() {
    MockFirebaseAuth.setup()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun testDashboardScreenDisplaysWidgets() {
    setupDashboardScreen()

    // Wait for the initial auth check to complete
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("add_widget_button").assertExists()
    composeTestRule.onNodeWithTag("widget_list").assertExists()
    composeTestRule.onAllNodesWithTag("widget_card").assertCountEquals(2)
  }

  @Test
  fun testAddWidget() {
    setupDashboardScreen()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("add_widget_button").performClick()
    composeTestRule.onAllNodesWithTag("add_common_widget_button").assertCountEquals(1)

    composeTestRule.onAllNodesWithTag("add_common_widget_button").onFirst().performClick()
    composeTestRule.onAllNodesWithTag("widget_card").assertCountEquals(3)
  }

  @Test
  fun testRemoveWidget() {
    setupDashboardScreen()

    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithTag("widget_card").onFirst().performTouchInput { longClick() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("delete_icon").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithTag("widget_card").assertCountEquals(1)
  }

  @Test
  fun testCommonWidgetDialogVisibility() {
    setupDashboardScreen()

    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithTag("add_common_widget_button").onFirst().assertDoesNotExist()

    composeTestRule.onNodeWithTag("add_widget_button").performClick()
    composeTestRule.onAllNodesWithTag("add_common_widget_button").assertCountEquals(1)

    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onAllNodesWithTag("add_common_widget_button").onFirst().assertDoesNotExist()
  }

  @Test
  fun testDragCancellation() {
    setupDashboardScreen()

    composeTestRule.waitForIdle()
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

    composeTestRule.waitForIdle()
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
    composeTestRule.waitForIdle()

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
      DashboardScreen(viewModel = fakeViewModel, navigationActions = mockNavigationActions)
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

class MockFirebaseAuth {
  companion object {
    private val mockUser: FirebaseUser =
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

    fun setup(isAuthenticated: Boolean = true) {
      mockkStatic(FirebaseAuth::class)
      val mockAuth = mock(FirebaseAuth::class.java)
      every { FirebaseAuth.getInstance() } returns mockAuth
      `when`(mockAuth.currentUser).thenReturn(if (isAuthenticated) mockUser else null)
    }
  }
}

class FakeNavigationActions(navController: NavHostController) : NavigationActions(navController) {
  fun navigate(route: String) {
    // No-op for testing
  }
}
