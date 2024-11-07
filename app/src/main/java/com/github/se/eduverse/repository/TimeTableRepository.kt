package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Scheduled
import java.util.Calendar

interface TimeTableRepository {
  fun getNewUid(): String

  fun getScheduled(
      firstDay: Calendar,
      ownerId: String,
      onSuccess: (List<Scheduled>) -> Unit,
      onFailure: (Exception) -> Unit
  )

  fun addScheduled(scheduled: Scheduled, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun updateScheduled(scheduled: Scheduled, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun deleteScheduled(scheduled: Scheduled, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
}
