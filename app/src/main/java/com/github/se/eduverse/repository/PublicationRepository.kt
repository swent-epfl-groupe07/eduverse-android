package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Publication
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

open class PublicationRepository(private val db: FirebaseFirestore) {

  // Fonction pour charger un sous-ensemble aléatoire de publications
  open suspend fun loadRandomPublications(limit: Long = 20): List<Publication> {
    return try {
      db.collection("publications")
          .orderBy("timestamp") // Utilise un champ ordonné pour la cohérence
          .limit(limit)
          .get()
          .await()
          .documents
          .mapNotNull { it.toObject(Publication::class.java) }
          .shuffled() // Mélange pour un effet aléatoire
    } catch (e: Exception) {
      emptyList()
    }
  }
}
