package com.github.se.eduverse.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.github.se.eduverse.repository.FileRepository
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.StorageReference
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class FileViewModelTest {

  private lateinit var fileRepository: FileRepository
  private lateinit var fileViewModel: FileViewModel

  @Before
  fun setUp() {
    fileRepository = mock(FileRepository::class.java)
    fileViewModel = FileViewModel(fileRepository)
  }

  @Test
  fun testCreateFile() {

    // When savePdfFile is called, capture the success callback
    `when`(fileRepository.savePdfFile(any(), any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(2)
      callback()
    }
    `when`(fileRepository.getNewUid()).thenReturn("uid")

    // Create Uri
    val uri = mock(Uri::class.java)
    `when`(uri.lastPathSegment).thenReturn("testFile.pdf")

    // Assert
    assertEquals(false, fileViewModel.validNewFile.value)
    assertEquals(null, fileViewModel.getNewFile())

    fileViewModel.createFile(uri)

    // Assert the values after success callback
    assertEquals(true, fileViewModel.validNewFile.value)
    assertEquals("testFile.pdf", fileViewModel.getNewFile()!!.name)
    assertEquals(false, fileViewModel.validNewFile.value)

    fileViewModel.createFile(uri)
    fileViewModel.setName("newName.pdf")

    assertEquals(true, fileViewModel.validNewFile.value)
    assertEquals("newName.pdf", fileViewModel.getNewFile()!!.name)
    assertEquals(false, fileViewModel.validNewFile.value)
  }

  @Test
  fun testOpenFile() {
    val storageReference = mock(StorageReference::class.java)
    val context = mock(Context::class.java)
    val task = mock(FileDownloadTask::class.java)

    var test = false

    `when`(fileRepository.accessFile(any(), any(), any())).then {
      val callback = it.getArgument<(StorageReference, String) -> Unit>(1)
      callback(storageReference, ".pdf")
    }
    `when`(storageReference.getFile(any<File>())).thenReturn(task)
    `when`(task.addOnSuccessListener(any())).thenReturn(task)
    `when`(task.addOnFailureListener(any())).then {
      test = true
      null
    }

    fileViewModel.openFile("", context)

    assert(test)

    verify(context, times(0)).startActivity(any())
  }

  @Test
  fun displayFileTest() {
    val context = mock(Context::class.java)
    val file = File.createTempFile("test", ".pdf")
    val intent = mock(Intent::class.java)
    val uri = mock(Uri::class.java)
    val name = mock(ComponentName::class.java)
    val packageManager = mock(PackageManager::class.java)

    var test = false

    `when`(context.packageManager).thenReturn(packageManager)
    `when`(intent.setDataAndType(any(), any())).thenReturn(intent)
    `when`(intent.resolveActivity(any())).thenReturn(name)
    `when`(context.startActivity(any())).then {
      test = true
      null
    }

    fileViewModel.displayFile(file, context, intent, uri)
    assert(test)
  }

  @Test
  fun deleteFileTest() {
    fileViewModel.deleteFile("fileId", {})

    verify(fileRepository, times(1)).deleteFile(any(), any(), any())
  }
}
