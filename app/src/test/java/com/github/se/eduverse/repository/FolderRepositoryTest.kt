package com.github.se.eduverse.repository

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.model.FilterTypes
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.google.android.gms.common.api.Batch
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import java.util.Calendar
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FolderRepositoryTest {

  @Mock private lateinit var mockFirestore: FirebaseFirestore
  @Mock private lateinit var mockDocumentReference: DocumentReference
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var mockFolderQuerySnapshot: QuerySnapshot
  @Mock private lateinit var mockFolderDocumentSnapshot: DocumentSnapshot
  @Mock private lateinit var mockBatch: WriteBatch

  private lateinit var folderRepositoryImpl: FolderRepositoryImpl

  private val file = MyFile("", "", "name 1", Calendar.getInstance(), Calendar.getInstance(), 0)

  private val folder = Folder("uid", MutableList(1) { file }, "folder", "1", archived = false)

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    // Initialize Firebase if necessary
    if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
      FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }

    folderRepositoryImpl = FolderRepositoryImpl(mockFirestore)

    `when`(mockFirestore.collection(any())).thenReturn(mockCollectionReference)
    `when`(mockFirestore.batch()).thenReturn(mockBatch)
    `when`(mockCollectionReference.document(any())).thenReturn(mockDocumentReference)
    `when`(mockCollectionReference.document()).thenReturn(mockDocumentReference)
    `when`(mockCollectionReference.whereIn(any<FieldPath>(), any())).thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.get()).thenReturn(Tasks.forResult(mockFolderQuerySnapshot))
    `when`(mockCollectionReference.whereEqualTo(anyString(), any()))
        .thenReturn(mockCollectionReference)
    `when`(mockFolderQuerySnapshot.documents).thenReturn(listOf())
  }

  @Test
  fun getNewUidTest() {
    `when`(mockDocumentReference.id).thenReturn("1")
    val uid = folderRepositoryImpl.getNewUid()
    assert(uid == "1")
  }

  @Test
  fun deleteFolder_shouldCallDocumentReferenceDelete() = runBlocking {
    `when`(mockBatch.commit()).thenReturn(Tasks.forResult(null))
      `when`(mockFolderQuerySnapshot.documents).thenReturn(listOf(mockFolderDocumentSnapshot))
      `when`(mockFolderDocumentSnapshot.id).thenReturn(folder.id)

    folderRepositoryImpl.deleteFolders(listOf(folder), onSuccess = {}, onFailure = {})

    shadowOf(Looper.getMainLooper()).idle() // Ensure all asynchronous operations complete

    verify(mockBatch).delete(any())
    Unit // Tests must return a Unit
  }

  @Test
  fun addFolder_shouldCallFirestoreCollection() {
    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null)) // Simulate success

    folderRepositoryImpl.addFolder(folder, onSuccess = {}, onFailure = {})
    shadowOf(Looper.getMainLooper()).idle()

    verify(mockDocumentReference).set(any())
  }

  @Test
  fun updateFolder_shouldCallFirestoreCollection() {
    `when`(mockDocumentReference.update(any()))
        .thenReturn(Tasks.forResult(null)) // Simulate success

    folderRepositoryImpl.updateFolder(folder, onSuccess = {}, onFailure = {})

    shadowOf(Looper.getMainLooper()).idle()

    verify(mockDocumentReference).update(any())
  }

  @Test
  fun getToDos_callsDocuments() {
    folderRepositoryImpl.getFolders(
        anyString(), onSuccess = {}, onFailure = { fail("Failure callback should not be called") })

    verify(timeout(100)) { (mockFolderQuerySnapshot).documents }
  }

  @Test
  fun convertFolderTest() {
    val m =
        mapOf(
            "NAME" to FilterTypes.NAME,
            "CREATION_UP" to FilterTypes.CREATION_UP,
            "CREATION_DOWN" to FilterTypes.CREATION_DOWN,
            "ACCESS_RECENT" to FilterTypes.ACCESS_RECENT,
            "ACCESS_OLD" to FilterTypes.ACCESS_OLD,
            "ACCESS_MOST" to FilterTypes.ACCESS_MOST,
            "ACCESS_LEAST" to FilterTypes.ACCESS_LEAST)

    val time0 = Calendar.getInstance()
    time0.timeInMillis = 0

    `when`(mockFolderDocumentSnapshot.id).thenReturn("id")
    `when`(mockFolderDocumentSnapshot.getString(any())).thenReturn("field")
    `when`(mockFolderDocumentSnapshot.get(anyString()))
        .thenReturn(
            listOf(
                mapOf(
                    "name" to file.name,
                    "fileId" to file.fileId,
                    "creationTime" to file.creationTime.timeInMillis.toString(),
                    "lastAccess" to file.lastAccess.timeInMillis.toString(),
                    "numberAccess" to file.numberAccess.toString())))

    m.forEach {
      `when`(mockFolderDocumentSnapshot.getString("filterType")).thenReturn(it.key)

      val folder = folderRepositoryImpl.convertFolder(mockFolderDocumentSnapshot)

      assertEquals(
          folder, Folder("field", mutableListOf(file), "field", "id", it.value, archived = false))
    }
  }
}
