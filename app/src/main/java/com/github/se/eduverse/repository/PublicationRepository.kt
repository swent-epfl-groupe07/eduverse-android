package com.github.se.eduverse.repository

import android.util.Log
import com.github.se.eduverse.model.Publication
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.tasks.await

open class PublicationRepository(private val db: FirebaseFirestore) {

  open suspend fun loadRandomPublications(
      followed: List<String>? = null,
      limit: Long = 20
  ): List<Publication> {
    return try {
      val random = UUID.randomUUID().toString()

      val orderedQuery = db.collection("publications").orderBy("id").startAt(random)

      val filteredQuery =
          if (followed == null) {
            orderedQuery
          } else {
            orderedQuery.whereIn("userId", followed)
          }

      filteredQuery
          .limit(limit)
          .get()
          .await()
          .documents
          .mapNotNull { it.toObject(Publication::class.java) }
          .distinctBy { it.id }
          .shuffled()
    } catch (e: Exception) {
      emptyList()
    }
  }

    open suspend fun loadCachePublications(limit: Long = 50): List<Publication> {
        return try {
            val publications =
                db.collection("publications")
                    .orderBy("id")
                    .limit(limit)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Publication::class.java) }
                    .shuffled()

            publications.forEach {
                Log.d("CACHE_DEBUG", "Fetched publication ID: ${it.id}, mediaUrl: ${it.mediaUrl}")
            }

            publications
        } catch (e: Exception) {
            Log.e("CACHE_ERROR", "Failed to load publications: ${e.message}")
            emptyList()
        }
    }

}
