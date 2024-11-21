package com.github.se.eduverse.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.se.eduverse.MainActivity
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
  /**
   * Prepare a notification for an event or a task
   *
   * @param scheduled the event/task scheduled
   */
  open fun scheduleNotification(scheduled: Scheduled) {
    cancelNotification(scheduled)

    val delay = scheduled.start.timeInMillis - System.currentTimeMillis()

    if (delay > 0) {
      val workRequest =
          OneTimeWorkRequestBuilder<NotificationWorker>()
              .setInitialDelay(delay, TimeUnit.MILLISECONDS)
              .addTag(scheduled.id)
              .setInputData(
                  workDataOf(
                      "title" to createTitle(scheduled),
                      "description" to scheduled.name,
                      "scheduledId" to scheduled.id))
              .build()

      workManager.enqueue(workRequest)
    } else {
      Log.w("NotificationScheduler", "Scheduled time is in the past for task: ${scheduled.name}")
    }
  }

  /**
   * Cancel the eventual planned notification for a scheduled
   *
   * @param scheduled the scheduled to cancel
   */
  open fun cancelNotification(scheduled: Scheduled) {
    workManager.cancelAllWorkByTag(scheduled.id)
  }

  /**
   * Create a title depending on the type of the scheduled
   *
   * @param scheduled the scheduled
   */
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

  /** Called at the time the work was planned */
  override fun doWork(): Result {
    val title = inputData.getString("title") ?: "Reminder"
    val description = inputData.getString("description") ?: "No details"
    val scheduledId = inputData.getString("scheduledId")

    showNotification(title, description, scheduledId)
    return Result.success()
  }

  /**
   * Create a notification
   *
   * @param title the title of the notification
   * @param text the text of the notification
   * @param notificationManager dependency injection for testing purpose
   */
  fun showNotification(
      title: String,
      text: String,
      scheduledId: String?,
      notificationManager: NotificationManager =
          applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  ) {
    val channelId = "task_channel"

    // Create Notification Channel (Android 8.0+)
    val channel =
        NotificationChannel(channelId, "Task Notifications", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Notifications for scheduled tasks" }
    notificationManager.createNotificationChannel(channel)

    // Intent to open TaskDetailsActivity
    val intent =
        Intent(applicationContext, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          putExtra("isNotification", true)
          putExtra("scheduledId", scheduledId)
        }

    // Create the PendingIntent
    val pendingIntent =
        PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val notification =
        NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
  }
}
