import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.se.eduverse.ui.gallery.DefaultFileDownloader
import com.github.se.eduverse.ui.gallery.adjustImageRotationInverse
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DefaultFileDownloaderTest {

  private lateinit var context: Context
  private lateinit var firebaseStorage: FirebaseStorage
  private lateinit var storageReference: StorageReference
  private lateinit var fileDownloader: DefaultFileDownloader

  @Rule @JvmField val tempFolder = TemporaryFolder()

  @Before
  fun setup() {
    // Mock the context and cache directory
    context = mock()
    whenever(context.cacheDir).thenReturn(tempFolder.root)

    // Mock FirebaseStorage and StorageReference
    firebaseStorage = mock()
    storageReference = mock()

    // Inject the mocked instance of FirebaseStorage into DefaultFileDownloader
    fileDownloader = DefaultFileDownloader(firebaseStorage)
  }

  @Test
  fun `ensureLocalFile handles download failure`() = runTest {
    val remoteUrl = "http://example.com/video.mp4"

    // Mock firebaseStorage.getReferenceFromUrl(remoteUrl) to return storageReference
    whenever(firebaseStorage.getReferenceFromUrl(remoteUrl)).thenReturn(storageReference)

    // Mock storageReference.getFile(any<File>()) to throw an exception
    whenever(storageReference.getFile(any<File>())).thenThrow(RuntimeException("Download failed"))

    // Call the method to test
    val result = fileDownloader.ensureLocalFile(context, remoteUrl)

    // Assertions
    assertNull("The result should be null in case of failure", result)
  }

  @Test
  fun `ensureLocalFile returns null when path is null or empty`() = runTest {
    val result = fileDownloader.ensureLocalFile(context, null)
    assertNull(result)

    val emptyResult = fileDownloader.ensureLocalFile(context, "")
    assertNull(emptyResult)
  }

  @Test
  fun `ensureLocalFile returns local file when path is local and file exists`() = runTest {
    val localPath = "/path/to/local/file.jpg"

    // Create a real file in the temporary folder
    val file = File(tempFolder.root, "file.jpg").apply { createNewFile() }
    val result = fileDownloader.ensureLocalFile(context, file.absolutePath)
    assertNotNull(result)
    assertEquals(file.absolutePath, result?.absolutePath)
  }

  @Test
  fun `ensureLocalFile returns null when path is local but file does not exist`() = runTest {
    val localPath = "/path/to/nonexistent/file.jpg"
    val result = fileDownloader.ensureLocalFile(context, localPath)
    assertNull(result)
  }

  @Test
  fun testAdjustImageRotationInverse() {
    // Create a test bitmap
    val originalWidth = 100
    val originalHeight = 200
    val originalBitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)

    // Call the function to test
    val rotatedBitmap = adjustImageRotationInverse(originalBitmap)

    // Verify that the returned bitmap is different from the original
    assertNotSame(
        "The returned bitmap should not be the same object", originalBitmap, rotatedBitmap)

    // Verify dimensions: after a rotation of -90°, height and width should be swapped
    assertEquals(
        "The width of the bitmap after rotation should be equal to the old height",
        originalHeight,
        rotatedBitmap.width)
    assertEquals(
        "The height of the bitmap after rotation should be equal to the old width",
        originalWidth,
        rotatedBitmap.height)
  }

  @ExperimentalCoroutinesApi
  @Test
  fun `ensureLocalFile downloads jpg, rotates image and returns file`() = runTest {
    val remoteUrl = "http://example.com/image.jpg"
    whenever(firebaseStorage.getReferenceFromUrl(remoteUrl)).thenReturn(storageReference)

    // Local file where the download will be simulated
    val downloadedFile = File(tempFolder.root, "downloaded.jpg")
    downloadedFile.createNewFile()

    // Mock the FileDownloadTask
    val fileDownloadTask = mock<FileDownloadTask>()
    whenever(storageReference.getFile(any<File>())).thenAnswer {
      val targetFile = it.getArgument<File>(0)
      targetFile.writeText("fake image data") // Simulate image data
      fileDownloadTask
    }

    // Simulate a successful download
    whenever(fileDownloadTask.addOnSuccessListener(any())).thenAnswer {
      val listener = it.getArgument<OnSuccessListener<in FileDownloadTask.TaskSnapshot>>(0)
      listener.onSuccess(mock()) // Invoke download success
      fileDownloadTask
    }
    whenever(fileDownloadTask.addOnFailureListener(any())).thenReturn(fileDownloadTask)

    // We need to mock BitmapFactory.decodeFile to return a bitmap
    val mockBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
    mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      mockedBitmapFactory
          .`when`<Bitmap> { BitmapFactory.decodeFile(downloadedFile.absolutePath) }
          .thenReturn(mockBitmap)

      val result = fileDownloader.ensureLocalFile(context, remoteUrl)

      assertNotNull("The result should not be null after a successful download", result)
      assertTrue("The returned file should be a .jpg", result!!.name.endsWith(".jpg"))
    }
  }
}
