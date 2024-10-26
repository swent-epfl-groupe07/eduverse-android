package com.github.se.eduverse.repository

import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.lang.Exception
import java.util.Calendar
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FileRepositoryTest {

  @Mock private lateinit var mockFirestore: FirebaseFirestore
  @Mock private lateinit var mockStorage: FirebaseStorage
  @Mock private lateinit var mockDocumentReference: DocumentReference
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var mockStorageReference: StorageReference
  @Mock private lateinit var mockUploadTask: UploadTask

  private lateinit var fileRepository: FileRepositoryImpl

  private val file = MyFile("", "", "name 1", Calendar.getInstance(), Calendar.getInstance(), 0)

  private val folder = Folder("uid", MutableList(1) { file }, "folder", "1")

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    // Initialize Firebase if necessary
    if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
      FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }

    fileRepository = FileRepositoryImpl(mockFirestore, mockStorage)

    `when`(mockFirestore.collection(any())).thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.document(any())).thenReturn(mockDocumentReference)
    `when`(mockCollectionReference.document()).thenReturn(mockDocumentReference)
  }

  @Test
  fun getNewUidTest() {
    `when`(mockDocumentReference.id).thenReturn("1")
    val uid = fileRepository.getNewUid()
    assert(uid == "1")
  }

  @Test
  fun saveFileTest() {
    var test1 = false
    var test2 = false
    `when`(mockStorage.reference).thenReturn(mockStorageReference)
    `when`(mockStorageReference.child(any())).thenReturn(mockStorageReference)
    `when`(mockStorageReference.putFile(any())).thenReturn(mockUploadTask)
    `when`(mockUploadTask.addOnSuccessListener(any())).then {
      test1 = true
      mockUploadTask
    }
    `when`(mockUploadTask.addOnFailureListener(any())).then {
      test2 = true
      null
    }

    fileRepository.saveFile(Uri.EMPTY, "", {}, {})

    assert(test1)
    assert(test2)
  }

  @Test
  fun modifyFileTest() {
    var test = false
    try {
      fileRepository.modifiyFile(Uri.EMPTY, "", {}, {})
    } catch (e: NotImplementedError) {
      test = true
    }
    assert(test)
  }

  @Test
  fun deleteFileTest() {
    var test = false
    try {
      fileRepository.deleteFile(Uri.EMPTY, "", {}, {})
    } catch (e: NotImplementedError) {
      test = true
    }
    assert(test)
  }

  @Test
  fun accessFileTest_onSuccess() {
    val documentSnapshot = mock(DocumentSnapshot::class.java)

    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(documentSnapshot))

    fileRepository.accessFile("", {}, {})

    verify(timeout(100)) { (documentSnapshot).getString("url") }
  }

  @Test
  fun accessFileTest_onFailure() {
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forException(Exception("message")))
    var test = false

    fileRepository.accessFile(
        "",
        {},
        {
          test = true
          assert(it.message == "message")
        })

    shadowOf(Looper.getMainLooper()).idle()

    assert(test)
  }

  @Test
  fun savePDFUrlToFirestoreTest() {
    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null))

    fileRepository.savePDFUrlToFirestore("", "", {})

    shadowOf(Looper.getMainLooper()).idle() // Ensure all asynchronous operations complete

    verify(mockDocumentReference).set(any())
  }
}
