package com.github.se.eduverse.repository

import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

open class AiAssistantRepository(private val client: OkHttpClient, private val apiKey: String) {

  open suspend fun askAssistant(userQuestion: String): String {
    val json =
        """
            {
              "model": "gpt-3.5-turbo",
              "messages": [
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": "$userQuestion"}
              ],
              "max_tokens": 1000,
              "temperature": 0.7
            }
        """
            .trimIndent()

    val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)
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
                  e.printStackTrace()
                  continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                  try {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                      val jsonObject = JSONObject(responseBody)
                      val choices = jsonObject.optJSONArray("choices")
                      if (choices != null && choices.length() > 0) {
                        val content =
                            choices
                                .getJSONObject(0)
                                .optJSONObject("message")
                                ?.optString("content", "")
                        if (!content.isNullOrEmpty()) {
                          continuation.resumeWith(Result.success(content.trim()))
                        } else {
                          continuation.resumeWith(Result.failure(IOException("Empty AI response")))
                        }
                      } else {
                        continuation.resumeWith(
                            Result.failure(IOException("No 'choices' in API response")))
                      }
                    } else {
                      continuation.resumeWith(Result.failure(IOException("Empty response body")))
                    }
                  } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.resumeWith(Result.failure(e))
                  }
                }
              })
    }
  }
}
