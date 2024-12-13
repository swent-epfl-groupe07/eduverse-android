package com.github.se.eduverse.viewmodel

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.repository.ConvertApiRepository
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.repository.OpenAiRepository
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.ui.pdfGenerator.PdfGeneratorOption
import java.io.File
import java.util.Calendar
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowToast

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
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
  private lateinit var fileRepository: FileRepository
  private lateinit var folderRepository: FolderRepository

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
    fileRepository = mock(FileRepository::class.java)
    folderRepository = mock(FolderRepository::class.java)
    viewModel =
        PdfGeneratorViewModel(
            pdfRepository, openAiRepository, convertApiRepository, fileRepository, folderRepository, mockIoDispatcher)
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

    assertEquals(PdfGeneratorViewModel.PdfGenerationState.Ready, viewModel.pdfGenerationState.value)
  }

  @Test
  fun `test savePdfToDevice on success`() {
    val mockContext = RuntimeEnvironment.getApplication()
    val fileName = "test"
    `when`(pdfRepository.savePdfToDevice(any(), any(), any(), any(), any())).then {
      val pdfFile = File("${it.getArgument<String>(1)}.pdf")
      it.getArgument<(File) -> Unit>(3)(pdfFile)
    }
    viewModel.setNewFileName(fileName)
    viewModel.savePdfToDevice(file, mockContext, viewModel.DEFAULT_DESTINATION_DIRECTORY)
    val latestToast = ShadowToast.getTextOfLatestToast()
    assertEquals(
        "$fileName.pdf saved to device folder: ${viewModel.DEFAULT_DESTINATION_DIRECTORY.name}",
        latestToast)
  }

  @Test
  fun `test savePdfToDevice on failure`() {
    val mockContext = RuntimeEnvironment.getApplication()
    val fileName = "test"
    `when`(pdfRepository.savePdfToDevice(any(), any(), any(), any(), any())).then {
      it.getArgument<(Exception) -> Unit>(4)(Exception())
    }
    viewModel.setNewFileName(fileName)
    viewModel.savePdfToDevice(file, mockContext, viewModel.DEFAULT_DESTINATION_DIRECTORY)
    val latestToast = ShadowToast.getTextOfLatestToast()
    assertEquals(
        "Failed to save generated PDF to device folder: ${viewModel.DEFAULT_DESTINATION_DIRECTORY.name}",
        latestToast)
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

  @Test
  fun `test savePdfToFolder calls repository`() {
    val folder = Folder("uid", mutableListOf(), "folder", "1", archived = false)
    val fileId = "testFileId"
    val newFileName = "test.pdf"

    `when`(fileRepository.getNewUid()).thenReturn(fileId)
    `when`(folderRepository.updateFolder(any(), any(), any())).then {
      val newFolder = it.getArgument<Folder>(0)

      assertEquals(folder.name, newFolder.name)
      assertEquals(folder.ownerID, newFolder.ownerID)
      assertEquals(folder.id, newFolder.id)
      assertEquals(folder.archived, newFolder.archived)
      assertEquals(1, newFolder.files.size)
      assertEquals(newFileName, newFolder.files[0].name)
      assertEquals(fileId, newFolder.files[0].fileId)
    }
    `when`(fileRepository.savePdfFile(any(), any(), any(), any())).then {
      it.getArgument<() -> Unit>(2)()
    }

    viewModel.setNewFileName("test")
    viewModel.savePdfToFolder(folder, uri, context, {}, {})
  }

  @Test
  fun `test savePdfToFolder with existing file name`() {
    val existingFile =
        MyFile("", "existingFileId", "test.pdf", Calendar.getInstance(), Calendar.getInstance(), 0)
    val folder = Folder("uid", mutableListOf(existingFile), "folder", "1", archived = false)
    val newFileName = "test(1).pdf"
    val fileId = "testFileId"

    `when`(fileRepository.getNewUid()).thenReturn(fileId)
    `when`(folderRepository.updateFolder(any(), any(), any())).then {
      val newFolder = it.getArgument<Folder>(0)
      assertEquals(folder.name, newFolder.name)
      assertEquals(folder.ownerID, newFolder.ownerID)
      assertEquals(folder.id, newFolder.id)
      assertEquals(folder.archived, newFolder.archived)
      assertEquals(2, newFolder.files.size)
      assertEquals(newFileName, newFolder.files[1].name)
      assertEquals(fileId, newFolder.files[1].fileId)
    }
    `when`(fileRepository.savePdfFile(any(), any(), any(), any())).then {
      it.getArgument<() -> Unit>(2)()
    }

    viewModel.setNewFileName("test")
    viewModel.savePdfToFolder(folder, uri, context, {}, {})
  }
}
