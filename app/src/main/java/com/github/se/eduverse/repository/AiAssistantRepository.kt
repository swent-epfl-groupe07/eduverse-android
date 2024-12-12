package com.github.se.eduverse.repository

import com.github.se.eduverse.api.Message
import com.github.se.eduverse.api.OpenAiRequest
import com.github.se.eduverse.api.OpenAiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

/**
 * Repository class for interacting with OpenAI's API. This class handles the creation and execution
 * of requests to OpenAI's GPT models.
 *
 * @param client An instance of OkHttpClient used to perform HTTP requests.
 * @param apiKey The API key for authenticating with OpenAI's API.
 */
open class AiAssistantRepository(private val client: OkHttpClient, private val apiKey: String) {

  // Moshi instance for JSON serialization and deserialization
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val requestAdapter = moshi.adapter(OpenAiRequest::class.java)
  private val responseAdapter = moshi.adapter(OpenAiResponse::class.java)

  /**
   * Sends a user question to the OpenAI API and retrieves the assistant's response.
   *
   * @param userQuestion The question or prompt to send to the OpenAI model.
   * @return The assistant's response as a String.
   * @throws IOException If there is an error during the request or response parsing.
   */
  open suspend fun askAssistant(userQuestion: String): String {
    // Prepare the request payload
    val requestBodyObject =
        OpenAiRequest(
            model = "gpt-3.5-turbo",
            messages =
                listOf(
                    Message(role = "system", content = "You are a helpful assistant."),
                    Message(role = "user", content = userQuestion)),
            temperature = 0.7)

    // Convert the payload to JSON and add the "max_tokens" parameter
    val jsonWithoutMaxTokens = requestAdapter.toJson(requestBodyObject)
    val jsonObject = JSONObject(jsonWithoutMaxTokens).apply { put("max_tokens", 1000) }
    val finalJson = jsonObject.toString()

    // Build the HTTP request
    val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), finalJson)
    val request =
        Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

    // Execute the HTTP request and handle the response
    return suspendCancellableCoroutine { continuation ->
      client
          .newCall(request)
          .enqueue(
              object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                  // Resume the coroutine with an exception if the request fails
                  continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                  try {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                      val openAiResponse = responseAdapter.fromJson(responseBody)
                      val content = openAiResponse?.choices?.firstOrNull()?.message?.content
                      if (!content.isNullOrEmpty()) {
                        // Resume the coroutine with the assistant's response
                        continuation.resumeWith(Result.success(content.trim()))
                      } else {
                        // Handle empty content in the response
                        continuation.resumeWith(Result.failure(IOException("Empty AI response")))
                      }
                    } else {
                      // Handle missing response body
                      continuation.resumeWith(Result.failure(IOException("Empty response body")))
                    }
                  } catch (e: Exception) {
                    // Resume the coroutine with an exception if parsing fails
                    continuation.resumeWith(Result.failure(e))
                  }
                }
              })
    }
  }
}
