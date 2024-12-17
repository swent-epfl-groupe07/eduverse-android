package com.github.se.eduverse.ui.offline

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.Log
import coil.compose.rememberAsyncImagePainter
import com.github.se.eduverse.model.MediaCacheManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineScreen(mediaCacheManager: MediaCacheManager) {
    val context = LocalContext.current
    var cachedFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // Charger les fichiers en cache
    LaunchedEffect(Unit) {
        cachedFiles = getCachedFiles(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Media") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            items(cachedFiles) { file ->
                CachedFileItem(context, file)
            }
        }
    }
}

@Composable
fun CachedFileItem(context: Context, file: File) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = file.name, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))

        when {
            file.name.endsWith(".jpg") || file.name.endsWith(".png") -> {
                Image(
                    painter = rememberAsyncImagePainter(file),
                    contentDescription = "Cached Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            file.name.endsWith(".mp4") -> {
                VideoPlayer(file, context)
            }
            else -> {
                Text("Unknown File: ${file.name}")
            }
        }
    }
}

@Composable
fun VideoPlayer(file: File, context: Context) {
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(Uri.fromFile(file))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
fun getCachedFiles(context: Context): List<File> {
    val cacheDir = context.cacheDir
    val files = cacheDir.listFiles { file ->
        file.name.endsWith(".jpg") || file.name.endsWith(".mp4")
    }
    Log.d("CACHE_FILES", "Already cached files: ${files?.joinToString { it.name }}")
    return files?.toList() ?: emptyList()
}

