package com.github.se.eduverse.repository

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AiAssistantRepositoryTest {

  /**
   * Fake OkHttpClient to simulate server responses. Allows testing various scenarios like
   * successful responses, network failures, and malformed data.
   *
   * @param responseCode The HTTP status code to simulate.
   * @param responseBody The body of the response (can be a JSONObject or a raw String for malformed
   *   JSON).
   * @param shouldFail Simulates a network failure when true.
   */
  class FakeOkHttpClient(
      private val responseCode: Int,
      private val responseBody: Any?, // Accepts JSONObject or raw String
      private val shouldFail: Boolean = false
  ) : OkHttpClient() {
    override fun newCall(request: Request): Call {
      return object : Call {
        override fun execute(): Response {
          throw UnsupportedOperationException("Synchronous execution not supported")
        }

        override fun enqueue(callback: Callback) {
          if (shouldFail) {
            callback.onFailure(this, IOException("Simulated network failure"))
          } else {
            val body =
                when (responseBody) {
                  is JSONObject ->
                      responseBody.toString().toResponseBody("application/json".toMediaTypeOrNull())
                  is String -> responseBody.toResponseBody("application/json".toMediaTypeOrNull())
                  else -> null
                }
            val response =
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(responseCode)
                    .message("OK")
                    .body(body)
                    .build()
            callback.onResponse(this, response)
          }
        }

        override fun isExecuted() = false

        override fun cancel() {}

        override fun isCanceled() = false

        override fun request() = request

        override fun timeout() = throw UnsupportedOperationException()

        override fun clone(): Call = this
      }
    }
  }

  /** Test a successful response with a valid JSON body. */
  @Test
  fun `test successful response`() = runBlocking {
    val message = JSONObject().put("role", "assistant").put("content", "Hello from AI")
    val choice = JSONObject().put("message", message)
    val validResponseJson = JSONObject().put("choices", JSONArray().put(choice))

    val client = FakeOkHttpClient(200, validResponseJson)
    val repository = AiAssistantRepository(client, "fake-api-key")
    val result = repository.askAssistant("What is AI?")
    assertEquals("Hello from AI", result)
  }

  /**
   * Test a response with an empty "choices" array in the JSON body. Should throw an IOException
   * with a specific message.
   */
  @Test
  fun `test empty choices response`() = runBlocking {
    val emptyResponseJson = JSONObject().put("choices", JSONArray())

    val client = FakeOkHttpClient(200, emptyResponseJson)
    val repository = AiAssistantRepository(client, "fake-api-key")

    try {
      repository.askAssistant("What is AI?")
      fail("Expected an IOException due to empty choices")
    } catch (e: IOException) {
      assertEquals("Empty AI response", e.message)
    }
  }

  /** Test a network failure scenario. Should throw an IOException with a specific message. */
  @Test
  fun `test network failure`() = runBlocking {
    val client = FakeOkHttpClient(200, null, shouldFail = true)
    val repository = AiAssistantRepository(client, "fake-api-key")

    try {
      repository.askAssistant("What is AI?")
      fail("Expected an IOException due to network failure")
    } catch (e: IOException) {
      assertEquals("Simulated network failure", e.message)
    }
  }

  /**
   * Test a response with a null or empty body. Should throw an IOException with a specific message.
   */
  @Test
  fun `test empty response body`() = runBlocking {
    val client = FakeOkHttpClient(200, null)
    val repository = AiAssistantRepository(client, "fake-api-key")

    try {
      repository.askAssistant("What is AI?")
      fail("Expected an IOException due to empty response body")
    } catch (e: IOException) {
      assertEquals("Empty response body", e.message)
    }
  }

  /**
   * Test a malformed JSON response. Should throw an IOException or JSONException, depending on
   * where the failure occurs.
   */
  @Test
  fun `test malformed JSON response`() = runBlocking {
    val malformedResponseJson = "{ invalid_json }"

    val client = FakeOkHttpClient(200, malformedResponseJson)
    val repository = AiAssistantRepository(client, "fake-api-key")

    try {
      repository.askAssistant("What is AI?")
      fail("Expected an IOException due to malformed JSON")
    } catch (e: Exception) {
      assertTrue(
          "Expected an IOException or JSONException, but got ${e.javaClass.simpleName}",
          e is IOException || e is org.json.JSONException)
    }
  }
}
