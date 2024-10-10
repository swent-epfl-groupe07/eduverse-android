package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Widget
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch

interface DashboardRepository {
  fun getWidgets(userId: String): Flow<List<Widget>>

  suspend fun addWidget(userId: String, widget: Widget)

  suspend fun removeWidget(userId: String, widgetId: String)
}

class DashboardRepositoryImpl(private val firestore: FirebaseFirestore) : DashboardRepository {

  override fun getWidgets(userId: String): Flow<List<Widget>> =
      callbackFlow {
            val widgetRef = firestore.collection("users").document(userId).collection("widgets")
            val listener =
                widgetRef.addSnapshotListener { snapshot, e ->
                  if (e != null || snapshot == null) {
                    trySend(emptyList()) // Send an empty list in case of error
                    close(e)
                  } else {
                    val widgets = snapshot.documents.mapNotNull { it.toObject(Widget::class.java) }
                    trySend(widgets).isSuccess
                  }
                }
            awaitClose { listener.remove() }
          }
          .catch { emit(emptyList()) } // Catch any exceptions and emit an empty list

  override suspend fun addWidget(userId: String, widget: Widget) {
    val widgetRef = firestore.collection("users").document(userId).collection("widgets")
    widgetRef.add(widget)
  }

  override suspend fun removeWidget(userId: String, widgetId: String) {
    val widgetRef =
        firestore.collection("users").document(userId).collection("widgets").document(widgetId)
    widgetRef.delete()
  }
}
