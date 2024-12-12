package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Publication
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.tasks.await

open class PublicationRepository(private val db: FirebaseFirestore) {

  open suspend fun loadRandomPublications(limit: Long = 20): List<Publication> {
    return try {
      val random = UUID.randomUUID().toString()
      db.collection("publications")
          .orderBy("id")
          .startAt(random)
          .limit(limit)
          .get()
          .await()
          .documents
          .mapNotNull { it.toObject(Publication::class.java) }
          .shuffled()
    } catch (e: Exception) {
      val t = e
      emptyList()
    }
  }
}
