package com.github.se.eduverse.ui.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.github.se.eduverse.R
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.github.se.eduverse.viewmodel.VideoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

@OptIn(UnstableApi::class)
@Composable
fun NextScreen(
    photoFile: File?,
    videoFile: File?,
    navigationActions: NavigationActions,
    photoViewModel: PhotoViewModel,
    folderViewModel: FolderViewModel
    videoViewModel: VideoViewModel // Ajout du VideoViewModel
) {
  val context = LocalContext.current
  val auth = FirebaseAuth.getInstance()
  val ownerId = auth.currentUser?.uid ?: "anonymous"
  var folder: Folder? = null

  // Chemin pour sauvegarder l'image ou la vidéo
  val mediaType = if (photoFile != null) "photos" else "videos"
  val path =
      "$mediaType/$ownerId/${System.currentTimeMillis()}.${if (photoFile != null) "jpg" else "mp4"}"

  val bitmap = photoFile?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEBF1F4)).padding(16.dp)) {
    if (bitmap != null) {
      // Affichage de l'image
      Image(
          bitmap = adjustImageRotation(bitmap.asAndroidBitmap()).asImageBitmap(),
          contentDescription = "Preview Image",
          contentScale = ContentScale.Crop,
          modifier =
              Modifier.align(Alignment.TopStart)
                  .padding(top = 56.dp, start = 16.dp)
                  .width(133.dp)
                  .height(164.dp)
                  .clip(RoundedCornerShape(15.dp))
                  .background(color = Color(0xFFD9D9D9))
                  .testTag("previewImage"))
    } else if (videoFile != null) {
      // Affichage de la vidéo
      val videoUri = Uri.fromFile(videoFile)
      val player = remember {
        ExoPlayer.Builder(context).build().apply {
          setMediaItem(MediaItem.fromUri(videoUri))
          repeatMode = ExoPlayer.REPEAT_MODE_ONE
          prepare()
          playWhenReady = true
        }
      }

      DisposableEffect(Unit) {
        player.playWhenReady = true
        player.prepare()

        onDispose { player.release() }
      }

      AndroidView(
          factory = {
            PlayerView(context).apply {
              this.player = player
              useController = false // Cacher les contrôles vidéo
              layoutParams =
                  ViewGroup.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
              resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
          },
          modifier =
              Modifier.align(Alignment.TopStart)
                  .padding(top = 56.dp, start = 16.dp)
                  .width(133.dp)
                  .height(164.dp)
                  .clip(RoundedCornerShape(15.dp))
                  .background(color = Color(0xFFD9D9D9))
                  .testTag("previewVideo"))
    }

    // Autres composants inchangés
    Text(
        text = "Add description...",
        color = Color.Gray,
        fontSize = 24.sp,
        modifier =
            Modifier.align(Alignment.TopStart)
                .padding(start = 16.dp, top = 240.dp)
                .testTag("addDescriptionText"),
        style =
            TextStyle(
                fontSize = 24.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0x424A4459),
                letterSpacing = 0.24.sp,
            ))

    Column(
        modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp, top = 320.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
          StyledButton(
              text = " Add to folder",
              iconRes = R.drawable.add,
              bitmap = bitmap,
              context = context,
              videoFile = videoFile, // Ajout du fichier vidéo
              testTag = "addLinkButton") {
                showBottomMenu(context, folderViewModel) { folder = it }
              }
          StyledButton(
              text = " More options",
              iconRes = R.drawable.more_horiz,
              bitmap = bitmap,
              context = context,
              videoFile = videoFile,
              testTag = "moreOptionsButton")
          StyledButton(
              text = " Share to",
              iconRes = R.drawable.share,
              bitmap = bitmap,
              context = context,
              videoFile = videoFile,
              testTag = "shareToButton")
        }

    // Ajout de la fonctionnalité Save pour photo et vidéo
    Row(
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Button(
              onClick = {
                bitmap?.let { bmp ->
                  val byteArray = imageBitmapToByteArray(bmp)
                  val photo = Photo(ownerId, byteArray, path)
                  photoViewModel.savePhoto(photo, folder) { id, name, folder ->
                    folderViewModel.createFileInFolder(id, name, folder)
                  }
                  navigationActions.goBack()
                  navigationActions.goBack()
                  navigationActions.goBack()
                }

                videoFile?.let { file ->
                  val videoByteArray = file.readBytes()
                  val video = Video(ownerId, videoByteArray, path.replace(".jpg", ".mp4"))
                  videoViewModel.saveVideo(video)
                  navigationActions.goBack()
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
                            color = Color(0xFF4A4459),
                            textAlign = TextAlign.Center),
                    modifier = Modifier.padding(horizontal = 8.dp))
              }

          Button(
              onClick = {
                val title =
                    "My Publication" // Tu peux récupérer ce titre d'un champ d'entrée utilisateur
                // si nécessaire

                // Cas de la photo
                bitmap?.let { bmp ->
                  val byteArray = imageBitmapToByteArray(bmp)
                  val storageRef =
                      FirebaseStorage.getInstance()
                          .reference
                          .child("public/media/${System.currentTimeMillis()}.jpg")

                  storageRef
                      .putBytes(byteArray)
                      .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                          val publication =
                              Publication(
                                  userId = ownerId,
                                  title = title,
                                  thumbnailUrl =
                                      uri.toString(), // L'URL de l'image en tant que vignette
                                  mediaUrl = uri.toString(),
                                  mediaType = MediaType.PHOTO)
                          FirebaseFirestore.getInstance()
                              .collection("publications")
                              .add(publication)
                              .addOnSuccessListener {
                                Toast.makeText(
                                        context, "Photo publiée avec succès", Toast.LENGTH_SHORT)
                                    .show()
                                navigationActions.goBack()
                                navigationActions.goBack()
                              }
                        }
                      }
                      .addOnFailureListener {
                        Toast.makeText(context, "Échec de l'upload de la photo", Toast.LENGTH_SHORT)
                            .show()
                      }
                }

                // Cas de la vidéo
                videoFile?.let { file ->
                  val storageRef =
                      FirebaseStorage.getInstance()
                          .reference
                          .child("public/media/${System.currentTimeMillis()}.mp4")

                  storageRef
                      .putFile(Uri.fromFile(file))
                      .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                          val publication =
                              Publication(
                                  userId = ownerId,
                                  title = title,
                                  thumbnailUrl =
                                      generateThumbnail(
                                          uri.toString()), // Fonction pour générer une vignette si
                                  // nécessaire
                                  mediaUrl = uri.toString(),
                                  mediaType = MediaType.VIDEO)
                          FirebaseFirestore.getInstance()
                              .collection("publications")
                              .add(publication)
                              .addOnSuccessListener {
                                Toast.makeText(
                                        context, "Vidéo publiée avec succès", Toast.LENGTH_SHORT)
                                    .show()
                                navigationActions.goBack()
                                navigationActions.goBack()
                              }
                        }
                      }
                      .addOnFailureListener {
                        Toast.makeText(context, "Échec de l'upload de la vidéo", Toast.LENGTH_SHORT)
                            .show()
                      }
                }
              },
              modifier = Modifier.weight(1f).height(56.dp).testTag("postButton"),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37CED5)),
              shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Post",
                    style =
                        TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight(500),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF4A4459),
                        ),
                    modifier = Modifier.width(50.dp).height(24.dp))
              }
        }

    Box(
        modifier =
            Modifier.align(Alignment.TopStart)
                .size(40.dp)
                .clickable { navigationActions.goBack() }
                .padding(4.dp)
                .testTag("closeButton")) {
          Image(
              painter = painterResource(id = R.drawable.close),
              contentDescription = "Close button",
              modifier = Modifier.fillMaxSize().padding(4.dp),
              contentScale = ContentScale.Fit)
        }
  }
}

