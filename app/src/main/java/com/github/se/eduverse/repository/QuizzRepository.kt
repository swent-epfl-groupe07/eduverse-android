package com.github.se.eduverse.repository

import com.github.se.eduverse.api.Question
import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

// Repository class for handling quiz-related operations using the OpenAI API
open class QuizzRepository(private val client: OkHttpClient, private val apiKey: String) {

  // Fetches quiz questions from GPT based on the provided topic, difficulty, and number of
  // questions
  open suspend fun getQuestionsFromGPT(
      topic: String,
      difficulty: String,
      numberOfQuestions: Int
  ): List<Question> {
    // JSON payload for the OpenAI API request
    val json =
        """
                {
                    "model": "gpt-3.5-turbo",
                    "messages": [
                        {"role": "system", "content": "You are a helpful assistant."},
                        {"role": "user", "content": "Create a quiz about $topic with $numberOfQuestions questions. The difficulty is $difficulty. Provide 4 answer options per question. Indicate the correct answer with: 'Correct Answer: <answer>'."}
                    ],
                    "max_tokens": 1000,
                    "temperature": 0.7
                }
            """

    // Create a request body with the JSON payload
    val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)
    // Build the HTTP request
    val request =
        Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey") // Authorization header with API key
            .post(requestBody)
            .build()

    // Perform the network call and process the response
    return suspendCancellableCoroutine { continuation ->
      client
          .newCall(request)
          .enqueue(
              object : Callback {
                // Handle network failure
                override fun onFailure(call: Call, e: IOException) {
                  e.printStackTrace()
                  continuation.resumeWith(Result.failure(e))
                }

                // Handle successful response
                override fun onResponse(call: Call, response: Response) {
                  try {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                      // Parse the JSON response
                      val jsonObject = JSONObject(responseBody)
                      val choicesArray = jsonObject.optJSONArray("choices")
                      if (choicesArray != null && choicesArray.length() > 0) {
                        val text =
                            choicesArray
                                .getJSONObject(0)
                                .optJSONObject("message")
                                ?.optString("content", "")
                        if (text != null && text.isNotEmpty()) {
                          // Parse the quiz questions from the response text
                          val questionList = parseQuestionsFromResponse(text)
                          continuation.resumeWith(Result.success(questionList))
                        } else {
                          continuation.resumeWith(
                              Result.failure(IOException("Empty question text")))
                        }
                      } else {
                        continuation.resumeWith(
                            Result.failure(IOException("No 'choices' found in API response")))
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

  // Parses quiz questions from the response text returned by GPT
  private fun parseQuestionsFromResponse(responseText: String): List<Question> {
    val questions = mutableListOf<Question>()
    // Split the response text into blocks representing individual questions
    responseText.split("\n\n").forEach { block ->
      val lines = block.split("\n") // Split block into lines
      // Ensure block contains at least 5 lines and ends with the correct answer
      if (lines.size >= 5 && lines.last().startsWith("Correct Answer: ")) {
        val questionText = lines[0] // First line is the question
        val answers = lines.subList(1, 5) // Next 4 lines are the answer options
        val correctAnswer = lines.last().removePrefix("Correct Answer: ") // Extract correct answer
        // Create a Question object and add it to the list
        questions.add(Question(questionText, answers, correctAnswer))
      }
    }
    return questions // Return the parsed list of questions
  }
}
