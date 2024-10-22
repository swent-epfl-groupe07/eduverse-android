package com.github.se.eduverse.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun PicTakenScreen(
    photoFile: File?, // Fichier photo
    videoFile: File?, // Fichier vidéo
    navigationActions: NavigationActions,
    viewModel: PhotoViewModel
) {
  val context = LocalContext.current
  val auth = FirebaseAuth.getInstance()
  val ownerId = auth.currentUser?.uid ?: "anonymous"
  val path = "media/$ownerId/${System.currentTimeMillis()}.jpg"

  val bitmap =
      photoFile?.let {
        BitmapFactory.decodeFile(it.path)?.let { adjustImageRotation(it) }?.asImageBitmap()
      }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Color(0xFFEBF1F4)) // Set background color to #EBF1F4
              .padding(8.dp)
              .testTag("picTakenScreenBox")) {
        if (bitmap != null) {
          // Afficher la photo avec les bords arrondis
          Image(
              bitmap = bitmap,
              contentDescription = "Captured Photo",
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.fillMaxWidth()
                      .fillMaxHeight(0.9f)
                      .align(Alignment.TopCenter)
                      .clip(RoundedCornerShape(12.dp)) // Bords arrondis
                      .background(Color.LightGray)
                      .testTag("capturedImage"))
        } else if (videoFile != null) {
          // Utiliser ExoPlayer pour afficher la vidéo en boucle avec ContentScale.Crop
          val videoUri = Uri.fromFile(videoFile)
          val player = remember {
            ExoPlayer.Builder(context).build().apply {
              setMediaItem(MediaItem.fromUri(videoUri))
              repeatMode = ExoPlayer.REPEAT_MODE_ONE // Répéter la vidéo en boucle
              prepare()
              playWhenReady = true
            }
          }

          DisposableEffect(Unit) {
            player.playWhenReady = true
            player.prepare()

            onDispose { player.release() }
          }

          // Ajout du PlayerView avec ContentScale.Crop
          AndroidView(
              factory = {
                PlayerView(context).apply {
                  this.player = player
                  useController = false // Masquer les contrôles de lecture
                  layoutParams =
                      ViewGroup.LayoutParams(
                          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                  scaleX = 1.2f // Ajustement du scale pour imiter ContentScale.Crop
                  scaleY = 1.2f // Ajustement du scale pour imiter ContentScale.Crop
                }
              },
              modifier =
                  Modifier.fillMaxWidth()
                      .fillMaxHeight(0.9f)
                      .align(Alignment.TopCenter)
                      .clip(RoundedCornerShape(12.dp)) // Bords arrondis identiques à la photo
                      .background(Color.LightGray)
                      .testTag("videoPlayer"))
        } else {
          // Cas où ni la photo ni la vidéo n'est fournie
          Image(
              painter = painterResource(id = R.drawable.google_logo),
              contentDescription = "Google Logo",
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.fillMaxWidth()
                      .fillMaxHeight(0.8f)
                      .align(Alignment.Center)
                      .clip(RoundedCornerShape(12.dp)) // Bords arrondis
                      .background(Color.LightGray)
                      .testTag("googleLogoImage"))
        }

        // Reste du contenu comme les icônes de crop et settings (inchangé)
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Image(
                  painter = painterResource(id = R.drawable.vector),
                  contentDescription = "Crop Photo",
                  modifier = Modifier.size(30.dp).clickable {}.testTag("cropIcon"))
              Image(
                  painter = painterResource(id = R.drawable.settings),
                  contentDescription = "Filters",
                  modifier = Modifier.size(40.dp).clickable {}.testTag("settingsIcon"))
            }

        // Boutons Save et Next (inchangé)
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
                      viewModel.savePhoto(photo)
                      navigationActions.goBack()
                      navigationActions.goBack()
                    }
                  },
                  modifier = Modifier.weight(1f).height(56.dp).testTag("saveButton"),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC6D3E1)),
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
                  onClick = {
                    val encodedPhotoPath = photoFile?.absolutePath?.let { Uri.encode(it) }
                    val encodedVideoPath = videoFile?.absolutePath?.let { Uri.encode(it) }

                    // Si la photo existe, on passe le chemin de la photo, sinon celui de la vidéo
                    navigationActions.navigateTo("nextScreen/$encodedPhotoPath/$encodedVideoPath")
                  },
                  modifier = Modifier.weight(1f).height(56.dp).testTag("nextButton"),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37CED5)),
                  shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "Next",
                        style =
                            TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight(500),
                                color = Color(0xFF4A4459)),
                        modifier = Modifier.padding(horizontal = 8.dp))
                  }
            }

        // Bouton pour fermer l'écran (inchangé)
        Box(
            modifier =
                Modifier.align(Alignment.TopStart)
                    .size(40.dp)
                    .clickable { navigationActions.goBack() }
                    .padding(8.dp)
                    .testTag("closeButton")) {
              Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close",
                  tint = Color.White,
                  modifier = Modifier.size(32.dp).align(Alignment.Center))
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
