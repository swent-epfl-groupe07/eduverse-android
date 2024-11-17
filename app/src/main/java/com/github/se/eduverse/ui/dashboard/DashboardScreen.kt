package com.github.se.eduverse.ui.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.se.eduverse.model.CommonWidgetType
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun DashboardScreen(
    navigationActions: NavigationActions,
    viewModel: DashboardViewModel = hiltViewModel()
) {
  val auth = FirebaseAuth.getInstance()

  val widgets by viewModel.widgetList.collectAsState()

  var showAddWidgetDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {

      navigationActions.navigateTo(Screen.AUTH)
      return@LaunchedEffect
    }
    auth.currentUser?.let { user -> viewModel.fetchWidgets(user.uid) }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  "Eduverse",
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag("app_title"))
            },
            actions = {
              IconButton(
                  onClick = { navigationActions.navigateTo(Screen.SEARCH) },
                  modifier = Modifier.testTag("search_button")) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search profiles",
                        tint = MaterialTheme.colorScheme.onPrimary)
                  }
            },
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = 4.dp)
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = { showAddWidgetDialog = true },
            modifier = Modifier.testTag("add_widget_button"),
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)) {
              Icon(Icons.Default.Add, "Add Widget")
            }
      },
      bottomBar = {
        BottomNavigationMenu(
            onTabSelect = { route -> navigationActions.navigateTo(route) },
            tabList = LIST_TOP_LEVEL_DESTINATION,
            selectedItem = Route.DASHBOARD)
      }) {
        Box(modifier = Modifier.fillMaxSize()) {
          val sortedWidgets = remember(widgets) { widgets.sortedBy { it.order } }

          ReorderableWidgetList(
              items = sortedWidgets,
              onReorder = { reorderedList ->
                // Update orders to be sequential starting from 0
                val updatedList =
                    reorderedList.mapIndexed { index, widget -> widget.copy(order = index) }
                viewModel.updateWidgetOrder(updatedList)
              },
              onDelete = { widgetId ->
                // Find the widget to delete and all widgets after it
                val deletedWidget =
                    widgets.find { it.widgetId == widgetId } ?: return@ReorderableWidgetList
                val remainingWidgets =
                    widgets
                        .filter { it.widgetId != widgetId }
                        .map { widget ->
                          // If the widget's order is greater than the deleted widget's order,
                          // decrease its order by 1
                          if (widget.order > deletedWidget.order) {
                            widget.copy(order = widget.order - 1)
                          } else {
                            widget
                          }
                        }

                viewModel.removeWidgetAndUpdateOrder(widgetId, remainingWidgets)
              },
              navigationActions = navigationActions)

          if (showAddWidgetDialog) {
            AddWidgetDialog(
                viewModel = viewModel,
                onDismiss = { showAddWidgetDialog = false },
                userId = auth.currentUser?.uid ?: return@Box)
          }
        }
      }
}

