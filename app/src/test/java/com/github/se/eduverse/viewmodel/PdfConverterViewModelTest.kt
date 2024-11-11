package com.github.se.eduverse.viewmodel

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.ui.converter.PdfConverterOption
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

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PdfConverterViewModelTest {

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var viewModel: PdfConverterViewModel
  private lateinit var pdfRepository: PdfRepository
  private lateinit var context: Context
  private lateinit var uri: Uri
  private lateinit var file: File

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    pdfRepository = mock(PdfRepository::class.java)
    context = mock(Context::class.java)
    uri = mock(Uri::class.java)
    file = mock(File::class.java)
    viewModel = PdfConverterViewModel(pdfRepository)
  }

  @Test
  fun `test setNewFileName sanitizes file name`() {
    viewModel.setNewFileName("sanitized/test@file#name.pdf")
    assertEquals("sanitized_test_file_name.pdf", viewModel.newFileName.value)
  }

  @Test
  fun `test generatePdf when conversion error`() = runTest {
    `when`(pdfRepository.convertImageToPdf(uri, context)).then { throw Exception() }
    viewModel.generatePdf(uri, context, PdfConverterOption.IMAGE_TO_PDF)
    advanceUntilIdle()
    assertEquals(PdfConverterViewModel.PdfGenerationState.Error, viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with IMAGE_TO_PDF option`() = runTest {
    val pdfDocument = mock(PdfDocument::class.java)
    `when`(pdfRepository.convertImageToPdf(uri, context)).thenReturn(pdfDocument)
    `when`(pdfRepository.writePdfDocumentToTempFile(pdfDocument, "test")).thenReturn(file)

    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfConverterOption.IMAGE_TO_PDF)
    advanceUntilIdle()
    assertEquals(
        PdfConverterViewModel.PdfGenerationState.Success(file), viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test generatePdf with TEXT_TO_PDF option`() = runTest {
    val pdfDocument = mock(PdfDocument::class.java)
    `when`(pdfRepository.convertTextToPdf(uri, context)).thenReturn(pdfDocument)
    `when`(pdfRepository.writePdfDocumentToTempFile(pdfDocument, "test")).thenReturn(file)

    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfConverterOption.TEXT_TO_PDF)
    advanceUntilIdle()
    assertEquals(
        PdfConverterViewModel.PdfGenerationState.Success(file), viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test setNewFileName with empty file name`() {
    viewModel.setNewFileName("")
    assertEquals("", viewModel.newFileName.value)
  }

  @Test
  fun `test generatePdf with NONE option throws exception`() = runTest {
    viewModel.generatePdf(uri, context, PdfConverterOption.NONE)
    advanceUntilIdle()
    assertEquals(PdfConverterViewModel.PdfGenerationState.Error, viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test setPdfGenerationStateToReady resets state and deletes file`() {
    viewModel.currentFile = file
    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfConverterOption.IMAGE_TO_PDF)
    viewModel.setPdfGenerationStateToReady()

    verify(pdfRepository).deleteTempPdfFile(any())
    assertEquals(PdfConverterViewModel.PdfGenerationState.Ready, viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test abortPdfGeneration cancels job and sets state`() = runTest {
    viewModel.setNewFileName("test")
    viewModel.generatePdf(uri, context, PdfConverterOption.IMAGE_TO_PDF)
    viewModel.abortPdfGeneration()

    assertTrue(viewModel.pdfGenerationJob?.isCancelled == true)
    assertEquals(
        PdfConverterViewModel.PdfGenerationState.Aborted, viewModel.pdfGenerationState.value)
  }
}
