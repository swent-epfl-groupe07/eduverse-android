package com.github.se.eduverse.repository

import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlin.Exception
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
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
  @Mock private lateinit var documentSnapshot: DocumentSnapshot
  @Mock private lateinit var void: Void

  private lateinit var fileRepository: FileRepositoryImpl

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
    `when`(documentSnapshot.getString("url")).thenReturn("url/to/file")

    `when`(mockStorage.reference).thenReturn(mockStorageReference)
    `when`(mockStorageReference.child(any())).thenReturn(mockStorageReference)
  }

  @Test
  fun getNewUidTest() {
    `when`(mockDocumentReference.id).thenReturn("1")
    val uid = fileRepository.getNewUid()
    assert(uid == "1")
  }

  @Test
  fun savePdfFileTest() {
    var test1 = false
    var test2 = false
    `when`(mockStorageReference.putFile(any())).thenReturn(mockUploadTask)
    `when`(mockUploadTask.addOnSuccessListener(any())).then {
      test1 = true
      mockUploadTask
    }
    `when`(mockUploadTask.addOnFailureListener(any())).then {
      test2 = true
      null
    }

    fileRepository.savePdfFile(Uri.EMPTY, "", {}, {})

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
  fun deleteFileTest_success() {
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(documentSnapshot))
    `when`(mockStorageReference.delete()).thenReturn(Tasks.forResult(void))
    `when`(mockDocumentReference.delete()).thenReturn(Tasks.forResult(void))

    var test = false

    fileRepository.deleteFile("fileId", { test = true }, { assert(false) })

    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun deleteFileTest_failureGet() {
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forException(Exception("")))

    var test = false

    fileRepository.deleteFile("fileId", { assert(false) }, { test = true })

    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun deleteFileTest_failureDeleteStorage() {
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(documentSnapshot))
    `when`(mockStorageReference.delete()).thenReturn(Tasks.forException(Exception("")))

    var test = false

    fileRepository.deleteFile("fileId", { assert(false) }, { test = true })

    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun deleteFileTest_failureDeleteFirestore() {
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(documentSnapshot))
    `when`(mockStorageReference.delete()).thenReturn(Tasks.forResult(void))
    `when`(mockDocumentReference.delete()).thenReturn(Tasks.forException(Exception("")))

    var test = false

    fileRepository.deleteFile("fileId", { assert(false) }, { test = true })

    shadowOf(Looper.getMainLooper()).idle()
    assert(test)
  }

  @Test
  fun accessFileTest_onSuccess() {

    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(documentSnapshot))

    fileRepository.accessFile("", { _, _ -> }, {})

    verify(timeout(100)) { (documentSnapshot).getString("url") }
  }

  @Test
  fun accessFileTest_onFailure() {
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forException(Exception("message")))
    var test = false

    fileRepository.accessFile(
        "",
        { _, _ -> },
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

    fileRepository.saveUrlToFirestore("", ".pdf", "") {}

    shadowOf(Looper.getMainLooper()).idle() // Ensure all asynchronous operations complete

    verify(mockDocumentReference).set(any())
  }
}
