package com.github.se.eduverse.ui.gallery

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.se.eduverse.R
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.model.Video
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.profile.ExoVideoPlayer
import com.github.se.eduverse.ui.showBottomMenu
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.github.se.eduverse.viewmodel.VideoViewModel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoThumbnailUtil {

  fun generateThumbnail(
      context: Context,
      videoPath: String,
      retriever: MediaMetadataRetriever = MediaMetadataRetriever()
  ): String? {
    return try {
      retriever.setDataSource(videoPath)
      val bitmap = retriever.getFrameAtTime(0)
      if (bitmap != null) {
        val thumbnailFile = File(context.cacheDir, "thumbnail_${videoPath.hashCode()}.jpg")
        FileOutputStream(thumbnailFile).use { out ->
          bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        thumbnailFile.absolutePath
      } else {
        null
      }
    } catch (e: Exception) {
      Log.e("VideoThumbnailUtil", "Erreur lors de la génération de la miniature", e)
      null
    } finally {
      retriever.release()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    ownerId: String,
    photoViewModel: PhotoViewModel,
    videoViewModel: VideoViewModel,
    folderViewModel: FolderViewModel,
    navigationActions: NavigationActions
) {
  val context = LocalContext.current
  val photos by photoViewModel.photos.collectAsState()
  val videos by videoViewModel.videos.collectAsState()

  var selectedMedia by remember { mutableStateOf<Any?>(null) }

  LaunchedEffect(ownerId) {
    photoViewModel.getPhotosByOwner(ownerId)
    videoViewModel.getVideosByOwner(ownerId)
  }

  val mediaItems = photos.map { it as Any } + videos.map { it as Any }

  Scaffold(topBar = { TopNavigationBar(navigationActions, screenTitle = null) }) { contentPadding ->
    if (mediaItems.isEmpty()) {
      Box(
          modifier = Modifier.fillMaxSize().padding(contentPadding).testTag("NoPhotosBox"),
          contentAlignment = Alignment.Center) {
            Text(
                "No media available",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("NoPhotosText"))
          }
    } else {
      LazyVerticalGrid(
          columns = GridCells.Fixed(3),
          modifier = Modifier.fillMaxSize().padding(contentPadding).testTag("MediaGrid"),
          contentPadding = PaddingValues(1.dp),
          horizontalArrangement = Arrangement.spacedBy(1.dp),
          verticalArrangement = Arrangement.spacedBy(1.dp)) {
            items(mediaItems) { media ->
              when (media) {
                is Photo -> {
                  val tag = "PhotoItem_${media.path ?: "unknown"}"
                  PublicationItem(
                      mediaType = MediaType.PHOTO,
                      thumbnailUrl = media.path,
                      onClick = { selectedMedia = media },
                      modifier = Modifier.testTag(tag))
                }
                is Video -> {
                  var thumbnailPath by remember { mutableStateOf<String?>(null) }

                  LaunchedEffect(media.path) {
                    thumbnailPath =
                        withContext(Dispatchers.IO) {
                          media.path?.let { VideoThumbnailUtil.generateThumbnail(context, it) }
                        }
                  }

                  val tag = "VideoItem_${media.path ?: "unknown"}"
                  PublicationItem(
                      mediaType = MediaType.VIDEO,
                      thumbnailUrl = thumbnailPath ?: media.path,
                      onClick = { selectedMedia = media },
                      modifier = Modifier.testTag(tag))
                }
              }
            }
          }
    }
  }

  val backgroundColor = MaterialTheme.colorScheme.background
  val surfaceColor = MaterialTheme.colorScheme.surface
  val contentColor = MaterialTheme.colorScheme.onSurface
  selectedMedia?.let { media ->
    when (media) {
      is Photo -> {
        MediaDetailDialog(
            media = media,
            onDismiss = { selectedMedia = null },
            onDownload = { downloadImage(media.path ?: "unknown") },
            onAddToFolder = {
              showBottomMenu(
                  context, folderViewModel, backgroundColor, surfaceColor, contentColor) { folder ->
                    photoViewModel.makeFileFromPhoto(media) {
                      folderViewModel.createFileInFolder(it, it, folder)
                    }
                  }
            })
      }
      is Video -> {
        MediaDetailDialog(
            media = media,
            onDismiss = { selectedMedia = null },
            onDownload = { downloadVideo(media.path ?: "unknown") },
            onAddToFolder = {
              Log.d("GalleryScreen", "Add to folder clicked for video: ${media.path}")
            })
      }
    }
  }
}

@Composable
fun PublicationItem(
    mediaType: MediaType,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Card(
      modifier =
          modifier
              .aspectRatio(1f)
              .clickable(onClick = onClick)
              .testTag(if (mediaType == MediaType.PHOTO) "PhotoCard" else "VideoCard"),
      shape = RoundedCornerShape(8.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
          AsyncImage(
              model =
                  ImageRequest.Builder(LocalContext.current)
                      .data(thumbnailUrl)
                      .crossfade(true)
                      .build(),
              contentDescription = null,
              modifier = Modifier.fillMaxSize().testTag("MediaThumbnail"),
              contentScale = ContentScale.Crop,
              error = painterResource(R.drawable.eduverse_logo_alone),
              fallback = painterResource(R.drawable.eduverse_logo_alone))

          if (mediaType == MediaType.VIDEO) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .testTag("VideoOverlay")) {
                  Icon(
                      imageVector = Icons.Default.PlayCircle,
                      contentDescription = "Video",
                      modifier = Modifier.size(48.dp).align(Alignment.Center).testTag("PlayIcon"),
                      tint = Color.White)
                }
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize().testTag("VideoThumbnail"),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.eduverse_logo_alone),
                fallback = painterResource(R.drawable.eduverse_logo_alone))
          }
        }
      }
}

@Composable
fun MediaDetailDialog(
    media: Any,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onAddToFolder: (() -> Unit)? = null
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("MediaDetailDialog")) {
          Column {
            Box(modifier = Modifier.weight(1f)) {
              when (media) {
                is Photo -> {
                  AsyncImage(
                      model = media.path,
                      contentDescription = null,
                      modifier = Modifier.fillMaxSize().testTag("PhotoDetail"),
                      contentScale = ContentScale.Fit)
                }
                is Video -> {
                  ExoVideoPlayer(videoUrl = media.path, modifier = Modifier.testTag("VideoDetail"))
                }
              }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                  IconButton(
                      onClick = { onDownload?.invoke() },
                      modifier =
                          Modifier.testTag(
                              if (media is Photo) "DownloadPhotoButton"
                              else "DownloadVideoButton")) {
                        Icon(Icons.Default.Download, contentDescription = "Download Media")
                      }

                  TextButton(
                      onClick = { onAddToFolder?.invoke() },
                      modifier =
                          Modifier.testTag(
                              if (media is Photo) "AddPhotoToFolderButton"
                              else "AddVideoToFolderButton")) {
                        Text("Add to folder")
                      }

                  TextButton(onClick = onDismiss, modifier = Modifier.testTag("CloseButton")) {
                    Text("Close")
                  }
                }
          }
        }
  }
}

fun downloadImage(imagePath: String) {
  Log.d("GalleryScreen", "Downloading image from: $imagePath")
  // Implement the actual download logic here
}

fun downloadVideo(videoPath: String) {
  Log.d("GalleryScreen", "Downloading video from: $videoPath")
  // Implement the actual download logic here
}
