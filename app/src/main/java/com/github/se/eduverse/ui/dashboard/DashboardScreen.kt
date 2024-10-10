package com.github.se.eduverse.ui.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

  LaunchedEffect(userId) { viewModel.fetchWidgets(userId) }

  Scaffold(
      floatingActionButton = {
        FloatingActionButton(
            onClick = { /* Add widget logic */},
            modifier = Modifier.testTag("add_widget_button") // Test tag added here
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add Widget")
            }
      },
      bottomBar = {
        // Placeholder for bottom navigation
        BottomNavigationPlaceholder()
      },
      modifier = Modifier.testTag("dashboard_screen") // Test tag for the whole screen
      ) {
        LazyColumn(modifier = Modifier.testTag("widget_list")) { // Test tag for the list
          items(widgetList) { widget -> WidgetCard(widget = widget) }
        }
      }
}

@Composable
fun WidgetCard(widget: Widget) {
  Card(
      modifier =
          Modifier.padding(8.dp).fillMaxWidth().testTag("widget_card") // Test tag for each card
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(text = widget.widgetTitle, style = MaterialTheme.typography.h6)
          Text(text = widget.widgetContent)
        }
      }
}

@Composable
fun BottomNavigationPlaceholder() {
  // This will be replaced with actual implementation
  Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
    Text("Bottom Nav Placeholder")
  }
}