@Composable
private fun ReorderableWidgetList(
    items: List<Widget>,
    onReorder: (List<Widget>) -> Unit,
    onDelete: (String) -> Unit,
    navigationActions: NavigationActions // Add navigation actions parameter
) {
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val dragChannel = remember { Channel<Float>(Channel.CONFLATED) }

  var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
  var dragOffset by remember { mutableStateOf(0f) }
  var draggingItem by remember { mutableStateOf<Widget?>(null) }

  // Store the initial touch position
  var initialTouchY by remember { mutableStateOf(0f) }
  // Store the height of items for threshold calculations
  var itemHeight by remember { mutableStateOf(0f) }

  LaunchedEffect(Unit) {
    for (delta in dragChannel) {
      listState.scrollBy(delta)
    }
  }

  LazyColumn(
      state = listState,
      modifier =
          Modifier.fillMaxSize().testTag("widget_list").pointerInput(items) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                  listState.layoutInfo.visibleItemsInfo
                      .firstOrNull { item ->
                        offset.y.toInt() in item.offset..(item.offset + item.size)
                      }
                      ?.let { item ->
                        draggingItemIndex = item.index
                        draggingItem = items.getOrNull(item.index)
                        initialTouchY = offset.y
                        itemHeight = item.size.toFloat()
                      }
                },
                onDragCancel = {
                  draggingItemIndex = null
                  dragOffset = 0f
                  draggingItem = null
                },
                onDragEnd = {
                  draggingItem?.let { currentItem ->
                    draggingItemIndex?.let { currentIndex ->
                      // Calculate final position based on drag offset
                      val totalDragOffset = dragOffset
                      val targetPosition =
                          ((totalDragOffset / itemHeight).roundToInt() + currentIndex).coerceIn(
                              0, items.lastIndex)

                      if (targetPosition != currentIndex) {
                        val reorderedList =
                            items.toMutableList().apply {
                              removeAt(currentIndex)
                              add(targetPosition, currentItem)
                            }
                        onReorder(reorderedList)
                      }
                    }
                  }
                  draggingItemIndex = null
                  dragOffset = 0f
                  draggingItem = null
                },
                onDrag = { change, dragAmount ->
                  change.consume()

                  val draggingIdx = draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                  val currentItem = draggingItem ?: return@detectDragGesturesAfterLongPress

                  // Update the cumulative drag offset
                  dragOffset += dragAmount.y

                  // Calculate the threshold for item movement (50% of item height)
                  val moveThreshold = itemHeight * 0.5f

                  // Calculate potential new index based on drag offset
                  val rawNewIndex =
                      (draggingIdx + (dragOffset / itemHeight).roundToInt()).coerceIn(
                          0, items.lastIndex)

                  // Only reorder if we've moved past the threshold
                  if (abs(dragOffset) >= moveThreshold) {
                    if (rawNewIndex != draggingIdx) {
                      val newList =
                          items.toMutableList().apply {
                            removeAt(draggingIdx)
                            add(rawNewIndex, currentItem)
                          }
                      onReorder(newList)
                      draggingItemIndex = rawNewIndex
                      // Reset dragOffset to remainder to maintain smooth movement
                      dragOffset = dragOffset % moveThreshold
                    }
                  }

                  // Handle auto-scroll near edges
                  val scrollThreshold = 100f // pixels from edge to trigger scroll
                  listState.layoutInfo.visibleItemsInfo.firstOrNull()?.let { firstVisible ->
                    val dragPos = firstVisible.offset + dragOffset
                    when {
                      dragPos < scrollThreshold -> {
                        scope.launch { dragChannel.send(-10f) }
                      }
                      dragPos > size.height - scrollThreshold -> {
                        scope.launch { dragChannel.send(10f) }
                      }
                    }
                  }
                })
          }) {
        itemsIndexed(items = items, key = { _, item -> item.widgetId }) { index, item ->
          val isDragging = index == draggingItemIndex
          val elevation by animateFloatAsState(if (isDragging) 8f else 1f)

          Card(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 8.dp)
                      .graphicsLayer {
                        if (isDragging) {
                          translationY = dragOffset
                          scaleX = 1.05f
                          scaleY = 1.05f
                          shadowElevation = elevation
                        }
                      }
                      .zIndex(if (isDragging) 1f else 0f)
                      .testTag("widget_card")
                      .clickable {
                        val route =
                            CommonWidgetType.entries.find { it.name == item.widgetType }?.route

                        // Navigate if route exists
                        route?.let { navigationActions.navigateTo(it) }
                      },
              elevation = elevation.dp,
              shape = RoundedCornerShape(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                  Row(
                      modifier = Modifier.fillMaxWidth().padding(16.dp),
                      verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector =
                                when (item.widgetType) {
                                  "TIMER" -> Icons.Default.Timer
                                  "CALCULATOR" -> Icons.Default.Calculate
                                  "PDF_CONVERTER" -> Icons.Default.PictureAsPdf
                                  "FOLDERS" -> Icons.Default.FolderOpen
                                  "TODO_LIST" -> Icons.Default.Checklist
                                  "TIME_TABLE" -> Icons.Default.DateRange
                                  else -> Icons.Default.Widgets
                                },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                          Text(text = item.widgetTitle, style = MaterialTheme.typography.titleLarge)
                          Text(
                              text = item.widgetContent,
                              style = MaterialTheme.typography.bodyMedium)
                        }

                        IconButton(
                            onClick = { onDelete(item.widgetId) },
                            modifier = Modifier.size(24.dp).testTag("delete_icon")) {
                              Icon(
                                  Icons.Default.Close,
                                  contentDescription = "Delete",
                                  tint = MaterialTheme.colorScheme.error)
                            }
                      }
                }
              }
        }
      }
}

@Composable
private fun AddWidgetDialog(viewModel: DashboardViewModel, onDismiss: () -> Unit, userId: String) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
      Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text(
            text = "Add Widget",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp))

        viewModel.getCommonWidgets().forEach { widget ->
          TextButton(
              onClick = {
                viewModel.addWidget(widget.copy(ownerUid = userId))
                onDismiss()
              },
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(vertical = 4.dp)
                      .testTag("add_common_widget_button")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          imageVector =
                              when (widget.widgetType) {
                                "TIMER" -> Icons.Default.Timer
                                "CALCULATOR" -> Icons.Default.Calculate
                                "PDF_CONVERTER" -> Icons.Default.PictureAsPdf
                                "FOLDERS" -> Icons.Default.FolderOpen
                                "TODO_LIST" -> Icons.Default.Checklist
                                "TIME_TABLE" -> Icons.Default.DateRange
                                else -> Icons.Default.Widgets
                              },
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(text = widget.widgetTitle)
                    }
              }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
      }
    }
  }
}
