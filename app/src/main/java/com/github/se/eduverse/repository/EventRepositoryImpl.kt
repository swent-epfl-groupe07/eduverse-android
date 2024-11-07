package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Event
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class EventRepositoryImpl(val db: FirebaseFirestore) : EventRepository {
  val collectionPath = "events"

  override fun getEvent(id: String, onSuccess: (Event) -> Unit, onFailure: (Exception) -> Unit) {
    db.collection(collectionPath)
        .document(id)
        .get()
        .addOnSuccessListener { onSuccess(convertEvent(it)) }
        .addOnFailureListener(onFailure)
  }

  override fun addEvent(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    val mappedEvent = hashMapOf("name" to event.name, "description" to event.description)

    db.collection(collectionPath)
        .document(event.id)
        .set(mappedEvent)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  override fun updateEvent(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    val mappedEvent = mapOf("name" to event.name, "description" to event.description)

    db.collection(collectionPath)
        .document(event.id)
        .update(mappedEvent)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  override fun deleteEvent(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    db.collection(collectionPath)
        .document(event.id)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  private fun convertEvent(document: DocumentSnapshot): Event {
    return Event(
        id = document.id,
        name = document.getString("name")!!,
        description = document.getString("description")!!)
  }
}
