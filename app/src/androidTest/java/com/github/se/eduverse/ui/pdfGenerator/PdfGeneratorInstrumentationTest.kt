package com.github.se.eduverse.ui.pdfGenerator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.se.eduverse.R
import com.github.se.eduverse.repository.ConvertApiRepository
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.repository.OpenAiRepository
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.repository.PdfRepositoryImpl
import com.github.se.eduverse.viewmodel.PdfGeneratorViewModel
import java.io.File
import junit.framework.TestCase
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class PdfGeneratorInstrumentationTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var pdfRepository: PdfRepository
  private lateinit var openAiRepository: OpenAiRepository
  private lateinit var context: Context
  private lateinit var pdfGeneratorViewModel: PdfGeneratorViewModel
  private lateinit var convertApiRepository: ConvertApiRepository
  private lateinit var fileRepository: FileRepository
  private lateinit var folderRepository: FolderRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    pdfRepository = PdfRepositoryImpl()
    openAiRepository = OpenAiRepository(OkHttpClient())
    convertApiRepository = ConvertApiRepository(OkHttpClient())
    fileRepository = mock(FileRepository::class.java)
    folderRepository = mock(FolderRepository::class.java)
    pdfGeneratorViewModel =
        PdfGeneratorViewModel(
            pdfRepository, openAiRepository, convertApiRepository, fileRepository, folderRepository)
  }

  @Test
  fun testConvertTextToPdf() {
    val textFile = File.createTempFile("test", ".txt")
    val testText = "This is a test text."
    textFile.writeText(testText)

    val pdfDocument: PdfDocument = pdfRepository.convertTextToPdf(Uri.fromFile(textFile), context)

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)

    assertTrue(pdfFile!!.exists())

    val extractedText = pdfRepository.readTextFromPdfFile(Uri.fromFile(pdfFile), context)

    assertEquals(testText.replace(" ", ""), extractedText.replace(" ", ""))

    textFile.delete()
    pdfFile.delete()
  }

  @Test
  fun testConvertTextToPdf_specialCharacters() {
    val textFile = File.createTempFile("test", ".txt")
    val testText = "Special characters: !@#$%^&*()_+-=<>?"
    textFile.writeText(testText)

    val pdfDocument: PdfDocument = pdfRepository.convertTextToPdf(Uri.fromFile(textFile), context)

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)

    assertTrue(pdfFile!!.exists())

    val extractedText = pdfRepository.readTextFromPdfFile(Uri.fromFile(pdfFile), context)

    assertEquals(testText.replace(" ", ""), extractedText.replace(" ", ""))

    textFile.delete()
    pdfFile.delete()
  }

  @Test
  fun testConvertTextToPdf_wrapsLines() {
    val textFile = File.createTempFile("test", ".txt")
    val testText =
        "This is a very long line that should wrap and continue on the next line in the PDF document."
            .repeat(2)
    textFile.writeText(testText)

    val pdfDocument: PdfDocument = pdfRepository.convertTextToPdf(Uri.fromFile(textFile), context)

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)

    assertTrue(pdfFile!!.exists())

    val extractedText = pdfRepository.readTextFromPdfFile(Uri.fromFile(pdfFile), context)

    val averageCharWidth = 0.6 * 12f
    val maxCharsPerLine = (pdfDocument.pages[0].pageWidth - 40 / averageCharWidth).toInt()
    val lines = extractedText.split("\n")
    for (line in lines) {
      assertTrue(line.length <= maxCharsPerLine)
    }

    assertEquals(testText.replace(" ", ""), extractedText.replace(" ", "").replace("\n", ""))

    textFile.delete()
    pdfFile.delete()
  }

  @Test
  fun testConvertTextToPdf_multiplePages() {
    val textFile = File.createTempFile("test", ".txt")
    val testText = "This is a line.\n".repeat(1000)
    textFile.writeText(testText)

    val pdfDocument: PdfDocument = pdfRepository.convertTextToPdf(Uri.fromFile(textFile), context)

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)

    assertTrue(pdfFile!!.exists())

    val extractedText = pdfRepository.readTextFromPdfFile(Uri.fromFile(pdfFile), context)

    assert(pdfDocument.pages.size > 1)

    assertEquals(
        testText.replace(" ", "").replace("\n", ""),
        extractedText.replace("\n", "").replace(" ", ""))

    textFile.delete()
    pdfFile.delete()
  }

  @Test
  fun testConvertTextToPdf_whenALineSpansMultiplePages() {
    val textFile = File.createTempFile("test", ".txt")
    val testText = "This is a very long line.".repeat(1000)
    textFile.writeText(testText)

    val pdfDocument: PdfDocument = pdfRepository.convertTextToPdf(Uri.fromFile(textFile), context)

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)

    assertTrue(pdfFile!!.exists())

    val extractedText = pdfRepository.readTextFromPdfFile(Uri.fromFile(pdfFile), context)

    assert(pdfDocument.pages.size > 1)

    assertEquals(
        testText.replace(" ", "").replace("\n", ""),
        extractedText.replace("\n", "").replace(" ", ""))

    textFile.delete()
    pdfFile.delete()
  }

  @Test
  fun testConvertImageToPdf() {
    Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).also {
      it.eraseColor(0xFFFFFFFF.toInt())
      val imageFile = File.createTempFile("test", ".png")
      it.compress(Bitmap.CompressFormat.PNG, 100, imageFile.outputStream())

      val pdfDocument: PdfDocument =
          pdfRepository.convertImageToPdf(Uri.fromFile(imageFile), context)

      val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)

      assertTrue(pdfFile!!.exists())

      assert(pdfDocument.pages.size == 1)
      assert(pdfDocument.pages[0].pageWidth == it.width)
      assert(pdfDocument.pages[0].pageHeight == it.height)

      imageFile.delete()
      pdfFile.delete()
    }
  }

  @Test
  fun testConvertImageToPdf_onGetImageBitmapError() {
    assertThrows(Exception::class.java) {
      pdfRepository.convertImageToPdf(Uri.parse("invalid uri"), context)
    }
  }

  @Test
  fun testWriteTextToPdf() {
    val text = "This is a test text."
    val pdfDocument: PdfDocument = pdfRepository.writeTextToPdf(text, context)
    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)
    assertTrue(pdfFile!!.exists())
    val extractedText = pdfRepository.readTextFromPdfFile(Uri.fromFile(pdfFile), context)
    assertEquals(text.replace(" ", ""), extractedText.replace(" ", ""))
    pdfFile.delete()
  }

  @Test
  fun testGetTempFileFromUri() {
    val tempFile = File.createTempFile("test", ".docx")
    val uri = Uri.fromFile(tempFile)

    val result = pdfRepository.getTempFileFromUri(uri, context)

    TestCase.assertTrue(result.exists())
    TestCase.assertTrue(result.name.endsWith(".docx"))
    tempFile.delete()
    result.delete()
  }

  @Test
  fun getTempFileFromUri_invalidUri_throwsException() {

    val uri = Uri.parse("content://nonexistent/file.txt")

    assertThrows(Exception::class.java) { pdfRepository.getTempFileFromUri(uri, context) }
  }

  @Test
  fun getTempFileFromUri_invalidFileType_throwsException() {
    val uri = Uri.parse("content://invalid/file.xy")

    assertThrows(Exception::class.java) { pdfRepository.getTempFileFromUri(uri, context) }
  }

  @Test
  fun testExtractTextImage_onSuccess() {
    val bitmap = context.resources.getDrawable(R.drawable.text_extraction).toBitmap()
    val imageFile = File.createTempFile("test", ".png")
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageFile.outputStream())

    pdfRepository.extractTextFromImage(
        Uri.fromFile(imageFile),
        context,
        { assertEquals("This is a test image with text", it) },
        {
          assertTrue(false) // Fail the test if onFailure is called
        })

    imageFile.delete()
  }

  @Test
  fun testExtractTextImage_onGetImageBitmapError() {
    assertThrows(Exception::class.java) {
      pdfRepository.extractTextFromImage(Uri.parse("invalid uri"), context, {}, {})
    }
  }

  @Test
  fun testReadTextFromPdfFile_readsTextSuccessfully() {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(100, 100, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint().apply { textSize = 12f }
    canvas.drawText("Hello, world!", 10f, 10f, paint)
    pdfDocument.finishPage(page)

    val tempFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)
    val text = pdfRepository.readTextFromPdfFile(Uri.fromFile(tempFile), context)

    assertEquals("Hello, world!".replace(" ", ""), text.replace(" ", ""))

    tempFile?.delete()
    pdfDocument.close()
  }

  @Test
  fun testSavePdfToDevice_failsToSaveFile() {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(100, 100, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    pdfDocument.finishPage(page)

    val tempFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)
    val destinationDirectory = File("/invalid/directory")

    pdfRepository.savePdfToDevice(
        tempFile!!,
        "testPdf",
        destinationDirectory,
        { assertTrue(false) }, // Fail the test if onSuccess is called
        { e -> assertTrue(e is Exception) })

    // Check that the temp file has been deleted after the failure
    assert(!tempFile.exists())
    pdfDocument.close()
  }

  @Test
  fun testWritePdfDocumentToTempFile_failsToWriteFile() {
    val pdfDocument = PdfDocument()
    pdfDocument.close() // Close the document to make it invalid and fail to write
    val tempFileName = "testPdf"

    val tempFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, tempFileName, context)

    assertEquals(tempFile, null)
    // Check that the created temp file has been deleted after the failure
    val cacheDir = context.externalCacheDir
    val files = cacheDir?.listFiles() ?: arrayOf()
    val containsTestPdf = files.any { it.name.contains("testPdf") && it.extension == "pdf" }
    assertFalse(containsTestPdf)
  }

  @Test
  fun testReadTextFromPdfFile_failsToReadText() {
    val invalidUri = Uri.parse("content://invalid/uri")

    val exception =
        assertThrows(Exception::class.java) {
          pdfRepository.readTextFromPdfFile(invalidUri, context)
        }

    assertEquals(
        "Failed to read text from PDF file, please try with another file.", exception.message)
  }

  @Test
  fun testReadTextFromPdfFile_failsWhenLimitIsExceeded() {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(100, 100, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint().apply { textSize = 12f }
    canvas.drawText("Hello, world!", 10f, 10f, paint)
    pdfDocument.finishPage(page)

    val tempFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf", context)
    val exception =
        assertThrows(Exception::class.java) {
          pdfRepository.readTextFromPdfFile(Uri.fromFile(tempFile), context, 5)
        }
    assertEquals(
        "The PDF file contains too much text (supported limit: 5 characters).", exception.message)
    tempFile?.delete()
    pdfDocument.close()
  }

  @Test
  fun testConvertImageToPdf_whenIOExceptionIsThrown() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val invalidUri = Uri.parse("file://nonexistent.jpg")

    try {
      pdfRepository.convertImageToPdf(invalidUri, context)
      fail("Expected an exception")
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Failed to read the image file, please try again."))
    }
  }
}
