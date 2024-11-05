package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.model.Video
import com.github.se.eduverse.model.repository.IVideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModelTest {

  @get:Rule private lateinit var videoRepositoryMock: IVideoRepository
  private lateinit var videoViewModel: VideoViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    videoRepositoryMock = mock()
    videoViewModel = VideoViewModel(videoRepositoryMock)
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun `saveVideo updates saveVideoState to true on success`() = runTest {
    // Arrange
    val video = Video("ownerId", byteArrayOf(0x01, 0x02), "videos/path.mp4")
    whenever(videoRepositoryMock.saveVideo(video)).thenReturn(true)

    // Act
    videoViewModel.saveVideo(video)
    advanceUntilIdle()

    // Assert
    assertEquals(true, videoViewModel.saveVideoState.first())
  }

  @Test
  fun `saveVideo updates saveVideoState to false on failure`() = runTest {
    // Arrange
    val video = Video("ownerId", byteArrayOf(0x01, 0x02), "videos/path.mp4")
    whenever(videoRepositoryMock.saveVideo(video)).thenReturn(false)

    // Act
    videoViewModel.saveVideo(video)
    advanceUntilIdle()

    // Assert
    assertEquals(false, videoViewModel.saveVideoState.first())
  }

  @Test
  fun `getVideosByOwner updates videos list on success`() = runTest {
    // Arrange
    val ownerId = "ownerId"
    val videoList =
        listOf(
            Video("ownerId", byteArrayOf(0x01, 0x02), "videos/path1.mp4"),
            Video("ownerId", byteArrayOf(0x03, 0x04), "videos/path2.mp4"))
    whenever(videoRepositoryMock.getVideosByOwner(ownerId)).thenReturn(videoList)

    // Act
    videoViewModel.getVideosByOwner(ownerId)
    advanceUntilIdle()

    // Assert
    assertEquals(videoList, videoViewModel.videos.first())
  }

  @Test
  fun `VideoViewModelFactory creates VideoViewModel with correct repository`() {
    // Arrange
    val factory = VideoViewModelFactory(videoRepositoryMock)

    // Act
    val viewModel = factory.create(VideoViewModel::class.java)

    // Assert
    assert(viewModel is VideoViewModel)
  }
}
