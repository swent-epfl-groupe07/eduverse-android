package com.github.se.eduverse.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.se.eduverse.model.NotifAutorizations
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import java.util.concurrent.TimeUnit

// Parameter authorizations is not used for now, it is here to make the class easier to upgrade
open class NotificationRepository(
    context: Context,
    val autorizations: NotifAutorizations,
    val workManager: WorkManager = WorkManager.getInstance(context)
) {
  open fun scheduleNotification(scheduled: Scheduled) {
    cancelNotification(scheduled)

    val delay = scheduled.start.timeInMillis - System.currentTimeMillis()

    if (delay > 0) {
      val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
              .setInitialDelay(delay, TimeUnit.MILLISECONDS)
              .addTag(scheduled.id)
              .setInputData(
                  workDataOf("title" to createTitle(scheduled), "description" to scheduled.name))
              .build()

      workManager.enqueue(workRequest)
    } else {
      Log.w("NotificationScheduler", "Scheduled time is in the past for task: ${scheduled.name}")
    }
  }

  open fun cancelNotification(scheduled: Scheduled) {
      workManager.cancelAllWorkByTag(scheduled.id)
  }

  fun createTitle(scheduled: Scheduled): String {
    if (scheduled.type == ScheduledType.TASK) {
      return "You should start working on a task."
    } else {
      return "An event is about to begin !"
    }
  }
}

class NotificationWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

  override fun doWork(): Result {
    val title = inputData.getString("title") ?: "Reminder"
    val description = inputData.getString("description") ?: "No details"

    showNotification(title, description)
    return Result.success()
  }

  fun showNotification(
      title: String,
      text: String,
      notificationManager: NotificationManager =
          applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  ) {
    val channelId = "task_channel"

    // Create Notification Channel (Android 8.0+)
    val channel =
        NotificationChannel(channelId, "Task Notifications", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Notifications for scheduled tasks" }
    notificationManager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
  }
}
