package com.github.se.eduverse.ui.speechRecognition

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import com.github.se.eduverse.showToast

interface SpeechRecognitionListener : RecognitionListener {
  override fun onResults(results: Bundle?)

  override fun onError(error: Int)

  override fun onRmsChanged(rmsdB: Float)

  override fun onReadyForSpeech(params: Bundle?)

  override fun onBeginningOfSpeech()

  override fun onEndOfSpeech()

  override fun onPartialResults(partialResults: Bundle?)

  override fun onEvent(eventType: Int, params: Bundle?)
}

/**
 * Custom implementation of the RecognitionListener interface to handle the speech recognition
 * events.
 *
 * @param context The context of the app
 * @param onRecordingChanged Callback function to notify the calling activity when the recording
 *   state changes
 * @param onResult Callback function to pass the recognized speech text to the calling activity
 * @param onRmsChanged Callback function to pass the RMS level of the audio input to the calling
 *   activity
 */
class CustomSpeechRecognitionListener(
    private val context: Context,
    private val onRecordingChanged: (Boolean) -> Unit,
    private val onResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit
) : SpeechRecognitionListener {
  // Handle the recognition results
  override fun onResults(results: Bundle?) {
    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { list ->
      val newText = list.firstOrNull().orEmpty()
      if (newText.isNotEmpty()) {
        onResult(newText)
      } else {
        context.showToast("No speech detected. Please try again.")
      }
    }
    onRecordingChanged(false)
  }

  // Handle the recognition errors
  override fun onError(error: Int) {
    context.showToast(getErrorMessage(error))
    onRecordingChanged(false)
  }

  // Update the RMS level of the audio input in order to animate the microphone icon when recording
  override fun onRmsChanged(rmsdB: Float) {
    onRmsChanged.invoke(rmsdB)
  }

  // Other methods of the RecognitionListener that are not used thus not implemented here
  override fun onReadyForSpeech(params: Bundle?) {}

  override fun onBeginningOfSpeech() {}

  override fun onEndOfSpeech() {}

  override fun onPartialResults(partialResults: Bundle?) {}

  override fun onEvent(eventType: Int, params: Bundle?) {}

  override fun onBufferReceived(buffer: ByteArray?) {}

  /**
   * Helper function to get the error message based on the recognition error code.
   *
   * @param errorCode The error code returned by the SpeechRecognizer
   * @return The error message corresponding to the error code
   */
  private fun getErrorMessage(errorCode: Int): String {
    return when (errorCode) {
      SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
      SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions."
      SpeechRecognizer.ERROR_NETWORK -> "Network error. Please check your connection."
      SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Please try again."
      SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
      SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
          "Recognition service is busy. Please try again later."
      SpeechRecognizer.ERROR_SERVER -> "Server error. Please try again later."
      SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout: no speech input. Please try again."
      else -> "Recognition Error. Please try again."
    }
  }
}
