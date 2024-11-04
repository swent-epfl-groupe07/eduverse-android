package com.github.se.eduverse.ui

// Android Imports
// Accompanist Pager Imports
// ExoPlayer Imports
// Coil Image Loading
// Project-Specific Imports
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.PublicationViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import com.github.se.eduverse.ui.navigation.Route

@OptIn(ExperimentalPagerApi::class)
@Composable
fun VideoScreen(
    navigationActions: NavigationActions,
    viewModel: PublicationViewModel,
) {
  val publications by viewModel.publications.collectAsState()
  val error by viewModel.error.collectAsState() // Récupération de l'état d'erreur
  val pagerState = rememberPagerState()
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
      bottomBar = {
        BottomNavigationMenu(
            onTabSelect = { route -> navigationActions.navigateTo(route) },
            tabList = LIST_TOP_LEVEL_DESTINATION,
            selectedItem = Route.VIDEOS)
      },
      modifier = Modifier.testTag("VideoScreen")) { paddingValues ->
        when {
          error != null -> { // En cas d'erreur, affiche un message
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.2f))
                        .testTag("ErrorIndicator")) {
                  Text(
                      text = error ?: "Une erreur est survenue",
                      color = Color.Red,
                      modifier = Modifier.align(Alignment.Center))
                }
          }
          publications.isNotEmpty() -> { // Si les publications sont chargées, les afficher
            VerticalPager(
                count = publications.size,
                state = pagerState,
                modifier =
                    Modifier.fillMaxSize().padding(paddingValues).testTag("VerticalPager")) { page
                  ->
                  val publication = publications[page]
                  if (publication.mediaType == MediaType.VIDEO) {
                    VideoItem(context = LocalContext.current, mediaUrl = publication.mediaUrl)
                  } else {
                    PhotoItem(thumbnailUrl = publication.thumbnailUrl)
                  }
                }

            // Charger plus de publications lorsque l’utilisateur atteint la dernière page visible
            LaunchedEffect(pagerState.currentPage) {
              if (pagerState.currentPage == publications.size - 1) {
                viewModel.loadMorePublications()
              }
            }
          }
          else -> { // Affiche un indicateur de chargement
            Box(modifier = Modifier.fillMaxSize().testTag("LoadingIndicator")) {
              CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
          }
        }
      }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoItem(
    context: Context,
    mediaUrl: String,
    exoPlayerProvider: () -> ExoPlayer = {
      ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(mediaUrl))
        prepare()
        playWhenReady = true
        repeatMode = ExoPlayer.REPEAT_MODE_ONE
      }
    }
) {
  val exoPlayer = remember { exoPlayerProvider() }

  DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

  AndroidView(
      factory = {
        PlayerView(context).apply {
          player = exoPlayer
          useController = false
          layoutParams =
              ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
          resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
      },
      modifier = Modifier.fillMaxSize().background(Color.Black).testTag("VideoItem"))
}

@Composable
fun PhotoItem(thumbnailUrl: String) {
  Box(modifier = Modifier.fillMaxSize().background(Color.Black).testTag("PhotoItem")) {
    SubcomposeAsyncImage(
        model = thumbnailUrl,
        contentDescription = "Photo de la publication",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize())
  }
}
