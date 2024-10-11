package com.github.se.eduverse.ui.camera

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cameraPreview_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Vérifie que la vue de prévisualisation de la caméra est bien affichée
        composeTestRule.onNodeWithTag("cameraPreview").assertIsDisplayed()
    }

    @Test
    fun closeButton_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Vérifie que le bouton de fermeture est bien affiché
        composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed()
    }

    @Test
    fun switchCameraButton_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Vérifie que le bouton de switch de la caméra est bien affiché
        composeTestRule.onNodeWithTag("switchCameraButton").assertIsDisplayed()
    }

    @Test
    fun photoButton_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Vérifie que le bouton Photo est bien affiché
        composeTestRule.onNodeWithTag("photoButton").assertIsDisplayed()
    }

    @Test
    fun videoButton_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Vérifie que le bouton Vidéo est bien affiché
        composeTestRule.onNodeWithTag("videoButton").assertIsDisplayed()
    }

    @Test
    fun takePhotoButton_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Vérifie que le bouton Take Photo est bien affiché
        composeTestRule.onNodeWithTag("takePhotoButton").assertIsDisplayed()
    }

    @Test
    fun rectangleLeft_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Vérifie que le rectangle gauche est bien affiché
        composeTestRule.onNodeWithTag("rectangleLeft").assertIsDisplayed()
    }

    @Test
    fun rectangleRight_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Vérifie que le rectangle droit est bien affiché
        composeTestRule.onNodeWithTag("rectangleRight").assertIsDisplayed()
    }

    @Test
    fun takePhotoButton_clickAction() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Simule un clic sur le bouton Take Photo
        composeTestRule.onNodeWithTag("takePhotoButton").performClick()

        // Ici, on pourrait ajouter des assertions pour vérifier le comportement après clic si nécessaire
    }

    @Test
    fun switchCameraButton_clickAction() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Simule un clic sur le bouton de switch de la caméra
        composeTestRule.onNodeWithTag("switchCameraButton").performClick()

        // Ici, on pourrait ajouter des assertions pour vérifier le comportement après clic si nécessaire
    }

    @Test
    fun photoButton_clickAction() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Simule un clic sur le bouton Photo
        composeTestRule.onNodeWithTag("photoButton").performClick()

        // Ici, on pourrait ajouter des assertions pour vérifier le comportement après clic si nécessaire
    }

    @Test
    fun videoButton_clickAction() {
        composeTestRule.setContent {
            CameraScreen()
        }

        // Simule un clic sur le bouton Vidéo
        composeTestRule.onNodeWithTag("videoButton").performClick()

        // Ici, on pourrait ajouter des assertions pour vérifier le comportement après clic si nécessaire
    }
}
