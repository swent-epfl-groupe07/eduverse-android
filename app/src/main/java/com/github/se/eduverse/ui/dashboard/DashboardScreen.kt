package com.github.se.eduverse.ui.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.se.eduverse.R
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

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
          TopAppBar(
              modifier =
                  Modifier.fillMaxWidth()
                      .background(
                          Brush.horizontalGradient(
                              colors =
                                  listOf(
                                      MaterialTheme.colorScheme.secondary,
                                      MaterialTheme.colorScheme.primary))),
              title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Image(
                      painter = painterResource(id = R.drawable.eduverse_logo_png),
                      contentDescription = "Logo",
                      modifier = Modifier.size(140.dp).testTag("screenTitle"))
                }
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
              backgroundColor = Color.Transparent,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              elevation = 0.dp)
        },
        bottomBar = {
          BottomNavigationMenu(
              onTabSelect = { route -> navigationActions.navigateTo(route) },
              tabList = LIST_TOP_LEVEL_DESTINATION,
              selectedItem = Route.DASHBOARD)
        },
        backgroundColor = MaterialTheme.colorScheme.background) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            val sortedWidgets = remember(widgets) { widgets.sortedBy { it.order } }

            ReorderableWidgetList(
                items = sortedWidgets,
                onReorder = { reorderedList ->
                  val updatedList =
                      reorderedList.mapIndexed { index, widget -> widget.copy(order = index) }
                  viewModel.updateWidgetOrder(updatedList)
                },
                onDelete = { widgetId ->
                  val deletedWidget =
                      widgets.find { it.widgetId == widgetId } ?: return@ReorderableWidgetList
                  val remainingWidgets =
                      widgets
                          .filter { it.widgetId != widgetId }
                          .map { widget ->
                            if (widget.order > deletedWidget.order) {
                              widget.copy(order = widget.order - 1)
                            } else {
                              widget
                            }
                          }
                  viewModel.removeWidgetAndUpdateOrder(widgetId, remainingWidgets)
                },
                navigationActions = navigationActions,
                onAddWidget = { showAddWidgetDialog = true })

            // FAB positioned manually
            if (widgets.isNotEmpty()) {
              Box(
                  modifier =
                      Modifier.align(Alignment.BottomEnd)
                          .padding(bottom = 76.dp, end = 16.dp)
                          .size(56.dp)
                          .shadow(
                              elevation = 6.dp,
                              shape = RoundedCornerShape(16.dp),
                              spotColor = MaterialTheme.colorScheme.primary)
                          .background(
                              color = MaterialTheme.colorScheme.primary,
                              shape = RoundedCornerShape(16.dp))
                          .clickable { showAddWidgetDialog = true }
                          .testTag("add_widget_button"),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Widget",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White)
                  }
            }

            if (showAddWidgetDialog) {
              AddWidgetDialog(
                  viewModel = viewModel,
                  onDismiss = { showAddWidgetDialog = false },
                  userId = auth.currentUser?.uid ?: return@Box)
            }
          }
        }
  }
}

