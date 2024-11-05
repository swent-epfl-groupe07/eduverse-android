package com.github.se.eduverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class PublicationViewModel(private val repository: PublicationRepository) : ViewModel() {

  private val _publications = MutableStateFlow<List<Publication>>(emptyList())
  open val publications: StateFlow<List<Publication>>
    get() = _publications

  private val _error = MutableStateFlow<String?>(null) // Null signifie qu'il n'y a pas d'erreur
  open val error: StateFlow<String?>
    get() = _error

  init {
    loadPublications()
  }

  // Charge un sous-ensemble aléatoire de publications avec gestion d'erreur
  private fun loadPublications() {
    viewModelScope.launch {
      try {
        val newPublications = repository.loadRandomPublications()
        _publications.value = newPublications
        _error.value = null // Réinitialise l'erreur en cas de succès
      } catch (e: Exception) {
        _error.value = "Échec du chargement des publications" // Définir un message d'erreur
      }
    }
  }

  // Charge plus de publications ou recharge une nouvelle sélection si toutes ont été vues
  open suspend fun loadMorePublications() {
    viewModelScope.launch {
      try {
        if (_publications.value.isEmpty()) {
          val newPublications = repository.loadRandomPublications()
          _publications.value = newPublications
        } else {
          val morePublications = repository.loadRandomPublications()
          _publications.value = (_publications.value + morePublications).shuffled()
        }
        _error.value = null // Réinitialise l'erreur en cas de succès
      } catch (e: Exception) {
        _error.value = "Échec du chargement des publications" // Définir un message d'erreur
      }
    }
  }
}
