// MediaCacheManagerTest.kt
package com.github.se.eduverse.model

import android.content.Context
import io.mockk.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
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
    // Initialize MockK
    MockKAnnotations.init(this, relaxed = true)

    // Mock the Context and its cacheDir to point to the temporary folder
    context = mockk()
    every { context.cacheDir } returns temporaryFolder.root

    // Mock Downloader and MetadataSerializer
    downloader = mockk()
    serializer = mockk()

    // Initialize MediaCacheManager with mocked dependencies
    mediaCacheManager = MediaCacheManager(context, downloader, serializer)
  }

  @After
  fun tearDown() {
    // Clear all MockK mocks to ensure test isolation
    clearAllMocks()
  }

  /** Test Case: saveFileToCache should save the file when it does not exist */
  @Test
  fun `saveFileToCache should save file when it does not exist`() {
    // Arrange
    val url = "https://example.com/media.mp4"
    val fileName = "media.mp4"
    val fileContent = "Sample media content"
    val inputStream = ByteArrayInputStream(fileContent.toByteArray())

    coEvery { downloader.openStream(url) } returns inputStream

    // Act
    val savedFile = mediaCacheManager.saveFileToCache(url, fileName)

    // Assert
    assertNotNull("Saved file should not be null", savedFile)
    assertTrue("File should exist", savedFile!!.exists())
    assertEquals("File name should match", fileName, savedFile.name)
    assertEquals("File content should match", fileContent, savedFile.readText())

    // Verify that downloader.openStream was called once with the correct URL
    coVerify(exactly = 1) { downloader.openStream(url) }
  }

  /** Test Case: saveFileToCache should not overwrite existing file */
  @Test
  fun `saveFileToCache should not overwrite existing file`() {
    // Arrange
    val url = "https://example.com/media.mp4"
    val fileName = "media.mp4"
    val initialContent = "Initial media content"
    val newContent = "New media content"

    // Create the file with initial content
    val file = temporaryFolder.newFile(fileName)
    file.writeText(initialContent)

    // Mock downloader.openStream to return new content if called
    coEvery { downloader.openStream(url) } returns ByteArrayInputStream(newContent.toByteArray())

    // Act
    val savedFile = mediaCacheManager.saveFileToCache(url, fileName)

    // Assert
    assertNotNull("Saved file should not be null", savedFile)
    assertTrue("File should exist", savedFile!!.exists())
    assertEquals("File name should match", fileName, savedFile.name)
    assertEquals("File content should remain unchanged", initialContent, savedFile.readText())

    // Verify that downloader.openStream was NOT called since file exists
    coVerify(exactly = 0) { downloader.openStream(url) }
  }

  /** Test Case: savePublicationToCache should save media and metadata successfully */
  @Test
  fun `savePublicationToCache should save media and metadata successfully`() {
    // Arrange
    val publication =
        Publication(
            id = "1",
            userId = "user1",
            title = "Test Publication",
            mediaUrl = "https://example.com/media.mp4",
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            mediaType = MediaType.VIDEO,
            timestamp = System.currentTimeMillis())
    val mediaFileName = "media.mp4"
    val metadataFileName = "metadata.json"
    val mediaContent = "Sample media content"
    val metadataJson = "{\"id\":\"1\",\"title\":\"Test Publication\"}"
    val inputStream = ByteArrayInputStream(mediaContent.toByteArray())

    coEvery { downloader.openStream(publication.mediaUrl) } returns inputStream
    every { serializer.serialize<Publication>(publication) } returns metadataJson

    // Act
    val result =
        mediaCacheManager.savePublicationToCache(
            publication, publication.mediaUrl, mediaFileName, metadataFileName)

    // Assert
    assertTrue("savePublicationToCache should return true", result)

    // Verify that media file was saved correctly
    val savedMediaFile = File(temporaryFolder.root, mediaFileName)
    assertTrue("Media file should exist", savedMediaFile.exists())
    assertEquals("Media file content should match", mediaContent, savedMediaFile.readText())

    // Verify that metadata file was saved correctly
    val savedMetadataFile = File(temporaryFolder.root, metadataFileName)
    assertTrue("Metadata file should exist", savedMetadataFile.exists())
    assertEquals("Metadata file content should match", metadataJson, savedMetadataFile.readText())

    // Verify that downloader.openStream was called once
    coVerify(exactly = 1) { downloader.openStream(publication.mediaUrl) }

    // Verify that serializer.serialize was called once with the correct type
    verify(exactly = 1) { serializer.serialize<Publication>(publication) }
  }

  /** Test Case: savePublicationToCache should return false if media file saving fails */
  @Test
  fun `savePublicationToCache should return false if media file saving fails`() {
    // Arrange
    val publication =
        Publication(
            id = "1",
            userId = "user1",
            title = "Test Publication",
            mediaUrl = "https://example.com/media.mp4",
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            mediaType = MediaType.VIDEO,
            timestamp = System.currentTimeMillis())
    val mediaFileName = "media.mp4"
    val metadataFileName = "metadata.json"

    // Mock downloader.openStream to throw an IOException
    coEvery { downloader.openStream(publication.mediaUrl) } throws
        IOException("Failed to open stream")

    // Act
    val result =
        mediaCacheManager.savePublicationToCache(
            publication, publication.mediaUrl, mediaFileName, metadataFileName)

    // Assert
    assertFalse("savePublicationToCache should return false when media saving fails", result)

    // Verify that media file was not created
    val savedMediaFile = File(temporaryFolder.root, mediaFileName)
    assertFalse("Media file should not exist", savedMediaFile.exists())

    // Verify that metadata file was not created
    val savedMetadataFile = File(temporaryFolder.root, metadataFileName)
    assertFalse("Metadata file should not exist", savedMetadataFile.exists())

    // Verify that downloader.openStream was called once
    coVerify(exactly = 1) { downloader.openStream(publication.mediaUrl) }

    // Verify that serializer.serialize was NOT called
    verify(exactly = 0) { serializer.serialize<Publication>(any()) }
  }

  /** Test Case: getFileFromCache should retrieve existing file */
  @Test
  fun `getFileFromCache should retrieve existing file`() {
    // Arrange
    val fileName = "media.mp4"
    val fileContent = "Sample media content"
    val file = temporaryFolder.newFile(fileName)
    file.writeText(fileContent)

    // Act
    val retrievedFile = mediaCacheManager.getFileFromCache(fileName)

    // Assert
    assertNotNull("Retrieved file should not be null", retrievedFile)
    assertTrue("Retrieved file should exist", retrievedFile!!.exists())
    assertEquals("Retrieved file name should match", fileName, retrievedFile.name)
    assertEquals("Retrieved file content should match", fileContent, retrievedFile.readText())
  }

  /** Test Case: getFileFromCache should return null if file does not exist */
  @Test
  fun `getFileFromCache should return null if file does not exist`() {
    // Arrange
    val fileName = "nonexistent_media.mp4"

    // Act
    val retrievedFile = mediaCacheManager.getFileFromCache(fileName)

    // Assert
    assertNull("Retrieved file should be null when it does not exist", retrievedFile)
  }

  /** Test Case: getPublicationFromCache should retrieve existing publication */
  @Test
  fun `getPublicationFromCache should retrieve existing publication`() {
    // Arrange
    val publication =
        Publication(
            id = "1",
            userId = "user1",
            title = "Test Publication",
            mediaUrl = "https://example.com/media.mp4",
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            mediaType = MediaType.VIDEO,
            timestamp = System.currentTimeMillis())
    val metadataFileName = "metadata.json"
    val metadataJson = "{\"id\":\"1\",\"userId\":\"user1\",\"title\":\"Test Publication\"}"
    val metadataFile = temporaryFolder.newFile(metadataFileName)
    metadataFile.writeText(metadataJson)

    // Mock serializer.deserialize to return the publication object
    every { serializer.deserialize<Publication>(metadataJson, Publication::class.java) } returns
        publication

    // Act
    val retrievedPublication =
        mediaCacheManager.getPublicationFromCache(metadataFileName, Publication::class.java)

    // Assert
    assertNotNull("Retrieved publication should not be null", retrievedPublication)
    assertEquals(
        "Retrieved publication should match the original", publication, retrievedPublication)

    // Verify that serializer.deserialize was called once with the correct type
    verify(exactly = 1) {
      serializer.deserialize<Publication>(metadataJson, Publication::class.java)
    }
  }

  /** Test Case: getPublicationFromCache should return null if metadata file does not exist */
  @Test
  fun `getPublicationFromCache should return null if metadata file does not exist`() {
    // Arrange
    val metadataFileName = "nonexistent_metadata.json"

    // Act
    val retrievedPublication =
        mediaCacheManager.getPublicationFromCache(metadataFileName, Publication::class.java)

    // Assert
    assertNull(
        "Retrieved publication should be null when metadata file does not exist",
        retrievedPublication)

    // Verify that serializer.deserialize was NOT called
    verify(exactly = 0) { serializer.deserialize<Publication>(any(), any<Class<Publication>>()) }
  }

  /** Test Case: deleteFileFromCache should delete existing file */
  @Test
  fun `deleteFileFromCache should delete existing file`() {
    // Arrange
    val fileName = "media.mp4"
    val file = temporaryFolder.newFile(fileName)
    assertTrue("File should exist before deletion", file.exists())

    // Act
    val result = mediaCacheManager.deleteFileFromCache(fileName)

    // Assert
    assertTrue("deleteFileFromCache should return true when deletion is successful", result)
    assertFalse("File should not exist after deletion", file.exists())
  }

  /** Test Case: deleteFileFromCache should return false if file does not exist */
  @Test
  fun `deleteFileFromCache should return false if file does not exist`() {
    // Arrange
    val fileName = "nonexistent_media.mp4"

    // Act
    val result = mediaCacheManager.deleteFileFromCache(fileName)

    // Assert
    assertFalse("deleteFileFromCache should return false when file does not exist", result)
  }

  /** Test Case: cleanCache should delete files older than 2 days */
  @Test
  fun `cleanCache should delete files older than 2 days`() {
    // Arrange
    val oldFileName = "old_media.mp4"
    val recentFileName = "recent_media.mp4"

    val oldFile = temporaryFolder.newFile(oldFileName)
    val recentFile = temporaryFolder.newFile(recentFileName)

    // Set last modified times
    val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L
    val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
    val currentTime = System.currentTimeMillis()

    oldFile.setLastModified(currentTime - threeDaysInMillis) // Older than 2 days
    recentFile.setLastModified(currentTime - twoDaysInMillis + 1000) // Just within 2 days

    // Act
    mediaCacheManager.cleanCache()

    // Assert
    assertFalse("Old file should be deleted", oldFile.exists())
    assertTrue("Recent file should still exist", recentFile.exists())
  }

  /** Test Case: cleanCache should not delete recent files */
  @Test
  fun `cleanCache should not delete recent files`() {
    // Arrange
    val recentFileName1 = "recent_media1.mp4"
    val recentFileName2 = "recent_media2.mp4"

    val recentFile1 = temporaryFolder.newFile(recentFileName1)
    val recentFile2 = temporaryFolder.newFile(recentFileName2)

    // Set last modified times within 2 days
    val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L
    val currentTime = System.currentTimeMillis()

    recentFile1.setLastModified(currentTime - twoDaysInMillis + 1000) // Just within 2 days
    recentFile2.setLastModified(currentTime - twoDaysInMillis + 2000) // Just within 2 days

    // Act
    mediaCacheManager.cleanCache()

    // Assert
    assertTrue("Recent file 1 should still exist", recentFile1.exists())
    assertTrue("Recent file 2 should still exist", recentFile2.exists())
  }

  /** Test Case: hasCachedFiles should return true when cache has files */
  @Test
  fun `hasCachedFiles should return true when cache has files`() {
    // Arrange
    val fileName = "media.mp4"
    temporaryFolder.newFile(fileName)

    // Act
    val hasFiles = mediaCacheManager.hasCachedFiles()

    // Assert
    assertTrue("hasCachedFiles should return true when cache has files", hasFiles)
  }

  /** Test Case: hasCachedFiles should return false when cache is empty */
  @Test
  fun `hasCachedFiles should return false when cache is empty`() {
    // Arrange
    // Ensure the temporary folder is empty
    temporaryFolder.root.deleteRecursively()
    temporaryFolder.create() // Recreate the folder

    // Act
    val hasFiles = mediaCacheManager.hasCachedFiles()

    // Assert
    assertFalse("hasCachedFiles should return false when cache is empty", hasFiles)
  }
}
