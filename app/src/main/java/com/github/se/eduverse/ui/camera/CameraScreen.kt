package com.github.se.eduverse.ui.camera

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.github.se.eduverse.ui.navigation.NavigationActions
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(navigationActions: NavigationActions) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

  var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
  val preview = remember { Preview.Builder().build() }
  var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

  LaunchedEffect(cameraSelector) {
    val cameraProvider = cameraProviderFuture.get()
    cameraProvider.unbindAll()
    imageCapture = ImageCapture.Builder().build()

    try {
      cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
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

    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Close",
        modifier =
            Modifier.align(Alignment.TopStart)
                .padding(16.dp)
                .size(24.dp)
                .clickable { navigationActions.goBack() }
                .testTag("closeButton"))

    Icon(
        imageVector = Icons.Default.Cameraswitch,
        contentDescription = "Switch Camera",
        modifier =
            Modifier.align(Alignment.TopEnd)
                .padding(16.dp)
                .size(24.dp)
                .clickable {
                  cameraSelector =
                      if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                      } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                      }
                }
                .testTag("switchCameraButton"))

    Column(
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          // Ajout des boutons Photo et Vidéo
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
              horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { /* Logique pour le mode Photo */},
                    modifier = Modifier.padding(horizontal = 8.dp).testTag("photoButton")) {
                      Text("Photo Mode")
                    }
                Button(
                    onClick = { /* Logique pour le mode Vidéo */},
                    modifier = Modifier.padding(horizontal = 8.dp).testTag("videoButton")) {
                      Text("Video Mode")
                    }
              }

          Spacer(modifier = Modifier.height(16.dp))

          // Bouton "Take Photo"
          Button(
              onClick = {
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
                          val encodedPath = Uri.encode(photoFile.absolutePath)
                          navigationActions.navigateTo("picTaken/$encodedPath")
                        }

                        override fun onError(exception: ImageCaptureException) {
                          Log.e("IMAGE NOT SAVED", "IMAGE NOT SAVED")
                        }
                      })
                }
              },
              modifier =
                  Modifier.padding(horizontal = 32.dp).fillMaxWidth().testTag("takePhotoButton")) {
                Text("Take Photo")
              }

          Spacer(modifier = Modifier.height(24.dp))

          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                Box(
                    modifier =
                        Modifier.size(48.dp)
                            .background(Color.Gray)
                            .padding(8.dp)
                            .testTag("rectangleLeft"))

                Box(
                    modifier =
                        Modifier.size(48.dp)
                            .background(Color.Gray)
                            .padding(8.dp)
                            .testTag("rectangleRight"))
              }
        }
  }
}
