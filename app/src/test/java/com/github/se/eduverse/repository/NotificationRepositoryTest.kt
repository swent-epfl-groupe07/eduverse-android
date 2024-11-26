package com.github.se.eduverse.repository

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.testing.TestWorkerBuilder
import com.github.se.eduverse.model.NotifAuthorizations
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationRepositoryTest {

  private lateinit var context: Context
  private lateinit var mockWorkManager: WorkManager
  private lateinit var repository: NotificationRepository
  private lateinit var notificationManager: NotificationManager
  private lateinit var notificationWorker: NotificationWorker

  private val notifAut = NotifAuthorizations(true, true)

  private val task =
      Scheduled("id", ScheduledType.TASK, Calendar.getInstance(), 7, "taskId", "ownerId", "name")
  private val event =
      Scheduled(
          id = "test_id",
          type = ScheduledType.EVENT,
          start = Calendar.getInstance(),
          length = 0,
          content = "",
          ownerId = "",
          name = "Test Event")

  @Before
  fun setup() {
    // Initialize the WorkManager for testing
    context = ApplicationProvider.getApplicationContext()
    mockWorkManager = mock(WorkManager::class.java)

    // Create the repository instance with mocks
    repository = NotificationRepository(context, notifAut, mockWorkManager)

    // Create the notification manager and worker
    notificationManager = mock(NotificationManager::class.java)

    val inputData =
        Data.Builder()
            .putString("title", "Test Title")
            .putString("description", "Test Description")
            .build()

    notificationWorker =
        TestWorkerBuilder.from(context, NotificationWorker::class.java)
            .setInputData(inputData)
            .build()
  }

  @Test
  fun notificationDoesNotEnqueuesWhenNotEnabled() {
    // Arrange
    notifAut.taskEnabled = false
    notifAut.eventEnabled = false

    // Act
    repository.scheduleNotification(task)
    repository.scheduleNotification(event)

    // Assert
    verifyNoInteractions(mockWorkManager)
  }

  @Test
  fun scheduleNotificationEnqueuesWorkWhenDelayIsPositive() {
    // Arrange
    task.start.add(Calendar.MINUTE, 2)

    // Act
    repository.scheduleNotification(task)

    // Assert
    verify(mockWorkManager).enqueue(any<WorkRequest>())
  }

  @Test
  fun scheduleNotificationLogsWarningWhenDelayIsNegative() {
    // Arrange
    task.start.add(Calendar.MINUTE, -1)

    // Act
    repository.scheduleNotification(task)

    // Assert
    verify(mockWorkManager, never()).enqueue(any<WorkRequest>())
  }

  @Test
  fun cancelNotificationCancelsWorkByTag() {
    // Act
    repository.cancelNotification(task)

    // Assert
    verify(mockWorkManager).cancelAllWorkByTag(eq(task.id))
  }

  @Test
  fun createTitleReturnsCorrectTitleForTASKType() {
    // Act
    val title = repository.createTitle(task)

    // Assert
    assertEquals("It's time to start working on task: name", title)
  }

  @Test
  fun createTitleReturnsCorrectTitleForEVENTType() {
    // Act
    val title = repository.createTitle(event)

    // Assert
    assertEquals("Event Test Event is about to begin !", title)
  }

  @Test
  fun createContentReturnsCorrectTitleForTASKType() {
    // Act
    val content = repository.createContent(task)

    // Assert
    assert(content.contains("Task name scheduled from "))
    assert(content.contains(" to"))
  }

  @Test
  fun createContentReturnsCorrectTitleForEVENTType() {
    // Act
    val content = repository.createContent(event)

    // Assert
    assert(content.contains("Event Test Event scheduled from "))
    assert(content.contains(" to"))
  }

  @Test
  fun doWorkReturnsSuccess() {
    val result = notificationWorker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun showNotificationCreatesNotification() {
    val title = "Test Title"
    val text = "Test Description"
    val id = "Scheduled Id"
    val channel = "task_channel"

    // Call showNotification directly
    notificationWorker.showNotification(title, text, id, channel, notificationManager)

    // Verify that the notification was created
    verify(notificationManager).createNotificationChannel(any())
    verify(notificationManager).notify(anyInt(), any())
  }
}
