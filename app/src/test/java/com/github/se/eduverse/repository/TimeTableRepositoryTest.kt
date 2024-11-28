package com.github.se.eduverse.repository

import android.os.Looper
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.Calendar
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TimeTableRepositoryTest {

  @Mock private lateinit var mockFirestore: FirebaseFirestore
  @Mock private lateinit var mockDocumentReference: DocumentReference
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var documentSnapshot: DocumentSnapshot
  @Mock private lateinit var querySnapshot: QuerySnapshot
  @Mock private lateinit var void: Void

  private lateinit var timeTableRepository: TimeTableRepositoryImpl

  private val scheduled =
      Scheduled(
          "id",
          ScheduledType.TASK,
          Calendar.getInstance().apply { timeInMillis = 12 },
          7,
          "taskId",
          "ownerId",
          "name")

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    `when`(mockFirestore.collection(any())).thenReturn(mockCollectionReference)

    timeTableRepository = TimeTableRepositoryImpl(mockFirestore)

    `when`(mockCollectionReference.document()).thenReturn(mockDocumentReference)
    `when`(mockCollectionReference.document(any())).thenReturn(mockDocumentReference)
    `when`(mockCollectionReference.whereEqualTo(anyString(), any()))
        .thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.whereGreaterThanOrEqualTo(anyString(), any()))
        .thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.whereLessThan(anyString(), any()))
        .thenReturn(mockCollectionReference)

    `when`(documentSnapshot.id).thenReturn("id")
    `when`(documentSnapshot.getString(eq("type"))).thenReturn("TASK")
    `when`(documentSnapshot.getLong(eq("startTime"))).thenReturn(12)
    `when`(documentSnapshot.getLong(eq("endTime"))).thenReturn(19)
    `when`(documentSnapshot.getString(eq("content"))).thenReturn("taskId")
    `when`(documentSnapshot.getString(eq("ownerId"))).thenReturn("ownerId")
    `when`(documentSnapshot.getString(eq("name"))).thenReturn("name")
  }

  @Test
  fun getNewUidTest() {
    `when`(mockDocumentReference.id).thenReturn("uid")
    val uid = timeTableRepository.getNewUid()
    assert(uid == "uid")
  }

  @Test
  fun getScheduledTest_success() {
    `when`(mockCollectionReference.get()).thenReturn(Tasks.forResult(querySnapshot))
    `when`(querySnapshot.documents).thenReturn(listOf(documentSnapshot))

    val correct =
        Scheduled(
            "id",
            ScheduledType.TASK,
            Calendar.getInstance().apply { timeInMillis = 12 },
            7,
            "taskId",
            "ownerId",
            "name")
    var result: Scheduled? = null

    timeTableRepository.getScheduled(
        Calendar.getInstance(),
        "ownerId",
        {
          assertEquals(1, it.size)
          result = it[0]
        },
        { assert(false) })
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(correct, result)
  }

  @Test
  fun getScheduledTest_failure() {
    `when`(mockCollectionReference.get()).thenReturn(Tasks.forException(Exception()))

    var test = false
    timeTableRepository.getScheduled(
        Calendar.getInstance(), "ownerId", { assert(false) }, { test = true })
    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun getScheduledByIdTest() = runBlocking {
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(documentSnapshot))
    val result = timeTableRepository.getScheduledById("")

    assertEquals("id", result.id)
    assertEquals("name", result.name)
  }

  @Test
  fun addScheduledTest_success() {
    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(void))

    var test = false
    timeTableRepository.addScheduled(scheduled, { test = true }, { assert(false) })
    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun addScheduledTest_failure() {
    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forException(Exception()))

    var test = false
    timeTableRepository.addScheduled(scheduled, { assert(false) }, { test = true })
    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun updateScheduledTest_success() {
    `when`(mockDocumentReference.update(any())).thenReturn(Tasks.forResult(void))

    var test = false
    timeTableRepository.updateScheduled(scheduled, { test = true }, { assert(false) })
    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun updateScheduledTest_failure() {
    `when`(mockDocumentReference.update(any())).thenReturn(Tasks.forException(Exception()))

    var test = false
    timeTableRepository.updateScheduled(scheduled, { assert(false) }, { test = true })
    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun deleteScheduledTest_success() {
    `when`(mockDocumentReference.delete()).thenReturn(Tasks.forResult(void))

    var test = false
    timeTableRepository.deleteScheduled(scheduled, { test = true }, { assert(false) })
    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun deleteScheduledTest_failure() {
    `when`(mockDocumentReference.delete()).thenReturn(Tasks.forException(Exception()))

    var test = false
    timeTableRepository.deleteScheduled(scheduled, { assert(false) }, { test = true })
    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }
}
