package com.github.se.eduverse.ui

// Android Imports
// Accompanist Pager Imports
// ExoPlayer Imports
// Coil Image Loading
// Project-Specific Imports
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.PublicationViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalPagerApi::class)
@Composable
fun VideoScreen(
    navigationActions: NavigationActions,
    publicationViewModel: PublicationViewModel,
    profileViewModel: ProfileViewModel,
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val publications by publicationViewModel.publications.collectAsState()
    val error by publicationViewModel.error.collectAsState()
    val pagerState = rememberPagerState()

    Scaffold(
        bottomBar = {
            BottomNavigationMenu(
                onTabSelect = { route -> navigationActions.navigateTo(route) },
                tabList = LIST_TOP_LEVEL_DESTINATION,
                selectedItem = Route.VIDEOS
            )
        },
        modifier = Modifier.testTag("VideoScreen")
    ) { paddingValues ->
        when {
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.2f))
                        .testTag("ErrorIndicator")
                ) {
                    Text(
                        text = error ?: "Une erreur est survenue",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            publications.isNotEmpty() -> {
                VerticalPager(
                    count = publications.size,
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .testTag("VerticalPager")
                ) { page ->
                    val publication = publications[page]
                    // Initialisation du state isLiked
                    val isLiked = remember { mutableStateOf(publication.likedBy.contains(currentUserId)) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        profileViewModel.likeAndAddToFavorites(currentUserId, publication.id)
                                        isLiked.value = true // Mettre à jour l'état local
                                    }
                                )
                            }
                            .testTag("PublicationItem_$page") // Ajout d'un testTag unique pour chaque publication
                    ) {
                        if (publication.mediaType == MediaType.VIDEO) {
                            VideoItem(context = LocalContext.current, mediaUrl = publication.mediaUrl)
                        } else {
                            PhotoItem(thumbnailUrl = publication.thumbnailUrl)
                        }

                        // Icône de cœur pour aimer
                        IconButton(
                            onClick = {
                                if (isLiked.value) {
                                    // Logique pour retirer le like (à implémenter dans le ViewModel)
                                    profileViewModel.removeLike(currentUserId, publication.id)
                                    isLiked.value = false
                                } else {
                                    profileViewModel.likeAndAddToFavorites(currentUserId, publication.id)
                                    isLiked.value = true
                                }
                                Log.d("AUTHHHHHHH", currentUserId)
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(y = 64.dp)
                                .padding(12.dp)
                                .testTag("LikeButton_$page") // Ajout d'un testTag unique pour le bouton like
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Like",
                                tint = if (isLiked.value) Color.Red else Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag(if (isLiked.value) "LikedIcon_$page" else "UnlikedIcon_$page") // Ajout d'un testTag conditionnel pour l'icône
                            )
                        }
                    }
                }

                // Charger plus de publications lorsque l’utilisateur atteint la dernière page visible
                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage == publications.size - 1) {
                        publicationViewModel.loadMorePublications()
                    }
                }
            }
            else -> {
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
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("VideoItem")
    )
}

@Composable
fun PhotoItem(thumbnailUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("PhotoItem")
    ) {
        SubcomposeAsyncImage(
            model = thumbnailUrl,
            contentDescription = "Photo de la publication",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
