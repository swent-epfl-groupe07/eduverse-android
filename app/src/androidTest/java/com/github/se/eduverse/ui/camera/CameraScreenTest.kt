package com.github.se.eduverse.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Composable
  private fun createFakeNavigationActions(): NavigationActions {
    val navController = rememberNavController()
    return NavigationActions(navController)
  }

  @Test
  fun cameraPreview_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("cameraPreview").assertIsDisplayed()
  }

  @Test
  fun closeButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed()
  }

  @Test
  fun switchCameraButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("switchCameraButton").assertIsDisplayed()
  }

  @Test
  fun photoButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("photoButton").assertIsDisplayed()
  }

  @Test
  fun videoButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("videoButton").assertIsDisplayed()
  }

  @Test
  fun takePhotoButton_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("takePhotoButton").assertIsDisplayed()
  }

  @Test
  fun rectangleLeft_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("rectangleLeft").assertIsDisplayed()
  }

  @Test
  fun rectangleRight_isDisplayed() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("rectangleRight").assertIsDisplayed()
  }

  @Test
  fun takePhotoButton_clickAction() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("takePhotoButton").performClick()
    // Ajouter des assertions pour vérifier le comportement après clic, si nécessaire
  }

  @Test
  fun switchCameraButton_clickAction() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("switchCameraButton").performClick()
    // Ajouter des assertions pour vérifier le comportement après clic, si nécessaire
  }

  @Test
  fun photoButton_clickAction() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("photoButton").performClick()
    // Ajouter des assertions pour vérifier le comportement après clic, si nécessaire
  }

  @Test
  fun videoButton_clickAction() {
    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }
    composeTestRule.onNodeWithTag("videoButton").performClick()
    // Ajouter des assertions pour vérifier le comportement après clic, si nécessaire
  }

  @Test
  fun closeButton_clickAction() {
    // Créer un faux NavigationActions avec un `goBack` capturé

    composeTestRule.setContent { CameraScreen(navigationActions = createFakeNavigationActions()) }

    // Vérifie que le bouton est bien affiché et cliquable
    composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed().performClick()

    // Vérifier que la fonction `goBack()` de NavigationActions est appelée
    // Normalement, cela nécessiterait un mock de la fonction, mais ici on peut capturer l'appel
    // en utilisant une structure de `verify` si possible dans le contexte de test actuel.
    composeTestRule.waitForIdle() // S'assurer que l'action est exécutée
    // Remarque : Vous pouvez également valider en fonction de l'état attendu après l'appel
  }
}
