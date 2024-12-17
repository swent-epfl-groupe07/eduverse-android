// MediaCacheManagerTest.kt
package com.github.se.eduverse.model

import android.content.Context
import io.mockk.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MediaCacheManagerTest {

  @get:Rule val temporaryFolder = TemporaryFolder()

  private lateinit var context: Context
  private lateinit var downloader: Downloader
  private lateinit var serializer: MetadataSerializer
  private lateinit var mediaCacheManager: MediaCacheManager

  @Before
  fun setup() {
    MockKAnnotations.init(this, relaxed = true)

    context = mockk()
    every { context.cacheDir } returns temporaryFolder.root

    downloader = mockk()
    serializer = mockk()

    mediaCacheManager = MediaCacheManager(context, downloader, serializer)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `saveFileToCache should save file with timestamp`() = runTest {
    val url = "https://example.com/media.mp4"
    val fileName = "media.mp4"
    val fileContent = "Sample media content"
    val inputStream = ByteArrayInputStream(fileContent.toByteArray())

    coEvery { downloader.openStream(url) } returns inputStream

    val savedFile = mediaCacheManager.saveFileToCache(url, fileName)

    assertNotNull(savedFile)
    assertTrue(savedFile!!.exists())
    assertTrue(savedFile.name.contains("media.mp4"))
    assertEquals(fileContent, savedFile.readText())

    coVerify(exactly = 1) { downloader.openStream(url) }
  }

  @Test
  fun `savePublicationToCache should save media and metadata with timestamped filenames`() = runTest {
    val publication = Publication(
      id = "1",
      userId = "user1",
      title = "Test Publication",
      mediaUrl = "https://example.com/media.mp4",
      thumbnailUrl = "https://example.com/thumbnail.jpg",
      mediaType = MediaType.VIDEO,
      timestamp = System.currentTimeMillis()
    )
    val mediaContent = "Sample media content"
    val metadataJson = "{\"id\":\"1\",\"title\":\"Test Publication\"}"
    val inputStream = ByteArrayInputStream(mediaContent.toByteArray())

    coEvery { downloader.openStream(publication.mediaUrl) } returns inputStream
    every { serializer.serialize(publication) } returns metadataJson

    val result = mediaCacheManager.savePublicationToCache(
      publication,
      publication.mediaUrl,
      "media.mp4",
      "metadata.json"
    )

    assertTrue(result)
    val savedMediaFile = temporaryFolder.root.listFiles()?.first { it.name.contains("media.mp4") }
    val savedMetadataFile = temporaryFolder.root.listFiles()?.first { it.name.contains("metadata.json") }

    assertNotNull(savedMediaFile)
    assertNotNull(savedMetadataFile)
    assertEquals(mediaContent, savedMediaFile!!.readText())
    assertEquals(metadataJson, savedMetadataFile!!.readText())
  }

  @Test
  fun `cleanCache should delete expired files`() = runTest {
    val currentTime = System.currentTimeMillis()
    val expiredFile = File(temporaryFolder.root, "${currentTime - 3 * 24 * 60 * 60 * 1000}_expired.mp4")
    val validFile = File(temporaryFolder.root, "${currentTime}_valid.mp4")
    expiredFile.writeText("Expired file content")
    validFile.writeText("Valid file content")

    assertTrue(expiredFile.exists())
    assertTrue(validFile.exists())

    mediaCacheManager.cleanCache()

    assertFalse(expiredFile.exists())
    assertTrue(validFile.exists())
  }

  @Test
  fun `hasCachedFiles should return true if recent files exist`() = runTest {
    val currentTime = System.currentTimeMillis()
    val recentFile = File(temporaryFolder.root, "${currentTime}_recent.mp4")
    recentFile.writeText("Recent file content")

    val result = mediaCacheManager.hasCachedFiles()

    assertTrue(result)
  }

  @Test
  fun `hasCachedFiles should return false if no recent files exist`() = runTest {
    val currentTime = System.currentTimeMillis()
    val oldFile = File(temporaryFolder.root, "${currentTime - 3 * 24 * 60 * 60 * 1000}_old.mp4")
    oldFile.writeText("Old file content")

    val result = mediaCacheManager.hasCachedFiles()

    assertFalse(result)
  }

  @Test
  fun `saveFileToCache should handle download failure`() = runTest {
    val url = "https://example.com/media.mp4"
    val fileName = "media.mp4"

    coEvery { downloader.openStream(url) } throws IOException("Failed to download file")

    val savedFile = mediaCacheManager.saveFileToCache(url, fileName)

    assertNull(savedFile)
    coVerify(exactly = 1) { downloader.openStream(url) }
  }

  @Test
  fun `deleteFileFromCache should delete existing file`() = runTest {
    val fileName = "media.mp4"
    val file = temporaryFolder.newFile(fileName)

    assertTrue(file.exists())

    val result = mediaCacheManager.deleteFileFromCache(fileName)

    assertTrue(result)
    assertFalse(file.exists())
  }
}
