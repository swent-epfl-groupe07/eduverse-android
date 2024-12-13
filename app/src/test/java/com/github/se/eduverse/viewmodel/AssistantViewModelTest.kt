package com.github.se.eduverse.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.se.eduverse.repository.AiAssistantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class AiAssistantViewModelTest {

  /** Rule to ensure LiveData operations run synchronously for testing. */
  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  // A test dispatcher for controlling coroutine execution
  private val testDispatcher = StandardTestDispatcher()

  /** Sets up the main dispatcher to use the test dispatcher before each test. */
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  /** Resets the main dispatcher to its original state after each test. */
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /**
   * Test case to verify the successful flow of sending a question to the AI assistant. Ensures that
   * the conversation state is updated correctly after receiving a response.
   */
  @Test
  fun testSendQuestionSuccess() = runTest {
    // Mock repository behavior to return a specific answer
    val mockRepository = Mockito.mock(AiAssistantRepository::class.java)
    Mockito.`when`(mockRepository.askAssistant("What is AI?"))
        .thenReturn("Artificial Intelligence is...")

    // Initialize the ViewModel with the mocked repository
    val viewModel = AiAssistantViewModel(mockRepository)

    // Send a question to the ViewModel
    viewModel.sendQuestion("What is AI?")
    advanceUntilIdle() // Ensure all coroutines have completed execution

    // Assert that the conversation state is updated with the new question and answer
    val conversation = viewModel.conversation.first()
    Assert.assertEquals(1, conversation.size)
    Assert.assertEquals("What is AI?" to "Artificial Intelligence is...", conversation.first())
  }

  /**
   * Test case to verify error handling when the repository throws an exception. Ensures that the
   * error message state is updated correctly.
   */
  @Test
  fun testSendQuestionError() = runTest {
    // Mock repository behavior to throw an exception
    val mockRepository = Mockito.mock(AiAssistantRepository::class.java)
    Mockito.`when`(mockRepository.askAssistant("Will this fail?"))
        .thenThrow(RuntimeException("Test error"))

    // Initialize the ViewModel with the mocked repository
    val viewModel = AiAssistantViewModel(mockRepository)

    // Send a question that triggers an exception
    viewModel.sendQuestion("Will this fail?")
    advanceUntilIdle()

    // Assert that the error message state is updated correctly
    val errorMessage = viewModel.errorMessage.first()
    Assert.assertEquals("An error occurred. Please try again.", errorMessage)
  }
}
