package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Publication
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

open class PublicationRepository(private val db: FirebaseFirestore) {

  open suspend fun loadRandomPublications(userId: String? = null, limit: Long = 20): List<Publication> {
    return try {
      val filter = if (userId == null) {
          Filter.notEqualTo("mediaUrl", null) // Only take publications and not comments
      } else {
          Filter.and(
              Filter.notEqualTo("mediaUrl", null),
              Filter.equalTo("userId", userId)
          )
      }
      db.collection("publications")
          .where(filter)
          .get()
          .await()
          .documents
          .shuffled()
          .take(limit.toInt())
          .mapNotNull { it.toObject(Publication::class.java) }
    } catch (e: Exception) {
      emptyList()
    }
  }
}
