// PublicationViewModelTest.kt
package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.model.MediaCacheManager
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PublicationViewModelTest {

  // Set up a TestDispatcher for controlling coroutine execution
  private val testDispatcher = StandardTestDispatcher()

  // The ViewModel under test
  private lateinit var viewModel: PublicationViewModel

  // Mocked dependencies
  private val mockRepository = mockk<PublicationRepository>(relaxed = true)
  private val mockMediaCacheManager = mockk<MediaCacheManager>(relaxed = true)

  @Before
  fun setup() {
    // Override the main dispatcher with our TestDispatcher
    Dispatchers.setMain(testDispatcher)
    // Initialize the ViewModel with mocked dependencies
    viewModel = PublicationViewModel(mockRepository, mockMediaCacheManager)
  }

  @After
  fun tearDown() {
    // Reset the main dispatcher to the original Main dispatcher
    Dispatchers.resetMain()
    // Clear all MockK mocks to ensure test isolation
    clearAllMocks()
  }

  /** Test Case: Handling exceptions when loading publications fails */
  @Test
  fun `test error handling when loading publications fails`() = runTest {
    // Arrange: Mock repository to throw an exception
    coEvery { mockRepository.loadRandomPublications() } throws Exception("Network Error")

    // Act: Attempt to load publications
    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert: Verify that error state is updated
    assertEquals("fail to load publications", viewModel.error.first())

    // Verify that repository methods were called
    coVerify(exactly = 1) { mockRepository.loadRandomPublications() }

    // Since an exception occurs, loadCachePublications should not be called
    coVerify(exactly = 0) { mockRepository.loadCachePublications(any()) }

    // Verify that MediaCacheManager methods were not called
    coVerify(exactly = 0) { mockMediaCacheManager.cleanCache() }
    coVerify(exactly = 0) { mockMediaCacheManager.hasCachedFiles() }
    coVerify(exactly = 0) {
      mockMediaCacheManager.savePublicationToCache<Publication>(any(), any(), any(), any())
    }
  }

  /** Test Case: Loading empty list of publications */
  @Test
  fun `test loading empty list of publications`() = runTest {
    // Arrange: Mock repository to return empty list
    coEvery { mockRepository.loadRandomPublications() } returns emptyList()
    coEvery { mockRepository.loadCachePublications(limit = 50) } returns emptyList()

    // Mock MediaCacheManager methods
    coEvery {
      mockMediaCacheManager.savePublicationToCache<Publication>(
          publication = any(), mediaUrl = any(), mediaFileName = any(), metadataFileName = any())
    } returns true

    coEvery { mockMediaCacheManager.cleanCache() } just Runs
    coEvery { mockMediaCacheManager.hasCachedFiles() } returns false

    // Act: Initialize publications
    viewModel.initializePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val result = viewModel.publications.value
    assertTrue("Publications list should be empty", result.isEmpty())

    // Verify that repository methods were called
    coVerify(exactly = 1) { mockRepository.loadRandomPublications() }
    coVerify(exactly = 1) { mockRepository.loadCachePublications(limit = 50) }

    // Verify that MediaCacheManager methods were called appropriately
    coVerify(exactly = 1) { mockMediaCacheManager.cleanCache() }
    coVerify(exactly = 1) { mockMediaCacheManager.hasCachedFiles() }
    // Since cachePublications is empty, savePublicationToCache should not be called
    coVerify(exactly = 0) {
      mockMediaCacheManager.savePublicationToCache<Publication>(any(), any(), any(), any())
    }

    // Verify no error is present
    assertNull("Error should be null when loading empty list", viewModel.error.first())
  }

  /** Test Case: loadPublications is called with correct limit */
  @Test
  fun `test loadPublications is called with correct limit`() = runTest {
    // Here we just verify that the method loadRandomPublications is called once
    // Since we don't have limit arguments anymore in the code, we just confirm it's called.
    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { mockRepository.loadRandomPublications() }
  }

  /** New Test: Verify that loadAndCachePublications handles exceptions gracefully */
  @Test
  fun `test loadAndCachePublications handles exceptions gracefully`() = runTest {
    // Arrange
    // Mock repository to throw exception when loadCachePublications is called
    coEvery { mockRepository.loadCachePublications(limit = 50) } throws Exception("Cache Error")

    // Mock repository to return some publications on loadRandomPublications
    val publicationsFromRandom =
        listOf(
            Publication(
                id = "3",
                userId = "user3",
                title = "Random Video",
                mediaUrl = "https://randomvideo.url",
                thumbnailUrl = "https://thumbnail.url",
                mediaType = MediaType.VIDEO,
                timestamp = System.currentTimeMillis()))
    coEvery { mockRepository.loadRandomPublications() } returns publicationsFromRandom

    // Mock MediaCacheManager methods
    coEvery { mockMediaCacheManager.cleanCache() } just Runs
    coEvery { mockMediaCacheManager.hasCachedFiles() } returns false
    coEvery {
      mockMediaCacheManager.savePublicationToCache<Publication>(
          publication = any(), mediaUrl = any(), mediaFileName = any(), metadataFileName = any())
    } returns true

    // Act: Initialize publications which triggers loadAndCachePublications
    viewModel.initializePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert:
    // Since loadCachePublications throws an exception, savePublicationToCache should not be called
    coVerify(exactly = 0) {
      mockMediaCacheManager.savePublicationToCache<Publication>(any(), any(), any(), any())
    }

    // Verify that publications are loaded from loadRandomPublications
    val result = viewModel.publications.value
    assertEquals("Publications size should match", publicationsFromRandom.size, result.size)
    assertTrue(
        "Publications should contain all random publications",
        result.containsAll(publicationsFromRandom))

    // Verify that error remains null as exceptions in caching are logged but do not affect
    // publications
    assertNull("Error should be null even if caching fails", viewModel.error.first())
  }

  /** New Test: Verify that caching is skipped when cached files exist */
  @Test
  fun `test loadAndCachePublications skips caching when cached files exist`() = runTest {
    // Arrange
    val cachedPublications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Cached Video",
                mediaUrl = "https://cachedvideo.url",
                thumbnailUrl = "https://thumbnail.url",
                mediaType = MediaType.VIDEO,
                timestamp = System.currentTimeMillis()),
            Publication(
                id = "2",
                userId = "user2",
                title = "Cached Photo",
                mediaUrl = "https://cachedphoto.url",
                thumbnailUrl = "https://thumbnail.url",
                mediaType = MediaType.PHOTO,
                timestamp = System.currentTimeMillis()))

    // Mock repository methods
    coEvery { mockRepository.loadRandomPublications() } returns cachedPublications
    // Note: Since caching should be skipped, loadCachePublications won't be called.
    coEvery { mockRepository.loadCachePublications(limit = 50) } returns cachedPublications

    // Mock MediaCacheManager methods
    coEvery { mockMediaCacheManager.cleanCache() } just Runs
    coEvery { mockMediaCacheManager.hasCachedFiles() } returns true
    // When hasCachedFiles is true, savePublicationToCache should not be called

    // Act: Initialize publications which triggers loadAndCachePublications
    viewModel.initializePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert:
    // Verify that savePublicationToCache is never called since cached files exist
    coVerify(exactly = 0) {
      mockMediaCacheManager.savePublicationToCache<Publication>(any(), any(), any(), any())
    }

    // Verify that publications are loaded correctly
    val result = viewModel.publications.value
    assertEquals("Publications size should match", cachedPublications.size, result.size)
    assertTrue(
        "Publications should contain all cached publications",
        result.containsAll(cachedPublications))

    // Verify that repository methods were called
    coVerify(exactly = 1) { mockRepository.loadRandomPublications() }

    // Verify that MediaCacheManager methods were called appropriately
    coVerify(exactly = 1) { mockMediaCacheManager.cleanCache() }
    coVerify(exactly = 1) { mockMediaCacheManager.hasCachedFiles() }

    // Verify no error is present
    assertNull("Error should be null after successful load and caching", viewModel.error.first())
  }

  @Test
  fun `test loadFollowedPublications with empty list`() = runTest {
    val publications =
        listOf(
            Publication(
                id = "1", userId = "user1", title = "Test Video", mediaType = MediaType.VIDEO))

    coEvery { mockRepository.loadRandomPublications(listOf("userId")) } returns publications

    viewModel.loadFollowedPublications(listOf("userId"))
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { mockRepository.loadRandomPublications(listOf("userId")) }

    val expectedIds = publications.map { it.id }.toSet()
    val actualIds = viewModel.followedPublications.first().map { it.id }.toSet()

    assertEquals(expectedIds, actualIds)
  }

  @Test
  fun `test loadFollowedPublications with multiple calls`() = runTest {
    val initialPublications =
        listOf(
            Publication(
                id = "1", userId = "user1", title = "Test Video", mediaType = MediaType.VIDEO))
    val morePublications =
        listOf(
            Publication(
                id = "2", userId = "user2", title = "Test Photo", mediaType = MediaType.PHOTO))

    coEvery { mockRepository.loadRandomPublications(listOf("userId")) } returnsMany
        listOf(initialPublications, morePublications)

    viewModel.loadFollowedPublications(listOf("userId"))
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.loadFollowedPublications(listOf("userId"))
    testDispatcher.scheduler.advanceUntilIdle()

    val expectedIds = (initialPublications + morePublications).map { it.id }.toSet()
    val actualIds = viewModel.followedPublications.first().map { it.id }.toSet()

    assertEquals(expectedIds, actualIds)
  }

  /** Test Case: Initial load of publications and caching process */
  @Test
  fun `test initial load of publications and caching`() = runTest {
    // Arrange
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaUrl = "https://video.url",
                thumbnailUrl = "https://thumbnail.url",
                mediaType = MediaType.VIDEO,
                timestamp = System.currentTimeMillis()),
            Publication(
                id = "2",
                userId = "user2",
                title = "Test Photo",
                mediaUrl = "https://photo.url",
                thumbnailUrl = "https://thumbnail.url",
                mediaType = MediaType.PHOTO,
                timestamp = System.currentTimeMillis()))

    // Mock repository to return publications on loadRandomPublications and loadCachePublications
    coEvery { mockRepository.loadRandomPublications() } returns publications
    coEvery { mockRepository.loadCachePublications(limit = 50) } returns publications

    // Mock MediaCacheManager methods
    coEvery {
      mockMediaCacheManager.savePublicationToCache<Publication>(
          publication = any(), mediaUrl = any(), mediaFileName = any(), metadataFileName = any())
    } returns true

    coEvery { mockMediaCacheManager.cleanCache() } just Runs
    coEvery { mockMediaCacheManager.hasCachedFiles() } returns false

    // Act
    viewModel.initializePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val result = viewModel.publications.value
    assertEquals("Publications size should match", publications.size, result.size)
    assertTrue("Publications should contain all expected items", result.containsAll(publications))

    // Verify that repository methods were called
    coVerify(exactly = 1) { mockRepository.loadRandomPublications() }
    coVerify(exactly = 1) { mockRepository.loadCachePublications(limit = 50) }

    // Verify that MediaCacheManager methods were called appropriately
    coVerify(exactly = 1) { mockMediaCacheManager.cleanCache() }
    coVerify(exactly = 1) { mockMediaCacheManager.hasCachedFiles() }

    publications.forEach { publication ->
      val expectedMetadataFileName = "${publication.id}_metadata.json"
      coVerify(exactly = 1) {
        mockMediaCacheManager.savePublicationToCache<Publication>(
            publication = publication,
            mediaUrl = publication.mediaUrl,
            mediaFileName =
                match {
                  it.endsWith("_${publication.id}_media.mp4") ||
                      it.endsWith("_${publication.id}_media.jpg")
                },
            metadataFileName = expectedMetadataFileName)
      }
    }

    // Verify no error is present
    assertNull("Error should be null after successful load", viewModel.error.first())
  }

  /** Test Case: Loading more publications when some are already present */
  @Test
  fun `test loading more publications when some are already present`() = runTest {
    // Arrange
    val initialPublications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Initial Video",
                mediaUrl = "https://video.url",
                thumbnailUrl = "https://thumbnail.url",
                mediaType = MediaType.VIDEO,
                timestamp = System.currentTimeMillis()))
    val morePublications =
        listOf(
            Publication(
                id = "2",
                userId = "user2",
                title = "More Photo",
                mediaUrl = "https://photo.url",
                thumbnailUrl = "https://thumbnail.url",
                mediaType = MediaType.PHOTO,
                timestamp = System.currentTimeMillis()))

    coEvery { mockRepository.loadRandomPublications() } returnsMany
        listOf(initialPublications, morePublications)
    coEvery { mockRepository.loadCachePublications(limit = 50) } returns
        initialPublications + morePublications

    coEvery {
      mockMediaCacheManager.savePublicationToCache<Publication>(
          publication = any(), mediaUrl = any(), mediaFileName = any(), metadataFileName = any())
    } returns true

    coEvery { mockMediaCacheManager.cleanCache() } just Runs
    coEvery { mockMediaCacheManager.hasCachedFiles() } returns false

    // Act: Load initial publications
    viewModel.initializePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    // Act: Load more publications
    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    // Combine initial and more publications
    val expectedIds = (initialPublications + morePublications).map { it.id }.toSet()
    val actualIds = viewModel.publications.value.map { it.id }.toSet()

    assertEquals("Total publications should match", expectedIds, actualIds)

    // Verify that repository methods were called
    coVerify(exactly = 2) { mockRepository.loadRandomPublications() }
    coVerify(exactly = 1) { mockRepository.loadCachePublications(limit = 50) }

    // Verify that MediaCacheManager methods were called appropriately
    coVerify(exactly = 1) { mockMediaCacheManager.cleanCache() }
    coVerify(exactly = 1) { mockMediaCacheManager.hasCachedFiles() }

    (initialPublications + morePublications).forEach { publication ->
      val expectedMetadataFileName = "${publication.id}_metadata.json"
      coVerify(exactly = 1) {
        mockMediaCacheManager.savePublicationToCache<Publication>(
            publication = publication,
            mediaUrl = publication.mediaUrl,
            mediaFileName =
                match {
                  it.endsWith("_${publication.id}_media.mp4") ||
                      it.endsWith("_${publication.id}_media.jpg")
                },
            metadataFileName = expectedMetadataFileName)
      }
    }

    // Verify no error is present
    assertNull("Error should be null after successful load", viewModel.error.first())
  }

  /** New Test: Verify that loadAndCachePublications saves publications to cache correctly */
  @Test
  fun `test loadAndCachePublications saves publications to cache`() = runTest {
    // Arrange
    val cachedPublications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Cached Video",
                mediaUrl = "https://cachedvideo.url",
                thumbnailUrl = "https://thumbnail.url",
                mediaType = MediaType.VIDEO,
                timestamp = System.currentTimeMillis()),
            Publication(
                id = "2",
                userId = "user2",
                title = "Cached Photo",
                mediaUrl = "https://cachedphoto.url",
                thumbnailUrl = "https://thumbnail.url",
                mediaType = MediaType.PHOTO,
                timestamp = System.currentTimeMillis()))

    // Mock repository methods
    coEvery { mockRepository.loadRandomPublications() } returns cachedPublications
    coEvery { mockRepository.loadCachePublications(limit = 50) } returns cachedPublications

    coEvery {
      mockMediaCacheManager.savePublicationToCache<Publication>(
          publication = any(), mediaUrl = any(), mediaFileName = any(), metadataFileName = any())
    } returns true

    coEvery { mockMediaCacheManager.cleanCache() } just Runs
    coEvery { mockMediaCacheManager.hasCachedFiles() } returns false

    // Act: Initialize publications which triggers loadAndCachePublications
    viewModel.initializePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert: Verify that savePublicationToCache is called with correct parameters
    cachedPublications.forEach { publication ->
      val expectedMetadataFileName = "${publication.id}_metadata.json"
      coVerify(exactly = 1) {
        mockMediaCacheManager.savePublicationToCache<Publication>(
            publication = publication,
            mediaUrl = publication.mediaUrl,
            mediaFileName =
                match {
                  it.endsWith("_${publication.id}_media.mp4") ||
                      it.endsWith("_${publication.id}_media.jpg")
                },
            metadataFileName = expectedMetadataFileName)
      }
    }

    // Verify that publications are loaded correctly
    val result = viewModel.publications.value
    assertEquals("Publications size should match", cachedPublications.size, result.size)
    assertTrue(
        "Publications should contain all cached publications",
        result.containsAll(cachedPublications))

    // Verify no error is present
    assertNull("Error should be null after successful caching", viewModel.error.first())
  }
}
