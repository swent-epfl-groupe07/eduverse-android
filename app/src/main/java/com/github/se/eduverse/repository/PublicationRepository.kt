package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Publication
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

open class PublicationRepository(private val db: FirebaseFirestore) {

  open suspend fun loadRandomPublications(limit: Long = 20): List<Publication> {
    return try {
      db.collection("publications")
          .whereNotEqualTo("mediaUrl", null) // Only take publications and not comments
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
