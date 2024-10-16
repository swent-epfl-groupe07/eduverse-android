import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.camera.imageBitmapToByteArray
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageConversionTest {

  @Test
  fun imageBitmapToByteArray_convertsImageBitmap_toByteArray() {
    // Arrange
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val imageBitmap = bitmap.asImageBitmap()

    // Act
    val byteArray = imageBitmapToByteArray(imageBitmap)

    // Assert
    assertTrue(byteArray.isNotEmpty())
  }

  @Test
  fun imageBitmapToByteArray_convertsDifferentBitmapConfigs() {
    // Arrange
    val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.RGB_565)
    val imageBitmap = bitmap.asImageBitmap()

    // Act
    val byteArray = imageBitmapToByteArray(imageBitmap)

    // Assert
    assertTrue(byteArray.isNotEmpty())
  }

  @Test
  fun imageBitmapToByteArray_generatesNonEmptyByteArray() {
    // Arrange
    val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
    val imageBitmap = bitmap.asImageBitmap()

    // Act
    val byteArray = imageBitmapToByteArray(imageBitmap)

    // Assert
    assertTrue("Byte array should not be empty after compression", byteArray.isNotEmpty())
    assertTrue("Byte array should contain compressed data", byteArray.size > 0)
  }
}
