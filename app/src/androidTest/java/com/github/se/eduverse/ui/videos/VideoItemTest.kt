package com.github.se.eduverse.ui.videos

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.VideoItem
import io.mockk.clearMocks
import io.mockk.spyk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoItemTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun testVideoItemDisplaysPlayerView() {
    val testMediaUrl = "https://example.com/test-video.mp4"

    composeTestRule.setContent {
      VideoItem(context = composeTestRule.activity, mediaUrl = testMediaUrl)
    }

    // Attendre que l'interface soit stabilisée
    composeTestRule.waitForIdle()

    // Vérifier que le VideoItem est affiché
    composeTestRule.onNodeWithTag("VideoItem").assertExists().assertIsDisplayed()
  }

  @Test
  fun testVideoItemWithInvalidUrl() {
    val invalidMediaUrl = ""

    composeTestRule.setContent {
      VideoItem(context = composeTestRule.activity, mediaUrl = invalidMediaUrl)
    }

    // Attendre que l'interface soit stabilisée
    composeTestRule.waitForIdle()

    // Vérifier que le VideoItem est toujours affiché même avec une URL invalide
    composeTestRule.onNodeWithTag("VideoItem").assertExists().assertIsDisplayed()
  }

  @Test
  fun testExoPlayerIsReleasedOnDispose() {
    val context = composeTestRule.activity
    val testMediaUrl = "https://example.com/test-video.mp4"

    // Créer un ExoPlayer spyé avec MockK
    val exoPlayer = spyk(ExoPlayer.Builder(context).build())

    val exoPlayerProvider: () -> ExoPlayer = { exoPlayer }

    // Utiliser un état mutable pour contrôler la présence de VideoItem
    val showVideoItem = mutableStateOf(true)

    // Charger le contenu avec VideoItem conditionnellement
    composeTestRule.setContent {
      if (showVideoItem.value) {
        VideoItem(context = context, mediaUrl = testMediaUrl, exoPlayerProvider = exoPlayerProvider)
      } else {
        // Si nécessaire, ajouter un contenu vide ou alternatif
        Box(modifier = Modifier.fillMaxSize())
      }
    }

    composeTestRule.waitForIdle()

    // Vérifier que VideoItem est affiché
    composeTestRule.onNodeWithTag("VideoItem").assertExists().assertIsDisplayed()

    // Simuler la suppression de VideoItem en modifiant l'état
    composeTestRule.runOnUiThread { showVideoItem.value = false }

    composeTestRule.waitForIdle()

    // Vérifier que release() a été appelé sur ExoPlayer
    verify(exactly = 1) { exoPlayer.release() }

    // Nettoyer les mocks après le test
    clearMocks(exoPlayer)
  }
}
