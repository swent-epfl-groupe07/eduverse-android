// VideoScreenTest.kt (dans le dossier test/)
package com.github.se.eduverse.ui.videos

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class HandleShareTest {

    private lateinit var mockClient: OkHttpClient
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockClient = mockk()
        mockContext = mockk(relaxed = true)

        val mockCacheDir = File("build/tmp/test/cache")
        val mockFilesDir = File("build/tmp/test/files")
        mockCacheDir.mkdirs()
        mockFilesDir.mkdirs()
        every { mockContext.cacheDir } returns mockCacheDir
        every { mockContext.filesDir } returns mockFilesDir
        every { mockContext.packageName } returns "com.github.se.eduverse"
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
        val publication = Publication(
            id = "testphoto",
            mediaUrl = "https://example.com/photo.jpg",
            mediaType = MediaType.PHOTO,
            thumbnailUrl = "",
            likedBy = emptyList()
        )
        val bytes = "image data".toByteArray()

        val file = createMediaFile(mockContext, publication, bytes)
        assertTrue(file.exists())
        assertTrue(file.name.startsWith("shared_"))
        assertTrue(file.name.endsWith(".jpg"))
    }

    @Test
    fun `createMediaFile - create a video file in fileDir`() {
        val publication = Publication(
            id = "testvideo",
            mediaUrl = "https://example.com/video.mp4",
            mediaType = MediaType.VIDEO,
            thumbnailUrl = "",
            likedBy = emptyList()
        )
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

        downloadBytes(url, mockClient) // Doit lancer une exception
    }
}
