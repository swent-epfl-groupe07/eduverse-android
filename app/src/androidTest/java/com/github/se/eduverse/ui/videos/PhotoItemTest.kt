package com.github.se.eduverse.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.github.se.eduverse.ui.videos.PhotoItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoItemTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun testPhotoItemDisplaysImage() {
    val testThumbnailUrl = "https://example.com/test-image.jpg"

    val context = composeTestRule.activity

    // Créer un ImageLoader personnalisé qui retourne une image factice
    val testImageLoader =
        ImageLoader.Builder(context)
            .crossfade(false)
            .components {
              // Aucun composant nécessaire ici
            }
            .build()

    composeTestRule.setContent {
      CompositionLocalProvider(LocalImageLoader provides testImageLoader) {
        PhotoItem(thumbnailUrl = testThumbnailUrl)
      }
    }

    // Attendre que l'interface soit stabilisée
    composeTestRule.waitForIdle()

    // Vérifier que le PhotoItem est affiché
    composeTestRule.onNodeWithTag("PhotoItem").assertExists().assertIsDisplayed()
  }

  @Test
  fun testPhotoItemWithNullUrl() {
    val testThumbnailUrl: String? = null

    composeTestRule.setContent { PhotoItem(thumbnailUrl = testThumbnailUrl ?: "") }

    // Attendre que l'interface soit stabilisée
    composeTestRule.waitForIdle()

    // Vérifier que le PhotoItem est affiché (ou gérer le cas où il ne devrait pas être affiché)
    composeTestRule.onNodeWithTag("PhotoItem").assertExists().assertIsDisplayed()
  }
}
