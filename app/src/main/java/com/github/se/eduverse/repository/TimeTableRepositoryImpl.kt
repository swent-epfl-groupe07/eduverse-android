package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

open class TimeTableRepositoryImpl(val db: FirebaseFirestore) : TimeTableRepository {
  val collectionPath = "scheduled"

  override fun getNewUid(): String {
    return db.collection(collectionPath).document().id
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
    db.collection(collectionPath)
        .whereEqualTo("ownerId", ownerId)
        .whereGreaterThanOrEqualTo("startTime", firstDay.timeInMillis)
        .whereLessThan("endTime", lastDay.timeInMillis)
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
            "type" to scheduled.type,
            "startTime" to scheduled.start.timeInMillis,
            "endTime" to scheduled.start.timeInMillis + scheduled.length,
            "taskOrEventId" to scheduled.taskOrEventId,
            "ownerId" to scheduled.ownerId,
            "name" to scheduled.name)

    db.collection(collectionPath)
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
            "type" to scheduled.type,
            "startTime" to scheduled.start.timeInMillis,
            "endTime" to scheduled.start.timeInMillis + scheduled.length,
            "taskOrEventId" to scheduled.taskOrEventId,
            "ownerId" to scheduled.ownerId,
            "name" to scheduled.name)

    db.collection(collectionPath)
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
    db.collection(collectionPath)
        .document(scheduled.id)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  private fun convertScheduled(document: DocumentSnapshot): Scheduled {
    return Scheduled(
        id = document.id,
        type = if (document.get("type")!! == "TASK") ScheduledType.TASK else ScheduledType.EVENT,
        start = Calendar.getInstance().apply { timeInMillis = document.getLong("startTime")!! },
        length = document.getLong("endTime")!! - document.getLong("startTime")!!,
        taskOrEventId = document.getString("taskOrEventId")!!,
        ownerId = document.getString("ownerId")!!,
        name = document.getString("name")!!)
  }
}
