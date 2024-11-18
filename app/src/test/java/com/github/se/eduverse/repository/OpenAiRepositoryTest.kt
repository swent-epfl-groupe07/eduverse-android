package com.github.se.eduverse.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.verify

class OpenAiRepositoryTest {

  private lateinit var mockClient: OkHttpClient
  private lateinit var openAiRepository: OpenAiRepository
  private lateinit var mockCall: Call

  @Before
  fun setUp() {
    mockClient = mockk()
    openAiRepository = OpenAiRepository(mockClient)
    mockCall = mockk()
  }

  @Test
  fun `summarizeText should call onSuccess with summary when response is successful`() {
    val responseBody =
        """
            {
              "choices": [
                { "text": "This is a summary." }
              ]
            }
        """
            .trimIndent()
            .toResponseBody("application/json".toMediaType())

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://api.openai.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .build()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.enqueue(any()) } answers
        {
          val callback = it.invocation.args[0] as Callback
          callback.onResponse(mockCall, mockResponse)
        }

    val onSuccess: (String?) -> Unit = mockk(relaxed = true)
    val onFailure: (Exception) -> Unit = mockk(relaxed = true)

    openAiRepository.summarizeText(
        text = "Sample text", onSuccess = onSuccess, onFailure = onFailure)

    verify { onSuccess("This is a summary.") }
    verify(exactly = 0) { onFailure(any()) }
  }

  @Test
  fun `summarizeText should call onFailure when response is unsuccessful`() {
    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.enqueue(any()) } answers
        {
          val callback = it.invocation.args[0] as Callback
          callback.onFailure(mockCall, IOException("Network error"))
        }

    val onSuccess: (String?) -> Unit = mockk(relaxed = true)
    val onFailure: (Exception) -> Unit = mockk(relaxed = true)

    openAiRepository.summarizeText(
        text = "Sample text", onSuccess = onSuccess, onFailure = onFailure)

    verify(exactly = 0) { onSuccess(any()) }
    verify { onFailure(ofType(IOException::class)) }
  }

  @Test
  fun `summarizeText should trigger onFailure when network call fails`() {

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.enqueue(any()) } answers
        {
          val callback = it.invocation.args[0] as Callback
          callback.onFailure(mockCall, IOException("Simulated network error"))
        }

    val onSuccess: (String?) -> Unit = mockk(relaxed = true)
    val onFailure: (Exception) -> Unit = mockk(relaxed = true)

    openAiRepository.summarizeText(
        text = "Sample text", onSuccess = onSuccess, onFailure = onFailure)

    verify { onFailure(ofType(IOException::class)) }
    verify(exactly = 0) { onSuccess(any()) }
  }
}
