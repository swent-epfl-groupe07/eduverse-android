package com.github.se.eduverse.ui.dashboard

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.viewmodel.DashboardViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, userId: String) {
  val widgetList by viewModel.widgetList.collectAsState()
  val availableWidgets by viewModel.availableWidgets.collectAsState()

  LaunchedEffect(userId) { viewModel.fetchWidgets(userId) }

  var showDropdown by remember { mutableStateOf(false) }
  var deleteWidgetId by remember {
    mutableStateOf<String?>(null)
  } // State to track which widget shows delete icon

  Scaffold(
      floatingActionButton = {
        FloatingActionButton(
            onClick = {
              showDropdown = !showDropdown
              if (showDropdown) {
                viewModel.fetchAvailableWidgets()
              }
            },
            modifier = Modifier.testTag("add_widget_button")) {
              Icon(Icons.Default.Add, contentDescription = "Add Widget")
            }
      },
      bottomBar = { BottomNavigationPlaceholder() },
      modifier = Modifier.testTag("dashboard_screen")) {
        Column {
          LazyColumn(modifier = Modifier.testTag("widget_list")) {
            items(widgetList) { widget ->
              WidgetCard(
                  widget = widget,
                  showDeleteIcon =
                      deleteWidgetId ==
                          widget.widgetId, // Only show delete icon for the long-pressed widget
                  onLongPress = {
                    deleteWidgetId =
                        if (deleteWidgetId == widget.widgetId) null else widget.widgetId
                  },
                  onDeleteClick = {
                    viewModel.removeWidget(userId, widget.widgetId)
                    Log.d("DashboardScreen", "Widget removed: ${widget.widgetId}")
                    viewModel.fetchWidgets(userId) // Fetch updated list after removing widget
                    deleteWidgetId = null // Reset delete icon state after deletion
                  })
            }
          }

          if (showDropdown) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              items(availableWidgets) { widget ->
                AvailableWidgetCard(
                    widget = widget,
                    onClick = {
                      viewModel.addWidget(userId, widget)
                      showDropdown = false
                    })
              }
            }
          }
        }
      }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetCard(
    widget: Widget,
    showDeleteIcon: Boolean, // Accept the state whether to show the delete icon
    onLongPress: () -> Unit,
    onDeleteClick: () -> Unit
) {
  Card(
      modifier =
          Modifier.padding(8.dp)
              .fillMaxWidth()
              .combinedClickable(
                  onClick = { /* Do nothing on normal click */},
                  onLongClick = {
                    onLongPress() // Toggle the long press action
                  })
              .testTag("widget_card"),
      elevation = 4.dp) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(text = widget.widgetTitle, style = MaterialTheme.typography.h6)
            Text(text = widget.widgetContent)
          }

          if (showDeleteIcon) { // Show delete icon if state is true
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).testTag("delete_icon")) {
                  Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Delete Widget",
                      tint = MaterialTheme.colors.error)
                }
          }
        }
      }
}

// Card to display available widgets
@Composable
fun AvailableWidgetCard(widget: Widget, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.padding(8.dp)
              .fillMaxWidth()
              .clickable(onClick = onClick)
              .testTag("available_widget_card")) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(text = widget.widgetTitle, style = MaterialTheme.typography.h6)
        }
      }
}

@Composable
fun BottomNavigationPlaceholder() {
  Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
    Text("Bottom Nav Placeholder")
  }
}