fun generateThumbnail(videoUrl: String): String {
  // Logique pour générer une vignette (si Firebase Storage permet d'accéder aux frames vidéo)
  // Cette partie dépend des bibliothèques que tu utilises pour le traitement vidéo
  // Si ce n'est pas faisable ici, tu peux générer les vignettes côté serveur ou lors de l'upload.
  return videoUrl // Pour simplifier, on utilise la même URL si aucune vignette spécifique
}

@Composable
fun StyledButton(
    text: String,
    iconRes: Int,
    bitmap: ImageBitmap?,
    context: Context,
    videoFile: File?, // Ajout du fichier vidéo en paramètre
    testTag: String,
    selectFolder: () -> Unit = {}
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .height(56.dp)
              .background(Color(0xFFEBF1F4))
              .clickable {
                if (text == " Share to") {
                  if (bitmap != null) {
                    // Partage d'une image
                    val photoFile = File(context.cacheDir, "shared_image.jpg")
                    val outputStream = FileOutputStream(photoFile)
                    bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()

                    val uri =
                        FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", photoFile)

                    val shareIntent =
                        Intent().apply {
                          action = Intent.ACTION_SEND
                          putExtra(Intent.EXTRA_STREAM, uri)
                          type = "image/jpeg"
                          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                    context.startActivity(Intent.createChooser(shareIntent, "Share image via"))
                  } else if (videoFile != null) {
                    // Partage d'une vidéo
                    val videoUri =
                        FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", videoFile)

                    val shareIntent =
                        Intent().apply {
                          action = Intent.ACTION_SEND
                          putExtra(Intent.EXTRA_STREAM, videoUri)
                          type = "video/mp4"
                          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                    context.startActivity(Intent.createChooser(shareIntent, "Share video via"))
                  }
                } else if (text == " Add to folder") {
                  selectFolder()
                }
              }
              .padding(horizontal = 16.dp)
              .testTag(testTag),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            modifier = Modifier.size(24.dp).padding(end = 8.dp),
            tint = Color.Gray)
        Text(
            text = text,
            color = Color.Gray,
            fontSize = 18.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
        )
      }
}

