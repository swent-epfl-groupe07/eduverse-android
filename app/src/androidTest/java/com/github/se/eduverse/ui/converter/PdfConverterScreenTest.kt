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
import com.github.se.eduverse.repository.OpenAiRepository
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.ui.converter.PdfConverterOption
import com.github.se.eduverse.ui.converter.PdfConverterScreen
import com.github.se.eduverse.ui.converter.PdfNameInputDialog
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.PdfConverterViewModel
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
class PdfConverterScreenTest {

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var pdfConverterViewModel: PdfConverterViewModel
  private lateinit var mockPdfRepository: PdfRepository
  private lateinit var mockOpenAiRepository: OpenAiRepository

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    Intents.init()

    mockNavigationActions = mock(NavigationActions::class.java)
    mockPdfRepository = mock(PdfRepository::class.java)
    mockOpenAiRepository = mock(OpenAiRepository::class.java)
    pdfConverterViewModel = PdfConverterViewModel(mockPdfRepository, mockOpenAiRepository)

    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.PDF_CONVERTER)
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  @Test
  fun topNavigationBarIsCorrectlyDisplayed() {
    composeTestRule.setContent { PdfConverterScreen(mockNavigationActions, pdfConverterViewModel) }
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertTextEquals("PDF Converter")
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").performClick()
    verify(mockNavigationActions).goBack()
  }

  @Test
  fun allPdfConverterOptionsAreCorrectlyDisplayed() {
    composeTestRule.setContent { PdfConverterScreen(mockNavigationActions, pdfConverterViewModel) }
    composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name)
        .assertTextEquals("Text to PDF")
    composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name)
        .assertTextEquals("Image to PDF")
    composeTestRule.onNodeWithTag(PdfConverterOption.DOCUMENT_TO_PDF.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfConverterOption.DOCUMENT_TO_PDF.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.DOCUMENT_TO_PDF.name)
        .assertTextEquals("Doc to PDF")
    composeTestRule.onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name)
        .assertTextEquals("Summarize file")
    composeTestRule.onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name)
        .assertTextEquals("Extract text")
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
  }

  @Test
  fun notImplementedOptionsClickDoesNotChangeGenerationState() {
    composeTestRule.setContent { PdfConverterScreen(mockNavigationActions, pdfConverterViewModel) }
    composeTestRule.onNodeWithTag(PdfConverterOption.DOCUMENT_TO_PDF.name).performClick()
    assertEquals(
        PdfConverterViewModel.PdfGenerationState.Ready,
        pdfConverterViewModel.pdfGenerationState.value)
    composeTestRule.onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name).performClick()
    assertEquals(
        PdfConverterViewModel.PdfGenerationState.Ready,
        pdfConverterViewModel.pdfGenerationState.value)
  }

  @Test
  fun clickingTextToPdfOption_launchesFilePicker() {
    composeTestRule.setContent { PdfConverterScreen(mockNavigationActions, pdfConverterViewModel) }

    // Set up the activity result for the intent
    // Simulate the file picker intent
    val expectedUri = Uri.parse("content://test-document-uri")
    val resultIntent = Intent().apply { data = expectedUri }
    Intent().apply { data = expectedUri }
    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))

    composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dismissCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    assertEquals(
        PdfConverterViewModel.PdfGenerationState.Ready,
        pdfConverterViewModel.pdfGenerationState.value)
  }

  @Test
  fun pdfGenerationStateIsSetToReadyOnError() {
    composeTestRule.setContent { PdfConverterScreen(mockNavigationActions, pdfConverterViewModel) }
    // Set up the activity result for the intent
    // Simulate the file picker intent
    val expectedUri = Uri.parse("content://test-image-uri")
    val resultIntent = Intent().apply { data = expectedUri }
    Intent().apply { data = expectedUri }
    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))
    `when`(mockPdfRepository.convertImageToPdf(any(), any())).then { throw Exception() }
    composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdfNameInput").performTextInput("test.pdf")
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    assertEquals("test.pdf", pdfConverterViewModel.newFileName.value)
    assertEquals(
        PdfConverterViewModel.PdfGenerationState.Ready,
        pdfConverterViewModel.pdfGenerationState.value)
  }

  @Test
  fun pdfGenerationStateIsSetToReadyOnSuccess() {
    composeTestRule.setContent { PdfConverterScreen(mockNavigationActions, pdfConverterViewModel) }
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
          pdfConverterViewModel.pdfGenerationState.value,
          PdfConverterViewModel.PdfGenerationState.Ready)
    }
    composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdfNameInput").performTextInput("test.pdf")
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    assertEquals("test.pdf", pdfConverterViewModel.newFileName.value)
  }

  @Test
  fun clickingImageToPdfOption_launchesFilePicker() {
    composeTestRule.setContent { PdfConverterScreen(mockNavigationActions, pdfConverterViewModel) }
    // Set up the activity result for the intent
    // Simulate the file picker intent
    val expectedUri = Uri.parse("content://test-image-uri")
    val resultIntent = Intent().apply { data = expectedUri }
    Intent().apply { data = expectedUri }
    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))
    composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdfNameInput").performTextInput("test.pdf")
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("loadingIndicator").assertIsDisplayed()
    composeTestRule.onNodeWithTag("abortButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("abortButton").performClick()
    assertEquals(
        PdfConverterViewModel.PdfGenerationState.Aborted,
        pdfConverterViewModel.pdfGenerationState.value)
  }

  @Test
  fun clickingSummarizeFileOption_launchesFilePicker() {
    composeTestRule.setContent { PdfConverterScreen(mockNavigationActions, pdfConverterViewModel) }
    // Set up the activity result for the intent
    // Simulate the file picker intent
    val expectedUri = Uri.parse("content://test-pdf-uri")
    val resultIntent = Intent().apply { data = expectedUri }
    Intent().apply { data = expectedUri }
    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))
    composeTestRule.onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name).performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdfNameInput").performTextInput("test.pdf")
    composeTestRule.onNodeWithTag("dismissCreatePdfButton").performClick()
  }

  @Test
  fun testPdfNameInputDialog() {
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
  fun testPdfNameInputDialog_confirm() {
    var confirmedFileName = ""
    composeTestRule.setContent {
      PdfNameInputDialog(
          pdfFileName = "test.pdf", onDismiss = {}, onConfirm = { confirmedFileName = it })
    }
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    assert(confirmedFileName == "test.pdf")
  }

  @Test
  fun testPdfNameInputDialog_confirmCallsOnConfirm() {
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
  fun filePickerWithNullUri_showsToastMessage() {
    composeTestRule.setContent { PdfConverterScreen(mockNavigationActions, pdfConverterViewModel) }

    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

    composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).performClick()

    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
  }
}
