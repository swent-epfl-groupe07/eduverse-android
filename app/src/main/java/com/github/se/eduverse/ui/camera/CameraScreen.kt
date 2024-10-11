package com.github.se.eduverse.ui.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var imageCapture: ImageCapture? = remember { null }
    val preview = remember { Preview.Builder().build() }

    // State to store the selected camera
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                val cameraProvider = cameraProviderFuture.get()
                val previewView = PreviewView(ctx).apply {
                    id = android.R.id.content
                }
                preview.setSurfaceProvider(previewView.surfaceProvider)

                imageCapture = ImageCapture.Builder().build()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )

                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("cameraPreview") // Test tag pour la caméra
        )

        // Cross (Close) button at the top left
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(24.dp)
                .testTag("closeButton") // Test tag pour le bouton Close
        )

        // Camera switch button at the top right
        Icon(
            imageVector = Icons.Default.Cameraswitch,
            contentDescription = "Switch Camera",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(24.dp)
                .clickable {
                    // Switch between front and back camera
                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    // Re-bind the camera with the new selector
                    cameraProviderFuture.get().unbindAll()
                    cameraProviderFuture.get().bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageCapture
                    )
                }
                .testTag("switchCameraButton") // Test tag pour le bouton de switch
        )

        // Bottom row with buttons and placeholder rectangles
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Buttons to select between Photo and Video modes
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { /* Select Photo mode */ },
                    modifier = Modifier.testTag("photoButton") // Test tag pour le bouton Photo
                ) {
                    Text("Photo")
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = { /* Select Video mode */ },
                    modifier = Modifier.testTag("videoButton") // Test tag pour le bouton Video
                ) {
                    Text("Video")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Row with extreme spacing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp) // Adjust padding as needed
            ) {
                // Rectangle on the left
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Gray).clickable { /*...*/ }
                        .padding(8.dp)
                        .testTag("rectangleLeft") // Test tag pour le rectangle gauche
                )

                // Button for taking a photo, centered between the rectangles
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            val photoFile = File(context.filesDir, "photo.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture?.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        // Handle the saved image here
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        // Handle the error here
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.testTag("takePhotoButton") // Test tag pour le bouton Take Photo
                ) {
                    Text("Take Photo")
                }

                // Rectangle on the right
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Gray).clickable { /*...*/ }
                        .padding(8.dp)
                        .testTag("rectangleRight") // Test tag pour le rectangle droit
                )
            }
        }
    }
}
