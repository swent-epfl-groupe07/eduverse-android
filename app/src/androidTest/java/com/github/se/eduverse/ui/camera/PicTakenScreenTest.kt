package com.github.se.eduverse.ui.camera

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PicTakenScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCloseButtonIsDisplayedAndClickable() {
        composeTestRule.setContent {
            PicTakenScreen(null)
        }
        composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun testImageDisplayedWhenBitmapIsNull() {
        composeTestRule.setContent {
            PicTakenScreen(null)
        }
        composeTestRule.onNodeWithTag("googleLogoImage").assertIsDisplayed()
    }

    /*@Test
    fun testImageDisplayedWhenBitmapIsNotNull() {
        composeTestRule.setContent {
            val context = LocalContext.current
            val photoFile = File(context.filesDir, "test_image.jpg")
            PicTakenScreen(photoFile)
        }

        // Vérifie que l'image capturée est affichée
        composeTestRule.onNodeWithTag("capturedImage").assertIsDisplayed()
    }*/

    @Test
    fun testCropIconIsDisplayedAndClickable() {
        composeTestRule.setContent {
            PicTakenScreen(null)
        }
        composeTestRule.onNodeWithTag("cropIcon").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun testFilterIconIsDisplayedAndClickable() {
        composeTestRule.setContent {
            PicTakenScreen(null)
        }
        composeTestRule.onNodeWithTag("filterIcon").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun testSaveButtonIsDisplayedAndClickable() {
        composeTestRule.setContent {
            PicTakenScreen(null)
        }
        composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun testPublishButtonIsDisplayedAndClickable() {
        composeTestRule.setContent {
            PicTakenScreen(null)
        }
        composeTestRule.onNodeWithTag("publishButton").assertIsDisplayed().assertHasClickAction()
    }
}

