package com.github.se.eduverse.ui.converter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.R
import com.github.se.eduverse.repository.ConvertApiRepository
import com.github.se.eduverse.repository.OpenAiRepository
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.repository.PdfRepositoryImpl
import com.github.se.eduverse.viewmodel.PdfConverterViewModel
import java.io.File
import junit.framework.TestCase
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfConverterInstrumentationTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var pdfRepository: PdfRepository
  private lateinit var openAiRepository: OpenAiRepository
  private lateinit var context: Context
  private lateinit var pdfConverterViewModel: PdfConverterViewModel
  private lateinit var convertApiRepository: ConvertApiRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    pdfRepository = PdfRepositoryImpl()
    openAiRepository = OpenAiRepository(OkHttpClient())
    convertApiRepository = ConvertApiRepository(OkHttpClient())
    pdfConverterViewModel =
        PdfConverterViewModel(pdfRepository, openAiRepository, convertApiRepository)
  }

  @Test
  fun testConvertTextToPdf() {
    val textFile = File.createTempFile("test", ".txt")
    val testText = "This is a test text."
    textFile.writeText(testText)

    val pdfDocument: PdfDocument = pdfRepository.convertTextToPdf(Uri.fromFile(textFile), context)

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf")

    assertTrue(pdfFile.exists())

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

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf")

    assertTrue(pdfFile.exists())

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

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf")

    assertTrue(pdfFile.exists())

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

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf")

    assertTrue(pdfFile.exists())

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

      val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf")

      assertTrue(pdfFile.exists())

      assert(pdfDocument.pages.size == 1)
      assert(pdfDocument.pages[0].pageWidth == it.width)
      assert(pdfDocument.pages[0].pageHeight == it.height)

      imageFile.delete()
      pdfFile.delete()
    }
  }

  @Test
  fun testWriteTextToPdf() {
    val text = "This is a test text."
    val pdfDocument: PdfDocument = pdfRepository.writeTextToPdf(text)
    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf")
    assertTrue(pdfFile.exists())
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
  fun testExtractTextImage_onImageWithoutText() {
    Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).also {
      it.eraseColor(0xFFFFFFFF.toInt())
      val imageFile = File.createTempFile("test", ".png")
      it.compress(Bitmap.CompressFormat.PNG, 100, imageFile.outputStream())

      pdfRepository.extractTextFromImage(
          Uri.fromFile(imageFile),
          context,
          {},
          { e -> assertEquals(Exception("No text found on image"), e) })

      imageFile.delete()
    }
  }

  @Test
  fun testExtractTextImage_onGetImageBitmapError() {
    pdfRepository.extractTextFromImage(
        Uri.parse("invalid uri"),
        context,
        {},
        { assertEquals("Failed to get image bitmap", it.message) })
  }
}
