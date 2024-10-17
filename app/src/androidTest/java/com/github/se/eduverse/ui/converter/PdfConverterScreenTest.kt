package com.github.se.eduverse.ui.converter

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.PdfConverterViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import java.util.concurrent.CompletableFuture.allOf

@RunWith(AndroidJUnit4::class)
class PdfConverterScreenTest {

    private lateinit var mockNavigationActions: NavigationActions

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Intents.init()

        mockNavigationActions = mock(NavigationActions::class.java)

        `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.PDF_CONVERTER)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun topNavigationBarIsCorrectlyDisplayed() {
        composeTestRule.setContent {
            PdfConverterScreen(mockNavigationActions)
        }
        composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("screenTitle").assertTextEquals("PDF Converter")
        composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
        composeTestRule.onNodeWithTag("goBackButton").performClick()
        verify(mockNavigationActions).goBack()
    }

    @Test
    fun allPdfConverterOptionsAreCorrectlyDisplayed() {
        composeTestRule.setContent {
            PdfConverterScreen(mockNavigationActions)
        }
        composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).assertTextEquals("Text to PDF")
        composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PdfConverterOption.IMAGE_TO_PDF.name).assertTextEquals("Image to PDF")
        composeTestRule.onNodeWithTag(PdfConverterOption.SCANNER_TOOL.name).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PdfConverterOption.SCANNER_TOOL.name).assertHasClickAction()
        composeTestRule.onNodeWithTag(PdfConverterOption.SCANNER_TOOL.name).assertTextEquals("Scanner tool")
        composeTestRule.onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name).assertHasClickAction()
        composeTestRule.onNodeWithTag(PdfConverterOption.SUMMARIZE_FILE.name).assertTextEquals("Summarize file")
        composeTestRule.onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name).assertHasClickAction()
        composeTestRule.onNodeWithTag(PdfConverterOption.EXTRACT_TEXT.name).assertTextEquals("Extract text")
        composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    }

    @Test
    fun clickingTextToPdfOption_launchesFilePicker() {
        composeTestRule.setContent {
            PdfConverterScreen(mockNavigationActions)
        }

        // Set up the activity result for the intent
        // Simulate the file picker intent
        val expectedUri = Uri.parse("content://test-document-uri")
        val resultIntent = Intent().apply { data = expectedUri }
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent))
        composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).performClick()
        composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dismissCreatePdfButton").performClick()
        composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsNotDisplayed()
    }

    @Test
    fun clickingImageToPdfOption_launchesFilePicker() {
        composeTestRule.setContent {
            PdfConverterScreen(mockNavigationActions)
        }
        composeTestRule.onNodeWithTag(PdfConverterOption.TEXT_TO_PDF.name).performClick()
        composeTestRule.onNodeWithTag("pdfNameInputDialog").assertIsDisplayed()
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
            PdfNameInputDialog(pdfFileName = "test.pdf", onDismiss = {}, onConfirm = { confirmedFileName = it })
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
            PdfNameInputDialog(pdfFileName = "test.pdf", onDismiss = {}, onConfirm = { confirmedFileName = it })
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

}