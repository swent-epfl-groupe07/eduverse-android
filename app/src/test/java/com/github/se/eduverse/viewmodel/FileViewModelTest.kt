package com.github.se.eduverse.viewmodel

import android.net.Uri
import com.github.se.eduverse.repository.FileRepository
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

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

        // When saveFile is called, capture the success callback
        `when`(fileRepository.saveFile(any(), any(), any(), any())).then {
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
}