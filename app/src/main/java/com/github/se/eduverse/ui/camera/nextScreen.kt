package com.github.se.eduverse.ui.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.github.se.eduverse.R
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.showBottomMenu
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(UnstableApi::class)
@Composable
fun NextScreen(
    photoFile: File?,
    videoFile: File?,
    navigationActions: NavigationActions,
    photoViewModel: PhotoViewModel,
    folderViewModel: FolderViewModel,
    videoViewModel: VideoViewModel // Added VideoViewModel
) {
  val context = LocalContext.current
  val auth = FirebaseAuth.getInstance()
  val ownerId = auth.currentUser?.uid ?: "anonymous"

  // Path to save the image or video
  val mediaType = if (photoFile != null) "photos" else "videos"
  val path =
      "$mediaType/$ownerId/${System.currentTimeMillis()}.${if (photoFile != null) "jpg" else "mp4"}"

  val bitmap = photoFile?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
  val backgroundColor = MaterialTheme.colorScheme.background
  val onBackgroundColor = MaterialTheme.colorScheme.onBackground

  Box(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(16.dp)) {
    if (bitmap != null) {
      // Display image
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
      // Display video
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
              useController = false // Hide video controls
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

    // Other unchanged components
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
                letterSpacing = 0.24.sp,
            ))

    Column(
        modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp, top = 320.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
          StyledButton(
              text = " Add to folder", iconRes = R.drawable.add, testTag = "addToFolderButton") {
                showBottomMenu(context, folderViewModel, backgroundColor, onBackgroundColor) {
                  bitmap?.let { bmp ->
                    val byteArray = imageBitmapToByteArray(bmp)
                    val photo = Photo(ownerId, byteArray, path)
                    photoViewModel.savePhoto(
                        photo, it, onSuccess = { mediaSavedToast(context, "Photo") }) {
                            id,
                            name,
                            folder ->
                          folderViewModel.createFileInFolder(id, name, folder)
                        }
                    navigationActions.goBack()
                    navigationActions.goBack()
                    navigationActions.goBack()
                  }

                  videoFile?.let { file ->
                    val videoByteArray = file.readBytes()
                    val video = Video(ownerId, videoByteArray, path.replace(".jpg", ".mp4"))
                    videoViewModel.saveVideo(
                        video, it, onSuccess = { mediaSavedToast(context, "Video") }) {
                            id,
                            name,
                            folder ->
                          folderViewModel.createFileInFolder(id, name, folder)
                        }
                    navigationActions.goBack()
                    navigationActions.goBack()
                    navigationActions.goBack()
                  }
                }
              }
          StyledButton(
              text = " More options",
              iconRes = R.drawable.more_horiz,
              testTag = "moreOptionsButton") {}
          StyledButton(text = " Share to", iconRes = R.drawable.share, testTag = "shareToButton") {
            handleShare(bitmap, context, videoFile)
          }
        }

    // Added Save functionality for photo and video
    Row(
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Button(
              onClick = {
                bitmap?.let { bmp ->
                  val byteArray = imageBitmapToByteArray(bmp)
                  val photo = Photo(ownerId, byteArray, path)
                  photoViewModel.savePhoto(photo, onSuccess = { mediaSavedToast(context, "Photo") })
                  navigationActions.goBack()
                  navigationActions.goBack()
                  navigationActions.goBack()
                }

                videoFile?.let { file ->
                  val videoByteArray = file.readBytes()
                  val video = Video(ownerId, videoByteArray, path.replace(".jpg", ".mp4"))
                  videoViewModel.saveVideo(video, onSuccess = { mediaSavedToast(context, "Video") })
                  navigationActions.goBack()
                  navigationActions.goBack()
                  navigationActions.goBack()
                }
              },
              modifier = Modifier.weight(1f).height(56.dp).testTag("saveButton"),
              colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
              shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Save",
                    style =
                        TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight(500),
                            textAlign = TextAlign.Center),
                    modifier = Modifier.padding(horizontal = 8.dp))
              }

          Button(
              onClick = {
                val title =
                    "My Publication" // You can get this title from a user input field if needed

                // Photo case
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
                                  id = UUID.randomUUID().toString(),
                                  userId = ownerId,
                                  title = title,
                                  thumbnailUrl = uri.toString(), // Image URL as thumbnail
                                  mediaUrl = uri.toString(),
                                  mediaType = MediaType.PHOTO)
                          FirebaseFirestore.getInstance()
                              .collection("publications")
                              .add(publication)
                              .addOnSuccessListener {
                                Toast.makeText(
                                        context, "Photo published successfully", Toast.LENGTH_SHORT)
                                    .show()
                                navigationActions.goBack()
                                navigationActions.goBack()
                                navigationActions.goBack()
                              }
                        }
                      }
                      .addOnFailureListener {
                        Toast.makeText(context, "Photo upload failed", Toast.LENGTH_SHORT).show()
                      }
                }

                // Video case
                videoFile?.let { file ->
                  val timestamp = System.currentTimeMillis()

                  // First generate the thumbnail
                  generateVideoThumbnail(context, file)?.let { thumbnailBytes ->
                    // References for both files
                    val thumbnailRef =
                        FirebaseStorage.getInstance()
                            .reference
                            .child("public/thumbnails/thumb_$timestamp.jpg")

                    val videoRef =
                        FirebaseStorage.getInstance()
                            .reference
                            .child("public/media/video_$timestamp.mp4")

                    // Upload thumbnail first
                    thumbnailRef
                        .putBytes(thumbnailBytes)
                        .addOnSuccessListener {
                          thumbnailRef.downloadUrl.addOnSuccessListener { thumbnailUri ->
                            // Then upload video
                            videoRef
                                .putFile(Uri.fromFile(file))
                                .addOnSuccessListener {
                                  videoRef.downloadUrl.addOnSuccessListener { videoUri ->
                                    // Create publication with correct URLs
                                    val publication =
                                        Publication(
                                            id = UUID.randomUUID().toString(),
                                            userId = ownerId,
                                            title = title,
                                            thumbnailUrl =
                                                thumbnailUri.toString(), // Thumbnail image URL
                                            mediaUrl = videoUri.toString(), // Video URL
                                            mediaType = MediaType.VIDEO)

                                    FirebaseFirestore.getInstance()
                                        .collection("publications")
                                        .add(publication)
                                        .addOnSuccessListener {
                                          Toast.makeText(
                                                  context,
                                                  "Video published successfully",
                                                  Toast.LENGTH_SHORT)
                                              .show()
                                          navigationActions.goBack()
                                          navigationActions.goBack()
                                          navigationActions.goBack()
                                        }
                                  }
                                }
                                .addOnFailureListener {
                                  Toast.makeText(context, "Video upload failed", Toast.LENGTH_SHORT)
                                      .show()
                                }
                          }
                        }
                        .addOnFailureListener {
                          Toast.makeText(context, "Thumbnail upload failed", Toast.LENGTH_SHORT)
                              .show()
                        }
                  }
                }
              },
              modifier = Modifier.weight(1f).height(56.dp).testTag("postButton"),
              colors =
                  ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Post",
                    style =
                        TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight(500),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimary,
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
          Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close button",
              tint = onBackgroundColor,
              modifier = Modifier.size(32.dp).align(Alignment.Center))
        }
  }
}

