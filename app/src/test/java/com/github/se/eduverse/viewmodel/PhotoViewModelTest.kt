package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.repository.IPhotoRepository
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
class PhotoViewModelTest {

  @get:Rule private lateinit var photoRepositoryMock: IPhotoRepository
  private lateinit var photoViewModel: PhotoViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    photoRepositoryMock = mock()
    photoViewModel = PhotoViewModel(photoRepositoryMock)
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun `savePhoto updates savePhotoState to true on success`() = runTest {
    // Arrange
    val photo = Photo("ownerId", byteArrayOf(0x01, 0x02), "path")
    whenever(photoRepositoryMock.savePhoto(photo)).thenReturn(true)

    // Act
    photoViewModel.savePhoto(photo)
    advanceUntilIdle()

    // Assert
    assertEquals(true, photoViewModel.savePhotoState.first())
  }

  @Test
  fun `savePhoto updates savePhotoState to false on failure`() = runTest {
    // Arrange
    val photo = Photo("ownerId", byteArrayOf(0x01, 0x02), "path")
    whenever(photoRepositoryMock.savePhoto(photo)).thenReturn(false)

    // Act
    photoViewModel.savePhoto(photo)
    advanceUntilIdle()

    // Assert
    assertEquals(false, photoViewModel.savePhotoState.first())
  }

  @Test
  fun `PhotoViewModelFactory creates PhotoViewModel with correct repository`() {
    // Arrange
    val factory = PhotoViewModelFactory(photoRepositoryMock)

    // Act
    val viewModel = factory.create(PhotoViewModel::class.java)

    // Assert
    assert(viewModel is PhotoViewModel)
  }
}
