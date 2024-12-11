package com.github.se.eduverse.viewmodel

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.se.eduverse.repository.ConvertApiRepository
import com.github.se.eduverse.repository.OpenAiRepository
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.ui.pdfGenerator.PdfGeneratorOption
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PdfGeneratorViewModelTest {

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var viewModel: PdfGeneratorViewModel
  private lateinit var pdfRepository: PdfRepository
  private lateinit var context: Context
  private lateinit var uri: Uri
  private lateinit var file: File
  private lateinit var openAiRepository: OpenAiRepository
  private lateinit var pdfDocument: PdfDocument
  private lateinit var convertApiRepository: ConvertApiRepository

  private val testDispatcher = StandardTestDispatcher()
  private val mockIoDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    pdfRepository = mock(PdfRepository::class.java)
    context = mock(Context::class.java)
    uri = mock(Uri::class.java)
    file = mock(File::class.java)
    openAiRepository = mock(OpenAiRepository::class.java)
    convertApiRepository = mock(ConvertApiRepository::class.java)
    viewModel =
        PdfGeneratorViewModel(
            pdfRepository, openAiRepository, convertApiRepository, mockIoDispatcher)
    pdfDocument = mock(PdfDocument::class.java)
    `when`(pdfRepository.writePdfDocumentToTempFile(pdfDocument, "test", context)).thenReturn(file)
    `when`(pdfRepository.readTextFromPdfFile(uri, context, viewModel.MAX_SUMMARY_INPUT_SIZE))
        .thenReturn("This is a test text.")
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `test setNewFileName sanitizes file name`() {
    viewModel.setNewFileName("sanitized/test@file#name.pdf")
    assertEquals("sanitized_test_file_name.pdf", viewModel.newFileName.value)
  }

  @Test
  fun `test generatePdf when conversion error`() = runTest {
    `when`(pdfRepository.convertImageToPdf(uri, context)).then {
      throw Exception("Image conversion failed")
    }
    viewModel.generatePdf(uri, context, PdfGeneratorOption.IMAGE_TO_PDF)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Error("Image conversion failed"),
        viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with IMAGE_TO_PDF option`() = runTest {
    `when`(pdfRepository.convertImageToPdf(uri, context)).thenReturn(pdfDocument)

    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.IMAGE_TO_PDF)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Success(file), viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with TEXT_TO_PDF option`() = runTest {
    `when`(pdfRepository.convertTextToPdf(uri, context)).thenReturn(pdfDocument)

    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.TEXT_TO_PDF)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Success(file), viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with DOCUMENT_TO_PDF option`() = runTest {
    val document = mock(File::class.java)
    `when`(pdfRepository.getTempFileFromUri(uri, context)).thenReturn(document)
    `when`(convertApiRepository.convertToPdf(any(), any(), any())).thenReturn(file)
    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.DOCUMENT_TO_PDF)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Success(file), viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with DOCUMENT_TO_PDF option on conversion failure`() = runTest {
    val tempFile = mock<File>()
    `when`(pdfRepository.getTempFileFromUri(uri, context)).thenReturn(tempFile)
    `when`(convertApiRepository.convertToPdf(tempFile, "test", context)).then {
      throw (Exception("Conversion error"))
    }

    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.DOCUMENT_TO_PDF)
    advanceUntilIdle()

    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Error(
            "Document conversion failed, please try again"),
        viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with SUMMARIZE_FILE option`() = runTest {
    val summary = "This is a summarized text."
    `when`(pdfRepository.writeTextToPdf(summary, context)).thenReturn(pdfDocument)
    `when`(openAiRepository.summarizeText(any(), any(), any())).then {
      it.getArgument<(String?) -> Unit>(1)(summary)
    }
    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.SUMMARIZE_FILE)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Success(file), viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with SUMMARIZE_FILE option on null result`() = runTest {
    `when`(openAiRepository.summarizeText(any(), any(), any())).then {
      it.getArgument<(String?) -> Unit>(1)(null)
    }

    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.SUMMARIZE_FILE)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Error("Summarization failed, please try again"),
        viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with SUMMARIZE_FILE option on summarization failure`() = runTest {
    `when`(openAiRepository.summarizeText(any(), any(), any())).then {
      it.getArgument<(Exception) -> Unit>(2)(Exception())
    }

    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.SUMMARIZE_FILE)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Error("Summarization failed, please try again"),
        viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with EXTRACT_TEXT option on extraction failure`() = runTest {
    `when`(pdfRepository.extractTextFromImage(any(), any(), any(), any())).then {
      it.getArgument<(Exception) -> Unit>(3)(Exception("Text extraction failed"))
    }

    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.EXTRACT_TEXT)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Error("Text extraction failed"),
        viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with EXTRACT_TEXT option on extraction success`() = runTest {
    val extractedText = "Test extracted text."
    `when`(pdfRepository.writeTextToPdf(extractedText, context)).thenReturn(pdfDocument)
    `when`(pdfRepository.extractTextFromImage(any(), any(), any(), any())).then {
      it.getArgument<(String) -> Unit>(2)(extractedText)
    }

    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.EXTRACT_TEXT)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Success(file), viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test setNewFileName with empty file name`() {
    viewModel.setNewFileName("")
    assertEquals("", viewModel.newFileName.value)
  }

  @Test
  fun `test generatePdf with NONE option throws exception`() = runTest {
    viewModel.generatePdf(uri, context, PdfGeneratorOption.NONE)
    advanceUntilIdle()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Error("No converter option selected"),
        viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test setPdfGenerationStateToReady resets state and deletes file`() {
    viewModel.currentFile = file
    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.IMAGE_TO_PDF)
    viewModel.setPdfGenerationStateToReady()

    verify(pdfRepository).deleteTempPdfFile(any())
    assertEquals(PdfGeneratorViewModel.PdfGenerationState.Ready, viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test savePdfToDevice calls repository`() {
    val pdfFile = mock(File::class.java)
    val fileName = "test.pdf"

    viewModel.setNewFileName(fileName)
    viewModel.savePdfToDevice(pdfFile, context, file)

    verify(pdfRepository).savePdfToDevice(eq(pdfFile), eq(fileName), eq(file), any(), any())
  }

  @Test
  fun `test abortPdfGeneration cancels job and sets state`() = runTest {
    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.IMAGE_TO_PDF)
    viewModel.abortPdfGeneration()

    assertTrue(viewModel.pdfGenerationJob?.isCancelled == true)
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Aborted, viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test abortPdfGeneration calls deleteTempPdfFile`() = runTest {
    viewModel.currentFile = file
    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfGeneratorOption.IMAGE_TO_PDF)
    viewModel.abortPdfGeneration()
    verify(pdfRepository).deleteTempPdfFile(eq(file))
  }
}
