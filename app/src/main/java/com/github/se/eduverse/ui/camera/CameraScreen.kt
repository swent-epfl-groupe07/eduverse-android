package com.github.se.eduverse.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.github.se.eduverse.R
import com.github.se.eduverse.ui.navigation.NavigationActions
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(navigationActions: NavigationActions) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

  var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
  var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
  var recording by remember { mutableStateOf<Recording?>(null) }
  val preview = remember { Preview.Builder().build() }
  var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
  var isVideoMode by remember { mutableStateOf(false) } // To track Photo/Video mode
  var isRecording by remember { mutableStateOf(false) } // To track if recording is in progress
  var clickCount by remember { mutableStateOf(0) } // Counter to track clicks
  var isCameraSelfie by remember {
    mutableStateOf(false)
  } // To track if the front camera is being used

  LaunchedEffect(cameraSelector) {
    val cameraProvider = cameraProviderFuture.get()
    cameraProvider.unbindAll()

    // Configure video capture
    val recorder =
        Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()

    videoCapture = VideoCapture.withOutput(recorder)
    imageCapture = ImageCapture.Builder().build()

    try {
      cameraProvider.bindToLifecycle(
          lifecycleOwner, cameraSelector, preview, imageCapture, videoCapture)
    } catch (exc: Exception) {
      exc.printStackTrace()
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    AndroidView(
        factory = { ctx ->
          val previewView = PreviewView(ctx).apply { id = android.R.id.content }
          preview.setSurfaceProvider(previewView.surfaceProvider)
          previewView
        },
        modifier = Modifier.fillMaxSize().testTag("cameraPreview"))

    // Close button
    Image(
        painter = painterResource(id = R.drawable.close),
        contentDescription = "Close",
        modifier =
            Modifier.align(Alignment.TopStart)
                .padding(16.dp)
                .size(32.dp)
                .clickable { navigationActions.goBackToPreviousRoute() }
                .testTag("closeButton"))

    // Button to switch camera (front/back)
    Image(
        painter = painterResource(id = R.drawable.flip_camera_ios),
        contentDescription = "Switch Camera",
        modifier =
            Modifier.align(Alignment.TopEnd)
                .padding(16.dp)
                .size(32.dp)
                .clickable {
                  cameraSelector =
                      if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        isCameraSelfie = true // Front camera activated
                        CameraSelector.DEFAULT_FRONT_CAMERA
                      } else {
                        isCameraSelfie = false // Back camera activated
                        CameraSelector.DEFAULT_BACK_CAMERA
                      }
                }
                .testTag("switchCameraButton"))

    // Button to take a photo or start/stop video recording
    Image(
        painter = painterResource(id = R.drawable.radio_button_checked),
        contentDescription = if (isVideoMode) "Start/Stop Recording Video" else "Take Photo",
        modifier =
            Modifier.size(120.dp)
                .align(Alignment.BottomCenter)
                .clickable {
                  if (isVideoMode) {
                    clickCount++
                    if (clickCount == 1) {
                      // Start video recording on first click
                      val videoFile = File(context.filesDir, "video.mp4")
                      val outputOptions = FileOutputOptions.Builder(videoFile).build()

                      recording =
                          videoCapture?.output?.prepareRecording(context, outputOptions)?.start(
                              ContextCompat.getMainExecutor(context)) {
                                // Recording is in progress
                                Log.e("VIDEO RECORDING", "Recording started")
                              }

                      isRecording = true

                      // Limit recording duration to 1 minute
                      CoroutineScope(Dispatchers.IO).launch {
                        delay(60_000L) // 60 seconds
                        if (isRecording) {
                          recording?.stop() // Automatically stop after 1 minute
                          isRecording = false
                          clickCount = 0

                          // Add delay to ensure video file is properly written
                          CoroutineScope(Dispatchers.IO).launch {
                            delay(2000) // Wait 2 seconds to ensure video file is saved
                            val videoFile = File(context.filesDir, "video.mp4")
                            val encodedPath = Uri.encode(videoFile.absolutePath)
                            CoroutineScope(Dispatchers.Main).launch {
                              navigationActions.navigateTo("picTaken/null?videoPath=$encodedPath")
                            }
                          }
                        }
                      }
                    } else if (clickCount == 2) {
                      // Stop recording on second click
                      recording?.stop()
                      isRecording = false
                      clickCount = 0 // Reset the counter

                      // Add delay to ensure video file is properly written
                      CoroutineScope(Dispatchers.IO).launch {
                        delay(2000) // Wait 2 seconds to ensure video file is saved
                        val videoFile = File(context.filesDir, "video.mp4")
                        val encodedPath = Uri.encode(videoFile.absolutePath)
                        CoroutineScope(Dispatchers.Main).launch {
                          navigationActions.navigateTo("picTaken/null?videoPath=$encodedPath")
                        }
                      }
                    }
                  } else {
                    // Logic to take a photo
                    CoroutineScope(Dispatchers.IO).launch {
                      val photoFile = File(context.filesDir, "photo.jpg")
                      val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                      imageCapture?.takePicture(
                          outputOptions,
                          ContextCompat.getMainExecutor(context),
                          object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(
                                outputFileResults: ImageCapture.OutputFileResults
                            ) {
                              Log.e("IMAGE SAVED", "IMAGE SAVED")

                              // Load the bitmap of the saved image
                              val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                              // Apply 180° rotation if selfie camera was used
                              val finalBitmap =
                                  if (isCameraSelfie) {
                                    mirrorImage(
                                        rotateImageIfSelfie(
                                            bitmap,
                                            isCameraSelfie)) // Apply rotation and mirror for selfie
                                  } else {
                                    bitmap // Use original bitmap if not a selfie
                                  }

                              // Save the corrected image
                              val outputStream = FileOutputStream(photoFile)
                              finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                              outputStream.flush()
                              outputStream.close()

                              val encodedPath = Uri.encode(photoFile.absolutePath)
                              navigationActions.navigateTo("picTaken/$encodedPath")
                            }

                            override fun onError(exception: ImageCaptureException) {
                              Log.e("IMAGE NOT SAVED", "IMAGE NOT SAVED")
                            }
                          })
                    }
                  }
                }
                .padding(4.dp)
                .testTag("takePhotoButton"))

    Row(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp),
        horizontalArrangement = Arrangement.Center) {
          // Photo button
          Text(
              text = "Photo",
              modifier =
                  Modifier.background(
                          if (!isVideoMode) Color(0xFFD6DCE5) else Color.Transparent, CircleShape)
                      .padding(8.dp)
                      .clickable {
                        isVideoMode = false // Switch to Photo mode
                      }
                      .testTag("photoButton"),
              color = if (!isVideoMode) Color.Black else Color.Gray)

          Spacer(modifier = Modifier.width(32.dp))

          // Video button
          Text(
              text = "Video",
              modifier =
                  Modifier.background(
                          if (isVideoMode) Color(0xFFD6DCE5) else Color.Transparent, CircleShape)
                      .padding(8.dp)
                      .clickable {
                        isVideoMode = true // Switch to Video mode
                      }
                      .testTag("videoButton"),
              color = if (isVideoMode) Color.Black else Color.Gray)
        }
  }
}

// Function to rotate the image if taken with the front camera
fun rotateImageIfSelfie(bitmap: Bitmap, isSelfie: Boolean): Bitmap {
  if (isSelfie) {
    val matrix =
        Matrix().apply {
          postRotate(180f) // Apply a 180-degree rotation
        }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }
  return bitmap
}

fun mirrorImage(bitmap: Bitmap): Bitmap {
  val matrix =
      Matrix().apply {
        preScale(1f, -1f) // Flip horizontally
      }
  return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