@Composable
private fun ReorderableWidgetList(
    items: List<Widget>,
    onReorder: (List<Widget>) -> Unit,
    onDelete: (String) -> Unit,
    navigationActions: NavigationActions,
    onAddWidget: () -> Unit
) {
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val dragChannel = remember { Channel<Float>(Channel.CONFLATED) }

  var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
  var dragOffset by remember { mutableStateOf(0f) }
  var draggingItem by remember { mutableStateOf<Widget?>(null) }
  var initialTouchY by remember { mutableStateOf(0f) }
  var itemHeight by remember { mutableStateOf(0f) }

  LaunchedEffect(Unit) {
    for (delta in dragChannel) {
      listState.scrollBy(delta)
    }
  }

  if (items.isEmpty()) {
    Box(
        modifier = Modifier.fillMaxSize().testTag("empty_dashboard_message"),
        contentAlignment = Alignment.Center) {
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary)

                Text(
                    text = "Welcome to Eduverse!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text =
                        "Your dashboard is empty. Start by adding widgets to customize your learning experience.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(24.dp))

                FilledTonalButton(
                    onClick = onAddWidget, modifier = Modifier.testTag("empty_state_add_button")) {
                      Icon(
                          Icons.Default.Add,
                          contentDescription = null,
                          modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Add Your First Widget")
                    }
              }
        }
  } else {
    LazyColumn(
        state = listState,
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 80.dp // Increased bottom padding to account for bottom bar
                ),
        modifier =
            Modifier.fillMaxSize()
                .testTag("widget_list")
                .padding(bottom = 64.dp) // Add padding to prevent overlap with bottom bar
                .pointerInput(items) {
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
                            val totalDragOffset = dragOffset
                            val targetPosition =
                                ((totalDragOffset / itemHeight).roundToInt() + currentIndex)
                                    .coerceIn(0, items.lastIndex)

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
                        val draggingIdx =
                            draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                        dragOffset += dragAmount.y

                        val moveThreshold = itemHeight * 0.5f
                        val rawNewIndex =
                            (draggingIdx + (dragOffset / itemHeight).roundToInt()).coerceIn(
                                0, items.lastIndex)

                        if (abs(dragOffset) >= moveThreshold && rawNewIndex != draggingIdx) {
                          val newList =
                              items.toMutableList().apply {
                                removeAt(draggingIdx)
                                add(rawNewIndex, items[draggingIdx])
                              }
                          onReorder(newList)
                          draggingItemIndex = rawNewIndex
                          dragOffset %= moveThreshold
                        }

                        // Auto-scroll
                        val scrollThreshold = 100f
                        val dragPos = change.position.y
                        when {
                          dragPos < scrollThreshold -> scope.launch { dragChannel.send(-10f) }
                          dragPos > size.height - scrollThreshold ->
                              scope.launch { dragChannel.send(10f) }
                        }
                      })
                }) {
          itemsIndexed(items = items, key = { _, item -> item.widgetId }) { index, item ->
            val isDragging = index == draggingItemIndex
            val elevation by animateFloatAsState(if (isDragging) 8f else 2f)

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
                          route?.let { navigationActions.navigateTo(it) }
                        },
                elevation = elevation.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = MaterialTheme.colorScheme.surface) {
                  Row(
                      modifier = Modifier.fillMaxWidth().padding(16.dp),
                      verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector =
                                when (item.widgetType) {
                                  "TIMER" -> Icons.Default.Timer
                                  "CALCULATOR" -> Icons.Default.Calculate
                                  "PDF_GENERATOR" -> Icons.Default.PictureAsPdf
                                  "FOLDERS" -> Icons.Default.FolderOpen
                                  "TODO_LIST" -> Icons.Default.Checklist
                                  "TIME_TABLE" -> Icons.Default.DateRange
                                  "QUIZZ" -> Icons.Default.Book
                                  "ASSISTANT" -> Icons.Default.Psychology
                                  "GALLERY" -> Icons.Default.PhotoLibrary
                                  else -> Icons.Default.Widgets
                                },
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary)

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                              text = item.widgetTitle,
                              style = MaterialTheme.typography.titleMedium,
                              color = MaterialTheme.colorScheme.onSurface)
                          Text(
                              text = item.widgetContent,
                              style = MaterialTheme.typography.bodyMedium,
                              color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(
                            onClick = { onDelete(item.widgetId) },
                            modifier = Modifier.size(32.dp).testTag("delete_icon")) {
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
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary) {
          Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = "Add Widget",
                color = MaterialTheme.colorScheme.onSurface,
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
                                    "PDF_GENERATOR" -> Icons.Default.PictureAsPdf
                                    "FOLDERS" -> Icons.Default.FolderOpen
                                    "TODO_LIST" -> Icons.Default.Checklist
                                    "TIME_TABLE" -> Icons.Default.DateRange
                                    "QUIZZ" -> Icons.Default.Book
                                    "ASSISTANT" -> Icons.Default.Psychology
                                    "GALLERY" -> Icons.Default.PhotoLibrary
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

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
              Text("Cancel")
            }
          }
        }
  }
}
