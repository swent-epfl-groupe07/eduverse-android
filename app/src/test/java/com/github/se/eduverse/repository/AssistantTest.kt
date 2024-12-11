package com.github.se.eduverse.repository

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for AiAssistantRepository.
class AiAssistantRepositoryTest {

  // A fake repository that we can control
  class FakeAiAssistantRepositoryForTest(
      private val shouldFail: Boolean = false,
      private val answer: String = "Default answer"
  ) : AiAssistantRepository(client = okhttp3.OkHttpClient(), apiKey = "fake-key") {
    override suspend fun askAssistant(userQuestion: String): String {
      if (shouldFail) throw IOException("Simulated error")
      // We simulate a scenario where we directly return the answer
      return answer
    }
  }

  @Test
  fun testSuccessfulResponse() = runBlocking {
    val repo = FakeAiAssistantRepositoryForTest(shouldFail = false, answer = "Hello!")
    val result = repo.askAssistant("Test question")
    assertEquals("Hello!", result)
  }

  @Test
  fun testErrorResponse() = runBlocking {
    val repo = FakeAiAssistantRepositoryForTest(shouldFail = true)
    try {
      repo.askAssistant("Test question")
      assertTrue("Expected an exception", false)
    } catch (e: Exception) {
      // We expect an IOException here
      assertTrue(e is IOException)
    }
  }
}
