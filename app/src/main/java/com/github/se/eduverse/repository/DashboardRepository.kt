package com.github.se.eduverse.repository

import android.util.Log
import com.github.se.eduverse.model.Widget
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

interface DashboardRepository {
  fun getWidgets(userId: String): Flow<List<Widget>>

  suspend fun addWidget(widget: Widget)

  suspend fun removeWidget(widgetId: String)

  suspend fun updateWidgets(widgets: List<Widget>)
}

class DashboardRepositoryImpl(private val firestore: FirebaseFirestore) : DashboardRepository {

  override fun getWidgets(userId: String): Flow<List<Widget>> =
      callbackFlow {
            val widgetRef = firestore.collection("widgets").whereEqualTo("ownerUid", userId)
            Log.d("DashboardRepository", "Fetching widgets for user: $userId")
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
          .catch { emit(emptyList()) }

  override suspend fun addWidget(widget: Widget) {
    val widgetRef = firestore.collection("widgets")
    widgetRef.document(widget.widgetId).set(widget)
  }

  override suspend fun removeWidget(widgetId: String) {
    val widgetRef = firestore.collection("widgets").document(widgetId)
    widgetRef.delete()
  }

  override suspend fun updateWidgets(widgets: List<Widget>) {
    val batch = firestore.batch()
    widgets.forEachIndexed { index, widget ->
      val updatedWidget = widget.copy(order = index) // Update the order
      val docRef = firestore.collection("widgets").document(widget.widgetId)
      batch.set(docRef, updatedWidget)
    }
    batch.commit().await()
  }
}
