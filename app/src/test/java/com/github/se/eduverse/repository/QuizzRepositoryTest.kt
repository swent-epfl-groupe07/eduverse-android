package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Question
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class QuizzRepositoryTest {

  private lateinit var quizzRepository: QuizzRepository
  private lateinit var mockClient: OkHttpClient
  private lateinit var mockCall: Call
  private lateinit var mockResponse: Response

  private val apiKey = "test_api_key"

  @Before
  fun setUp() {
    mockClient = mockk()
    mockCall = mockk()
    mockResponse = mockk()

    quizzRepository = QuizzRepository(mockClient, apiKey)

    every { mockClient.newCall(any()) } returns mockCall
  }

  @Test
  fun `getQuestionsFromGPT returns questions when response is successful`() = runBlocking {
    val topic = "Math"
    val difficulty = "easy"
    val numberOfQuestions = 2

    val mockResponseBody =
        """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "created": 1677652288,
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "1. What is 2 + 2?\nA. 3\nB. 4\nC. 5\nD. 6\nCorrect Answer: B\n\n2. What is 5 * 6?\nA. 11\nB. 30\nC. 20\nD. 40\nCorrect Answer: B"
                  },
                  "finish_reason": "stop"
                }
              ]
            }
        """
            .trimIndent()

    every { mockCall.enqueue(any()) } answers
        {
          val callback = it.invocation.args[0] as Callback
          every { mockResponse.isSuccessful } returns true
          every { mockResponse.body } returns
              mockResponseBody.toResponseBody("application/json".toMediaType())
          callback.onResponse(mockCall, mockResponse)
        }

    val questions = quizzRepository.getQuestionsFromGPT(topic, difficulty, numberOfQuestions)

    assertNotNull(questions)
    assertEquals(2, questions.size)

    val question1 = questions[0]
    assertEquals("1. What is 2 + 2?", question1.text)
    assertEquals(listOf("A. 3", "B. 4", "C. 5", "D. 6"), question1.answers)
    assertEquals("B", question1.correctAnswer)

    val question2 = questions[1]
    assertEquals("2. What is 5 * 6?", question2.text)
    assertEquals(listOf("A. 11", "B. 30", "C. 20", "D. 40"), question2.answers)
    assertEquals("B", question2.correctAnswer)
  }

  @Test
  fun `getQuestionsFromGPT throws IOException when response body is null`() = runBlocking {
    val topic = "Math"
    val difficulty = "easy"
    val numberOfQuestions = 2

    every { mockCall.enqueue(any()) } answers
        {
          val callback = it.invocation.args[0] as Callback
          every { mockResponse.body } returns null
          callback.onResponse(mockCall, mockResponse)
        }

    try {
      quizzRepository.getQuestionsFromGPT(topic, difficulty, numberOfQuestions)
      fail("Expected IOException")
    } catch (e: IOException) {
      assertEquals("Empty response body", e.message)
    }
  }

  @Test
  fun `getQuestionsFromGPT throws IOException when choices array is missing`() = runBlocking {
    val topic = "Math"
    val difficulty = "easy"
    val numberOfQuestions = 2

    val mockResponseBody =
        """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "created": 1677652288,
              "choices": []
            }
        """
            .trimIndent()

    every { mockCall.enqueue(any()) } answers
        {
          val callback = it.invocation.args[0] as Callback
          every { mockResponse.isSuccessful } returns true
          every { mockResponse.body } returns
              mockResponseBody.toResponseBody("application/json".toMediaType())
          callback.onResponse(mockCall, mockResponse)
        }

    try {
      quizzRepository.getQuestionsFromGPT(topic, difficulty, numberOfQuestions)
      fail("Expected IOException")
    } catch (e: IOException) {
      assertEquals("No 'choices' found in API response", e.message)
    }
  }

  @Test
  fun `getQuestionsFromGPT handles failure callback`() = runBlocking {
    val topic = "Math"
    val difficulty = "easy"
    val numberOfQuestions = 2

    val exception = IOException("Network error")

    every { mockCall.enqueue(any()) } answers
        {
          val callback = it.invocation.args[0] as Callback
          callback.onFailure(mockCall, exception)
        }

    try {
      quizzRepository.getQuestionsFromGPT(topic, difficulty, numberOfQuestions)
      fail("Expected IOException")
    } catch (e: IOException) {
      assertEquals("Network error", e.message)
    }
  }

  @Test
  fun `parseQuestionsFromResponse parses questions correctly`() {
    val responseText =
        """
            1. What is 2 + 2?
            A. 3
            B. 4
            C. 5
            D. 6
            Correct Answer: B

            2. What is 5 * 6?
            A. 11
            B. 30
            C. 20
            D. 40
            Correct Answer: B
        """
            .trimIndent()

    val method =
        quizzRepository.javaClass.getDeclaredMethod(
            "parseQuestionsFromResponse", String::class.java)
    method.isAccessible = true
    val questions = method.invoke(quizzRepository, responseText) as List<Question>

    assertNotNull(questions)
    assertEquals(2, questions.size)

    val question1 = questions[0]
    assertEquals("1. What is 2 + 2?", question1.text)
    assertEquals(listOf("A. 3", "B. 4", "C. 5", "D. 6"), question1.answers)
    assertEquals("B", question1.correctAnswer)

    val question2 = questions[1]
    assertEquals("2. What is 5 * 6?", question2.text)
    assertEquals(listOf("A. 11", "B. 30", "C. 20", "D. 40"), question2.answers)
    assertEquals("B", question2.correctAnswer)
  }
}
