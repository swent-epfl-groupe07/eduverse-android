// File: VideoThumbnailUtilTest.kt
package com.github.se.eduverse.ui.gallery

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VideoThumbnailUtilTest {

  @Mock private lateinit var context: Context

  @Mock private lateinit var mockBitmap: Bitmap

  @Mock private lateinit var retriever: MediaMetadataRetriever

  @Rule @JvmField val tempFolder = TemporaryFolder()

  private lateinit var retrieverFactory: MediaRetrieverFactory

  @Before
  fun setup() {
    `when`(context.cacheDir).thenReturn(tempFolder.root)
    retrieverFactory = mock(MediaRetrieverFactory::class.java)
    `when`(retrieverFactory.create()).thenReturn(retriever)
  }

  @Test
  fun generateThumbnail_withValidLocalVideoPath_shouldReturnThumbnailPath() {
    // Given
    val videoPath = "/storage/videos/test.mp4"
    `when`(retriever.getFrameAtTime(0)).thenReturn(mockBitmap)

    // When
    val result = VideoThumbnailUtil.generateThumbnail(context, videoPath, retrieverFactory)

    // Then
    assertNotNull(result)
    assertTrue(result!!.contains("thumbnail_"))
    verify(mockBitmap).compress(eq(Bitmap.CompressFormat.JPEG), eq(90), any())
    verify(mockBitmap).recycle()
    verify(retriever).release()
  }

  @Test
  fun generateThumbnail_withValidHttpUrl_shouldReturnThumbnailPath() {
    // Given
    val videoUrl = "http://example.com/video.mp4"
    `when`(retriever.getFrameAtTime(0)).thenReturn(mockBitmap)

    // When
    val result = VideoThumbnailUtil.generateThumbnail(context, videoUrl, retrieverFactory)

    // Then
    assertNotNull(result)
    assertTrue(result!!.contains("thumbnail_"))
    verify(mockBitmap).compress(eq(Bitmap.CompressFormat.JPEG), eq(90), any())
    verify(mockBitmap).recycle()
    verify(retriever).release()
  }

  @Test
  fun generateThumbnail_shouldReturnNull_whenBitmapIsNull() {
    // Given
    val videoPath = "/storage/videos/test.mp4"
    `when`(retriever.getFrameAtTime(0)).thenReturn(null)

    // When
    val result = VideoThumbnailUtil.generateThumbnail(context, videoPath, retrieverFactory)

    // Then
    assertNull(result)
    verify(retriever).release()
  }

  @Test
  fun generateThumbnail_shouldReturnNull_whenExceptionOccurs() {
    // Given
    val videoPath = "/storage/videos/test.mp4"
    `when`(retriever.setDataSource(videoPath)).thenThrow(RuntimeException("Error"))

    // When
    val result = VideoThumbnailUtil.generateThumbnail(context, videoPath, retrieverFactory)

    // Then
    assertNull(result)
    verify(retriever).release()
  }
}
