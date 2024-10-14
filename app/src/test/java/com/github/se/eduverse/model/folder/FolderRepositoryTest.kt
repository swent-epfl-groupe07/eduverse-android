package com.github.se.eduverse.model.folder

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.Calendar
import junit.framework.TestCase.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
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
  @Mock private lateinit var mockFileDocumentReference: DocumentReference
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var mockFileCollectionReference: CollectionReference
  @Mock private lateinit var mockFolderQuerySnapshot: QuerySnapshot
  @Mock private lateinit var mockFileQuerySnapshot: QuerySnapshot

  private lateinit var folderRepositoryImpl: FolderRepositoryImpl

  private val file = MyFile("", "", "name 1", Calendar.getInstance(), Calendar.getInstance(), 0)

  private val folder = Folder("uid", MutableList(1) { file }, "folder", "1")

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    // Initialize Firebase if necessary
    if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
      FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }

    folderRepositoryImpl = FolderRepositoryImpl(mockFirestore)

    `when`(mockFirestore.collection(any())).thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.document(any())).thenReturn(mockDocumentReference)
    `when`(mockCollectionReference.document()).thenReturn(mockDocumentReference)
    `when`(mockDocumentReference.collection(any())).thenReturn(mockFileCollectionReference)
    `when`(mockFileCollectionReference.document(any())).thenReturn(mockFileDocumentReference)
    `when`(mockFileCollectionReference.document()).thenReturn(mockFileDocumentReference)
  }

  @Test
  fun getNewFolderUidTest() {
    `when`(mockDocumentReference.id).thenReturn("1")
    val uid = folderRepositoryImpl.getNewFolderUid()
    assert(uid == "1")
  }

  @Test
  fun getNewFileUidTest() {
    `when`(mockFileDocumentReference.id).thenReturn("1")
    val uid = folderRepositoryImpl.getNewFileUid(folder)
    assert(uid == "1")
  }

  @Test
  fun deleteFolder_shouldCallDocumentReferenceDelete() {
    `when`(mockDocumentReference.delete()).thenReturn(Tasks.forResult(null))

    folderRepositoryImpl.deleteFolder(folder, onSuccess = {}, onFailure = {})

    shadowOf(Looper.getMainLooper()).idle() // Ensure all asynchronous operations complete

    verify(mockDocumentReference).delete()
  }

  @Test
  fun addFolder_shouldCallFirestoreCollection() {
    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null)) // Simulate success
    `when`(mockFileDocumentReference.set(any())).thenReturn(Tasks.forResult(null))

    folderRepositoryImpl.addFolder(folder, onSuccess = {}, onFailure = {})

    shadowOf(Looper.getMainLooper()).idle()

    verify(mockDocumentReference).set(any())
    verify(mockFileDocumentReference).set(any())
  }

  @Test
  fun updateFolder_shouldCallFirestoreCollection() {
    `when`(mockDocumentReference.update(any()))
        .thenReturn(Tasks.forResult(null)) // Simulate success
    `when`(mockFileDocumentReference.update(any())).thenReturn(Tasks.forResult(null))

    folderRepositoryImpl.updateFolder(folder, onSuccess = {}, onFailure = {})

    shadowOf(Looper.getMainLooper()).idle()

    verify(mockDocumentReference).update(any())
    verify(mockFileDocumentReference).update(any())
  }

  @Test
  fun getToDos_callsDocuments() {
    `when`(mockCollectionReference.whereEqualTo(anyString(), any()))
        .thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.get()).thenReturn(Tasks.forResult(mockFolderQuerySnapshot))
    `when`(mockFileCollectionReference.get()).thenReturn(Tasks.forResult(mockFileQuerySnapshot))

    `when`(mockFolderQuerySnapshot.documents).thenReturn(listOf())
    `when`(mockFileQuerySnapshot.documents).thenReturn(listOf())

    folderRepositoryImpl.getFolders(
        anyString(), onSuccess = {}, onFailure = { fail("Failure callback should not be called") })

    verify(timeout(100)) { (mockFolderQuerySnapshot).documents }
    verify(timeout(100)) { (mockFileQuerySnapshot).documents }
  }
}
