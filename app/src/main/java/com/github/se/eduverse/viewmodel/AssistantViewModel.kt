package com.github.se.eduverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.repository.AiAssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the state and logic of an AI assistant screen. This class interacts with
 * the `AiAssistantRepository` to fetch AI responses and exposes state flows for UI updates.
 *
 * @param repository The repository used to interact with the OpenAI API.
 */
open class AiAssistantViewModel(private val repository: AiAssistantRepository) : ViewModel() {

  // MutableStateFlow for the conversation (list of user-question, AI-answer pairs)
  private val _conversation = MutableStateFlow<List<Pair<String, String>>>(emptyList())

  /** Public read-only StateFlow for observing the conversation in the UI. */
  open val conversation: StateFlow<List<Pair<String, String>>> = _conversation

  // MutableStateFlow for tracking loading state
  private val _isLoading = MutableStateFlow(false)

  /** Public read-only StateFlow for observing the loading state in the UI. */
  open val isLoading: StateFlow<Boolean> = _isLoading

  // MutableStateFlow for handling error messages
  private val _errorMessage = MutableStateFlow<String?>(null)

  /** Public read-only StateFlow for observing error messages in the UI. */
  open val errorMessage: StateFlow<String?> = _errorMessage

  /**
   * Sends a question to the AI assistant and updates the conversation state.
   *
   * @param question The user's question to send to the AI assistant. If the question is blank, this
   *   method does nothing.
   */
  fun sendQuestion(question: String) {
    // Ignore empty or blank questions
    if (question.isBlank()) return

    // Launch a coroutine to fetch the AI's response
    viewModelScope.launch {
      _isLoading.value = true // Set loading state to true
      _errorMessage.value = null // Clear any existing error messages

      try {
        // Fetch the AI's answer from the repository
        val answer = repository.askAssistant(question)
        // Update the conversation with the new question and answer
        _conversation.value = _conversation.value + (question to answer)
      } catch (e: Exception) {
        // Set an error message if an exception occurs
        _errorMessage.value = "An error occurred. Please try again."
      } finally {
        // Reset the loading state to false
        _isLoading.value = false
      }
    }
  }
}
