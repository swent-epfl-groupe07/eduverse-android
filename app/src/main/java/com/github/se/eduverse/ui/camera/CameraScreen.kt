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
import androidx.compose.foundation.layout.*
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
  var isVideoMode by remember { mutableStateOf(false) } // Pour suivre le mode Photo/Vidéo
  var isRecording by remember {
    mutableStateOf(false)
  } // Pour suivre si l'enregistrement est en cours
  var clickCount by remember { mutableStateOf(0) } // Compteur pour suivre les clics
  var isCameraSelfie by remember {
    mutableStateOf(false)
  } // Pour suivre si la caméra frontale est utilisée

  LaunchedEffect(cameraSelector) {
    val cameraProvider = cameraProviderFuture.get()
    cameraProvider.unbindAll()

    // Configurer la capture de vidéo
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

    // Bouton pour fermer
    Image(
        painter = painterResource(id = R.drawable.close),
        contentDescription = "Close",
        modifier =
            Modifier.align(Alignment.TopStart)
                .padding(16.dp)
                .size(32.dp)
                .clickable { navigationActions.goBack() }
                .testTag("closeButton"))

    // Bouton pour changer de caméra (avant/arrière)
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
                        isCameraSelfie = true // Caméra frontale activée
                        CameraSelector.DEFAULT_FRONT_CAMERA
                      } else {
                        isCameraSelfie = false // Caméra arrière activée
                        CameraSelector.DEFAULT_BACK_CAMERA
                      }
                }
                .testTag("switchCameraButton"))

    // Bouton pour prendre une photo ou enregistrer une vidéo
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
                      // Démarrer l'enregistrement vidéo au premier clic
                      val videoFile = File(context.filesDir, "video.mp4")
                      val outputOptions = FileOutputOptions.Builder(videoFile).build()

                      recording =
                          videoCapture?.output?.prepareRecording(context, outputOptions)?.start(
                              ContextCompat.getMainExecutor(context)) {
                                // L'enregistrement est en cours
                                Log.e("VIDEO RECORDING", "Recording started")
                              }

                      isRecording = true

                      // Limiter la durée de l'enregistrement à 1 minute
                      CoroutineScope(Dispatchers.IO).launch {
                        delay(60_000L) // 60 secondes
                        if (isRecording) {
                          recording?.stop() // Arrêter automatiquement après 1 minute
                          isRecording = false
                          clickCount = 0

                          // Ajouter un délai pour s'assurer que le fichier vidéo est bien écrit
                          CoroutineScope(Dispatchers.IO).launch {
                            delay(
                                2000) // Attendre 2 secondes pour être sûr que le fichier vidéo est
                            // bien enregistré
                            val videoFile = File(context.filesDir, "video.mp4")
                            val encodedPath = Uri.encode(videoFile.absolutePath)
                            CoroutineScope(Dispatchers.Main).launch {
                              navigationActions.navigateTo("picTaken/null?videoPath=$encodedPath")
                            }
                          }
                        }
                      }
                    } else if (clickCount == 2) {
                      // Arrêter l'enregistrement au deuxième clic
                      recording?.stop()
                      isRecording = false
                      clickCount = 0 // Remettre le compteur à 0

                      // Ajouter un délai pour s'assurer que le fichier vidéo est bien écrit
                      CoroutineScope(Dispatchers.IO).launch {
                        delay(
                            2000) // Attendre 2 secondes pour être sûr que le fichier vidéo est bien
                        // enregistré
                        val videoFile = File(context.filesDir, "video.mp4")
                        val encodedPath = Uri.encode(videoFile.absolutePath)
                        CoroutineScope(Dispatchers.Main).launch {
                          navigationActions.navigateTo("picTaken/null?videoPath=$encodedPath")
                        }
                      }
                    }
                  } else {
                    // Logique pour prendre la photo
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

                              // Charger le bitmap de l'image sauvegardée
                              val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                              // Appliquer la rotation de 180° si la caméra selfie a été utilisée
                              val finalBitmap =
                                  if (isCameraSelfie) {
                                    mirrorImage(
                                        rotateImageIfSelfie(
                                            bitmap,
                                            isCameraSelfie)) // Appliquer la rotation et le miroir
                                    // si selfie
                                  } else {
                                    bitmap // Utiliser le bitmap original si ce n'est pas un selfie
                                  } // val finalBitmap = bitmap

                              // Sauvegarder l'image corrigée
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
          // Bouton Photo
          Text(
              text = "Photo",
              modifier =
                  Modifier.background(
                          if (!isVideoMode) Color(0xFFD6DCE5) else Color.Transparent, CircleShape)
                      .padding(8.dp)
                      .clickable {
                        isVideoMode = false // Bascule en mode Photo
                      }
                      .testTag("photoButton"),
              color = if (!isVideoMode) Color.Black else Color.Gray)

          Spacer(modifier = Modifier.width(32.dp))

          // Bouton Vidéo
          Text(
              text = "Vidéo",
              modifier =
                  Modifier.background(
                          if (isVideoMode) Color(0xFFD6DCE5) else Color.Transparent, CircleShape)
                      .padding(8.dp)
                      .clickable {
                        isVideoMode = true // Bascule en mode Vidéo
                      }
                      .testTag("videoButton"),
              color = if (isVideoMode) Color.Black else Color.Gray)
        }
  }
}

// Fonction pour ajuster l'image si elle a été prise en selfie
fun rotateImageIfSelfie(bitmap: Bitmap, isSelfie: Boolean): Bitmap {
  if (isSelfie) {
    val matrix =
        Matrix().apply {
          postRotate(180f) // Appliquer une rotation de 180 degrés
        }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }
  return bitmap
}

fun mirrorImage(bitmap: Bitmap): Bitmap {
  val matrix =
      Matrix().apply {
        preScale(1f, -1f) // Flip horizontal
      }
  return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
