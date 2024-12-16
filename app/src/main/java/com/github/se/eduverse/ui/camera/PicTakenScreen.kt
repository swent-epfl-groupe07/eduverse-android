package com.github.se.eduverse.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.github.se.eduverse.R
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun PicTakenScreen(
    photoFile: File?, // Photo file
    videoFile: File?, // Video file
    navigationActions: NavigationActions,
    photoViewModel: PhotoViewModel,
    videoViewModel: VideoViewModel
) {
  val context = LocalContext.current
  val auth = FirebaseAuth.getInstance()
  val ownerId = auth.currentUser?.uid ?: "anonymous"

  // Use the appropriate path for photos and videos
  val mediaType = if (photoFile != null) "photos" else "videos"
  val path =
      "$mediaType/$ownerId/${System.currentTimeMillis()}.${
            if (photoFile != null) "jpg" else "mp4"
        }"

  val bitmap =
      photoFile?.let {
        BitmapFactory.decodeFile(it.path)?.let { adjustImageRotation(it) }?.asImageBitmap()
      }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(8.dp)
              .testTag("picTakenScreenBox")) {
        if (bitmap != null) {
          // Display the photo with rounded corners
          Image(
              bitmap = bitmap,
              contentDescription = "Captured Photo",
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.fillMaxWidth()
                      .fillMaxHeight(0.9f)
                      .clip(RoundedCornerShape(12.dp))
                      .background(Color.LightGray)
                      .testTag("capturedImage"))

          // Crop and settings icons
          Column(
              modifier = Modifier.align(Alignment.TopEnd),
              verticalArrangement = Arrangement.spacedBy(30.dp)) {
                Spacer(modifier = Modifier.height(0.5.dp))
                Image(
                    painter = painterResource(id = R.drawable.vector),
                    contentDescription = "Crop Photo",
                    modifier =
                        Modifier.size(36.dp)
                            .clickable {
                              val encodedPath = Uri.encode(photoFile.absolutePath)
                              navigationActions.navigateTo("cropPhotoScreen/$encodedPath")
                            }
                            .testTag("cropIcon"))
                Image(
                    painter = painterResource(id = R.drawable.settings),
                    contentDescription = "Filters",
                    modifier = Modifier.size(36.dp).clickable {}.testTag("settingsIcon"))
              }
        } else if (videoFile != null) {
          // Use ExoPlayer to display the video in loop with ContentScale.Crop
          val videoUri = Uri.fromFile(videoFile)
          val player = remember {
            ExoPlayer.Builder(context).build().apply {
              setMediaItem(MediaItem.fromUri(videoUri))
              repeatMode = ExoPlayer.REPEAT_MODE_ONE // Loop the video
              prepare()
              playWhenReady = true
            }
          }

          DisposableEffect(Unit) {
            player.playWhenReady = true
            player.prepare()

            onDispose { player.release() }
          }

          // Add PlayerView with ContentScale.Crop
          AndroidView(
              factory = {
                PlayerView(context).apply {
                  this.player = player
                  useController = false // Hide playback controls
                  layoutParams =
                      ViewGroup.LayoutParams(
                          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                  scaleX = 1.2f // Adjust scale to mimic ContentScale.Crop
                  scaleY = 1.2f // Adjust scale to mimic ContentScale.Crop
                }
              },
              modifier =
                  Modifier.fillMaxWidth()
                      .fillMaxHeight(0.9f)
                      .align(Alignment.TopCenter)
                      .clip(RoundedCornerShape(12.dp)) // Rounded corners same as photo
                      .background(Color.LightGray)
                      .testTag("videoPlayer"))
        } else {
          // Case when neither photo nor video is provided
          Image(
              painter = painterResource(id = R.drawable.google_logo),
              contentDescription = "Google Logo",
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.fillMaxWidth()
                      .fillMaxHeight(0.8f)
                      .align(Alignment.Center)
                      .clip(RoundedCornerShape(12.dp)) // Rounded corners
                      .background(Color.LightGray)
                      .testTag("googleLogoImage"))
        }

        // Save and Next buttons (updated to handle videos)
        Row(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              Button(
                  onClick = {
                    bitmap?.let {
                      val byteArray = imageBitmapToByteArray(it)
                      val photo = Photo(ownerId, byteArray, path)
                      photoViewModel.savePhoto(
                          photo, onSuccess = { mediaSavedToast(context, "Photo") })
                      navigationActions.goBack()
                      navigationActions.goBack()
                    }

                    videoFile?.let {
                      val videoByteArray = it.readBytes() // Convert video file to byte array
                      val video = Video(ownerId, videoByteArray, path.replace(".jpg", ".mp4"))
                      videoViewModel.saveVideo(
                          video, onSuccess = { mediaSavedToast(context, "Video") })
                      navigationActions.goBack()
                      navigationActions.goBack()
                    }
                  },
                  modifier = Modifier.weight(1f).height(56.dp).testTag("saveButton"),
                  colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                  shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "Save",
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight(500)),
                        modifier = Modifier.padding(horizontal = 8.dp))
                  }

              Button(
                  onClick = {
                    val encodedPhotoPath = photoFile?.absolutePath?.let { Uri.encode(it) }
                    val encodedVideoPath = videoFile?.absolutePath?.let { Uri.encode(it) }

                    // If photo exists, pass the photo path, otherwise pass the video path
                    navigationActions.navigateTo("nextScreen/$encodedPhotoPath/$encodedVideoPath")
                  },
                  modifier = Modifier.weight(1f).height(56.dp).testTag("nextButton"),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "Next",
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight(500)),
                        modifier = Modifier.padding(horizontal = 8.dp))
                  }
            }

        // Close button
        Box(
            modifier =
                Modifier.align(Alignment.TopStart)
                    .size(56.dp)
                    .clickable { navigationActions.goBack() }
                    .padding(8.dp)
                    .testTag("closeButton")) {
              Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close",
                  tint = Color.White,
                  modifier = Modifier.size(50.dp).align(Alignment.Center))
            }
      }
}

fun imageBitmapToByteArray(imageBitmap: ImageBitmap): ByteArray {
  val bitmap = imageBitmap.asAndroidBitmap()
  val byteArrayOutputStream = ByteArrayOutputStream()
  bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
  return byteArrayOutputStream.toByteArray()
}

fun adjustImageRotation(bitmap: Bitmap): Bitmap {
  val matrix = Matrix()
  matrix.postRotate(90f)
  return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun mediaSavedToast(context: Context, media: String) {
  Toast.makeText(context, "$media saved successfully", Toast.LENGTH_SHORT).show()
}
