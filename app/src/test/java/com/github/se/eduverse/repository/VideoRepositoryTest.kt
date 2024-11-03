import android.net.Uri
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.repository.VideoRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class VideoRepositoryTest {

  private lateinit var firestoreMock: FirebaseFirestore
  private lateinit var storageMock: FirebaseStorage
  private lateinit var videoRepository: VideoRepository
  private lateinit var storageReferenceMock: StorageReference

  @Before
  fun setUp() {
    firestoreMock = mock(FirebaseFirestore::class.java)
    storageMock = mock(FirebaseStorage::class.java)
    storageReferenceMock = mock(StorageReference::class.java)
    videoRepository = VideoRepository(firestoreMock, storageMock)
  }

  @Test
  fun `saveVideo should return true on successful save`() = runBlocking {
    // Arrange
    val video = Video("ownerId123", byteArrayOf(0x01, 0x02), "videos/path.mp4")

    // Simuler le comportement de succès pour Firebase Storage
    val uploadTaskMock = mock(UploadTask::class.java)
    val uriMock = mock(Uri::class.java)

    `when`(storageMock.reference).thenReturn(storageReferenceMock)
    `when`(storageReferenceMock.child(video.path)).thenReturn(storageReferenceMock)
    `when`(storageReferenceMock.putBytes(video.video)).thenReturn(uploadTaskMock)
    `when`(uploadTaskMock.isComplete).thenReturn(true)
    `when`(uploadTaskMock.isSuccessful).thenReturn(true)
    `when`(storageReferenceMock.downloadUrl).thenReturn(Tasks.forResult(uriMock))
    `when`(uriMock.toString()).thenReturn("http://example.com/videos/path.mp4")

    // Simuler un CollectionReference et un DocumentReference pour Firestore
    val collectionReferenceMock = mock(CollectionReference::class.java)
    val documentTaskMock = Tasks.forResult(mock(DocumentReference::class.java))

    `when`(firestoreMock.collection("videos")).thenReturn(collectionReferenceMock)
    `when`(collectionReferenceMock.add(any())).thenReturn(documentTaskMock)

    // Act
    val result = videoRepository.saveVideo(video)

    // Assert
    assertTrue(result)
  }

  @Test(timeout = 3000)
  fun `saveVideo returns false on failure`() =
      runBlocking(Dispatchers.IO) {
        // Arrange
        val video = Video("ownerId", ByteArray(0), "path")

        // Simuler l'échec de l'upload avec une exception
        `when`(storageMock.reference).thenReturn(storageReferenceMock)
        `when`(storageReferenceMock.child(video.path)).thenReturn(storageReferenceMock)
        `when`(storageReferenceMock.putBytes(video.video))
            .thenThrow(RuntimeException("Upload failed"))

        // Act
        val result = videoRepository.saveVideo(video)

        // Assert
        assertFalse(result)
      }
}
