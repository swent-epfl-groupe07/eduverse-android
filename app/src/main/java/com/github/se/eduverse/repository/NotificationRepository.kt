package com.github.se.eduverse.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.se.eduverse.MainActivity
import com.github.se.eduverse.model.NotifAuthorizations
import com.github.se.eduverse.model.NotificationType
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import com.github.se.eduverse.model.millisecInMin
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit

// Parameter authorizations is not used for now, it is here to make the class easier to upgrade
open class NotificationRepository(
    context: Context,
    val authorizations: NotifAuthorizations,
    val workManager: WorkManager = WorkManager.getInstance(context)
) {
  /**
   * Prepare a notification for an event or a task. Notification will appear a variable number of
   * minutes before the starting time
   *
   * @param scheduled the event/task scheduled
   * @param timeBefore the number of minute before the start of the task/event the notification
   *   should appear
   */
  open fun scheduleNotification(scheduled: Scheduled, timeBefore: Int = 1) {
    cancelNotification(scheduled)

    val delay =
        scheduled.start.timeInMillis - System.currentTimeMillis() - timeBefore * millisecInMin

    if (delay > 0) {
      val workRequest =
          OneTimeWorkRequestBuilder<NotificationWorker>()
              .setInitialDelay(delay, TimeUnit.MILLISECONDS)
              .addTag(scheduled.id)
              .setInputData(
                  workDataOf(
                      "title" to createTitle(scheduled),
                      "description" to createContent(scheduled),
                      "type" to NotificationType.valueOf(scheduled.type.name),
                      "objectId" to scheduled.id,
                      "channelId" to "task_channel"))
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
      return "It's time to start working on task: ${scheduled.name}"
    } else {
      return "Event ${scheduled.name} is about to begin !"
    }
  }

  /**
   * Create a description depending on the type of the scheduled
   *
   * @param scheduled the scheduled
   */
  open fun createContent(scheduled: Scheduled): String {
    val start = scheduled.start
    val end = (start.clone() as Calendar).apply { timeInMillis += scheduled.length }
    val formatter = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
    if (scheduled.type == ScheduledType.TASK) {
      return "Task ${scheduled.name} scheduled from ${formatter.format(start.time)}" +
          " to ${formatter.format(end.time)}"
    } else {
      return "Event ${scheduled.name} scheduled from ${formatter.format(start.time)}" +
          " to ${formatter.format(end.time)}"
    }
  }
}

class NotificationWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

  private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

  /** Called at the time the work was planned */
  override fun doWork(): Result {
    val title = inputData.getString("title") ?: "Reminder"
    val description = inputData.getString("description") ?: "No details"
    val type = inputData.getString("type") ?: NotificationType.DEFAULT.name
    val objectId = inputData.getString("objectId")
    val channelId = inputData.getString("channelId") ?: "default_channel"

      val json = sharedPreferences.getString("notifAuthKey", null)
      val notifAuthorizations =
          if (json != null) {
              Json.decodeFromString<NotifAuthorizations>(json)
          } else {
              NotifAuthorizations(true, true) // Default value
          }

    if (type == NotificationType.TASK.name && notifAuthorizations.taskEnabled ||
        type == NotificationType.EVENT.name && notifAuthorizations.eventEnabled) {
        showNotification(title, description, type, objectId, channelId)
    }

    return Result.success()
  }

  /**
   * Create a notification
   *
   * @param title the title of the notification
   * @param text the text of the notification
   * @param channelId the channel in which the notification will be showed
   * @param notificationManager dependency injection for testing purpose
   */
  fun showNotification(
      title: String,
      text: String,
      notificationType: String,
      objectId: String?,
      channelId: String,
      notificationManager: NotificationManager =
          applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  ) {
    // Create Notification Channel (Android 8.0+)
    val channel =
        when (channelId) {
          "task_channel" ->
              NotificationChannel(
                      channelId, "Task Notifications", NotificationManager.IMPORTANCE_HIGH)
                  .apply { description = "Notifications for scheduled tasks" }
          else ->
              NotificationChannel(
                      channelId, "Eduverse Notifications", NotificationManager.IMPORTANCE_HIGH)
                  .apply { description = "Notifications from unknown source" }
        }

    notificationManager.createNotificationChannel(channel)

    // Intent to open TaskDetailsActivity
    val intent =
        Intent(applicationContext, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          putExtra("isNotification", true)
          putExtra("notificationType", notificationType)
          putExtra("objectId", objectId)
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
