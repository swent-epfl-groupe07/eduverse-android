package com.github.se.eduverse.repository

import com.github.se.eduverse.BuildConfig
import com.github.se.eduverse.api.Message
import com.github.se.eduverse.api.OpenAiRequest
import com.github.se.eduverse.api.OpenAiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

open class OpenAiRepository(private val client: OkHttpClient) {
  private val apiKey = BuildConfig.OPENAI_API_KEY
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val requestAdapter = moshi.adapter(OpenAiRequest::class.java)
  private val responseAdapter = moshi.adapter(OpenAiResponse::class.java)
  private val OPENAI_SUMMARY_MODEL = "gpt-3.5-turbo"
  private val DEFAULT_SUMMARY_TEMPERATURE = 0.3

  /**
   * Summarizes the given text using the OpenAI API.
   *
   * @param text The text to summarize
   * @param onSuccess The callback to be called when the summary is successfully generated
   * @param onFailure The callback to be called when an error occurs
   */
  open fun summarizeText(
      text: String,
      onSuccess: (String?) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val messages =
        listOf(Message(role = "user", content = "Provide a summary of the following: $text"))

    val jsonBody =
        requestAdapter.toJson(
            OpenAiRequest(
                model = OPENAI_SUMMARY_MODEL,
                messages = messages,
                temperature = DEFAULT_SUMMARY_TEMPERATURE))

    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

    val request =
        Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

    client
        .newCall(request)
        .enqueue(
            object : Callback {
              override fun onFailure(call: Call, e: IOException) {
                onFailure(e)
              }

              override fun onResponse(call: Call, response: Response) {
                response.use {
                  if (!response.isSuccessful) {
                    onFailure(IOException("Unsuccessful openAI response: $response"))
                  } else {
                    response.body?.let { body ->
                      val openAiResponse = responseAdapter.fromJson(body.string())
                      val reply = openAiResponse?.choices?.firstOrNull()?.message?.content?.trim()
                      onSuccess(reply)
                    } ?: onSuccess(null)
                  }
                }
              }
            })
  }
}
