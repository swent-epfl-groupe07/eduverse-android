package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.CommonWidgetType
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

open class DashboardViewModel(private val dashboardRepository: DashboardRepository) : ViewModel() {

  private val _widgetList = MutableStateFlow<List<Widget>>(emptyList())
  open val widgetList: StateFlow<List<Widget>>
    get() = _widgetList

  open fun fetchWidgets(userId: String) {
    viewModelScope.launch {
      dashboardRepository
          .getWidgets(userId)
          ?.catch { e ->
            Log.e("DashboardViewModel", "Error fetching widgets", e)
            _widgetList.value = emptyList()
          }
          ?.collect { widgets -> _widgetList.value = widgets }
          ?: run {
            Log.e("DashboardViewModel", "getWidgets returned null")
            _widgetList.value = emptyList()
          }
    }
  }

  open fun addWidget(widget: Widget) {
    viewModelScope.launch {
      dashboardRepository.addWidget(widget)
      Log.d("DashboardViewModel", "Widget added: ${widget.widgetId}")
    }
  }

  open fun removeWidget(widgetId: String) {
    viewModelScope.launch {
      dashboardRepository.removeWidget(widgetId)
      Log.d("DashboardViewModel", "Widget removed: $widgetId")
    }
  }

  open fun getCommonWidgets(): List<Widget> {
    return CommonWidgetType.values().map { commonWidget ->
      Widget(
          widgetId = commonWidget.name, // Unique identifier based on enum name
          widgetType = "COMMON", // Indicating this is a common widget
          widgetTitle = commonWidget.title,
          widgetContent = commonWidget.content,
          ownerUid = "" // No owner because it's a common widget template
          )
    }
  }
}
