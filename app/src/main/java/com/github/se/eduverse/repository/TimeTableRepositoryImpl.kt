package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

open class TimeTableRepositoryImpl(val db: FirebaseFirestore) : TimeTableRepository {
  private val collection = db.collection("scheduled")

  private val typeString = "type"
  private val startTimeString = "startTime"
  private val endTimeString = "endTime"
  private val contentString = "content"
  private val ownerIdString = "ownerId"
  private val nameString = "name"

  override fun getNewUid(): String {
    return collection.document().id
  }

  override fun getScheduled(
      firstDay: Calendar,
      ownerId: String,
      onSuccess: (List<Scheduled>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val lastDay =
        Calendar.getInstance().apply {
          timeInMillis = firstDay.timeInMillis
          add(Calendar.WEEK_OF_YEAR, 1)
        }
    collection
        .whereEqualTo(ownerIdString, ownerId)
        .whereGreaterThanOrEqualTo(startTimeString, firstDay.timeInMillis)
        .whereLessThan(endTimeString, lastDay.timeInMillis)
        .get()
        .addOnSuccessListener {
          onSuccess(it.documents.map { document -> convertScheduled(document) })
        }
        .addOnFailureListener(onFailure)
  }

  override fun addScheduled(
      scheduled: Scheduled,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val mappedScheduled =
        hashMapOf(
            typeString to scheduled.type,
            startTimeString to scheduled.start.timeInMillis,
            endTimeString to scheduled.start.timeInMillis + scheduled.length,
            contentString to scheduled.content,
            ownerIdString to scheduled.ownerId,
            nameString to scheduled.name)

    collection
        .document(scheduled.id)
        .set(mappedScheduled)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  override fun updateScheduled(
      scheduled: Scheduled,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val mappedScheduled =
        mapOf(
            typeString to scheduled.type,
            startTimeString to scheduled.start.timeInMillis,
            endTimeString to scheduled.start.timeInMillis + scheduled.length,
            contentString to scheduled.content,
            ownerIdString to scheduled.ownerId,
            nameString to scheduled.name)

    collection
        .document(scheduled.id)
        .update(mappedScheduled)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  override fun deleteScheduled(
      scheduled: Scheduled,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    collection
        .document(scheduled.id)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  private fun convertScheduled(document: DocumentSnapshot): Scheduled {
    return Scheduled(
        id = document.id,
        type =
            if (document.getString(typeString)!! == "TASK") ScheduledType.TASK
            else ScheduledType.EVENT,
        start = Calendar.getInstance().apply { timeInMillis = document.getLong(startTimeString)!! },
        length = document.getLong(endTimeString)!! - document.getLong(startTimeString)!!,
        content = document.getString(contentString)!!,
        ownerId = document.getString(ownerIdString)!!,
        name = document.getString(nameString)!!)
  }
}
