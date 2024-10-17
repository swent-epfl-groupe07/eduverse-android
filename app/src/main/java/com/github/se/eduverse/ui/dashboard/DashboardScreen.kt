package com.github.se.eduverse.ui.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt

var auth: FirebaseAuth = FirebaseAuth.getInstance()

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    navigationActions: NavigationActions,
    viewModel: DashboardViewModel,
    userId: String = auth.currentUser!!.uid
) {
  val widgetList by viewModel.widgetList.collectAsState()
  var showAddWidgetDialog by remember { mutableStateOf(false) }

  LaunchedEffect(userId) { viewModel.fetchWidgets(userId) }

  Scaffold(
      floatingActionButton = {
        FloatingActionButton(
            onClick = { showAddWidgetDialog = true },
            modifier = Modifier.padding(16.dp).testTag("add_widget_button"),
            backgroundColor = Color(0xFF26A69A)) {
              Icon(Icons.Default.Edit, contentDescription = "Add Widget", tint = Color.White)
            }
      },
      bottomBar = {
        BottomNavigationMenu(
            onTabSelect = { route -> navigationActions.navigateTo(route) },
            tabList = LIST_TOP_LEVEL_DESTINATION,
            selectedItem = navigationActions.currentRoute())
      }) {
        Box(modifier = Modifier.fillMaxSize()) {
          ReorderableDashboard(
              widgets = widgetList,
              onReorder = { reorderedWidgets -> viewModel.updateWidgetOrder(reorderedWidgets) },
              onDelete = { widgetId -> viewModel.removeWidget(widgetId) },
              onWidgetClick = { widget ->
                // Handle widget click if needed
              })

          if (showAddWidgetDialog) {
            CommonWidgetDialog(
                viewModel = viewModel, onDismiss = { showAddWidgetDialog = false }, userId = userId)
          }
        }
      }
}

@Composable
fun ReorderableDashboard(
    widgets: List<Widget>,
    onReorder: (List<Widget>) -> Unit,
    onDelete: (String) -> Unit,
    onWidgetClick: (Widget) -> Unit
) {
  var draggedWidget by remember { mutableStateOf<Widget?>(null) }
  var draggedIndex by remember { mutableStateOf<Int?>(null) }
  var dragOffset by remember { mutableStateOf(0f) }

  val itemHeights = remember { mutableStateListOf<Int>() }

  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("widget_list"),
      contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(widgets.sortedBy { it.order }) { index, widget ->
          key(widget.widgetId) {
            var showDeleteButton by remember { mutableStateOf(false) }

            val isDragging = draggedWidget == widget
            val elevation by animateFloatAsState(if (isDragging) 8f else 1f)
            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f)

            Card(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .graphicsLayer {
                          translationY = if (isDragging) dragOffset else 0f
                          scaleX = scale
                          scaleY = scale
                          this.shadowElevation = elevation
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .onGloballyPositioned { coordinates ->
                          if (itemHeights.size <= index) {
                            itemHeights.add(coordinates.size.height)
                          } else {
                            itemHeights[index] = coordinates.size.height
                          }
                        }
                        .pointerInput(Unit) {
                          detectTapGestures(
                              onLongPress = { showDeleteButton = true },
                              onTap = { onWidgetClick(widget) })
                        }
                        .pointerInput(Unit) {
                          detectDragGestures(
                              onDragStart = {
                                draggedWidget = widget
                                draggedIndex = index
                                showDeleteButton = true
                              },
                              onDragEnd = {
                                if (draggedWidget != null) {
                                  onReorder(widgets)
                                  draggedWidget = null
                                  draggedIndex = null
                                  dragOffset = 0f
                                }
                              },
                              onDragCancel = {
                                draggedWidget = null
                                draggedIndex = null
                                dragOffset = 0f
                              },
                              onDrag = { change, dragAmount ->
                                change.consume()
                                if (draggedWidget == widget) {
                                  dragOffset += dragAmount.y
                                  val currentIndex =
                                      widgets.indexOfFirst { it.widgetId == widget.widgetId }
                                  val itemHeight =
                                      itemHeights.getOrNull(currentIndex)
                                          ?: return@detectDragGestures

                                  val moveAmount = (dragOffset / (itemHeight * 0.6f)).roundToInt()

                                  if (moveAmount != 0) {
                                    val targetIndex =
                                        (currentIndex + moveAmount).coerceIn(0, widgets.lastIndex)
                                    if (targetIndex != currentIndex) {
                                      onReorder(
                                          widgets.toMutableList().apply {
                                            removeAt(currentIndex)
                                            add(targetIndex, widget)
                                          })
                                      draggedIndex = targetIndex
                                      dragOffset = 0f
                                    }
                                  }
                                }
                              })
                        }
                        .testTag("widget_card")) {
                  Box(modifier = Modifier.fillMaxWidth()) {
                    WidgetContent(widget = widget)
                    if (showDeleteButton) {
                      IconButton(
                          onClick = {
                            onDelete(widget.widgetId)
                            showDeleteButton = false
                          },
                          modifier = Modifier.align(Alignment.TopEnd).testTag("delete_icon")) {
                            Icon(Icons.Default.Close, contentDescription = "Delete Widget")
                          }
                    }
                  }
                }
          }
        }
      }
}

@Composable
fun WidgetContent(widget: Widget) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .background(Color(0xFFE0F7FA), shape = RoundedCornerShape(8.dp))
              .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector =
                when (widget.widgetTitle) {
                  "Calculator" -> Icons.Default.Calculate
                  "PDF Converter" -> Icons.Default.PictureAsPdf
                  "Weekly Planner" -> Icons.Default.DateRange
                  "Pomodoro Timer" -> Icons.Default.Timer
                  else -> Icons.Default.Widgets
                },
            contentDescription = null,
            tint = Color.Black)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
          Text(text = widget.widgetTitle, style = MaterialTheme.typography.h6)
          Text(text = widget.widgetContent, style = MaterialTheme.typography.body2)
        }
      }
}

@Composable
fun CommonWidgetDialog(viewModel: DashboardViewModel, onDismiss: () -> Unit, userId: String) {
  val commonWidgets = viewModel.getCommonWidgets()

  Dialog(onDismissRequest = onDismiss) {
    Column(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0F7FA)).padding(16.dp)) {
          Text(
              text = "Add widget",
              style = MaterialTheme.typography.h6,
              modifier = Modifier.padding(bottom = 16.dp))

          commonWidgets.forEach { widget ->
            Text(
                text = widget.widgetTitle,
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable {
                          viewModel.addWidget(
                              widget.copy(
                                  widgetId = "${userId}_${widget.widgetId}", ownerUid = userId))
                          onDismiss()
                        }
                        .padding(vertical = 12.dp)
                        .testTag("add_common_widget_button"))
            Divider(color = Color.LightGray, thickness = 0.5.dp)
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
              onClick = onDismiss,
              modifier = Modifier.align(Alignment.End),
              colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF26A69A))) {
                Text("Cancel", color = Color.White)
              }
        }
  }
}
