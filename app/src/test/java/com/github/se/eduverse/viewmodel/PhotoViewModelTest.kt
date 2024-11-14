package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.repository.IPhotoRepository
import com.github.se.eduverse.repository.FileRepository
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
  @get:Rule private lateinit var fileRepositoryMock: FileRepository
  private lateinit var photoViewModel: PhotoViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    photoRepositoryMock = mock()
    fileRepositoryMock = mock()
    photoViewModel = PhotoViewModel(photoRepositoryMock, fileRepositoryMock)
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
  fun `makeFileFromPhoto create a file with correct path`() = runTest {
    // Arrange
    val photo = Photo("ownerId", byteArrayOf(0x01, 0x02), "path")
    whenever(fileRepositoryMock.getNewUid()).thenReturn("uid")

    // Act
    photoViewModel.makeFileFromPhoto(photo) {}
    advanceUntilIdle()

    // Assert
    verify(fileRepositoryMock).savePathToFirestore(eq("path"), eq(".jpg"), eq("uid"), any())
  }

  @Test
  fun `savePhoto create if folder not null`() = runTest {
    // Arrange
    val photo = Photo("ownerId", byteArrayOf(0x01, 0x02), "path")
    whenever(photoRepositoryMock.savePhoto(photo)).thenReturn(true)

    val folder = Folder("uid", emptyList<MyFile>().toMutableList(), "name", "id", archived = false)
    whenever(fileRepositoryMock.getNewUid()).thenReturn("fileUid")
    whenever(fileRepositoryMock.savePathToFirestore(eq("path"), eq(".jpg"), eq("fileUid"), any()))
        .then {
          val callback = it.getArgument<() -> Unit>(3)
          callback()
        }

    // Act
    var test = false
    photoViewModel.savePhoto(photo, folder) { _, _, _ -> test = true }
    advanceUntilIdle()

    // Assert
    assert(test)
  }

  @Test
  fun `PhotoViewModelFactory creates PhotoViewModel with correct repository`() {
    // Arrange
    val factory = PhotoViewModelFactory(photoRepositoryMock, fileRepositoryMock)

    // Act
    val viewModel = factory.create(PhotoViewModel::class.java)

    // Assert
    assert(viewModel is PhotoViewModel)
  }
}
