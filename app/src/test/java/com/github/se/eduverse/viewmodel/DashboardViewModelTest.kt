package com.github.se.eduverse.viewmodel

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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DashboardViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var viewModel: DashboardViewModel
  private val mockRepository: DashboardRepository = mock(DashboardRepository::class.java)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    viewModel = DashboardViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `fetchWidgets should update widgetList from repository`() = runTest {
    val widgetList =
        listOf(
            Widget("1", "Type 1", "Title 1", "Content 1", "owner1"),
            Widget("2", "Type 2", "Title 2", "Content 2", "owner2"))

    whenever(mockRepository.getWidgets("userId")).thenReturn(flowOf(widgetList))

    viewModel.fetchWidgets("userId")
    advanceUntilIdle()

    verify(mockRepository).getWidgets("userId")
    assertEquals(widgetList, viewModel.widgetList.value)
  }

  @Test
  fun `addWidget should call repository addWidget`() = runTest {
    val newWidget = Widget("1", "Type", "Title", "Content", "userId")

    viewModel.addWidget(newWidget)
    advanceUntilIdle()

    verify(mockRepository).addWidget(newWidget)
  }

  @Test
  fun `removeWidget should call repository removeWidget`() = runTest {
    val widgetId = "widgetId"

    viewModel.removeWidget(widgetId)
    advanceUntilIdle()

    verify(mockRepository).removeWidget(widgetId)
  }

  @Test
  fun `getCommonWidgets should return list of common widgets`() {
    val commonWidgets = viewModel.getCommonWidgets()
    assertEquals(4, commonWidgets.size) // Assuming there are 4 CommonWidgetTypes
    assertEquals("TIMER", commonWidgets[0].widgetId)
    assertEquals("CALCULATOR", commonWidgets[1].widgetId)
    assertEquals("PDF_CONVERTER", commonWidgets[2].widgetId)
    assertEquals("WEEKLY_PLANNER", commonWidgets[3].widgetId)
  }

  @Test
  fun `fetchWidgets should handle null flow from repository`() = runTest {
    whenever(mockRepository.getWidgets("userId")).thenReturn(null)

    viewModel.fetchWidgets("userId")
    advanceUntilIdle()

    verify(mockRepository).getWidgets("userId")
    assertEquals(emptyList<Widget>(), viewModel.widgetList.value)
  }

  @Test
  fun `fetchWidgets should handle exception in flow`() = runTest {
    val errorFlow = flow<List<Widget>> { throw RuntimeException("Test exception") }
    whenever(mockRepository.getWidgets("userId")).thenReturn(errorFlow)

    viewModel.fetchWidgets("userId")
    advanceUntilIdle()

    verify(mockRepository).getWidgets("userId")
    assertEquals(emptyList<Widget>(), viewModel.widgetList.value)
  }
}
