package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.model.CommonWidgetType
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.repository.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DashboardViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var viewModel: TestDashboardViewModel
  private val mockRepository: DashboardRepository = mock()

  private class TestDashboardViewModel(repository: DashboardRepository) :
      DashboardViewModel(repository) {
    fun setWidgetListForTest(widgets: List<Widget>) {
      _widgetList.value = widgets
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    viewModel = TestDashboardViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `fetchWidgets should update widgetList from repository with correct order`() = runTest {
    // Create widgets with different orders
    val widgetList =
        listOf(
            Widget("1", "Type1", "Title1", "Content1", "owner1", order = 2),
            Widget("2", "Type2", "Title2", "Content2", "owner1", order = 0),
            Widget("3", "Type3", "Title3", "Content3", "owner1", order = 1))

    whenever(mockRepository.getWidgets("userId")).thenReturn(flowOf(widgetList))

    viewModel.fetchWidgets("userId")
    advanceUntilIdle()

    // Verify widgets are sorted by order
    assertEquals(listOf("2", "3", "1"), viewModel.widgetList.value.map { it.widgetId })
  }

  @Test
  fun `addWidget should not add duplicate widget type`() = runTest {
    // Setup initial state with a widget
    val initialWidget = Widget("1", "TIMER", "Timer", "Content", "user1", 0)
    viewModel.setWidgetListForTest(listOf(initialWidget))

    // Try to add another widget of the same type
    val duplicateWidget = Widget("2", "TIMER", "Timer 2", "Content 2", "user1", 1)
    viewModel.addWidget(duplicateWidget)
    advanceUntilIdle()

    // Verify repository was not called - use verify(mock, never()) instead of any()
    verify(mockRepository, never()).addWidget(duplicateWidget)
  }

  @Test
  fun `addWidget should set correct order and ID`() = runTest {
    // Setup initial state
    val initialWidgets =
        listOf(
            Widget("1", "Type1", "Title1", "Content1", "user1", 0),
            Widget("2", "Type2", "Title2", "Content2", "user1", 1))
    viewModel.setWidgetListForTest(initialWidgets)

    // Add new widget
    val newWidget = Widget("", "Type3", "Title3", "Content3", "user1", 0)
    viewModel.addWidget(newWidget)
    advanceUntilIdle()

    // Capture the widget passed to repository
    argumentCaptor<Widget>().apply {
      verify(mockRepository).addWidget(capture())
      assertEquals(2, firstValue.order) // Should be assigned next available order
      assertEquals("user1_Type3", firstValue.widgetId) // Should have correct ID format
    }
  }

  @Test
  fun `removeWidgetAndUpdateOrder should update remaining widgets correctly`() = runTest {
    // Setup initial widgets
    val initialWidgets =
        listOf(
            Widget("1", "Type1", "Title1", "Content1", "user1", 0),
            Widget("2", "Type2", "Title2", "Content2", "user1", 1),
            Widget("3", "Type3", "Title3", "Content3", "user1", 2))

    // Remove middle widget and update orders
    val updatedWidgets =
        listOf(
            Widget("1", "Type1", "Title1", "Content1", "user1", 0),
            Widget("3", "Type3", "Title3", "Content3", "user1", 1))

    viewModel.removeWidgetAndUpdateOrder("2", updatedWidgets)
    advanceUntilIdle()

    // Verify correct repository calls
    verify(mockRepository).removeWidget("2")
    verify(mockRepository).updateWidgets(updatedWidgets)
  }

  @Test
  fun `updateWidgetOrder should call repository with reordered widgets`() = runTest {
    val reorderedWidgets =
        listOf(
            Widget("1", "Type1", "Title1", "Content1", "user1", 2),
            Widget("2", "Type2", "Title2", "Content2", "user1", 0),
            Widget("3", "Type3", "Title3", "Content3", "user1", 1))

    viewModel.updateWidgetOrder(reorderedWidgets)
    advanceUntilIdle()

    // Simply verify with the list directly
    verify(mockRepository).updateWidgets(reorderedWidgets)
  }

  @Test
  fun `getCommonWidgets should return correctly configured widgets`() {
    val commonWidgets = viewModel.getCommonWidgets()
    val commonTypes = CommonWidgetType.values()

    // First verify the size matches
    assertEquals(commonTypes.size, commonWidgets.size)

    // Then verify each widget's properties
    commonWidgets.forEachIndexed { index, widget ->
      val expectedType = commonTypes[index]
      assertEquals("", widget.ownerUid)
      assertEquals(0, widget.order)
      assertEquals(expectedType.name, widget.widgetId)
      assertEquals(expectedType.name, widget.widgetType)
      assertEquals(expectedType.title, widget.widgetTitle)
      assertEquals(expectedType.content, widget.widgetContent)
    }
  }

  @Test
  fun `fetchWidgets should handle error and emit empty list`() = runTest {
    val errorFlow = flow<List<Widget>> { throw RuntimeException("Test exception") }
    whenever(mockRepository.getWidgets("userId")).thenReturn(errorFlow)

    viewModel.fetchWidgets("userId")
    advanceUntilIdle()

    assertEquals(emptyList<Widget>(), viewModel.widgetList.value)
  }
}
