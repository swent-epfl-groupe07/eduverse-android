package com.github.se.eduverse.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream

@Composable
fun CropPhotoScreen(
    photoFile: File,
    photoViewModel: PhotoViewModel,
    navigationActions: NavigationActions
) {
  val originalBitmap = adjustImageRotation(BitmapFactory.decodeFile(photoFile.path)).asImageBitmap()
  val auth = FirebaseAuth.getInstance()
  val ownerId = auth.currentUser?.uid ?: "anonymous"
  val path = "photos/$ownerId/${System.currentTimeMillis()}.jpg"

  val context = LocalContext.current

  // Variables to hold the actual size of the Image composable
  var imageWidth by remember { mutableStateOf(0f) }
  var imageHeight by remember { mutableStateOf(0f) }

  // Corner points for crop area
  var topLeft by remember { mutableStateOf(Offset.Zero) }
  var topRight by remember { mutableStateOf(Offset.Zero) }
  var bottomLeft by remember { mutableStateOf(Offset.Zero) }
  var bottomRight by remember { mutableStateOf(Offset.Zero) }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEBF1F4)).padding(8.dp)) {
    Image(
        bitmap = originalBitmap,
        contentDescription = "Photo to Crop",
        contentScale = ContentScale.FillBounds,
        modifier =
            Modifier.fillMaxWidth()
                .fillMaxHeight(0.9f)
                .onSizeChanged {
                  imageWidth = it.width.toFloat()
                  imageHeight = it.height.toFloat()
                }
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray)
                .testTag("cropImage"))

    // Initialize corner points after image dimensions are known
    LaunchedEffect(imageWidth, imageHeight) {
      if (imageWidth > 0f && imageHeight > 0f) {
        topLeft = Offset(imageWidth * 0.2f, imageHeight * 0.2f)
        topRight = Offset(imageWidth * 0.8f, imageHeight * 0.2f)
        bottomLeft = Offset(imageWidth * 0.2f, imageHeight * 0.8f)
        bottomRight = Offset(imageWidth * 0.8f, imageHeight * 0.8f)
      }
    }

    Canvas(
        modifier =
            Modifier.fillMaxWidth()
                .fillMaxHeight(0.9f)
                // .align(Alignment.Center)
                .pointerInput(Unit) {
                  detectDragGestures { change, dragAmount ->
                    val position = change.position

                    fun restrictOffset(
                        offset: Offset,
                        minX: Float,
                        maxX: Float,
                        minY: Float,
                        maxY: Float
                    ): Offset {
                      return Offset(
                          x = offset.x.coerceIn(minX, maxX), y = offset.y.coerceIn(minY, maxY))
                    }

                    val minX = 0f
                    val maxX = imageWidth
                    val minY = 0f
                    val maxY = imageHeight

                    when {
                      position.isNear(topLeft) -> {
                        val newTopLeft = topLeft + dragAmount
                        topLeft =
                            restrictOffset(
                                newTopLeft, minX, topRight.x - 50f, minY, bottomLeft.y - 50f)
                        bottomLeft = bottomLeft.copy(x = topLeft.x)
                        topRight = topRight.copy(y = topLeft.y)
                      }
                      position.isNear(topRight) -> {
                        val newTopRight = topRight + dragAmount
                        topRight =
                            restrictOffset(
                                newTopRight, topLeft.x + 50f, maxX, minY, bottomRight.y - 50f)
                        bottomRight = bottomRight.copy(x = topRight.x)
                        topLeft = topLeft.copy(y = topRight.y)
                      }
                      position.isNear(bottomLeft) -> {
                        val newBottomLeft = bottomLeft + dragAmount
                        bottomLeft =
                            restrictOffset(
                                newBottomLeft, minX, bottomRight.x - 50f, topLeft.y + 50f, maxY)
                        topLeft = topLeft.copy(x = bottomLeft.x)
                        bottomRight = bottomRight.copy(y = bottomLeft.y)
                      }
                      position.isNear(bottomRight) -> {
                        val newBottomRight = bottomRight + dragAmount
                        bottomRight =
                            restrictOffset(
                                newBottomRight, bottomLeft.x + 50f, maxX, topRight.y + 50f, maxY)
                        topRight = topRight.copy(x = bottomRight.x)
                        bottomLeft = bottomLeft.copy(y = bottomRight.y)
                      }
                    }
                  }
                }) {
          drawLine(Color.White, topLeft, topRight, strokeWidth = 4.dp.toPx())
          drawLine(Color.White, topRight, bottomRight, strokeWidth = 4.dp.toPx())
          drawLine(Color.White, bottomRight, bottomLeft, strokeWidth = 4.dp.toPx())
          drawLine(Color.White, bottomLeft, topLeft, strokeWidth = 4.dp.toPx())

          drawCornerHandle(topLeft, "topLeft")
          drawCornerHandle(topRight, "topRight")
          drawCornerHandle(bottomLeft, "bottomLeft")
          drawCornerHandle(bottomRight, "bottomRight")
        }

    Row(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Button(
              onClick = {
                val cropRect =
                    Rect(
                        topLeft.x.toInt(),
                        topLeft.y.toInt(),
                        bottomRight.x.toInt(),
                        bottomRight.y.toInt())

                if (cropRect.width() > 0 && cropRect.height() > 0) {
                  // Calculate scaling factors using actual image dimensions
                  val widthScale = originalBitmap.width.toFloat() / imageWidth
                  val heightScale = originalBitmap.height.toFloat() / imageHeight

                  // Calculate adjusted coordinates
                  val adjustedLeft = (cropRect.left * widthScale).toInt()
                  val adjustedTop = (cropRect.top * heightScale).toInt()
                  val adjustedWidth = (cropRect.width() * widthScale).toInt()
                  val adjustedHeight = (cropRect.height() * heightScale).toInt()

                  // Create the cropped bitmap with bounds checking
                  val croppedBitmap =
                      Bitmap.createBitmap(
                          originalBitmap.asAndroidBitmap(),
                          adjustedLeft.coerceIn(0, originalBitmap.width - 1),
                          adjustedTop.coerceIn(0, originalBitmap.height - 1),
                          adjustedWidth.coerceAtMost(originalBitmap.width - adjustedLeft),
                          adjustedHeight.coerceAtMost(originalBitmap.height - adjustedTop))

                  saveCroppedImage(croppedBitmap, photoFile)
                  val croppedImageBitmap = croppedBitmap.asImageBitmap()
                  val byteArray = imageBitmapToByteArray(croppedImageBitmap)
                  val photo = Photo(ownerId, byteArray, path)
                  photoViewModel.savePhoto(photo)
                  navigationActions.goBack()
                  navigationActions.goBack()
                } else {
                  Toast.makeText(context, "Cannot make this crop", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier.weight(1f).height(56.dp).testTag("saveButton"),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37CED5)),
              shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Save",
                    style =
                        TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight(500),
                            color = Color(0xFF4A4459)),
                    modifier = Modifier.padding(horizontal = 8.dp))
              }

          Button(
              onClick = { navigationActions.goBack() },
              modifier = Modifier.weight(1f).height(56.dp).testTag("cancelButton"),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC6D3E1)),
              shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Cancel",
                    style =
                        TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight(500),
                            color = Color(0xFF4A4459)),
                    modifier = Modifier.padding(horizontal = 8.dp))
              }
        }
  }
}

