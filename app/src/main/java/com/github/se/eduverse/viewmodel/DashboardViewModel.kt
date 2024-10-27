package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.CommonWidgetType
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
open class DashboardViewModel
@Inject
constructor(private val dashboardRepository: DashboardRepository) : ViewModel() {

  private val _widgetList = MutableStateFlow<List<Widget>>(emptyList())
  open val widgetList: StateFlow<List<Widget>> = _widgetList

  fun fetchWidgets(userId: String) {
    viewModelScope.launch {
      dashboardRepository
          .getWidgets(userId)
          .catch { e ->
            Log.e("DashboardViewModel", "Error fetching widgets", e)
            _widgetList.value = emptyList()
          }
          .collect { widgets -> _widgetList.value = widgets.sortedBy { it.order } }
    }
  }

  open fun addWidget(widget: Widget) {
    viewModelScope.launch {
      if (_widgetList.value.any { it.widgetType == widget.widgetType }) {
        Log.d("DashboardViewModel", "Widget type ${widget.widgetType} already exists")
        return@launch
      }

      val newWidget =
          widget.copy(
              widgetId = "${widget.ownerUid}_${widget.widgetType}", order = _widgetList.value.size)

      dashboardRepository.addWidget(newWidget)
    }
  }

  fun removeWidgetAndUpdateOrder(widgetId: String, updatedWidgets: List<Widget>) {
    viewModelScope.launch {
      // First remove the widget
      dashboardRepository.removeWidget(widgetId)

      // Then update the order of remaining widgets
      dashboardRepository.updateWidgets(updatedWidgets)
    }
  }

  fun updateWidgetOrder(reorderedWidgets: List<Widget>) {
    viewModelScope.launch { dashboardRepository.updateWidgets(reorderedWidgets) }
  }

  open fun getCommonWidgets(): List<Widget> {
    return CommonWidgetType.values().map { commonWidget ->
      Widget(
          widgetId = commonWidget.name,
          widgetType = commonWidget.name,
          widgetTitle = commonWidget.title,
          widgetContent = commonWidget.content,
          ownerUid = "",
          order = 0)
    }
  }
}
