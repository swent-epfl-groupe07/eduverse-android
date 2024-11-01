// Android Imports
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
// Accompanist Pager Imports
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

// ExoPlayer Imports
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

// Coil Image Loading
import coil.compose.rememberImagePainter

// Coroutines
import kotlinx.coroutines.launch

// Your Project-Specific Imports (adjust as needed based on your package structure)
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.model.MediaType

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.TopLevelDestination


import androidx.compose.material3.Scaffold
import androidx.media3.common.util.UnstableApi
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun VideoScreen(
    navigationActions: NavigationActions,
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var publications by remember { mutableStateOf(listOf<Publication>()) }
    val pagerState = rememberPagerState()

    // Charger les publications initiales aléatoirement
    LaunchedEffect(Unit) {
        loadRandomPublications(db) { newPublications ->
            publications = newPublications
        }
    }

    // Charger plus de publications quand on atteint la dernière page visible
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == publications.size - 1) {
            loadRandomPublications(db) { newPublications ->
                publications = publications + newPublications
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationMenu(
                onTabSelect = { route -> navigationActions.navigateTo(route) },
                tabList = LIST_TOP_LEVEL_DESTINATION,
                selectedItem = navigationActions.currentRoute()
            )
        }
    ) { paddingValues ->
        if (publications.isNotEmpty()) {
            VerticalPager(
                count = publications.size,
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { page ->
                val publication = publications[page]
                if (publication.mediaType == MediaType.VIDEO) {
                    VideoItem(context = context, mediaUrl = publication.mediaUrl)
                } else {
                    PhotoItem(thumbnailUrl = publication.thumbnailUrl)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoItem(context: Context, mediaUrl: String) {
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUrl))
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ONE // Répéter la vidéo en boucle
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { PlayerView(context).apply {
            player = exoPlayer
            useController = false // Cache les contrôles du lecteur
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }},
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}


@Composable
fun PhotoItem(thumbnailUrl: String) {
    Image(
        painter = rememberImagePainter(data = thumbnailUrl),
        contentDescription = "Photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}

// Fonction pour charger des publications aléatoires
fun loadRandomPublications(
    db: FirebaseFirestore,
    onLoaded: (List<Publication>) -> Unit
) {
    db.collection("publications")
        .orderBy("timestamp") // Trie par timestamp pour un chargement cohérent
        .limit(100) // Récupère un grand nombre de publications pour mélanger localement
        .get()
        .addOnSuccessListener { result ->
            val publications = result.documents.mapNotNull { it.toObject(Publication::class.java) }
            onLoaded(publications.shuffled()) // Mélange les publications pour un effet aléatoire
        }
        .addOnFailureListener {
            // Gérer les erreurs de récupération ici si nécessaire
        }
}
