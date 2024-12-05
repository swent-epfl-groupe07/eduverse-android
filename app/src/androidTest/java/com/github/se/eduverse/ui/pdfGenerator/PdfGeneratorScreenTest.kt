import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.api.SUPPORTED_CONVERSION_TYPES
import com.github.se.eduverse.repository.ConvertApiRepository
import com.github.se.eduverse.repository.OpenAiRepository
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.pdfGenerator.PdfConverterOption
import com.github.se.eduverse.ui.pdfGenerator.PdfGeneratorScreen
import com.github.se.eduverse.ui.pdfGenerator.PdfNameInputDialog
import com.github.se.eduverse.viewmodel.PdfGeneratorViewModel
import java.io.File
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class PdfGeneratorScreenTest {

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var pdfGeneratorViewModel: PdfGeneratorViewModel
  private lateinit var mockPdfRepository: PdfRepository
  private lateinit var mockOpenAiRepository: OpenAiRepository
  private lateinit var mockConvertApiRepository: ConvertApiRepository

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    Intents.init()

    mockNavigationActions = mock(NavigationActions::class.java)
    mockPdfRepository = mock(PdfRepository::class.java)
    mockOpenAiRepository = mock(OpenAiRepository::class.java)
    mockConvertApiRepository = mock(ConvertApiRepository::class.java)
    pdfGeneratorViewModel =
        PdfGeneratorViewModel(mockPdfRepository, mockOpenAiRepository, mockConvertApiRepository)

    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.PDF_GENERATOR)
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  @Test
  fun topNavigationBarIsCorrectlyDisplayed() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertTextEquals("PDF Converter")
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").performClick()
    verify(mockNavigationActions).goBack()
  }

  @Test
  fun allPdfConverterOptionsAreCorrectlyDisplayed() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name)
        .assertTextContains("Text to PDF")
        .assertTextContains("Converts a .txt file to PDF")
    composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name)
        .assertTextContains("Image to PDF")
        .assertTextContains("Converts an image to PDF")
    composeTestRule.onNodeWithTag(PdfConverterOption.DOCUMENT_TO_PDF.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfConverterOption.DOCUMENT_TO_PDF.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.DOCUMENT_TO_PDF.name)
        .assertTextContains("Doc to PDF")
        .assertTextContains("Converts a document to PDF")
    composeTestRule.onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name)
        .assertTextContains("Summarize file")
        .assertTextContains("Generates a summary of a file")
    composeTestRule.onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name)
        .assertTextContains("Extract text")
        .assertTextContains("Extracts text from an image")
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
  }

  @Test
  fun testPdfNameInputDialog_isCorrectlyDisplayed() {
    composeTestRule.setContent {
      PdfNameInputDialog(pdfFileName = "test.pdf", onDismiss = {}, onConfirm = {})
    }

    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdfNameInput").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdfNameInput").assertTextContains("test.pdf")
    composeTestRule.onNodeWithTag("pdfNameInput").assertTextContains("PDF File Name")
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").assertTextEquals("Create PDF")
    composeTestRule.onNodeWithTag("dismissCreatePdfButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dismissCreatePdfButton").assertTextEquals("Cancel")
  }

  @Test
  fun testPdfNameInputDialog_confirmButtonDisabledWithEmptyFileName() {
    composeTestRule.setContent {
      PdfNameInputDialog(pdfFileName = "", onDismiss = {}, onConfirm = {})
    }

    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").assertIsNotEnabled()
  }

  @Test
  fun testPdfNameInputDialog_confirmButtonWithInputText() {
    var confirmedFileName = ""
    composeTestRule.setContent {
      PdfNameInputDialog(pdfFileName = "", onDismiss = {}, onConfirm = { confirmedFileName = it })
    }
    composeTestRule.onNodeWithTag("pdfNameInput").performTextInput("test.pdf")
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    assert(confirmedFileName == "test.pdf")
  }

  @Test
  fun testPdfNameInputDialog_confirmWithoutInputKeepsDefaultFileName() {
    var confirmedFileName = ""
    composeTestRule.setContent {
      PdfNameInputDialog(
          pdfFileName = "test.pdf", onDismiss = {}, onConfirm = { confirmedFileName = it })
    }

    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    assert(confirmedFileName == "test.pdf")
  }

  @Test
  fun testPdfNameInputDialog_callsOnDismissWhenCancelPressed() {
    var dismissed = false
    composeTestRule.setContent {
      PdfNameInputDialog(pdfFileName = "test.pdf", onDismiss = { dismissed = true }, onConfirm = {})
    }

    composeTestRule.onNodeWithTag("dismissCreatePdfButton").performClick()
    assert(dismissed)
  }

  @Test
  fun clickingTextToPdfOption_correctlyDisplaysInfoWindow() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("Text to PDF converter")
    composeTestRule
        .onNodeWithTag("infoWindowText")
        .assertTextEquals("Select a .txt file to convert to PDF")
    composeTestRule.onNodeWithTag("infoWindowDismissButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowDismissButton").assertTextContains("Cancel")
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").assertTextContains("Select file")
    composeTestRule.onNodeWithTag("infoWindowDismissButton").performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsNotDisplayed()
  }

  @Test
  fun clickingImageToPdfOption_correctlyDisplaysInfoWindow() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("Image to PDF converter")
    composeTestRule
        .onNodeWithTag("infoWindowText")
        .assertTextEquals("Select an image to convert to PDF")
  }

  @Test
  fun clickingSummarizeFileOption_correctlyDisplaysInfoWindow() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name).performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("Pdf file summarizer")
    composeTestRule
        .onNodeWithTag("infoWindowText")
        .assertTextEquals(
            "Select a PDF file to summarize. The summary will be generated in a PDF file")
  }

  @Test
  fun clickingDocumentToPdfOption_correctlyDisplaysInfoWindow() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfConverterOption.DOCUMENT_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("Document to PDF converter")
    composeTestRule
        .onNodeWithTag("infoWindowText")
        .assertTextEquals(
            "Select a document to convert to PDF. Supported document types are: ${
      SUPPORTED_CONVERSION_TYPES.joinToString(
        ", "
      )
    }")
  }

  @Test
  fun clickingExtractTextOption_correctlyDisplaysInfoWindow() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name).performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("Text extractor")
    composeTestRule
        .onNodeWithTag("infoWindowText")
        .assertTextEquals(
            "Select an image to extract text from. Make sure the selected image contains text. The extracted text will be generated in a PDF file")
  }

  @Test
  fun selectSourceFileDialogIsCorrectlyDisplayed() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectSourceFileDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("appFoldersButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("appFoldersButton").assertTextContains("App folders")
    composeTestRule.onNodeWithTag("appFoldersButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("deviceStorageButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("deviceStorageButton").assertTextContains("Device storage")
    composeTestRule.onNodeWithTag("deviceStorageButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("selectSourceFileDismissButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("selectSourceFileDismissButton").assertTextContains("Cancel")
    composeTestRule.onNodeWithTag("selectSourceFileDismissButton").performClick()
    composeTestRule.onNodeWithTag("selectSourceFileDialog").assertIsNotDisplayed()
  }

  @Test
  fun clickingDeviceStorageButtonInSelectSourceFileDialog_launchesFilePicker() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    // Set up the activity result for the intent
    // Simulate the file picker intent
    val expectedUri = Uri.parse("content://test-document-uri")
    val resultIntent = Intent().apply { data = expectedUri }
    Intent().apply { data = expectedUri }
    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))
    composeTestRule.onNodeWithTag(PdfConverterOption.DOCUMENT_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").performClick()
    composeTestRule.onNodeWithTag("deviceStorageButton").performClick()
    composeTestRule.onNodeWithTag("selectSourceFileDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dismissCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
  }

  @Test
  fun pdfGenerationStateIsSetToReadyOnSuccess() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    // Set up the activity result for the intent
    // Simulate the file picker intent
    val expectedUri = Uri.parse("content://test-image-uri")
    val resultIntent = Intent().apply { data = expectedUri }
    Intent().apply { data = expectedUri }
    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))
    `when`(mockPdfRepository.convertImageToPdf(any(), any())).thenReturn(PdfDocument())
    `when`(mockPdfRepository.writePdfDocumentToTempFile(any(), any())).thenReturn(File("test.pdf"))
    `when`(mockPdfRepository.savePdfToDevice(any(), any(), any(), any(), any())).then {
      assertEquals(
          pdfGeneratorViewModel.pdfGenerationState.value,
          PdfGeneratorViewModel.PdfGenerationState.Ready)
    }
    composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").performClick()
    composeTestRule.onNodeWithTag("deviceStorageButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInput").performTextInput("test.pdf")
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    assertEquals("test.pdf", pdfGeneratorViewModel.newFileName.value)
  }

  @Test
  fun pdfGenerationStateIsSetToReadyOnError() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    // Set up the activity result for the intent
    // Simulate the file picker intent
    val expectedUri = Uri.parse("content://test-image-uri")
    val resultIntent = Intent().apply { data = expectedUri }
    Intent().apply { data = expectedUri }
    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))
    `when`(mockPdfRepository.convertImageToPdf(any(), any())).then { throw Exception() }
    composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").performClick()
    composeTestRule.onNodeWithTag("deviceStorageButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInput").performTextInput("test.pdf")
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    assertEquals("test.pdf", pdfGeneratorViewModel.newFileName.value)
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Ready,
        pdfGeneratorViewModel.pdfGenerationState.value)
  }

  @Test
  fun testAbortPdfGeneration_setsPdfGenerationStateToAborted() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    // Set up the activity result for the intent
    // Simulate the file picker intent
    val expectedUri = Uri.parse("content://test-image-uri")
    val resultIntent = Intent().apply { data = expectedUri }
    Intent().apply { data = expectedUri }
    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))
    composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").performClick()
    composeTestRule.onNodeWithTag("deviceStorageButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdfNameInput").performTextInput("test.pdf")
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("loadingIndicator").assertIsDisplayed()
    composeTestRule.onNodeWithTag("abortButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("abortButton").performClick()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Aborted,
        pdfGeneratorViewModel.pdfGenerationState.value)
  }

  @Test
  fun testFilePickerWithNullUri_resultsInCorrectBehavior() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }

    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

    composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").performClick()
    composeTestRule.onNodeWithTag("deviceStorageButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Ready,
        pdfGeneratorViewModel.pdfGenerationState.value)
  }
}