private fun Offset.isNear(target: Offset, threshold: Float = 150f): Boolean {
  return (this - target).getDistance() < threshold
}

// Auxiliary function to draw a corner handle based on the corner's position
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerHandle(
    corner: Offset,
    position: String
) {
  val handleSize = 30.dp.toPx()
  val strokeWidth = 8.dp.toPx()
  when (position) {
    "topLeft" -> {
      drawLine(Color.White, corner, corner + Offset(handleSize, 0f), strokeWidth)
      drawLine(Color.White, corner, corner + Offset(0f, handleSize), strokeWidth)
    }
    "topRight" -> {
      drawLine(Color.White, corner, corner + Offset(-handleSize, 0f), strokeWidth)
      drawLine(Color.White, corner, corner + Offset(0f, handleSize), strokeWidth)
    }
    "bottomLeft" -> {
      drawLine(Color.White, corner, corner + Offset(handleSize, 0f), strokeWidth)
      drawLine(Color.White, corner, corner + Offset(0f, -handleSize), strokeWidth)
    }
    "bottomRight" -> {
      drawLine(Color.White, corner, corner + Offset(-handleSize, 0f), strokeWidth)
      drawLine(Color.White, corner, corner + Offset(0f, -handleSize), strokeWidth)
    }
  }
}
// Function to crop the bitmap
fun cropBitmap(originalBitmap: ImageBitmap, cropRect: Rect): Bitmap {
  val bitmap = originalBitmap.asAndroidBitmap()
  return Bitmap.createBitmap(
      bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
}

// Function to save the cropped image to file
fun saveCroppedImage(croppedBitmap: Bitmap, destinationFile: File) {
  FileOutputStream(destinationFile).use { out ->
    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    out.flush()
  }
}
