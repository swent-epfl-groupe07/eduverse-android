package com.github.se.eduverse.ui.speechRecognition

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.ui.camera.EnablePermissionButton
import java.util.Locale

/**
 * Interface to use the speech recognition service in the app.
 *
 * @param context The context of the app
 * @param title The title of the dialog
 * @param description The description of the way to use the speech recognition (specific to each use
 *   case)
 * @param onDismiss The callback to dismiss the dialog
 * @param onResult The callback to handle the recognized text
 * @param finishButton The button to finish the speech recognition process and close the dialog (it
 *   is enabled when the recording is stopped)
 */
@Composable
fun SpeechRecognizerInterface(
    context: Context,
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
    finishButton: @Composable (enabled: Boolean) -> Unit
) {
  val permissionState = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)

  // Check if the audio recording permission is granted
  if (permissionState != PERMISSION_GRANTED) {
    EnableAudioPermissionDialog(context, onDismiss)
  } else {
    SpeechRecognitionDialog(context, title, description, onDismiss, onResult, finishButton)
  }
}

/**
 * Dialog to show the speech recognition interface.
 *
 * @param context The context of the app
 * @param title The title of the dialog
 * @param description The description of the way to use the speech recognition (specific to each use
 *   case)
 * @param onDismiss The callback to dismiss the dialog
 * @param onResult The callback to receive the recognized text
 * @param finishButton The button to finish the speech recognition process and close the dialog (it
 *   is enabled when the recording is stopped)
 */
@Composable
fun SpeechRecognitionDialog(
    context: Context,
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
    finishButton: @Composable (enabled: Boolean) -> Unit
) {
  var isRecording by remember { mutableStateOf(false) }
  var rmsLevel by remember { mutableFloatStateOf(0f) }

  // Initialize the SpeechRecognizer
  val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

  // Initialize the RecognizerIntent
  val recognizerIntent = remember {
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
      putExtra(
          RecognizerIntent.EXTRA_MAX_RESULTS, 1) // Get only a single result, to lower memory usage
      putExtra(
          RecognizerIntent.EXTRA_PREFER_OFFLINE,
          true) // Use offline mode if available, for better efficiency
    }
  }

  DisposableEffect(Unit) {
    // Set the recognition listener on the SpeechRecognizer
    val recognitionListener =
        CustomSpeechRecognitionListener(
            context,
            onRecordingChanged = { isRecording = it },
            onResult = onResult,
            onRmsChanged = { rmsLevel = it })

    speechRecognizer.setRecognitionListener(recognitionListener)

    // Clean up the SpeechRecognizer when the composable is disposed
    onDispose {
      speechRecognizer.cancel()
      speechRecognizer.destroy()
    }
  }

  AlertDialog(
      modifier = Modifier.testTag("speechDialog"),
      onDismissRequest = {
        /**
         * Do nothing so that the user doesn't discard the whole ongoing transcription if he
         * accidentally clicks outside the dialog.
         */
      },
      title = {
        Text(
            title,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("speechDialogTitle"))
      },
      text = {
        Text(
            description,
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth().testTag("speechDialogDescription"),
            fontWeight = FontWeight.Bold)
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
          Button(
              onClick = { onDismiss() }, modifier = Modifier.testTag("speechDialogDismissButton")) {
                Text("Cancel", textAlign = TextAlign.Center)
              }
          Spacer(modifier = Modifier.width(4.dp))
          finishButton(!isRecording)
        }
      },
      dismissButton = {
        Button(
            onClick = {
              if (isRecording) {
                speechRecognizer.stopListening()
                isRecording = false
              } else {
                speechRecognizer.startListening(recognizerIntent)
                isRecording = true
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("speechDialogRecordButton")) {
              AnimatedMicroIcon(rmsLevel, isRecording)
            }
        // Instructions to the user on how to record speech
        Text(
            "Press the button below to start/stop recording. Speak clearly when recording. If you stay silent for a while, the recording will stop automatically.",
            Modifier.fillMaxWidth().testTag("speechDialogRecordInstructions"),
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp)
      })
}

/**
 * Dialog to show when the audio recording permission is denied.
 *
 * @param context The context of the app
 * @param onDismiss The callback to dismiss the dialog
 */
@Composable
fun EnableAudioPermissionDialog(context: Context, onDismiss: () -> Unit) {
  AlertDialog(
      modifier = Modifier.testTag("permissionDialog"),
      onDismissRequest = onDismiss,
      title = {
        Text(
            "Permission Denied",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("permissionDialogTitle"))
      },
      text = {
        Column {
          Text(
              "Audio recording permission is required to use this feature. Please enable the permission in the app settings, to be able to proceed.",
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().testTag("permissionDialogMessage"))
        }
      },
      confirmButton = { EnablePermissionButton(context) },
      dismissButton = {
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().testTag("permissionDialogDismissButton")) {
              Text("Cancel", textAlign = TextAlign.Center)
            }
      })
}

/**
 * Animated microphone icon to show the audio input level when recording.
 *
 * @param rmsLevel The RMS level of the audio input
 * @param isRecording The flag to indicate if the recording is in progress
 */
@Composable
fun AnimatedMicroIcon(rmsLevel: Float, isRecording: Boolean = false) {
  // Animate opacity of the icon based on the RMS level of the audio input when recording
  val animatedAlpha by
      animateFloatAsState(
          targetValue = 0.3f + (rmsLevel / 10).coerceIn(0f, 0.7f),
          animationSpec = tween(durationMillis = 300))

  Icon(
      imageVector = Icons.Filled.Mic,
      contentDescription = "Microphone",
      tint = LocalContentColor.current.copy(alpha = if (isRecording) animatedAlpha else 1f),
      modifier = Modifier.size(32.dp))
}
