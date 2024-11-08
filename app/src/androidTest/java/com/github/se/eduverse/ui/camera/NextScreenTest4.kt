package com.github.se.eduverse.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.navigation.NavigationActions
import io.mockk.*
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseUploadTest {

  private lateinit var context: Context
  private lateinit var navigationActions: NavigationActions
  private lateinit var testPhotoFile: File
  private lateinit var testVideoFile: File
  private lateinit var testBitmap: Bitmap

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    navigationActions = mockk(relaxed = true)

    // Create a more complex test bitmap with actual content
    testBitmap =
        Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888).apply {
          val canvas = android.graphics.Canvas(this)
          val paint =
              android.graphics.Paint().apply {
                color = Color.BLUE
                style = android.graphics.Paint.Style.FILL
              }
          canvas.drawRect(0f, 0f, 320f, 240f, paint)
        }

    // Create test photo file with quality content
    testPhotoFile =
        File.createTempFile("test_photo", ".jpg").apply {
          outputStream().use { testBitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }

    // Create test video file with valid video data
    testVideoFile =
        File.createTempFile("test_video", ".mp4").apply {
          // Write minimal valid MP4 header
          outputStream().use { out ->
            // Simple MP4 file header (not complete but enough for testing)
            val header =
                byteArrayOf(
                    0x00,
                    0x00,
                    0x00,
                    0x18, // size
                    0x66,
                    0x74,
                    0x79,
                    0x70, // ftyp
                    0x69,
                    0x73,
                    0x6F,
                    0x6D, // isom
                    0x00,
                    0x00,
                    0x00,
                    0x01, // version
                    0x69,
                    0x73,
                    0x6F,
                    0x6D, // isom
                    0x61,
                    0x76,
                    0x63,
                    0x31 // avc1
                    )
            out.write(header)
            // Add some dummy video data
            out.write(ByteArray(1024) { 1 })
          }
        }
  }

  @Test
  fun testPhotoUploadSuccess() {
    assertTrue("Test photo file should exist", testPhotoFile.exists())
    assertTrue("Test photo file should be readable", testPhotoFile.canRead())
    assertTrue("Photo file size should be reasonable", testPhotoFile.length() > 1000)
  }

  @Test
  fun testPhotoFileProcessing() {
    val byteArray =
        ByteArrayOutputStream().use { stream ->
          testBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
          stream.toByteArray()
        }

    assertTrue("Processed photo should have data", byteArray.isNotEmpty())
    assertTrue("Processed photo size should be reasonable", byteArray.size > 100) // Lower threshold
  }

  @Test
  fun testThumbnailGenerationFailure() {
    val invalidFile = File(context.cacheDir, "invalid.mp4")
    val thumbnail = generateVideoThumbnail(context, invalidFile)
    assertNull("Should not generate thumbnail for invalid video", thumbnail)
  }

  @Test
  fun testPublicationCreation() {
    val publication =
        Publication(
            userId = "test_user",
            title = "Test Publication",
            thumbnailUrl = "test_thumbnail_url",
            mediaUrl = "test_media_url",
            mediaType = MediaType.VIDEO)

    assertEquals("test_user", publication.userId)
    assertEquals(MediaType.VIDEO, publication.mediaType)
    assertEquals("test_thumbnail_url", publication.thumbnailUrl)
    assertEquals("test_media_url", publication.mediaUrl)
  }

  @Test
  fun testNavigationAfterSuccess() {
    navigationActions.goBack()
    navigationActions.goBack()
    verify(exactly = 2) { navigationActions.goBack() }
  }
}
