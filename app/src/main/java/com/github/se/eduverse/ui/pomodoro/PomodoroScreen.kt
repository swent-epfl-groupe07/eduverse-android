package com.github.se.eduverse.ui.pomodoro

//noinspection UsingMaterialAndMaterial3Libraries
import android.graphics.DashPathEffect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposePathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.todo.DefaultTodoTimeIcon
import com.github.se.eduverse.ui.todo.TodoItem
import com.github.se.eduverse.viewmodel.TimerViewModel
import com.github.se.eduverse.viewmodel.TodoListViewModel
import kotlin.math.roundToInt

@Composable
fun PomodoroScreen(
    navigationActions: NavigationActions,
    timerViewModel: TimerViewModel = viewModel(),
    todoListViewModel: TodoListViewModel = viewModel(factory = TodoListViewModel.Factory)
) {
  val timerState by timerViewModel.timerState.collectAsState()
  val todos = todoListViewModel.actualTodos.collectAsState()
  val selectedTodo = todoListViewModel.selectedTodo.collectAsState()
  var showTodoDialog by remember { mutableStateOf(false) }
  val currentTodoElapsedTime by timerViewModel.currentTodoElapsedTime.collectAsState()
  var lastTimerPausedState by remember { mutableStateOf(false) }

  // Update the time spent on the selected todo when the screen is disposed so that the correct time
  // appears in the todo list
  DisposableEffect(Unit) {
    onDispose {
      selectedTodo.value?.let {
        todoListViewModel.updateTodoTimeSpent(it, currentTodoElapsedTime!!)
      }
    }
  }

  Scaffold(
      topBar = { TopNavigationBar(navigationActions, screenTitle = null) },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.padding(paddingValues).fillMaxSize().testTag("pomodoroScreenContent"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {

              // If a todo is selected the todoItem is displayed, otherwise the button to select a
              // todo is displayed
              selectedTodo.value?.let { selectedTodo ->
                TodoItem(
                    selectedTodo,
                    {
                      /** Nothing to do on undo on this screen */
                    },
                    {
                      todoListViewModel.setTodoDoneFromPomodoro(
                          selectedTodo.copy(timeSpent = currentTodoElapsedTime!!))
                      todoListViewModel.unselectTodo()
                      timerViewModel.unbindTodoFromTimer()
                    },
                    {
                      if (timerViewModel.pomodoroSessionActive()) AnimatedTodoTimeIcon()
                      else DefaultTodoTimeIcon(selectedTodo)
                    },
                    {
                      IconButton(
                          onClick = {
                            todoListViewModel.updateTodoTimeSpent(
                                selectedTodo, currentTodoElapsedTime!!)
                            todoListViewModel.unselectTodo()
                            timerViewModel.unbindTodoFromTimer()
                          },
                          modifier = Modifier.testTag("unselectTodoButton")) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Unselect Todo",
                                tint = MaterialTheme.colorScheme.error)
                          }
                    },
                    currentTodoElapsedTime?.let { "${it/3600}h${it/60}" } ?: "")
              }
                  ?: SelectTodoButton(
                      enabled = todos.value.isNotEmpty(),
                      onClick = {
                        lastTimerPausedState = timerState.isPaused
                        timerViewModel.stopTimer()
                        showTodoDialog = true
                      },
                      text = "Choose a Todo to work on")

              Spacer(modifier = Modifier.height(48.dp))

              // Timer display
              Box(
                  modifier = Modifier.padding(16.dp).testTag("timerDisplay"),
                  contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(250.dp).testTag("timerProgressIndicator"),
                        color = MaterialTheme.colorScheme.primary,
                        progress = {
                          timerState.remainingSeconds.toFloat() /
                              when (timerState.currentTimerType) {
                                TimerType.POMODORO -> timerState.focusTime
                                TimerType.SHORT_BREAK -> timerState.shortBreakTime
                                TimerType.LONG_BREAK -> timerState.longBreakTime
                              }
                        },
                        strokeWidth = 20.dp)
                    Text(
                        text =
                            "${timerState.remainingSeconds / 60}:${String.format("%02d", timerState.remainingSeconds % 60)}",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.testTag("timerText"))
                  }

              // Timer type icons
              Row(
                  modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("timerTypeIcons"),
                  horizontalArrangement = Arrangement.SpaceEvenly) {
                    TimerTypeIcon(
                        icon = Icons.Default.Work,
                        label = "Focus",
                        isActive = timerState.currentTimerType == TimerType.POMODORO,
                        modifier = Modifier.testTag("focusIcon"))
                    TimerTypeIcon(
                        icon = Icons.Default.Coffee,
                        label = "Short Break",
                        isActive = timerState.currentTimerType == TimerType.SHORT_BREAK,
                        modifier = Modifier.testTag("shortBreakIcon"))
                    TimerTypeIcon(
                        icon = Icons.Default.Weekend,
                        label = "Long Break",
                        isActive = timerState.currentTimerType == TimerType.LONG_BREAK,
                        modifier = Modifier.testTag("longBreakIcon"))
                  }

              Text(
                  text = "Cycle: ${timerState.currentCycle}/${timerState.cycles}",
                  style = MaterialTheme.typography.bodyLarge,
                  modifier = Modifier.padding(bottom = 16.dp).testTag("cycleText"))

              // Control buttons
              Row(
                  modifier = Modifier.padding(16.dp).testTag("controlButtons"),
                  horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = {
                          if (timerState.isPaused) timerViewModel.startTimer()
                          else timerViewModel.stopTimer()
                        },
                        modifier = Modifier.size(48.dp).testTag("playPauseButton")) {
                          Icon(
                              imageVector =
                                  if (timerState.isPaused) Icons.Default.PlayArrow
                                  else Icons.Default.Pause,
                              contentDescription = if (timerState.isPaused) "Start" else "Pause",
                              modifier = Modifier.size(32.dp))
                        }
                    IconButton(
                        onClick = { timerViewModel.resetTimer() },
                        modifier = Modifier.size(48.dp).testTag("resetButton")) {
                          Icon(
                              Icons.Default.Refresh,
                              contentDescription = "Reset",
                              modifier = Modifier.size(32.dp))
                        }
                    IconButton(
                        onClick = { timerViewModel.skipToNextTimer() },
                        modifier = Modifier.size(48.dp).testTag("skipButton")) {
                          Icon(
                              Icons.Default.SkipNext,
                              contentDescription = "Skip",
                              modifier = Modifier.size(32.dp))
                        }
                  }

              // Settings
              var showSettings by remember { mutableStateOf(false) }
              Button(
                  onClick = { showSettings = true },
                  modifier = Modifier.testTag("settingsButton")) {
                    Text("Settings")
                  }
              if (showSettings) {
                SettingsDialog(
                    timerState = timerState,
                    onDismiss = { showSettings = false },
                    onSave = { focus, short, long, cycles ->
                      timerViewModel.updateSettings(focus * 60, short * 60, long * 60, cycles)
                      showSettings = false
                    })
              }

              // Select todo dialog
              if (showTodoDialog) {
                SelectTodoDialog(
                    todos = todos.value,
                    onDismiss = {
                      if (!lastTimerPausedState) timerViewModel.startTimer()
                      showTodoDialog = false
                    },
                    onSelect = {
                      todoListViewModel.selectTodo(it)
                      timerViewModel.bindTodoToTimer(it.timeSpent)
                    })
              }
            }
      }
}

