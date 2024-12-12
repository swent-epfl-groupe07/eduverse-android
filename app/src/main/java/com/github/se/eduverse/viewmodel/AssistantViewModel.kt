package com.github.se.eduverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.repository.AiAssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class AiAssistantViewModel(private val repository: AiAssistantRepository) : ViewModel() {

  private val _conversation = MutableStateFlow<List<Pair<String, String>>>(emptyList())
  open val conversation: StateFlow<List<Pair<String, String>>> = _conversation

  private val _isLoading = MutableStateFlow(false)
  open val isLoading: StateFlow<Boolean> = _isLoading

  private val _errorMessage = MutableStateFlow<String?>(null)
  open val errorMessage: StateFlow<String?> = _errorMessage

  fun sendQuestion(question: String) {
    if (question.isBlank()) return

    viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null

      try {
        val answer = repository.askAssistant(question)
        _conversation.value = _conversation.value + (question to answer)
      } catch (e: Exception) {
        _errorMessage.value = "An error occurred. Please try again."
      } finally {
        _isLoading.value = false
      }
    }
  }
}
