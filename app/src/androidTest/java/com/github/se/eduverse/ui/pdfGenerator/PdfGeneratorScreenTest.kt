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
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.repository.ConvertApiRepository
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.repository.OpenAiRepository
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.pdfGenerator.InfoWindow
import com.github.se.eduverse.ui.pdfGenerator.InputNewFolderNameDialog
import com.github.se.eduverse.ui.pdfGenerator.PdfGeneratorOption
import com.github.se.eduverse.ui.pdfGenerator.PdfGeneratorScreen
import com.github.se.eduverse.ui.pdfGenerator.PdfNameInputDialog
import com.github.se.eduverse.ui.pdfGenerator.SelectDestinationDialog
import com.github.se.eduverse.ui.pdfGenerator.SelectFolderDialog
import com.github.se.eduverse.ui.profile.auth
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PdfGeneratorViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import java.io.File
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
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
  private lateinit var mockFileRepository: FileRepository
  private lateinit var mockFolderRepository: FolderRepository
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var auth: FirebaseAuth
  private lateinit var currentUser: FirebaseUser

  private var deleted = false
  private val folders =
      mutableListOf(
          Folder("uid", mutableListOf(), "folder1", "1", archived = false),
          Folder("uid", mutableListOf(), "folder2", "2", archived = false))

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    // Initialize the Intent
    Intents.init()

    // Mock the dependencies
    mockNavigationActions = mock(NavigationActions::class.java)
    mockPdfRepository = mock(PdfRepository::class.java)
    mockOpenAiRepository = mock(OpenAiRepository::class.java)
    mockConvertApiRepository = mock(ConvertApiRepository::class.java)
    mockFileRepository = mock(FileRepository::class.java)
    mockFolderRepository = mock(FolderRepository::class.java)
    auth = mock(FirebaseAuth::class.java)
    currentUser = mock(FirebaseUser::class.java)

    // Mock the current authenticated user retrieval
    `when`(auth.currentUser).thenReturn(currentUser)
    `when`(currentUser.uid).thenReturn("uid")

    // Initialize the view models
    pdfGeneratorViewModel =
        PdfGeneratorViewModel(
            mockPdfRepository,
            mockOpenAiRepository,
            mockConvertApiRepository,
            mockFileRepository,
            mockFolderRepository)
    folderViewModel = FolderViewModel(mockFolderRepository, auth)

    // Mock folder repository methods
    `when`(mockFolderRepository.getFolders(any(), any(), any(), any())).then {
      (it.arguments[2] as (List<Folder>) -> Unit)(folders.toList())
    }
    `when`(mockFolderRepository.getNewUid()).thenReturn("folderId")

    // Mock file repository methods
    `when`(mockFileRepository.getNewUid()).thenReturn("fileId")

    // Mock pdf repository methods
    `when`(mockPdfRepository.convertTextToPdf(any(), any())).thenReturn(PdfDocument())
    `when`(mockPdfRepository.writePdfDocumentToTempFile(any(), any())).thenReturn(File("test.pdf"))
    `when`(mockPdfRepository.deleteTempPdfFile(any())).then {
      deleted = true
      // Check that view model's current file is the one being deleted
      assertEquals(it.getArgument(0), pdfGeneratorViewModel.currentFile)
    }

    // Mock navigation actions methods
    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.PDF_GENERATOR)
  }

  @After
  fun tearDown() {
    // Release the Intent
    Intents.release()
  }

  @Test
  fun topNavigationBarIsCorrectlyDisplayed() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertTextEquals("PDF Generator")
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").performClick()
    verify(mockNavigationActions).goBack()
  }

  @Test
  fun allPdfGeneratorOptionsAreCorrectlyDisplayed() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfGeneratorOption.TEXT_TO_PDF.name).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PdfGeneratorOption.TEXT_TO_PDF.name)
        .assertTextContains("Text to PDF")
        .assertTextContains("Converts a .txt file to PDF")
    composeTestRule.onNodeWithTag(PdfGeneratorOption.IMAGE_TO_PDF.name).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PdfGeneratorOption.IMAGE_TO_PDF.name)
        .assertTextContains("Image to PDF")
        .assertTextContains("Converts an image to PDF")
    composeTestRule.onNodeWithTag(PdfGeneratorOption.DOCUMENT_TO_PDF.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfGeneratorOption.DOCUMENT_TO_PDF.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfGeneratorOption.DOCUMENT_TO_PDF.name)
        .assertTextContains("Doc to PDF")
        .assertTextContains("Converts a document to PDF")
    composeTestRule.onNodeWithTag(PdfGeneratorOption.SUMMARIZE_FILE.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfGeneratorOption.SUMMARIZE_FILE.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfGeneratorOption.SUMMARIZE_FILE.name)
        .assertTextContains("Summarize file")
        .assertTextContains("Generates a summary of a file")
    composeTestRule.onNodeWithTag(PdfGeneratorOption.EXTRACT_TEXT.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfGeneratorOption.EXTRACT_TEXT.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfGeneratorOption.EXTRACT_TEXT.name)
        .assertTextContains("Extract text")
        .assertTextContains("Extracts text from an image")
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(PdfGeneratorOption.TRANSCRIBE_SPEECH.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PdfGeneratorOption.TRANSCRIBE_SPEECH.name).assertHasClickAction()
    composeTestRule
        .onNodeWithTag(PdfGeneratorOption.TRANSCRIBE_SPEECH.name)
        .assertTextContains("Speech to PDF")
        .assertTextContains("Transcribes speech to text")
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
    composeTestRule.onNodeWithTag(PdfGeneratorOption.TEXT_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("Text to PDF converter")
    composeTestRule
        .onNodeWithTag("infoWindowText")
        .assertTextEquals("Select a .txt file to convert to PDF")
    composeTestRule.onNodeWithTag("infoWindowDismissButton").performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsNotDisplayed()
  }

  @Test
  fun clickingImageToPdfOption_correctlyDisplaysInfoWindow() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfGeneratorOption.IMAGE_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("Image to PDF converter")
    composeTestRule
        .onNodeWithTag("infoWindowText")
        .assertTextEquals("Select an image to convert to PDF")
    composeTestRule.onNodeWithTag("infoWindowDismissButton").performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsNotDisplayed()
  }

  @Test
  fun clickingSummarizeFileOption_correctlyDisplaysInfoWindow() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfGeneratorOption.SUMMARIZE_FILE.name).performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("Pdf file summarizer")
    composeTestRule
        .onNodeWithTag("infoWindowText")
        .assertTextEquals(
            "Select a PDF file to summarize. The summary will be generated in a PDF file")
    composeTestRule.onNodeWithTag("infoWindowDismissButton").performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsNotDisplayed()
  }

  @Test
  fun clickingDocumentToPdfOption_correctlyDisplaysInfoWindow() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfGeneratorOption.DOCUMENT_TO_PDF.name).performClick()
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
    composeTestRule.onNodeWithTag("infoWindowDismissButton").performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsNotDisplayed()
  }

  @Test
  fun clickingExtractTextOption_correctlyDisplaysInfoWindow() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    composeTestRule.onNodeWithTag(PdfGeneratorOption.EXTRACT_TEXT.name).performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("Text extractor")
    composeTestRule
        .onNodeWithTag("infoWindowText")
        .assertTextEquals(
            "Select an image to extract text from. Make sure the selected image contains text. The extracted text will be generated in a PDF file")
    composeTestRule.onNodeWithTag("infoWindowDismissButton").performClick()
    composeTestRule.onNodeWithTag("infoWindow").assertIsNotDisplayed()
  }

  @Test
  fun clickingSelectSourceFileButtonFromInfoWindow_launchesFilePicker() {
    composeTestRule.setContent { PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel) }
    setupFilePickerIntent()
    composeTestRule.onNodeWithTag(PdfGeneratorOption.TEXT_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dismissCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
  }

  @Test
  fun pdfGenerationStateIsSetToReadyOnSuccess_whenDeviceStorageIsSelected() {
    `when`(mockPdfRepository.savePdfToDevice(any(), any(), any(), any(), any())).then {
      it.getArgument<(File) -> Unit>(3)
    }
    initialStepsOfPdfGeneration(PdfGeneratorOption.TEXT_TO_PDF.name)
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("deviceStorageButton").performClick()
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsNotDisplayed()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Ready,
        pdfGeneratorViewModel.pdfGenerationState.value)
  }

  @Test
  fun pdfGenerationStateIsSetToReadyOnSuccess_whenDiscardIsSelected() {
    deleted = false
    initialStepsOfPdfGeneration(PdfGeneratorOption.TEXT_TO_PDF.name)
    composeTestRule.onNodeWithTag("discardButton").performClick()
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsNotDisplayed()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Ready,
        pdfGeneratorViewModel.pdfGenerationState.value)
    assertTrue(deleted) // Check that deleteTempPdfFile was called
  }

  @Test
  fun selectFolderDialogIsCorrectlyDisplayed_whenAppFoldersIsSelected() {
    `when`(mockFolderRepository.updateFolder(any(), any(), any())).then { invocation ->
      val newFolder = invocation.getArgument<Folder>(0)
      folders.replaceAll { if (it.id == newFolder.id) newFolder else it }
      invocation.getArgument<() -> Unit>(1)()
    }
    `when`(mockFileRepository.savePdfFile(any(), any(), any(), any())).then {
      it.getArgument<() -> Unit>(2)()
    }
    deleted = false
    initialStepsOfPdfGeneration(PdfGeneratorOption.TEXT_TO_PDF.name)
    folderViewModel.getUserFolders()
    composeTestRule.onNodeWithTag("appFoldersButton").performClick()
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectFolderDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancelSelectFolderButton").performClick()
    composeTestRule.onNodeWithTag("selectFolderDialog").assertIsNotDisplayed()
    // Check that the select destination dialog is displayed again when the cancel button is clicked
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("appFoldersButton").performClick()
    composeTestRule.onNodeWithTag("folderButton_1").performClick()
    composeTestRule.onNodeWithTag("selectFolderDialog").assertIsNotDisplayed()
    assertEquals(1, folders[0].files.size)
    assertEquals("test.pdf", folders[0].files[0].name)
    assertEquals("fileId", folders[0].files[0].fileId)
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Ready,
        pdfGeneratorViewModel.pdfGenerationState.value)
    assertTrue(deleted) // Check that deleteTempPdfFile was called
  }

  @Test
  fun testCreateNewFolderButtonInSelectFolderDialog() {
    `when`(mockFolderRepository.addFolder(any(), any(), any())).then {
      folders.add(it.getArgument(0))
      it.getArgument<() -> Unit>(1)()
    }
    `when`(mockFolderRepository.updateFolder(any(), any(), any())).then { invocation ->
      val newFolder = invocation.getArgument<Folder>(0)
      folders.replaceAll { if (it.id == newFolder.id) newFolder else it }
      invocation.getArgument<() -> Unit>(1)()
    }
    `when`(mockFileRepository.savePdfFile(any(), any(), any(), any())).then {
      it.getArgument<() -> Unit>(2)()
    }
    deleted = false
    initialStepsOfPdfGeneration(PdfGeneratorOption.TEXT_TO_PDF.name)
    folderViewModel.getUserFolders()
    composeTestRule.onNodeWithTag("appFoldersButton").performClick()
    composeTestRule.onNodeWithTag("createFolderButton").performClick()
    composeTestRule.onNodeWithTag("inputNewFolderNameField").performTextInput("testFolder")
    composeTestRule.onNodeWithTag("confirmCreateFolderButton").performClick()
    composeTestRule.onNodeWithTag("inputNewFolderNameDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectFolderDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsNotDisplayed()
    assertEquals(3, folders.size)
    assertEquals("testFolder", folders[2].name)
    assertEquals("folderId", folders[2].id)
    assertEquals(1, folders[2].files.size)
    assertEquals("test.pdf", folders[2].files[0].name)
    assertTrue(deleted) // Check that deleteTempPdfFile was called
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Ready,
        pdfGeneratorViewModel.pdfGenerationState.value)
  }

  @Test
  fun selectFolderBehavior_onSavePdfFileError() {
    `when`(mockFileRepository.savePdfFile(any(), any(), any(), any())).then {
      it.getArgument<(Exception) -> Unit>(3).invoke(Exception())
    }
    deleted = false
    initialStepsOfPdfGeneration(PdfGeneratorOption.TEXT_TO_PDF.name)
    folderViewModel.getUserFolders()
    composeTestRule.onNodeWithTag("appFoldersButton").performClick()
    composeTestRule.onNodeWithTag("folderButton_1").performClick()
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsDisplayed()
    // Check that the generated PDF file is not deleted when saving it to the folder fails
    assertNotNull(pdfGeneratorViewModel.currentFile)
    assertFalse(deleted)
  }

  @Test
  fun selectFolderBehavior_onUpdateFolderError() {
    `when`(mockFolderRepository.updateFolder(any(), any(), any())).then {
      it.getArgument<(Exception) -> Unit>(2).invoke(Exception())
    }
    `when`(mockFileRepository.savePdfFile(any(), any(), any(), any())).then {
      it.getArgument<() -> Unit>(2)()
    }
    deleted = false
    initialStepsOfPdfGeneration(PdfGeneratorOption.TEXT_TO_PDF.name)
    folderViewModel.getUserFolders()
    composeTestRule.onNodeWithTag("appFoldersButton").performClick()
    composeTestRule.onNodeWithTag("folderButton_1").performClick()
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsDisplayed()
    // Check that the generated PDF file is not deleted when updating the folder fails
    assertNotNull(pdfGeneratorViewModel.currentFile)
    assertFalse(deleted)
    // Check that deleteFile is called to delete the uploaded PDF file when savePdfFile is
    // successful but updateFolder fails
    verify(mockFileRepository).deleteFile(any(), any(), any())
  }

  @Test
  fun createNewFolderBehavior_onAddFolderError() {
    `when`(mockFolderRepository.addFolder(any(), any(), any())).then {
      it.getArgument<(Exception) -> Unit>(2).invoke(Exception())
    }
    deleted = false
    initialStepsOfPdfGeneration(PdfGeneratorOption.TEXT_TO_PDF.name)
    folderViewModel.getUserFolders()
    composeTestRule.onNodeWithTag("appFoldersButton").performClick()
    composeTestRule.onNodeWithTag("createFolderButton").performClick()
    composeTestRule.onNodeWithTag("inputNewFolderNameField").performTextInput("test")
    composeTestRule.onNodeWithTag("confirmCreateFolderButton").performClick()
    composeTestRule.onNodeWithTag("inputNewFolderNameDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsDisplayed()
    // Check that the generated PDF file is not deleted when creating a new folder fails
    assertNotNull(pdfGeneratorViewModel.currentFile)
    assertFalse(deleted)
  }

  @Test
  fun pdfGenerationStateIsSetToReadyOnError() {
    `when`(mockPdfRepository.convertImageToPdf(any(), any())).then { throw Exception() }
    initialStepsOfPdfGeneration(PdfGeneratorOption.IMAGE_TO_PDF.name)
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Ready,
        pdfGeneratorViewModel.pdfGenerationState.value)
  }

  @Test
  fun testAbortPdfGeneration_setsPdfGenerationStateToAborted() {
    `when`(mockPdfRepository.convertImageToPdf(any(), any())).thenReturn(PdfDocument())
    initialStepsOfPdfGeneration(PdfGeneratorOption.IMAGE_TO_PDF.name)
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

    composeTestRule.onNodeWithTag(PdfGeneratorOption.TEXT_TO_PDF.name).performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    assertEquals(
        PdfGeneratorViewModel.PdfGenerationState.Ready,
        pdfGeneratorViewModel.pdfGenerationState.value)
  }

  @Test
  fun testInfoWindowIsCorrectlyDisplayed() {
    composeTestRule.setContent { InfoWindow("title", "text", {}, {}) }
    composeTestRule.onNodeWithTag("infoWindow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowTitle").assertTextEquals("title")
    composeTestRule.onNodeWithTag("infoWindowText").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowText").assertTextEquals("text")
    composeTestRule.onNodeWithTag("infoWindowDismissButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowDismissButton").assertTextContains("Cancel")
    composeTestRule.onNodeWithTag("infoWindowDismissButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").assertTextContains("Select file")
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").assertHasClickAction()
  }

  @Test
  fun testSelectDestinationDialogIsCorrectlyDisplayed() {
    composeTestRule.setContent { SelectDestinationDialog({}, {}, {}) }
    composeTestRule.onNodeWithTag("selectDestinationDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("selectDestinationDialogTitle").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("selectDestinationDialogTitle")
        .assertTextEquals("PDF file generation is complete. Choose where to save it.")
    composeTestRule.onNodeWithTag("appFoldersButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("appFoldersButton").assertTextContains("App folders")
    composeTestRule.onNodeWithTag("appFoldersButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("deviceStorageButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("deviceStorageButton").assertTextContains("Device storage")
    composeTestRule.onNodeWithTag("deviceStorageButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("discardButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("discardButton").assertTextContains("Discard PDF")
    composeTestRule.onNodeWithTag("discardButton").assertHasClickAction()
  }

  @Test
  fun testSelectFolderDialogIsCorrectlyDisplayed() {
    composeTestRule.setContent { SelectFolderDialog(folders, {}, {}, {}) }
    composeTestRule.onNodeWithTag("selectFolderDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("selectFolderDialogTitle").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("selectFolderDialogTitle")
        .assertTextEquals("Select destination folder")
    composeTestRule.onNodeWithTag("folderButton_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderButton_1").assertTextContains("folder1")
    composeTestRule.onNodeWithTag("folderButton_1").assertHasClickAction()
    composeTestRule.onNodeWithTag("folderButton_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderButton_2").assertTextContains("folder2")
    composeTestRule.onNodeWithTag("folderButton_2").assertHasClickAction()
    composeTestRule.onNodeWithTag("createFolderButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("createFolderButton").assertTextContains("Create a new folder")
    composeTestRule.onNodeWithTag("cancelSelectFolderButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancelSelectFolderButton").assertTextContains("Cancel")
    composeTestRule.onNodeWithTag("cancelSelectFolderButton").assertHasClickAction()
  }

  @Test
  fun testSelectFolderDialogIsCorrectlyDisplayed_whenNoFolderExists() {
    composeTestRule.setContent { SelectFolderDialog(emptyList(), {}, {}, {}) }
    composeTestRule.onNodeWithTag("selectFolderDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("noFoldersFoundText").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("noFoldersFoundText")
        .assertTextEquals("No folders found. Create a new folder to save the generated PDF file.")
  }

  @Test
  fun testInputNewFolderNameDialogIsCorrectlyDisplayed() {
    composeTestRule.setContent { InputNewFolderNameDialog({}, {}) }
    composeTestRule.onNodeWithTag("inputNewFolderNameDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("inputNewFolderNameDialogTitle").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("inputNewFolderNameDialogTitle")
        .assertTextEquals("Enter a name for the new folder")
    composeTestRule.onNodeWithTag("inputNewFolderNameField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("inputNewFolderNameField").assertTextContains("Folder name")
    composeTestRule.onNodeWithTag("inputNewFolderNameField").performTextInput("test")
    composeTestRule.onNodeWithTag("inputNewFolderNameField").assertTextContains("test")
    composeTestRule.onNodeWithTag("confirmCreateFolderButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirmCreateFolderButton").assertTextContains("Create folder")
    composeTestRule.onNodeWithTag("confirmCreateFolderButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("dismissCreateFolderButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dismissCreateFolderButton").assertTextContains("Cancel")
    composeTestRule.onNodeWithTag("dismissCreateFolderButton").assertHasClickAction()
  }

  private fun initialStepsOfPdfGeneration(option: String) {
    composeTestRule.setContent {
      PdfGeneratorScreen(mockNavigationActions, pdfGeneratorViewModel, folderViewModel)
    }
    setupFilePickerIntent()
    composeTestRule.onNodeWithTag(option).performClick()
    composeTestRule.onNodeWithTag("infoWindowConfirmButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInput").performTextInput("test")
    composeTestRule.onNodeWithTag("confirmCreatePdfButton").performClick()
    composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    assertEquals("test", pdfGeneratorViewModel.newFileName.value)
  }

  /**
   * Set up mocking for the file picker intent (defining it in a separate function to avoid code
   * duplication and improve readability)
   */
  private fun setupFilePickerIntent() {
    // Set up the activity result for the intent
    // Simulate the file picker intent
    val expectedUri = Uri.parse("content://test-uri")
    val resultIntent = Intent().apply { data = expectedUri }
    Intent().apply { data = expectedUri }
    Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))
  }
}
