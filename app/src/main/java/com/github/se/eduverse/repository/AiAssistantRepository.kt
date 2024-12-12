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

open class AiAssistantRepository(private val client: OkHttpClient, private val apiKey: String) {

  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val requestAdapter = moshi.adapter(OpenAiRequest::class.java)
  private val responseAdapter = moshi.adapter(OpenAiResponse::class.java)

  open suspend fun askAssistant(userQuestion: String): String {
    val requestBodyObject =
        OpenAiRequest(
            model = "gpt-3.5-turbo",
            messages =
                listOf(
                    Message(role = "system", content = "You are a helpful assistant."),
                    Message(role = "user", content = userQuestion)),
            temperature = 0.7)

    val jsonWithoutMaxTokens = requestAdapter.toJson(requestBodyObject)
    val jsonObject = JSONObject(jsonWithoutMaxTokens)
    jsonObject.put("max_tokens", 1000)
    val finalJson = jsonObject.toString()

    val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), finalJson)
    val request =
        Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

    return suspendCancellableCoroutine { continuation ->
      client
          .newCall(request)
          .enqueue(
              object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                  continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                  try {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                      val openAiResponse = responseAdapter.fromJson(responseBody)
                      val content = openAiResponse?.choices?.firstOrNull()?.message?.content
                      if (!content.isNullOrEmpty()) {
                        continuation.resumeWith(Result.success(content.trim()))
                      } else {
                        continuation.resumeWith(Result.failure(IOException("Empty AI response")))
                      }
                    } else {
                      continuation.resumeWith(Result.failure(IOException("Empty response body")))
                    }
                  } catch (e: Exception) {
                    continuation.resumeWith(Result.failure(e))
                  }
                }
              })
    }
  }
}