@Composable
fun TimerTypeIcon(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
    Box(
        modifier =
            Modifier.size(56.dp)
                .background(
                    color =
                        if (isActive) MaterialTheme.colorScheme.tertiaryContainer
                        else Color.Transparent,
                    shape = CircleShape)
                .padding(8.dp),
        contentAlignment = Alignment.Center) {
          Icon(
              imageVector = icon,
              contentDescription = label,
              tint =
                  if (isActive) MaterialTheme.colorScheme.onTertiaryContainer
                  else MaterialTheme.colorScheme.onSurface)
        }
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp))
  }
}

@Composable
fun SettingsDialog(
    timerState: TimerState,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Long, Int) -> Unit
) {
  var focusTime by remember { mutableStateOf(timerState.focusTime / 60f) }
  var shortBreakTime by remember { mutableStateOf(timerState.shortBreakTime / 60f) }
  var longBreakTime by remember { mutableStateOf(timerState.longBreakTime / 60f) }
  var cycles by remember { mutableStateOf(timerState.cycles.toFloat()) }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Settings") },
      text = {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
              SliderSetting("Focus Time", focusTime, 1f..60f) { focusTime = it }
              SliderSetting("Short Break", shortBreakTime, 1f..30f) { shortBreakTime = it }
              SliderSetting("Long Break", longBreakTime, 1f..60f) { longBreakTime = it }
              SliderSetting("Cycles", cycles, 1f..10f) { cycles = it }
            }
      },
      confirmButton = {
        Button(
            onClick = {
              onSave(
                  focusTime.roundToInt().toLong(),
                  shortBreakTime.roundToInt().toLong(),
                  longBreakTime.roundToInt().toLong(),
                  cycles.roundToInt())
            },
            modifier = Modifier.testTag("settingsSaveButton")) {
              Text("Save")
            }
      },
      dismissButton = {
        Button(onClick = onDismiss, modifier = Modifier.testTag("settingsCancelButton")) {
          Text("Cancel")
        }
      },
      modifier = Modifier.testTag("settingsDialog"),
      backgroundColor = MaterialTheme.colorScheme.surface)
}

