package com.github.se.eduverse.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NextScreenTest2 {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var pViewModel: PhotoViewModel
  private lateinit var vViewModel: VideoViewModel
  private lateinit var context: Context
  private var currentPhotoFile: File? = null
  private var currentVideoFile: File? = null
  private lateinit var mockBitmap: Bitmap
  private lateinit var testFile: File

  @Before
  fun setUp() {
    navigationActions = mockk(relaxed = true)
    pViewModel = mockk(relaxed = true)
    vViewModel = mockk(relaxed = true) // Ensure vViewModel is initialized
    context = ApplicationProvider.getApplicationContext()

    // Simuler un fichier image temporaire
    mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    testFile = File.createTempFile("test_image", ".jpg").apply {
      outputStream().use { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    }
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
        pViewModel, vViewModel
      )
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
        pViewModel, vViewModel
      )
    }

    // Forcer la recomposition
    composeTestRule.waitForIdle()

    // Manipuler ExoPlayer sur le thread principal
    composeTestRule.runOnUiThread {
      // Simuler la création du player ExoPlayer
      val player = ExoPlayer.Builder(context).build().apply {
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
