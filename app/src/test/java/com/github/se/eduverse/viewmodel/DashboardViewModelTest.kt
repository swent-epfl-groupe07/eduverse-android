package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.repository.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class DashboardViewModelTest {

  // Create a TestCoroutineDispatcher
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var viewModel: DashboardViewModel
  private val mockRepository: DashboardRepository = mock(DashboardRepository::class.java)

  @Before
  fun setUp() {
    // Set the Main dispatcher to the test dispatcher before the test starts
    Dispatchers.setMain(testDispatcher)
    viewModel = DashboardViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    // Reset the Main dispatcher after the test
    Dispatchers.resetMain() // Reset to the original Main dispatcher
  }

  @Test
  fun `fetchWidgets should update widgetList from repository`() = runTest {
    val widgetList =
        listOf(
            Widget("1", "Type 1", "Title 1", "Content 1", "owner1"),
            Widget("2", "Type 2", "Title 2", "Content 2", "owner2"))

    // Mock the repository to return the widgetList
    `when`(mockRepository.getWidgets("userId")).thenReturn(flowOf(widgetList))

    // Call fetchWidgets in ViewModel
    viewModel.fetchWidgets("userId")

    // Advance until idle to allow coroutines to execute
    advanceUntilIdle()

    // Verify that the repository method was called
    verify(mockRepository).getWidgets("userId")

    // Assert that the ViewModel's widget list is updated correctly
    assertEquals(widgetList, viewModel.widgetList.first())
  }

  @Test
  fun `addWidget should call repository addWidget`() = runTest {
    val newWidget = Widget("1", "Type", "Title", "Content", "ownerId")

    // Call addWidget in ViewModel
    viewModel.addWidget("userId", newWidget)

    // Advance until idle to allow coroutines to execute
    advanceUntilIdle()

    // Verify that the repository's addWidget method is called
    verify(mockRepository).addWidget("userId", newWidget)
  }

  @Test
  fun `removeWidget should call repository removeWidget`() = runTest {
    val widgetId = "widgetId"

    // Call removeWidget in ViewModel
    viewModel.removeWidget("userId", widgetId)

    // Advance until idle to allow coroutines to execute
    advanceUntilIdle()

    // Verify that the repository's removeWidget method is called
    verify(mockRepository).removeWidget("userId", widgetId)
  }

  @Test
  fun `fetchAvailableWidgets should update availableWidgets filtering out already added ones`() =
      runTest {
        val widgetList =
            listOf(
                Widget("1", "Type 1", "Title 1", "Content 1", "owner1"), // Already on the dashboard
                Widget("2", "Type 2", "Title 2", "Content 2", "owner2") // Already on the dashboard
                )
        val availableWidgetList =
            listOf(
                Widget("1", "Type 1", "Title 1", "Content 1", "owner1"),
                Widget("2", "Type 2", "Title 2", "Content 2", "owner2"),
                Widget("3", "Type 3", "Title 3", "Content 3", "owner3") // New available widget
                )

        // Mock the repository to return available widgets and widgets already in the dashboard
        `when`(mockRepository.getWidgets("userId")).thenReturn(flowOf(widgetList))
        `when`(mockRepository.getAvailableWidgets()).thenReturn(flowOf(availableWidgetList))

        // Call fetchWidgets in ViewModel to populate dashboard widgets
        viewModel.fetchWidgets("userId")
        advanceUntilIdle()

        // Call fetchAvailableWidgets in ViewModel
        viewModel.fetchAvailableWidgets()
        advanceUntilIdle()

        // Verify that the repository's getAvailableWidgets method is called
        verify(mockRepository).getAvailableWidgets()

        // Assert that the availableWidgets list is updated correctly (widget "1" and "2" filtered
        // out)
        val expectedFilteredWidgets =
            listOf(Widget("3", "Type 3", "Title 3", "Content 3", "owner3"))
        assertEquals(expectedFilteredWidgets, viewModel.availableWidgets.first())
      }
}
