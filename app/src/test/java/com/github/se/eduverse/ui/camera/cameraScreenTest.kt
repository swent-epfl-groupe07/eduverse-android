package com.github.se.eduverse.ui.camera

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class CameraScreenTest {

  @Test
  fun rotateImageIfSelfie_rotatesImageCorrectlyWhenSelfie() {
    // Créer un Bitmap factice avec une taille de 100x100
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    // Appliquer la rotation
    val rotatedBitmap = rotateImageIfSelfie(bitmap, isSelfie = true)

    // Vérifier que le bitmap retourné n'est pas nul
    assertNotNull(rotatedBitmap)
  }

  @Test
  fun rotateImageIfSelfie_doesNotRotateWhenNotSelfie() {
    // Créer un Bitmap factice
    val bitmap = Mockito.mock(Bitmap::class.java)

    // Ne pas appliquer la rotation si ce n'est pas un selfie
    val rotatedBitmap = rotateImageIfSelfie(bitmap, isSelfie = false)

    // Vérifier que le bitmap d'origine est retourné sans modification
    assertEquals(bitmap, rotatedBitmap)
  }

  @Test
  fun mirrorImage_flipsBitmapCorrectly() {
    // Créer un Bitmap factice avec une taille de 100x100
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    // Appliquer l'effet miroir
    val mirroredBitmap = mirrorImage(bitmap)

    // Vérifier que le bitmap retourné n'est pas nul
    assertNotNull(mirroredBitmap)

    // Vérifier que la taille du bitmap n'a pas changé après l'effet miroir
    assertEquals(bitmap.width, mirroredBitmap.width)
    assertEquals(bitmap.height, mirroredBitmap.height)
  }
}
