package com.github.se.eduverse.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class DashboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _widgetList = MutableStateFlow<List<Widget>>(emptyList())
    open val widgetList: StateFlow<List<Widget>> get() = _widgetList

    open fun fetchWidgets(userId: String) {
        viewModelScope.launch {
            dashboardRepository.getWidgets(userId).collect {
                _widgetList.value = it
            }
        }
    }

    fun addWidget(userId: String, widget: Widget) {
        viewModelScope.launch {
            dashboardRepository.addWidget(userId, widget)
        }
    }

    fun removeWidget(userId: String, widgetId: String) {
        viewModelScope.launch {
            dashboardRepository.removeWidget(userId, widgetId)
        }
    }
}


