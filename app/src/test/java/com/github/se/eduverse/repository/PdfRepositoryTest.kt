package com.github.se.eduverse.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.InputStream
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class PdfRepositoryTest {
  private lateinit var context: Context
  private lateinit var contentResolver: ContentResolver
  private lateinit var fileUri: Uri
  private lateinit var imageUri: Uri
  private lateinit var mockInputStream: InputStream
  private lateinit var pdfRepository: PdfRepository

  @Before
  fun setup() {
    context = mock(Context::class.java)
    contentResolver = mock(ContentResolver::class.java)
    fileUri = mock(Uri::class.java)
    imageUri = mock(Uri::class.java)
    mockInputStream = mock(InputStream::class.java)
    pdfRepository = PdfRepositoryImpl()

    `when`(context.contentResolver).thenReturn(contentResolver)
  }

  @After
  fun tearDown() {
    val destinationDirectory = File(System.getProperty("java.io.tmpdir"))
    destinationDirectory.deleteRecursively()
  }

  @Test
  fun `test savePdfToDevice saves file successfully`() {
    val fileName = "test"
    val pdfFile = File.createTempFile(fileName, ".pdf")
    val destinationDirectory = File(System.getProperty("java.io.tmpdir"))

    pdfRepository.savePdfToDevice(
        pdfFile,
        fileName,
        destinationDirectory,
        { savedFile ->
          assertTrue(savedFile.exists())
          assertEquals(savedFile.parentFile, destinationDirectory)
        },
        { e ->
          assertTrue(false) // Fail the test if onFailure is called
        })

    val pdfFile2 = File.createTempFile(fileName, ".pdf")
    pdfRepository.savePdfToDevice(
        pdfFile2,
        fileName,
        destinationDirectory,
        { savedFile ->
          assertTrue(savedFile.exists())
          assertEquals(savedFile.parentFile, destinationDirectory)
          // Test create unique file works as expected
          assertEquals(savedFile.name, fileName + "(1).pdf")
        },
        { e ->
          assertTrue(false) // Fail the test if onFailure is called
        })
  }

  @Test
  fun `test writePdfDocumentToTempFile writes file successfully`() {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(100, 100, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    pdfDocument.finishPage(page)
    val tempFileName = "testPdf"

    val tempFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, tempFileName, context)

    assertTrue(tempFile!!.exists())
    assertTrue(tempFile.name.startsWith(tempFileName))
    assertTrue(tempFile.extension == "pdf")
    tempFile.delete()

    pdfDocument.close()
  }

  @Test
  fun `test deleteTempPdfFile deletes file successfully`() {
    val tempFile = File.createTempFile("test", ".pdf")
    assertTrue(tempFile.exists())

    pdfRepository.deleteTempPdfFile(tempFile)

    assertTrue(!tempFile.exists())
  }
}