fun generateVideoThumbnail(context: Context, videoFile: File): ByteArray? {
  return try {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, Uri.fromFile(videoFile))

    // Extract a frame from 1 second into the video
    val bitmap =
        retriever.getFrameAtTime(
            1000000, // 1 second in microseconds
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

    retriever.release()

    // Convert bitmap to byte array
    bitmap?.let { bmp ->
      ByteArrayOutputStream().use { stream ->
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        stream.toByteArray()
      }
    }
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

@Composable
fun StyledButton(text: String, iconRes: Int, testTag: String, onClick: () -> Unit) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .height(56.dp)
              .clickable { onClick() }
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

fun handleShare(
    bitmap: ImageBitmap?,
    context: Context,
    videoFile: File? // Added video file parameter
) {
  if (bitmap != null) {
    // Share an image
    val photoFile = File(context.cacheDir, "shared_image.jpg")
    val outputStream = FileOutputStream(photoFile)
    bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    outputStream.flush()
    outputStream.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)

    val shareIntent =
        Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_STREAM, uri)
          type = "image/jpeg"
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    context.startActivity(Intent.createChooser(shareIntent, "Share image via"))
  } else if (videoFile != null) {
    // Share a video
    val videoUri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", videoFile)

    val shareIntent =
        Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_STREAM, videoUri)
          type = "video/mp4"
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    context.startActivity(Intent.createChooser(shareIntent, "Share video via"))
  }
}
