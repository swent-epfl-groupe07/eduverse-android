package com.github.se.eduverse.ui.Pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.TimerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    navigationActions: NavigationActions,
    timerViewModel: TimerViewModel = viewModel()
) {
  val timerState by timerViewModel.timerState.collectAsState()

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  "Pomodoro Timer",
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag("topBarTitle"))
            },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("backButton")) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Go back")
                  }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent))
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.padding(paddingValues).fillMaxSize().testTag("pomodoroScreenContent"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              // Timer display
              Box(
                  modifier = Modifier.padding(16.dp).testTag("timerDisplay"),
                  contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(250.dp).testTag("timerProgressIndicator"),
                        progress =
                            timerState.remainingSeconds.toFloat() /
                                when (timerState.currentTimerType) {
                                  TimerType.POMODORO -> timerState.focusTime
                                  TimerType.SHORT_BREAK -> timerState.shortBreakTime
                                  TimerType.LONG_BREAK -> timerState.longBreakTime
                                },
                        strokeWidth = 12.dp)
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
                        if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                    shape = CircleShape)
                .padding(8.dp),
        contentAlignment = Alignment.Center) {
          Icon(
              imageVector = icon,
              contentDescription = label,
              tint =
                  if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
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
      modifier = Modifier.testTag("settingsDialog"))
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
