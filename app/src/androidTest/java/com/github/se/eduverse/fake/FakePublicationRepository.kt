package com.github.se.eduverse.fake

import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import com.google.firebase.firestore.FirebaseFirestore

class FakePublicationRepository : PublicationRepository(db = FirebaseFirestore.getInstance()) {

  private val fakePublications = mutableListOf<Publication>()

  fun setPublications(publications: List<Publication>) {
    fakePublications.clear()
    fakePublications.addAll(publications)
  }

  override suspend fun loadRandomPublications(limit: Long): List<Publication> {
    return fakePublications.take(limit.toInt())
  }
}