fun showBottomMenu(context: Context, folderViewModel: FolderViewModel, select: (Folder) -> Unit) {
  // Create the BottomSheetDialog
  val bottomSheetDialog = BottomSheetDialog(context)

  // Define a list of Folder objects
  folderViewModel.getUserFolders()
  val folders = folderViewModel.folders.value

  // Set the inflated view as the content of the BottomSheetDialog
  bottomSheetDialog.setContentView(
      ComposeView(context).apply {
        setContent {
          Column(modifier = Modifier.padding(16.dp).fillMaxWidth().testTag("button_container")) {
            folders.forEach { folder ->
              Card(
                  modifier =
                      Modifier.padding(8.dp)
                          .fillMaxWidth()
                          .clickable {
                            select(folder)
                            bottomSheetDialog.dismiss()
                          }
                          .testTag("folder_button${folder.id}"),
                  elevation = 4.dp) {
                    Text(
                        text = folder.name,
                        modifier = Modifier.padding(16.dp),
                        style = androidx.compose.material.MaterialTheme.typography.h6)
                  }
            }
          }
        }
      })

  // Show the dialog
  bottomSheetDialog.show()

  // Dismiss the dialog on lifecycle changes
  if (context is LifecycleOwner) {
    @Suppress("DEPRECATION")
    val lifecycleObserver =
        object : LifecycleObserver {
          @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
          fun onStop() {
            if (bottomSheetDialog.isShowing) {
              bottomSheetDialog.dismiss()
            }
          }

          @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
          fun onDestroy() {
            // Remove observer to prevent memory leaks
            (context as LifecycleOwner).lifecycle.removeObserver(this)
          }
        }

    (context as LifecycleOwner).lifecycle.addObserver(lifecycleObserver)
  }
}
