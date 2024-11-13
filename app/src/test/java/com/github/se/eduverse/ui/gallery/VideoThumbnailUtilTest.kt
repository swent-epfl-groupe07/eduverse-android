import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.github.se.eduverse.ui.gallery.VideoThumbnailUtil
import java.io.File
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

  @Rule @JvmField val tempFolder = TemporaryFolder()

  @Before
  fun setup() {
    `when`(context.cacheDir).thenReturn(tempFolder.root)
  }

  @Test
  fun generateThumbnail_withValidLocalVideoPath_shouldReturnThumbnailPath() {
    // Given
    val videoPath = "/storage/videos/test.mp4"
    val retriever = mock(MediaMetadataRetriever::class.java)
    `when`(retriever.getFrameAtTime(0)).thenReturn(mockBitmap)

    // When
    val result = VideoThumbnailUtil.generateThumbnail(context, videoPath, retriever)

    // Then
    assertNotNull(result)
    assertTrue(result!!.contains("thumbnail_"))
    verify(mockBitmap).compress(eq(Bitmap.CompressFormat.JPEG), eq(90), any())
    verify(mockBitmap).recycle()
  }

  @Test
  fun `generateThumbnail with valid http url should return thumbnail path`() {
    // Given
    val videoUrl = "http://example.com/video.mp4"
    val retriever = mock(MediaMetadataRetriever::class.java)
    val outputFile = File(tempFolder.root, "thumbnail_${videoUrl.hashCode()}.jpg")
    `when`(retriever.getFrameAtTime(0)).thenReturn(mockBitmap)

    // When
    val result = VideoThumbnailUtil.generateThumbnail(context, videoUrl, retriever)

    // Then
    assertNotNull(result)
    assertTrue(result!!.contains("thumbnail_"))
    verify(mockBitmap).compress(eq(Bitmap.CompressFormat.JPEG), eq(90), any())
    verify(mockBitmap).recycle()
  }

  @Test
  fun `generateThumbnail should return null when bitmap is null`() {
    // Given
    val videoPath = "/storage/videos/test.mp4"
    val retriever = mock(MediaMetadataRetriever::class.java)
    lenient().`when`(retriever.getFrameAtTime(0)).thenReturn(null)

    // When
    val result = VideoThumbnailUtil.generateThumbnail(context, videoPath, retriever)

    // Then
    assertNull(result)
  }

  @Test
  fun `generateThumbnail should return null when exception occurs`() {
    // Given
    val videoPath = "/storage/videos/test.mp4"
    val retriever = mock(MediaMetadataRetriever::class.java)
    lenient().`when`(retriever.getFrameAtTime(0)).thenThrow(RuntimeException("Error"))

    // When
    val result = VideoThumbnailUtil.generateThumbnail(context, videoPath, retriever)

    // Then
    assertNull(result)
  }
}
