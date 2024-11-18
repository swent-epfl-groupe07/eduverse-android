package com.github.se.eduverse.repository

import com.github.se.eduverse.BuildConfig
import com.github.se.eduverse.api.OpenAiRequest
import com.github.se.eduverse.api.OpenAiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAiRepository(private val client: OkHttpClient) {
  private val apiKey = BuildConfig.OPENAI_API_KEY
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val requestAdapter = moshi.adapter(OpenAiRequest::class.java)
  private val responseAdapter = moshi.adapter(OpenAiResponse::class.java)
  private val DEFAULT_MAX_SUMMARY_TOKENS = 1000 // Maximum number of tokens in the summary
  private val OPENAI_SUMMARY_MODEL = "text-davinci-003"
  private val DEFAULT_SUMMARY_TEMPERATURE = 0.5

  /**
   * Summarizes the given text using the OpenAI API.
   *
   * @param text The text to summarize
   * @param maxTokens The maximum number of tokens the summary should contain
   * @param temperature The temperature(degree of creativity/liberty in the generated text) to use
   *   when generating the summary
   * @param onSuccess The callback to be called when the summary is successfully generated
   * @param onFailure The callback to be called when an error occurs
   */
  fun summarizeText(
      text: String,
      maxTokens: Int = DEFAULT_MAX_SUMMARY_TOKENS,
      temperature: Double = DEFAULT_SUMMARY_TEMPERATURE,
      onSuccess: (String?) -> Unit,
      onFailure: (Exception) -> Unit
  ) {

    val jsonBody =
        requestAdapter.toJson(
            OpenAiRequest(
                model = OPENAI_SUMMARY_MODEL,
                prompt = "Summarize the following text:\n$text",
                max_tokens = maxTokens,
                temperature = temperature))

    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

    val request =
        Request.Builder()
            .url("https://api.openai.com/v1/completions")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
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
                    onFailure(IOException("Unexpected code $response"))
                  } else {
                    response.body?.let { body ->
                      val openAiResponse = responseAdapter.fromJson(body.string())
                      val summary = openAiResponse?.choices?.firstOrNull()?.text?.trim()
                      onSuccess(summary)
                    } ?: onSuccess(null)
                  }
                }
              }
            })
  }
}
