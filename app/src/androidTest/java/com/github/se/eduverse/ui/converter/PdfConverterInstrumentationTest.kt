package com.github.se.eduverse.ui.converter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.repository.PdfRepositoryImpl
import com.github.se.eduverse.viewmodel.PdfConverterViewModel
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import java.io.File
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
  private lateinit var context: Context
  private lateinit var pdfConverterViewModel: PdfConverterViewModel

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    pdfRepository = PdfRepositoryImpl()
    pdfConverterViewModel = PdfConverterViewModel(pdfRepository)
  }

  @Test
  fun testConvertTextToPdf() {
    val textFile = File.createTempFile("test", ".txt")
    val testText = "This is a test text."
    textFile.writeText(testText)

    val pdfDocument: PdfDocument = pdfRepository.convertTextToPdf(Uri.fromFile(textFile), context)

    val pdfFile = pdfRepository.writePdfDocumentToTempFile(pdfDocument, "testPdf")

    assertTrue(pdfFile.exists())

    val pdfReader = PdfReader(pdfFile.inputStream())
    val pdfTextExtractor = PdfTextExtractor(pdfReader)
    val extractedText = pdfTextExtractor.getTextFromPage(1)

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

    val pdfReader = PdfReader(pdfFile.inputStream())
    val pdfTextExtractor = PdfTextExtractor(pdfReader)
    val extractedText = pdfTextExtractor.getTextFromPage(1)

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

    val pdfReader = PdfReader(pdfFile.inputStream())
    val pdfTextExtractor = PdfTextExtractor(pdfReader)
    val extractedText = pdfTextExtractor.getTextFromPage(1)

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

    val pdfReader = PdfReader(pdfFile.inputStream())
    val pdfTextExtractor = PdfTextExtractor(pdfReader)
    var extractedText = ""
    for (i in 1..pdfDocument.pages.size) {
      extractedText += pdfTextExtractor.getTextFromPage(i)
    }

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
}
