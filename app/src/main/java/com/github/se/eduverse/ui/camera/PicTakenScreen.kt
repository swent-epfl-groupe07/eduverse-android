package com.github.se.eduverse.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.R
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import java.io.ByteArrayOutputStream
import java.io.File

fun adjustImageRotation(bitmap: Bitmap): Bitmap {
  val matrix = Matrix()
  matrix.postRotate(90f)
  return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@Composable
fun PicTakenScreen(
    photoFile: File?,
    navigationActions: NavigationActions,
    viewModel: PhotoViewModel
) {
  val ownerId = "user123"
  val path = "photos/$ownerId/${System.currentTimeMillis()}.jpg"

  val bitmap =
      if (photoFile != null && photoFile.exists()) {
        BitmapFactory.decodeFile(photoFile.path)?.let { adjustImageRotation(it) }?.asImageBitmap()
      } else {
        null
      }

  Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    if (bitmap != null) {
      Image(
          bitmap = bitmap,
          contentDescription = "Captured Photo",
          contentScale = ContentScale.Crop,
          modifier =
              Modifier.fillMaxWidth()
                  .fillMaxHeight(0.85f)
                  .align(Alignment.TopCenter)
                  .background(Color.LightGray)
                  .testTag("capturedImage"))
    } else {
      Image(
          painter = painterResource(id = R.drawable.google_logo),
          contentDescription = "Google Logo",
          contentScale = ContentScale.Crop,
          modifier =
              Modifier.fillMaxWidth()
                  .fillMaxHeight(0.85f)
                  .align(Alignment.TopCenter)
                  .background(Color.LightGray)
                  .testTag("googleLogoImage"))
    }

    Column(
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 60.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)) {
          Icon(
              imageVector = Icons.Default.Crop,
              contentDescription = "Crop Photo",
              modifier =
                  Modifier.size(40.dp)
                      .clickable {}
                      .testTag("cropIcon"))
          Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Filters",
              modifier =
                  Modifier.size(40.dp)
                      .clickable { }
                      .testTag("filterIcon"))
        }

    Row(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Button(
              onClick = {
                bitmap?.let {
                  val byteArray = imageBitmapToByteArray(it)
                  val photo = Photo(ownerId, byteArray, path)
                  viewModel.savePhoto(photo)
                  navigationActions.goBack()
                  navigationActions.goBack()
                }
              },
              modifier = Modifier.weight(1f).height(56.dp).testTag("saveButton")) {
                Text("Save")
              }

          Button(
              onClick = {},
              modifier = Modifier.weight(1f).height(56.dp).testTag("publishButton")) {
                Text("Next")
              }
        }

    Box(
        modifier =
            Modifier.align(Alignment.TopStart)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                .clickable { navigationActions.goBack() }
                .padding(8.dp)
                .testTag("closeButton")) {
          Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = Color.White,
              modifier = Modifier.size(24.dp).align(Alignment.Center))
        }
  }
}

fun imageBitmapToByteArray(imageBitmap: ImageBitmap): ByteArray {
  val bitmap = imageBitmap.asAndroidBitmap()
  val byteArrayOutputStream = ByteArrayOutputStream()
  bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
  return byteArrayOutputStream.toByteArray()
}
