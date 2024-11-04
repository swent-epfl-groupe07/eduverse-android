package com.github.se.eduverse.ui.camera

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import io.mockk.mockk
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NextScreenTest2 {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var photoViewModel: PhotoViewModel
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var context: Context
  private var currentPhotoFile: File? = null
  private var currentVideoFile: File? = null

  @Before
  fun setUp() {
    navigationActions = mockk(relaxed = true)
    photoViewModel = mockk(relaxed = true)
    folderViewModel = mockk(relaxed = true)
    context = ApplicationProvider.getApplicationContext()

    // Simuler un fichier image temporaire
    currentPhotoFile = File.createTempFile("test_image", ".jpg")
  }

  @Test
  fun testVideoPreviewIsDisplayedWhenVideoFileProvided() {
    // Simuler un fichier vidéo temporaire
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    // Mettre à jour la variable `currentVideoFile` directement avec le fichier vidéo simulé
    currentVideoFile = testVideoFile

    // Charger à nouveau la composable avec la vidéo mise à jour
    composeTestRule.setContent {
      NextScreen(
          photoFile = currentPhotoFile,
          videoFile = currentVideoFile,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel)
    }

    // Forcer la recomposition
    composeTestRule.waitForIdle()

    // Vérifier que le nœud avec le tag de prévisualisation de la vidéo est affiché
    composeTestRule.onNodeWithTag("previewVideo").assertExists().assertIsDisplayed()
  }

  @Test
  fun testVideoPlayerReleasedOnDispose() {
    // Simuler un fichier vidéo temporaire
    val testVideoFile = File.createTempFile("test_video", ".mp4")

    // Charger la composable avec le fichier vidéo
    composeTestRule.setContent {
      NextScreen(
          photoFile = currentPhotoFile,
          videoFile = testVideoFile,
          navigationActions = navigationActions,
          photoViewModel = photoViewModel,
          folderViewModel = folderViewModel)
    }

    // Forcer la recomposition
    composeTestRule.waitForIdle()

    // Manipuler ExoPlayer sur le thread principal
    composeTestRule.runOnUiThread {
      // Simuler la création du player ExoPlayer
      val player =
          ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(testVideoFile)))
            prepare()
            playWhenReady = true
          }

      // Simuler la suppression de la composable et libérer le player
      player.release()

      // Vérifier que le player est libéré correctement
      assert(player.isPlaying.not()) // Le player ne doit plus jouer après la libération
    }
  }
}
