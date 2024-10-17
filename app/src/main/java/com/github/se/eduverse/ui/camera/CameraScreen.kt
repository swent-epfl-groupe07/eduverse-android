package com.github.se.eduverse.ui.camera

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
            modifier = Modifier.fillMaxSize().testTag("cameraPreview")
        )

        Image(
            painter = painterResource(id = R.drawable.close),
            contentDescription = "Close",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(32.dp) // Augmente la taille pour rendre l'icône plus grande
                .clickable { navigationActions.goBack() }
                .testTag("closeButton")
        )

        Image(
            painter = painterResource(id = R.drawable.flip_camera_ios),
            contentDescription = "Switch Camera",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(32.dp) // Augmente la taille pour rendre l'icône plus grande
                .clickable {
                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }
                .testTag("switchCameraButton")
        )

        Image(
            painter = painterResource(id = R.drawable.radio_button_checked),
            contentDescription = "Take Photo",
            modifier = Modifier
                .size(120.dp) // Augmente la taille pour agrandir le bouton
                .align(Alignment.BottomCenter)
                .clickable {
                    CoroutineScope(Dispatchers.IO).launch {
                        val photoFile = File(context.filesDir, "photo.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        imageCapture?.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    Log.e("IMAGE SAVED", "IMAGE SAVED")
                                    val encodedPath = Uri.encode(photoFile.absolutePath)
                                    navigationActions.navigateTo("picTaken/$encodedPath")
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("IMAGE NOT SAVED", "IMAGE NOT SAVED")
                                }
                            }
                        )
                    }
                }
                .padding(4.dp)
                .testTag("takePhotoButton")
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp), // Ajuste pour remonter les boutons
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Photo",
                modifier = Modifier
                    .background(Color(0xFFD6DCE5), CircleShape)
                    .padding(8.dp)
                    .clickable { /* Switch to Photo Mode */ },
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(32.dp)) // Espacement entre les boutons
            Text(
                text = "Video",
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { /* Switch to Video Mode */ },
                color = Color.Gray
            )
        }
    }
}
