package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class DashboardViewModel(private val dashboardRepository: DashboardRepository) : ViewModel() {

  private val _widgetList = MutableStateFlow<List<Widget>>(emptyList())
  open val widgetList: StateFlow<List<Widget>>
    get() = _widgetList

  private val _availableWidgets = MutableStateFlow<List<Widget>>(emptyList())
  open val availableWidgets: StateFlow<List<Widget>>
    get() = _availableWidgets

  open fun fetchWidgets(userId: String) {
    viewModelScope.launch {
      dashboardRepository.getWidgets(userId).collect { _widgetList.value = it }
    }
  }

  open fun addWidget(userId: String, widget: Widget) {
    viewModelScope.launch {
      dashboardRepository.addWidget(userId, widget)
      Log.d("DashboardViewModel", "Widget added: ${widget.widgetId}")
    }
  }

  open fun removeWidget(userId: String, widgetId: String) {
    viewModelScope.launch {
      dashboardRepository.removeWidget(userId, widgetId)
      Log.d("DashboardViewModel", "Widget removed: $widgetId")
    }
  }

  // New function to fetch available widgets that are not yet on the user's dashboard
  open fun fetchAvailableWidgets() {
    viewModelScope.launch {
      dashboardRepository.getAvailableWidgets().collect { available ->
        // Filter out widgets already in the dashboard
        val widgetsNotOnDashboard =
            available.filter { widget -> _widgetList.value.none { it.widgetId == widget.widgetId } }
        _availableWidgets.value = widgetsNotOnDashboard
      }
    }
  }
}
