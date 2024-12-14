package com.github.se.eduverse.fake

import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import com.google.firebase.firestore.FirebaseFirestore

class FakePublicationRepository : PublicationRepository(db = FirebaseFirestore.getInstance()) {

  private val fakePublications = mutableListOf<Publication>()
  private val fakeFollowedPublications = mutableListOf<Publication>()

  fun setPublications(publications: List<Publication>) {
    fakePublications.clear()
    fakePublications.addAll(publications)
  }

  fun setFollowedPublications(publications: List<Publication>) {
    fakeFollowedPublications.clear()
    fakeFollowedPublications.addAll(publications)
  }

  override suspend fun loadRandomPublications(
      followed: List<String>?,
      limit: Long
  ): List<Publication> {
    return if (followed == null) {
      fakePublications.take(limit.toInt())
    } else {
      fakeFollowedPublications.take(limit.toInt())
    }
  }
}
