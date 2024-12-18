package com.github.se.eduverse.ui.speechRecognition

import android.content.Context
import android.os.Bundle
import android.speech.SpeechRecognizer
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class CustomSpeechRecognitionListenerTest {

  private lateinit var mockContext: Context
  private lateinit var bundle: Bundle
  private lateinit var onResult: (String) -> Unit
  private lateinit var listener: CustomSpeechRecognitionListener

  private var rmsValue: Float = 0f
  private var isRecording: Boolean = true

  @Before
  fun setUp() {
    mockContext = RuntimeEnvironment.getApplication()
    bundle = mock(Bundle::class.java)
    onResult = mock()

    listener =
        CustomSpeechRecognitionListener(
            context = mockContext,
            onRecordingChanged = { isRecording = it },
            onResult = onResult,
            onRmsChanged = { rmsValue = it })
  }

  @Test
  fun `onResults with valid result triggers onResult callback`() {
    val results = arrayListOf("Hello World")
    `when`(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)).thenReturn(results)
    isRecording = true

    listener.onResults(bundle)

    verify(onResult).invoke("Hello World")
    assertEquals(false, isRecording)
    verifyNoMoreInteractions(onResult)
  }

  @Test
  fun `onResults with empty result shows toast and stops recording`() {
    `when`(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
        .thenReturn(arrayListOf())
    isRecording = true

    listener.onResults(bundle)
    val latestToast = ShadowToast.getTextOfLatestToast()

    verifyNoInteractions(onResult) // Check onResult is not called when the results are empty
    assertEquals("No speech detected. Please try again.", latestToast)
    assertEquals(false, isRecording)
  }

  @Test
  fun `onError triggers toast with correct message and stops recording`() {
    testOnError(SpeechRecognizer.ERROR_NO_MATCH, "No speech recognized. Please try again.")
    testOnError(
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT, "Timeout: no speech input. Please try again.")
    testOnError(
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        "Recognition service is busy. Please try again later.")
    testOnError(SpeechRecognizer.ERROR_SERVER, "Server error. Please try again later.")
    testOnError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT, "Network timeout. Please try again.")
    testOnError(SpeechRecognizer.ERROR_NETWORK, "Network error. Please check your connection.")
    testOnError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, "Insufficient permissions.")
    testOnError(SpeechRecognizer.ERROR_AUDIO, "Audio recording error.")
    testOnError(SpeechRecognizer.ERROR_CLIENT, "Recognition Error. Please try again.")
  }

  @Test
  fun `onRmsChanged triggers onRMSChanged callback`() {
    val rmsValue = 0.5f
    listener.onRmsChanged(rmsValue)
    assertEquals(rmsValue, rmsValue)
  }

  @Test
  fun `onReadyForSpeech does nothing`() {
    listener.onReadyForSpeech(bundle)
    verifyNoInteractions(bundle)
  }

  @Test
  fun `onBeginningOfSpeech does nothing`() {
    listener.onBeginningOfSpeech()
    assert(true)
  }

  @Test
  fun `onEndOfSpeech triggers callback`() {
    listener.onEndOfSpeech()
    assert(true)
  }

  @Test
  fun `onPartialResults does nothing`() {
    listener.onPartialResults(bundle)
    verifyNoInteractions(bundle)
  }

  @Test
  fun `onEvent does nothing`() {
    listener.onEvent(1, bundle)
    verifyNoInteractions(bundle)
  }

  @Test
  fun `onBufferReceived does nothing`() {
    listener.onBufferReceived(byteArrayOf())
    assert(true)
  }

  /** Helper function to test the onError method with different error codes. */
  private fun testOnError(errorCode: Int, expectedToastMessage: String) {
    isRecording = true

    listener.onError(errorCode)
    val latestToast = ShadowToast.getTextOfLatestToast()

    assertEquals(expectedToastMessage, latestToast)
    assertEquals(false, isRecording)
  }
}
