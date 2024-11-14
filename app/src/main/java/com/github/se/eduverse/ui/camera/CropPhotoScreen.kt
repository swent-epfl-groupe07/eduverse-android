package com.github.se.eduverse.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
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

  // Define corner points for crop area
  var topLeft by remember { mutableStateOf(Offset(100f, 100f)) }
  var topRight by remember { mutableStateOf(Offset(500f, 100f)) }
  var bottomLeft by remember { mutableStateOf(Offset(100f, 500f)) }
  var bottomRight by remember { mutableStateOf(Offset(500f, 500f)) }

  // Calculate scale factor based on displayed image and original image dimensions
  val context = LocalContext.current
  val displayMetrics = context.resources.displayMetrics
  val displayWidth = displayMetrics.widthPixels
  val displayHeight =
      (displayWidth * (originalBitmap.height / originalBitmap.width.toFloat())).toInt()

  val scaleFactorX = originalBitmap.width / displayWidth.toFloat()
  val scaleFactorY = originalBitmap.height / displayHeight.toFloat()

  // UI Layout for cropping screen
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Color(0xFFEBF1F4)) // Set background color to #EBF1F4
              .padding(8.dp)) {
        // Display the original photo in the background
        Image(
            bitmap = originalBitmap,
            contentDescription = "Photo to Crop",
            contentScale = ContentScale.Crop, // Ensures the image fills the box, cropping as needed
            modifier =
                Modifier.fillMaxWidth()
                    .fillMaxHeight(0.9f) // Makes the image take up full screen height and width
                    .clip(RoundedCornerShape(12.dp)) // Rounded corners if desired
                    .background(Color.LightGray)
                    .testTag("cropImage"))

        Canvas(
            modifier =
                Modifier.fillMaxWidth().fillMaxHeight(0.9f).align(Alignment.Center).pointerInput(
                    Unit) {
                      detectDragGestures { change, dragAmount ->
                        // Move the angle
                        val position = change.position
                        when {
                          position.isNear(topLeft) -> {
                            topLeft += dragAmount
                            // Link the other corners based on the movement of topLeft
                            topRight = Offset(topRight.x, topLeft.y)
                            bottomLeft = Offset(topLeft.x, bottomLeft.y)
                          }
                          position.isNear(topRight) -> {
                            topRight += dragAmount
                            // Link the other corners based on the movement of topRight
                            topLeft = Offset(bottomLeft.x, topRight.y)
                            bottomRight = Offset(topRight.x, bottomLeft.y)
                          }
                          position.isNear(bottomLeft) -> {
                            bottomLeft += dragAmount
                            // Link the other corners based on the movement of bottomLeft
                            bottomRight = Offset(topRight.x, bottomLeft.y)
                            topLeft = Offset(bottomLeft.x, topLeft.y)
                          }
                          position.isNear(bottomRight) -> {
                            bottomRight += dragAmount
                            // Link the other corners based on the movement of bottomRight
                            topRight = Offset(bottomRight.x, topLeft.y)
                            bottomLeft = Offset(topLeft.x, bottomRight.y)
                          }
                        }
                      }
                    }) {
              // Draw the crop area lines
              drawLine(Color.White, topLeft, topRight, strokeWidth = 4.dp.toPx())
              drawLine(Color.White, topRight, bottomRight, strokeWidth = 4.dp.toPx())
              drawLine(Color.White, bottomRight, bottomLeft, strokeWidth = 4.dp.toPx())
              drawLine(Color.White, bottomLeft, topLeft, strokeWidth = 4.dp.toPx())

              // Draw corner "angle" handles
              drawCornerHandle(topLeft, "topLeft")
              drawCornerHandle(topRight, "topRight")
              drawCornerHandle(bottomLeft, "bottomLeft")
              drawCornerHandle(bottomRight, "bottomRight")
            }

        // Save and Cancel buttons
        Row(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              Button(
                  onClick = {
                    // Set the amount of right shift (you can change this value to control the
                    // shift)
                    val xOffset =
                        134f // Change this value to adjust how much you want to shift the crop area
                    // to the right
                    val yOffset = (displayHeight * 0.1f) / 2 // Y-axis offset remains unchanged

                    // Adjust the top-left and bottom-right coordinates by adding the xOffset
                    val mappedTopLeft =
                        Offset(
                            (topLeft.x + xOffset) * scaleFactorX, // Apply the right shift here
                            (topLeft.y - yOffset) * scaleFactorY)
                    val mappedBottomRight =
                        Offset(
                            (bottomRight.x + xOffset) * scaleFactorX, // Apply the right shift here
                            (bottomRight.y - yOffset) * scaleFactorY)

                    val cropRect =
                        Rect(
                            mappedTopLeft.x.toInt(),
                            mappedTopLeft.y.toInt(),
                            mappedBottomRight.x.toInt(),
                            mappedBottomRight.y.toInt())
                    // Crop the image based on the selected corners
                    val croppedBitmap = cropBitmap(originalBitmap, cropRect)
                    saveCroppedImage(croppedBitmap, photoFile)
                    val croppedImageBitmap = croppedBitmap.asImageBitmap()
                    val byteArray = imageBitmapToByteArray(croppedImageBitmap)
                    val photo = Photo(ownerId, byteArray, path)
                    photoViewModel.savePhoto(photo)
                    navigationActions.goBack()
                    navigationActions.goBack()
                    // Callback to go back to CameraScreen
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
                  onClick = { navigationActions.goBack() }, // Callback to go back without saving
                  modifier = Modifier.weight(1f).height(56.dp).testTag("cancelButton"),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC6D3E1)),
                  shape = RoundedCornerShape(8.dp),
              ) {
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

private fun Offset.isNear(target: Offset, threshold: Float = 100f): Boolean {
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
