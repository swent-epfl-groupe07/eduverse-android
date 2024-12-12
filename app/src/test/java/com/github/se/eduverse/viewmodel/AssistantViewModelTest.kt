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

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun testSendQuestionSuccess() = runTest {
    val mockRepository = Mockito.mock(AiAssistantRepository::class.java)
    Mockito.`when`(mockRepository.askAssistant("What is AI?"))
        .thenReturn("Artificial Intelligence is...")

    val viewModel = AiAssistantViewModel(mockRepository)
    viewModel.sendQuestion("What is AI?")

    advanceUntilIdle() // S'assurer que toutes les coroutines ont fini de s'exécuter

    val conversation = viewModel.conversation.first()
    Assert.assertEquals(1, conversation.size)
    Assert.assertEquals("What is AI?" to "Artificial Intelligence is...", conversation.first())
  }

  @Test
  fun testSendQuestionError() = runTest {
    val mockRepository = Mockito.mock(AiAssistantRepository::class.java)
    Mockito.`when`(mockRepository.askAssistant("Will this fail?"))
        .thenThrow(RuntimeException("Test error"))

    val viewModel = AiAssistantViewModel(mockRepository)
    viewModel.sendQuestion("Will this fail?")

    advanceUntilIdle()

    val errorMessage = viewModel.errorMessage.first()
    Assert.assertEquals("An error occurred. Please try again.", errorMessage)
  }
}
