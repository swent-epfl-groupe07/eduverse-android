package com.github.se.eduverse.ui.videos

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.github.se.eduverse.R
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import java.io.File

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OfflineScreen(
    onGoBack: () -> Unit,
    getCachedFilesFun: (Context) -> List<File> = ::getCachedFiles,
    checkConnectionFun: (Context) -> Boolean = ::checkConnection
) {
  val context = LocalContext.current
  val cachedFiles = remember { mutableStateOf<List<File>>(emptyList()) }
  val isConnected = remember { mutableStateOf(checkConnectionFun(context)) }
  val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
  val isFavorited = remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { cachedFiles.value = getCachedFilesFun(context) }

  Scaffold(
      topBar = {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(56.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary)))
                    .testTag("TopAppBar")) {
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    IconButton(onClick = onGoBack, modifier = Modifier.testTag("BackButton")) {
                      Icon(
                          imageVector = Icons.Default.ArrowBack,
                          contentDescription = "Go Back",
                          tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Offline Media",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("OfflineMediaTitle"))
                  }
            }
      }) { paddingValues ->
        if (cachedFiles.value.isNotEmpty()) {
          val pagerState = rememberPagerState()
          ModalBottomSheetLayout(
              sheetState = sheetState,
              sheetContent = { Spacer(modifier = Modifier.height(1.dp)) }) {
                VerticalPager(
                    count = cachedFiles.value.size,
                    state = pagerState,
                    modifier =
                        Modifier.fillMaxSize().padding(paddingValues).testTag("MediaPager")) { page
                      ->
                      val file = cachedFiles.value[page]
                      val isLiked = remember { mutableStateOf(false) }

                      Box(
                          modifier =
                              Modifier.fillMaxSize()
                                  .background(Color.Black)
                                  .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                          if (isConnected.value) {
                                            Toast.makeText(
                                                    context,
                                                    "You're online! Keep your offline videos for when you don't have a connection!",
                                                    Toast.LENGTH_SHORT)
                                                .show()
                                          } else {
                                            Toast.makeText(
                                                    context,
                                                    "Action unavailable offline",
                                                    Toast.LENGTH_SHORT)
                                                .show()
                                          }
                                        })
                                  }
                                  .testTag("MediaItem")) {
                            if (file.name.endsWith(".mp4")) {
                              CachedVideoPlayer(file, context)
                            } else {
                              CachedImageDisplay(file)
                            }

                            Column(modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp)) {
                              IconButton(
                                  onClick = {
                                    if (isConnected.value) {
                                      isLiked.value = !isLiked.value
                                      Toast.makeText(
                                              context,
                                              "You're online! Keep your offline videos for when you don't have a connection!",
                                              Toast.LENGTH_SHORT)
                                          .show()
                                    } else {
                                      Toast.makeText(
                                              context,
                                              "Action unavailable offline",
                                              Toast.LENGTH_SHORT)
                                          .show()
                                    }
                                  },
                                  modifier = Modifier.testTag("LikeButton")) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Like",
                                        tint = if (isLiked.value) Color.Red else Color.White,
                                        modifier = Modifier.size(48.dp))
                                  }
                              Spacer(modifier = Modifier.height(12.dp))
                              IconButton(
                                  onClick = {
                                    if (isConnected.value) {
                                      Toast.makeText(
                                              context,
                                              "You're online! Keep your offline videos for when you don't have a connection!",
                                              Toast.LENGTH_SHORT)
                                          .show()
                                    } else {
                                      Toast.makeText(
                                              context,
                                              "Action unavailable offline",
                                              Toast.LENGTH_SHORT)
                                          .show()
                                    }
                                  },
                                  modifier = Modifier.testTag("CommentButton")) {
                                    Image(
                                        painter = painterResource(id = R.drawable.comment),
                                        contentDescription = "Comment",
                                        modifier = Modifier.size(52.dp))
                                  }
                              Spacer(modifier = Modifier.height(12.dp))
                              IconButton(
                                  onClick = {
                                    if (isConnected.value) {
                                      Toast.makeText(
                                              context,
                                              "You're online! Keep your offline videos for when you don't have a connection!",
                                              Toast.LENGTH_SHORT)
                                          .show()
                                    } else {
                                      Toast.makeText(
                                              context,
                                              "Action unavailable offline",
                                              Toast.LENGTH_SHORT)
                                          .show()
                                    }
                                  },
                                  modifier = Modifier.testTag("ShareButton")) {
                                    Image(
                                        painter = painterResource(id = R.drawable.share2),
                                        contentDescription = "Share",
                                        modifier = Modifier.size(48.dp))
                                  }
                              Spacer(modifier = Modifier.height(12.dp))
                              IconButton(
                                  onClick = {
                                    if (isConnected.value) {
                                      isFavorited.value = !isFavorited.value
                                      Toast.makeText(
                                              context,
                                              "You're online! Keep your offline videos for when you don't have a connection!",
                                              Toast.LENGTH_SHORT)
                                          .show()
                                    } else {
                                      Toast.makeText(
                                              context,
                                              "Action unavailable offline",
                                              Toast.LENGTH_SHORT)
                                          .show()
                                    }
                                  },
                                  modifier = Modifier.testTag("BookmarkButton")) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkAdd,
                                        contentDescription = "Bookmark",
                                        tint = if (isFavorited.value) Color.Yellow else Color.White,
                                        modifier = Modifier.size(48.dp))
                                  }
                            }
                          }
                    }
              }
        } else {
          Box(
              modifier = Modifier.fillMaxSize().testTag("NoOfflineMediaBox"),
              contentAlignment = Alignment.Center) {
                Text(
                    "No offline media available",
                    color = Color.Gray,
                    modifier = Modifier.testTag("NoOfflineMediaText"))
              }
        }
      }
}

@Composable
fun CachedImageDisplay(file: File) {
  SubcomposeAsyncImage(
      model = file,
      contentDescription = "Cached Image",
      modifier = Modifier.fillMaxSize().testTag("CachedImageDisplay"),
      contentScale = ContentScale.Crop)
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun CachedVideoPlayer(file: File, context: Context) {
  val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
      val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
      setMediaItem(mediaItem)
      prepare()
      playWhenReady = true
      repeatMode = ExoPlayer.REPEAT_MODE_ONE
    }
  }

  DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

  AndroidView(
      factory = {
        PlayerView(context).apply {
          player = exoPlayer
          useController = false
          layoutParams =
              android.view.ViewGroup.LayoutParams(
                  android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                  android.view.ViewGroup.LayoutParams.MATCH_PARENT)
          resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
      },
      modifier = Modifier.fillMaxSize().testTag("CachedVideoPlayer"))
}

fun getCachedFiles(context: Context): List<File> {
  val cacheDir = context.cacheDir
  return cacheDir
      .listFiles { file -> file.name.endsWith(".jpg") || file.name.endsWith(".mp4") }
      ?.toList() ?: emptyList()
}

fun checkConnection(context: Context): Boolean {
  val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
  val network = connectivityManager.activeNetwork
  val capabilities = connectivityManager.getNetworkCapabilities(network)
  return capabilities != null &&
      capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
