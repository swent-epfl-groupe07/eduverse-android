package com.github.se.eduverse.repository

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.testing.TestWorkerBuilder
import com.github.se.eduverse.model.NotifAutorizations
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

  private val notifAut = NotifAutorizations(true, true)

  private val scheduled =
      Scheduled("id", ScheduledType.TASK, Calendar.getInstance(), 7, "taskId", "ownerId", "name")

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
  fun scheduleNotificationEnqueuesWorkWhenDelayIsPositive() {
    // Arrange
    scheduled.start.add(Calendar.MINUTE, 1)

    // Act
    repository.scheduleNotification(scheduled)

    // Assert
    verify(mockWorkManager).enqueue(any<WorkRequest>())
  }

  @Test
  fun scheduleNotificationLogsWarningWhenDelayIsNegative() {
    // Arrange
    scheduled.start.add(Calendar.MINUTE, -1)

    // Act
    repository.scheduleNotification(scheduled)

    // Assert
    verify(mockWorkManager, never()).enqueue(any<WorkRequest>())
  }

  @Test
  fun cancelNotificationCancelsWorkByTag() {
    // Act
    repository.cancelNotification(scheduled)

    // Assert
    verify(mockWorkManager).cancelAllWorkByTag(any())
  }

  @Test
  fun createTitleReturnsCorrectTitleForTASKType() {
    // Act
    val title = repository.createTitle(scheduled)

    // Assert
    assertEquals("You should start working on a task.", title)
  }

  @Test
  fun createTitleReturnsCorrectTitleForEVENTType() {
    // Arrange
    val event =
        Scheduled(
            id = "test_id",
            type = ScheduledType.EVENT,
            start = Calendar.getInstance(),
            length = 0,
            content = "",
            ownerId = "",
            name = "Test Event")

    // Act
    val title = repository.createTitle(event)

    // Assert
    assertEquals("An event is about to begin !", title)
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

    // Call showNotification directly
    notificationWorker.showNotification(title, text, id, notificationManager)

    // Verify that the notification was created
    verify(notificationManager).createNotificationChannel(any())
    verify(notificationManager).notify(anyInt(), any())
  }
}
