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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.google.firebase.auth.FirebaseAuth

var auth: FirebaseAuth = FirebaseAuth.getInstance()

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun DashboardScreen(
    navigationActions: NavigationActions,
    viewModel: DashboardViewModel,
    userId: String = auth.currentUser!!.uid
) {
  val widgetList by viewModel.widgetList.collectAsState()
  var showAddWidgetDialog by remember { mutableStateOf(false) }

  LaunchedEffect(userId) { viewModel.fetchWidgets(userId) }

  var deleteWidgetId by remember { mutableStateOf<String?>(null) }

  Scaffold(
      floatingActionButton = {
        FloatingActionButton(
            onClick = { showAddWidgetDialog = true }, // Show dialog to add widget
            modifier = Modifier.testTag("add_widget_button")) {
              Icon(Icons.Default.Add, contentDescription = "Add Widget")
            }
      },
      bottomBar = {
        BottomNavigationMenu(
            onTabSelect = { route -> navigationActions.navigateTo(route) },
            tabList = LIST_TOP_LEVEL_DESTINATION,
            selectedItem = navigationActions.currentRoute())
      },
      modifier = Modifier.testTag("dashboard_screen")) {
        Box {
          Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.testTag("widget_list")) {
              items(widgetList) { widget ->
                WidgetCard(
                    widget = widget,
                    showDeleteIcon = deleteWidgetId == widget.widgetId,
                    onLongPress = {
                      deleteWidgetId =
                          if (deleteWidgetId == widget.widgetId) null else widget.widgetId
                    },
                    onDeleteClick = {
                      viewModel.removeWidget(widget.widgetId)
                      Log.d("DashboardScreen", "Widget removed: ${widget.widgetId}")
                      viewModel.fetchWidgets(userId)
                      deleteWidgetId = null
                    })
              }
            }
          }

          if (showAddWidgetDialog) {
            CommonWidgetDialog(
                viewModel = viewModel, onDismiss = { showAddWidgetDialog = false }, userId = userId)
          }
        }
      }
}

@Composable
fun CommonWidgetDialog(viewModel: DashboardViewModel, onDismiss: () -> Unit, userId: String) {
  val commonWidgets = viewModel.getCommonWidgets()

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Add Widget") },
      text = {
        Column {
          commonWidgets.forEach { widget ->
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                          viewModel.addWidget(
                              widget.copy(
                                  widgetId =
                                      "${userId}_${widget.widgetId}", // Make ID unique per user
                                  ownerUid = userId))
                          onDismiss()
                        }
                        .testTag("add_common_widget_button")) {
                  Text(text = widget.widgetTitle, style = MaterialTheme.typography.body1)
                }
          }
        }
      },
      confirmButton = { Button(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetCard(
    widget: Widget,
    showDeleteIcon: Boolean,
    onLongPress: () -> Unit,
    onDeleteClick: () -> Unit
) {
  Card(
      modifier =
          Modifier.padding(8.dp)
              .fillMaxWidth()
              .combinedClickable(
                  onClick = { /* Do nothing on normal click */}, onLongClick = { onLongPress() })
              .testTag("widget_card"),
      elevation = 4.dp) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(text = widget.widgetTitle, style = MaterialTheme.typography.h6)
            Text(text = widget.widgetContent)
          }

          if (showDeleteIcon) {
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