@Composable
fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
  Column {
    Text(text = "$label: ${value.roundToInt()} min")
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = (valueRange.endInclusive - valueRange.start).toInt() - 1,
        modifier = Modifier.testTag("$label Slider"))
  }
}

/**
 * Composable that represents a dialog to select a todo to work on
 *
 * @param todos the list of todos to select from
 * @param onDismiss code executed when the dialog is dismissed
 * @param onSelect code executed when a todo is selected
 */
@Composable
fun SelectTodoDialog(todos: List<Todo>, onDismiss: () -> Unit, onSelect: (Todo) -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(
            "Select Todo",
            modifier = Modifier.fillMaxWidth().testTag("selectTodoDialogTitle"),
            textAlign = TextAlign.Center)
      },
      text = {
        LazyColumn {
          items(todos.size) { i ->
            val todo = todos[i]
            TextButton(
                modifier = Modifier.testTag("todoOption_${todo.uid}"),
                onClick = {
                  onSelect(todo)
                  onDismiss()
                }) {
                  Text(todo.name)
                }
            Divider()
          }
        }
      },
      confirmButton = {
        Button(onClick = onDismiss, modifier = Modifier.testTag("selectTodoDismissButton")) {
          Text("Cancel")
        }
      },
      modifier = Modifier.testTag("selectTodoDialog"),
      backgroundColor = MaterialTheme.colorScheme.surface)
}

/**
 * Composable for the button that allows the user to select a todo to work on
 *
 * @param enabled whether the button is enabled
 * @param onClick the action to perform when the button is clicked
 * @param text the text to display inside the button
 */
@Composable
fun SelectTodoButton(enabled: Boolean, onClick: () -> Unit, text: String) {
  Box(
      modifier =
          Modifier.testTag("selectTodoButton")
              .height(48.dp)
              .padding(start = 24.dp, end = 24.dp)
              .fillMaxWidth()
              .background(Color.Transparent)
              .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val halfStrokeWidth = strokeWidth / 2
                val pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)

                drawRoundRect(
                    color = Color.Gray,
                    size =
                        this.size.copy(
                            width = this.size.width - strokeWidth,
                            height = this.size.height - strokeWidth),
                    topLeft = androidx.compose.ui.geometry.Offset(halfStrokeWidth, halfStrokeWidth),
                    style =
                        Stroke(width = strokeWidth, pathEffect = pathEffect.toComposePathEffect()),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()))
              }
              .clickable(enabled = enabled, onClick = onClick),
      contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
          Text(
              text = text,
              color = Color.Gray,
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(start = 8.dp))
        }
      }
}

/** Composable to display an animated timer icon when the timer is running */
@Composable
fun AnimatedTodoTimeIcon() {
  val infiniteTransition = rememberInfiniteTransition(label = "opacity")
  val alpha by
      infiniteTransition.animateFloat(
          initialValue = 0.1f,
          targetValue = 1f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 30000, easing = LinearEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "opacity")
  Icon(
      imageVector = Icons.Filled.Timer,
      contentDescription = "Animated Timer Icon",
      modifier = Modifier.size(22.dp).alpha(alpha),
      tint = MaterialTheme.colorScheme.tertiary)
}
