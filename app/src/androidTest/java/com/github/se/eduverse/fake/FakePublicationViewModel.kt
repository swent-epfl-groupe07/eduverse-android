package com.github.se.eduverse.fake

import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.viewmodel.PublicationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakePublicationViewModel(repository: FakePublicationRepository) :
    PublicationViewModel(repository) {

  val _publications = MutableStateFlow<List<Publication>>(emptyList())
  override val publications: StateFlow<List<Publication>>
    get() = _publications

  val _error = MutableStateFlow<String?>(null)
  override val error: StateFlow<String?>
    get() = _error

  fun setPublications(publications: List<Publication>) {
    _publications.value = publications
  }

  override suspend fun loadMorePublications() {
    try {
      val morePublications = repository.loadRandomPublications()
      _publications.value = (_publications.value + morePublications).shuffled()
      _error.value = null
    } catch (e: Exception) {
      _error.value = "Échec du chargement des publications"
    }
  }
}
