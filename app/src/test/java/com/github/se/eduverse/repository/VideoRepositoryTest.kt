import android.net.Uri
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.repository.VideoRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    // Simuler le succès pour Firebase Storage
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

        // Simuler un échec d'upload avec une exception
        `when`(storageMock.reference).thenReturn(storageReferenceMock)
        `when`(storageReferenceMock.child(video.path)).thenReturn(storageReferenceMock)
        `when`(storageReferenceMock.putBytes(video.video))
            .thenThrow(RuntimeException("Upload failed"))

        // Act
        val result = videoRepository.saveVideo(video)

        // Assert
        assertFalse(result)
      }

  @Test
  fun `updateVideo should return true on successful update`() = runBlocking {
    // Arrange
    val video = Video("ownerId123", byteArrayOf(0x01, 0x02), "videos/path.mp4")
    val videoId = "video123"

    // Mock Firebase Storage interactions
    val uploadTaskMock = mock(UploadTask::class.java)
    val uriMock = mock(Uri::class.java)

    `when`(storageMock.reference).thenReturn(storageReferenceMock)
    `when`(storageReferenceMock.child(video.path)).thenReturn(storageReferenceMock)
    `when`(storageReferenceMock.putBytes(video.video)).thenReturn(uploadTaskMock)
    `when`(uploadTaskMock.isComplete).thenReturn(true)
    `when`(uploadTaskMock.isSuccessful).thenReturn(true)
    `when`(storageReferenceMock.downloadUrl).thenReturn(Tasks.forResult(uriMock))
    `when`(uriMock.toString()).thenReturn("http://example.com/videos/path.mp4")

    // Mock Firestore interactions for updating document
    val collectionReferenceMock = mock(CollectionReference::class.java)
    val documentReferenceMock = mock(DocumentReference::class.java)
    val videoData =
        hashMapOf(
            "ownerId" to video.ownerId,
            "videoUrl" to "http://example.com/videos/path.mp4",
            "path" to video.path)

    `when`(firestoreMock.collection("videos")).thenReturn(collectionReferenceMock)
    `when`(collectionReferenceMock.document(videoId)).thenReturn(documentReferenceMock)
    `when`(documentReferenceMock.set(videoData)).thenReturn(Tasks.forResult(null))

    // Act
    val result = videoRepository.updateVideo(videoId, video)

    // Assert
    assertTrue(result)
  }

  @Test(timeout = 3000)
  fun `updateVideo returns false on failure`() =
      runBlocking(Dispatchers.IO) {
        // Arrange
        val video = Video("ownerId123", byteArrayOf(0x01, 0x02), "videos/path.mp4")
        val videoId = "video123"

        // Simuler l'échec de l'upload avec une exception
        `when`(storageMock.reference).thenReturn(storageReferenceMock)
        `when`(storageReferenceMock.child(video.path)).thenReturn(storageReferenceMock)
        `when`(storageReferenceMock.putBytes(video.video))
            .thenThrow(RuntimeException("Upload failed"))

        // Act
        val result = videoRepository.updateVideo(videoId, video)

        // Assert
        assertFalse(result)
      }

  @Test
  fun `deleteVideo should return true on successful deletion`() = runBlocking {
    // Arrange
    val videoId = "video123"

    // Mock Firestore interactions for deleting a document
    val collectionReferenceMock = mock(CollectionReference::class.java)
    val documentReferenceMock = mock(DocumentReference::class.java)

    `when`(firestoreMock.collection("videos")).thenReturn(collectionReferenceMock)
    `when`(collectionReferenceMock.document(videoId)).thenReturn(documentReferenceMock)
    `when`(documentReferenceMock.delete()).thenReturn(Tasks.forResult(null))

    // Act
    val result = videoRepository.deleteVideo(videoId)

    // Assert
    assertTrue(result)
  }

  @Test(timeout = 3000)
  fun `deleteVideo returns false on failure`() =
      runBlocking(Dispatchers.IO) {
        // Arrange
        val videoId = "video123"

        // Simuler l'échec de la suppression avec une exception
        val collectionReferenceMock = mock(CollectionReference::class.java)
        val documentReferenceMock = mock(DocumentReference::class.java)

        `when`(firestoreMock.collection("videos")).thenReturn(collectionReferenceMock)
        `when`(collectionReferenceMock.document(videoId)).thenReturn(documentReferenceMock)
        `when`(documentReferenceMock.delete()).thenThrow(RuntimeException("Delete failed"))

        // Act
        val result = videoRepository.deleteVideo(videoId)

        // Assert
        assertFalse(result)
      }

  @Test
  fun `getVideosByOwner should return list of videos on successful retrieval`() = runBlocking {
    // Arrange
    val ownerId = "ownerId123"
    val videoPath = "videos/path.mp4"
    val downloadUrl = "http://example.com/videos/path.mp4"

    // Mock Firestore interactions for retrieving documents
    val collectionReferenceMock = mock(CollectionReference::class.java)
    val querySnapshotMock = mock(QuerySnapshot::class.java)
    val documentSnapshotMock = mock(DocumentSnapshot::class.java)
    val storageReferenceMock = mock(StorageReference::class.java)
    val videoReferenceMock = mock(StorageReference::class.java)
    val uriMock = mock(Uri::class.java)

    // Configure mocks
    `when`(firestoreMock.collection("videos")).thenReturn(collectionReferenceMock)
    `when`(collectionReferenceMock.whereEqualTo("ownerId", ownerId))
        .thenReturn(collectionReferenceMock)
    `when`(collectionReferenceMock.get()).thenReturn(Tasks.forResult(querySnapshotMock))
    `when`(querySnapshotMock.documents).thenReturn(listOf(documentSnapshotMock))

    // Document fields
    `when`(documentSnapshotMock.getString("path")).thenReturn(videoPath)
    `when`(documentSnapshotMock.getString("ownerId")).thenReturn(ownerId)

    // Mock Storage reference and download URL
    `when`(storageMock.reference).thenReturn(storageReferenceMock)
    `when`(storageReferenceMock.child(videoPath)).thenReturn(videoReferenceMock)
    `when`(videoReferenceMock.downloadUrl).thenReturn(Tasks.forResult(uriMock))
    `when`(uriMock.toString()).thenReturn(downloadUrl)

    // Act
    val result = videoRepository.getVideosByOwner(ownerId)

    // Assert
    assertTrue(result.isNotEmpty())
    assertEquals(ownerId, result[0].ownerId)
    assertEquals(downloadUrl, result[0].path)
  }

  @Test
  fun `getVideosByOwner should return empty list on failure`() = runBlocking {
    // Arrange
    val ownerId = "ownerId123"

    // Mock Firestore interactions pour simuler une exception
    val collectionReferenceMock = mock(CollectionReference::class.java)
    `when`(firestoreMock.collection("videos")).thenReturn(collectionReferenceMock)
    `when`(collectionReferenceMock.whereEqualTo("ownerId", ownerId))
        .thenReturn(collectionReferenceMock)
    `when`(collectionReferenceMock.get()).thenThrow(RuntimeException("Failed to retrieve videos"))

    // Mock Storage reference pour éviter NullPointerException
    val storageReferenceMock = mock(StorageReference::class.java)
    `when`(storageMock.reference).thenReturn(storageReferenceMock)

    // Act
    val result = videoRepository.getVideosByOwner(ownerId)

    // Assert
    assertTrue(result.isEmpty())
  }
}
