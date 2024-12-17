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
  fun `saveFileToCache should save file when it does not exist`() = runTest {
    val url = "https://example.com/media.mp4"
    val fileName = "media.mp4"
    val fileContent = "Sample media content"
    val inputStream = ByteArrayInputStream(fileContent.toByteArray())

    coEvery { downloader.openStream(url) } returns inputStream

    val savedFile = mediaCacheManager.saveFileToCache(url, fileName)

    assertNotNull(savedFile)
    assertTrue(savedFile!!.exists())
    assertEquals(fileName, savedFile.name)
    assertEquals(fileContent, savedFile.readText())

    coVerify(exactly = 1) { downloader.openStream(url) }
  }

  @Test
  fun `savePublicationToCache should save media and metadata successfully`() = runTest {
    val publication = Publication(
      id = "1",
      userId = "user1",
      title = "Test Publication",
      mediaUrl = "https://example.com/media.mp4",
      thumbnailUrl = "https://example.com/thumbnail.jpg",
      mediaType = MediaType.VIDEO,
      timestamp = System.currentTimeMillis()
    )
    val mediaFileName = "media.mp4"
    val metadataFileName = "metadata.json"
    val mediaContent = "Sample media content"
    val metadataJson = "{\"id\":\"1\",\"title\":\"Test Publication\"}"

    val inputStream = ByteArrayInputStream(mediaContent.toByteArray())
    coEvery { downloader.openStream(publication.mediaUrl) } returns inputStream
    every { serializer.serialize(publication) } returns metadataJson

    val result = mediaCacheManager.savePublicationToCache(
      publication, publication.mediaUrl, mediaFileName, metadataFileName
    )

    assertTrue(result)

    val savedMediaFile = File(temporaryFolder.root, mediaFileName)
    assertTrue(savedMediaFile.exists())
    assertEquals(mediaContent, savedMediaFile.readText())

    val savedMetadataFile = File(temporaryFolder.root, metadataFileName)
    assertTrue(savedMetadataFile.exists())
    assertEquals(metadataJson, savedMetadataFile.readText())

    coVerify(exactly = 1) { downloader.openStream(publication.mediaUrl) }
    verify(exactly = 1) { serializer.serialize(publication) }
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

  @Test
  fun `deleteFileFromCache should return false if file does not exist`() = runTest {
    val fileName = "nonexistent_media.mp4"

    val result = mediaCacheManager.deleteFileFromCache(fileName)

    assertFalse(result)
  }

  @Test
  fun `getFileFromCache should return file if exists`() = runTest {
    val fileName = "media.mp4"
    val fileContent = "Sample media content"
    val file = temporaryFolder.newFile(fileName)
    file.writeText(fileContent)

    val retrievedFile = mediaCacheManager.getFileFromCache(fileName)

    assertNotNull(retrievedFile)
    assertEquals(fileContent, retrievedFile!!.readText())
  }

  @Test
  fun `getFileFromCache should return null if file does not exist`() = runTest {
    val fileName = "nonexistent_media.mp4"

    val retrievedFile = mediaCacheManager.getFileFromCache(fileName)

    assertNull(retrievedFile)
  }
}
