// VideoScreenTest.kt (dans le dossier test/)
package com.github.se.eduverse.ui.videos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.videos.ShareUtils.createMediaFile
import com.github.se.eduverse.ui.videos.ShareUtils.createShareIntent
import com.github.se.eduverse.ui.videos.ShareUtils.downloadBytes
import com.github.se.eduverse.ui.videos.ShareUtils.getFileExtension
import com.github.se.eduverse.ui.videos.ShareUtils.handleShare
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HandleShareTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockClient: OkHttpClient
  private lateinit var mockContext: Context

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk(relaxed = true)
    mockClient = mockk()

    val mockCacheDir = File("build/tmp/test/cache_integration")
    val mockFilesDir = File("build/tmp/test/files_integration")
    mockCacheDir.mkdirs()
    mockFilesDir.mkdirs()
    every { mockContext.cacheDir } returns mockCacheDir
    every { mockContext.filesDir } returns mockFilesDir
    every { mockContext.packageName } returns "com.github.se.eduverse"
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    unmockkAll()
    Dispatchers.resetMain()
  }

  @Test
  fun `getFileExtension PHOTO`() {
    assertEquals(".jpg", getFileExtension(MediaType.PHOTO))
  }

  @Test
  fun `getFileExtension VIDEO`() {
    assertEquals(".mp4", getFileExtension(MediaType.VIDEO))
  }

  @Test
  fun `createMediaFile - create a photo file in the cache`() {
    val publication =
        Publication(
            id = "testphoto",
            mediaUrl = "https://example.com/photo.jpg",
            mediaType = MediaType.PHOTO,
            thumbnailUrl = "",
            likedBy = emptyList())
    val bytes = "image data".toByteArray()

    val file = createMediaFile(mockContext, publication, bytes)
    assertTrue(file.exists())
    assertTrue(file.name.startsWith("shared_"))
    assertTrue(file.name.endsWith(".jpg"))
  }

  @Test
  fun `createMediaFile - create a video file in fileDir`() {
    val publication =
        Publication(
            id = "testvideo",
            mediaUrl = "https://example.com/video.mp4",
            mediaType = MediaType.VIDEO,
            thumbnailUrl = "",
            likedBy = emptyList())
    val bytes = "video data".toByteArray()

    val file = createMediaFile(mockContext, publication, bytes)
    assertTrue(file.exists())
    assertTrue(file.name.startsWith("shared_"))
    assertTrue(file.name.endsWith(".mp4"))
  }

  /*@Test
  fun `createShareIntent - correct mime type photo`() {
      val publication = Publication(
          id = "pubphoto",
          mediaUrl = "https://example.com/photo.jpg",
          mediaType = MediaType.PHOTO,
          thumbnailUrl = "",
          likedBy = emptyList()
      )

      // Vérifiez que mediaType est bien PHOTO
      assertEquals(MediaType.PHOTO, publication.mediaType)

      val uri = Uri.parse("content://test.uri/file")
      val intent = createShareIntent(mockContext, publication, uri)

      assertNotNull(intent)
      assertEquals("image/jpeg", intent.type)
      assertTrue(intent.hasExtra(Intent.EXTRA_STREAM))
      assertEquals(uri, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
  }*/

  @Test
  fun `downloadBytes - success`() = runBlocking {
    val url = "https://example.com/image.jpg"
    val expectedBytes = "testdata".toByteArray()

    val responseBody = ResponseBody.create(null, expectedBytes)
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns responseBody

    val result = downloadBytes(url, mockClient)
    assertArrayEquals(expectedBytes, result)
  }

  @Test(expected = Exception::class)
  fun `downloadBytes -dl failure`(): Unit = runBlocking {
    val url = "https://example.com/fail.jpg"

    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns false
    every { mockResponse.code } returns 404
    every { mockResponse.body } returns null

    downloadBytes(url, mockClient)
  }

  @Test
  fun `handleShare - success scenario calls startActivity and shows toast`() = runTest {
    val publication =
        Publication(
            id = "successPub",
            userId = "user1",
            title = "Test Video",
            mediaType = MediaType.VIDEO,
            mediaUrl = "https://example.com/video.mp4",
            thumbnailUrl = "",
            likedBy = emptyList())

    mockkObject(ShareUtils)
    mockkStatic(FileProvider::class)
    mockkStatic(Toast::class)

    every { ShareUtils.downloadBytes(publication.mediaUrl, any()) } returns
        "videodata".toByteArray()

    val mockMediaFile =
        File(mockContext.filesDir, "shared_successPub.mp4").apply { writeText("video content") }
    every { ShareUtils.createMediaFile(mockContext, publication, any()) } returns mockMediaFile

    val mockUri = mockk<Uri>(relaxed = true)
    assertNotNull(mockUri)

    every {
      FileProvider.getUriForFile(mockContext, "com.github.se.eduverse.fileprovider", mockMediaFile)
    } returns mockUri

    val mockShareIntent = Intent(Intent.ACTION_SEND)
    every { ShareUtils.createShareIntent(mockContext, publication, mockUri) } returns
        mockShareIntent

    every { mockContext.startActivity(any()) } just Runs

    val mockToast = mockk<Toast>(relaxed = true)
    every { Toast.makeText(mockContext, any<String>(), any()) } returns mockToast

    ShareUtils.handleShare(
        publication = publication,
        context = mockContext,
        ioDispatcher = testDispatcher,
        mainDispatcher = testDispatcher,
        client = mockClient)

    testScheduler.advanceUntilIdle()

    verify {
      mockContext.startActivity(
          match { intent ->
            intent.action == Intent.ACTION_CHOOSER &&
                intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) == mockShareIntent
          })
    }

    verify {
      Toast.makeText(mockContext, "Partage lancé", Toast.LENGTH_SHORT)
      mockToast.show()
    }
  }

  @Test
  fun `handleShare - error scenario shows error toast`() = runTest {
    val publication =
        Publication(
            id = "errorPub",
            userId = "user2",
            title = "Test Video Error",
            mediaType = MediaType.VIDEO,
            mediaUrl = "https://example.com/fail.mp4",
            thumbnailUrl = "",
            likedBy = emptyList())

    mockkObject(ShareUtils)
    mockkStatic(Toast::class)

    every { ShareUtils.downloadBytes(publication.mediaUrl, any()) } throws
        Exception("404 Not Found")

    val mockToast = mockk<Toast>(relaxed = true)
    every { Toast.makeText(mockContext, any<String>(), any()) } returns mockToast

    ShareUtils.handleShare(
        publication = publication,
        context = mockContext,
        ioDispatcher = testDispatcher,
        mainDispatcher = testDispatcher,
        client = mockClient)

    testScheduler.advanceUntilIdle()

    verify(inverse = true) { mockContext.startActivity(any()) }

    verify {
      Toast.makeText(mockContext, "Erreur lors du partage: 404 Not Found", Toast.LENGTH_SHORT)
      mockToast.show()
    }
  }

  @Test
  fun `createShareIntent - correct mime type and extras for PHOTO`() {
    val publication =
        Publication(
            id = "pubPhoto",
            userId = "user1",
            title = "Test Photo",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            thumbnailUrl = "",
            likedBy = emptyList())

    val uri = Uri.parse("content://test.uri/filephoto") // Juste un URI factice

    val intent = createShareIntent(mockContext, publication, uri)

    assertNotNull(intent)
    assertEquals("image/jpeg", intent.type)

    assertTrue(intent.hasExtra(Intent.EXTRA_STREAM))
    assertEquals(uri, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))

    assertEquals("", intent.getStringExtra(Intent.EXTRA_TEXT))

    assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
  }

  @Test
  fun `createShareIntent - correct mime type and extras for VIDEO`() {
    val publication =
        Publication(
            id = "pubVideo",
            userId = "user2",
            title = "Test Video",
            mediaType = MediaType.VIDEO,
            mediaUrl = "https://example.com/video.mp4",
            thumbnailUrl = "",
            likedBy = emptyList())

    val uri = Uri.parse("content://test.uri/filevideo")

    val intent = createShareIntent(mockContext, publication, uri)

    assertNotNull(intent)
    assertEquals("video/mp4", intent.type)

    assertTrue(intent.hasExtra(Intent.EXTRA_STREAM))
    assertEquals(uri, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))

    assertEquals("", intent.getStringExtra(Intent.EXTRA_TEXT))

    assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
  }
}
