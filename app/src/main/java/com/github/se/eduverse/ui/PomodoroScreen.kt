package com.github.se.eduverse.ui

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.eduverse.model.TimerType
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    navigationActions: NavigationActions,
    timerViewModel: TimerViewModel = viewModel()
) {
    val timerState by timerViewModel.timerState.collectAsState()
    val timerSeconds =
        if (timerState.lastTimer == TimerType.POMODORO)
            POMODORO_TIMER_SECONDS
        else
            REST_TIMER_SECONDS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomodoro Timer") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.goBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Go back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val progress =
                timerState.remainingSeconds.toFloat() / (timerSeconds.toFloat())
            val minutes = timerState.remainingSeconds / SECONDS_IN_A_MINUTE
            val seconds = timerState.remainingSeconds - (minutes * SECONDS_IN_A_MINUTE)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(256.dp), // Replace with size instead of width/height
                        progress = progress
                    )
                    Text(
                        text = "Timer\n$minutes : ${String.format("%02d", seconds)}",
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        modifier = Modifier.padding(bottom = 8.dp),
                        onClick = {
                            if (timerState.isPaused) {
                                timerViewModel.startTimer(timerState.remainingSeconds)
                            } else {
                                timerViewModel.stopTimer()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (timerState.isPaused) Icons.Filled.PlayArrow
                            else Icons.Filled.Pause,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        modifier = Modifier.padding(bottom = 8.dp),
                        onClick = {
                            timerViewModel.resetTimer(POMODORO_TIMER_SECONDS)
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    }
                }
            }
        }
    }
}

const val POMODORO_TIMER_SECONDS = 1500L
const val REST_TIMER_SECONDS = 300L
const val SECONDS_IN_A_MINUTE = 60L
