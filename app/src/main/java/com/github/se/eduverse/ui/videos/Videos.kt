// VideoScreen.kt
package com.github.se.eduverse.ui.videos

// Android Imports
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.TabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.showToast
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.videos.ShareUtils.handleShare
import com.github.se.eduverse.viewmodel.CommentsUiState
import com.github.se.eduverse.viewmodel.CommentsViewModel
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.PublicationViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class)
@Composable
fun VideoScreen(
    navigationActions: NavigationActions,
    publicationViewModel: PublicationViewModel,
    profileViewModel: ProfileViewModel,
    commentsViewModel: CommentsViewModel,
    currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
) {
  LaunchedEffect(Unit) { publicationViewModel.initializePublications() }

  var isPublicationGlobal by remember { mutableStateOf(true) }
  var followedUsers by remember { mutableStateOf(emptyList<String>()) }

  val globalPublications by publicationViewModel.publications.collectAsState()
  val followedPublications by publicationViewModel.followedPublications.collectAsState()
  val publications = if (isPublicationGlobal) globalPublications else followedPublications
  val publicationError by publicationViewModel.error.collectAsState()
  val pagerState = rememberPagerState()

  var isCommentsVisible by remember { mutableStateOf(false) }
  var selectedPublicationId by remember { mutableStateOf<String?>(null) }

  val context = LocalContext.current

  val sheetState =
      rememberModalBottomSheetState(
          initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

  // Load the list of followed users and the associated initial publications
  LaunchedEffect(Unit) {
    followedUsers = profileViewModel.getFollowing(currentUserId).map { it.id }
    publicationViewModel.loadFollowedPublications(followedUsers)
  }

  // Handle opening and closing of the sheet
  LaunchedEffect(isCommentsVisible) {
    if (isCommentsVisible) {
      sheetState.show()
    } else {
      sheetState.hide()
    }
  }

  // Observe changes in the sheet's state to update isCommentsVisible
  LaunchedEffect(sheetState.currentValue) {
    if (sheetState.currentValue == ModalBottomSheetValue.Hidden) {
      isCommentsVisible = false
      selectedPublicationId = null
    }
  }

  ModalBottomSheetLayout(
      sheetContent = {
        if (isCommentsVisible && selectedPublicationId != null) {
          CommentsMenuContent(
              publicationId = selectedPublicationId!!,
              commentsViewModel = commentsViewModel,
              currentUserId,
              onDismiss = {
                isCommentsVisible = false
                selectedPublicationId = null
                Log.d("COMMENT", "Comments menu dismissed")
              })
        } else {
          Spacer(modifier = Modifier.height(1.dp))
        }
      },
      sheetState = sheetState,
      scrimColor = Color.Black.copy(alpha = 0.32f)) {
        Scaffold(
            bottomBar = {
              BottomNavigationMenu(
                  onTabSelect = { route -> navigationActions.navigateTo(route) },
                  tabList = LIST_TOP_LEVEL_DESTINATION,
                  selectedItem = Route.VIDEOS)
            },
            modifier = Modifier.testTag("VideoScreen")) { paddingValues ->
              when {
                publicationError != null -> {
                  Box(
                      modifier =
                          Modifier.fillMaxSize()
                              .background(Color.Red.copy(alpha = 0.2f))
                              .testTag("ErrorIndicator")) {
                        Text(
                            text = publicationError ?: "An error occurred",
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center))
                      }
                }
                publications.isNotEmpty() -> {
                  Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    // Pager with scrollable videos
                    VerticalPager(
                        count = publications.size,
                        state = pagerState,
                        modifier = Modifier.fillMaxSize().testTag("VerticalPager")) { page ->
                          val publication = publications[page]

                          val isLiked = remember {
                            mutableStateOf(publication.likedBy.contains(currentUserId))
                          }

                          Box(
                              modifier =
                                  Modifier.fillMaxSize()
                                      .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                              profileViewModel.likeAndAddToFavorites(
                                                  currentUserId, publication.id)
                                              isLiked.value = true
                                            })
                                      }
                                      .testTag("PublicationItem_$page")) {
                                if (publication.mediaType == MediaType.VIDEO) {
                                  VideoItem(
                                      context = LocalContext.current,
                                      mediaUrl = publication.mediaUrl,
                                      modifier = Modifier.testTag("VideoItem_$page"))
                                } else {
                                  PhotoItem(
                                      thumbnailUrl = publication.thumbnailUrl,
                                      modifier = Modifier.testTag("PhotoItem_$page"))
                                }

                                // Icon to like
                                IconButton(
                                    onClick = {
                                      if (isLiked.value) {
                                        profileViewModel.removeLike(currentUserId, publication.id)
                                        isLiked.value = false
                                      } else {
                                        profileViewModel.likeAndAddToFavorites(
                                            currentUserId, publication.id)
                                        isLiked.value = true
                                      }
                                      Log.d(
                                          "LIKE",
                                          "Like button clicked for publication: ${publication.id}")
                                    },
                                    modifier =
                                        Modifier.align(Alignment.CenterEnd)
                                            .offset(y = 64.dp)
                                            .padding(12.dp)
                                            .testTag("LikeButton_$page")) {
                                      Icon(
                                          imageVector = Icons.Default.Favorite,
                                          contentDescription = "Like",
                                          tint = if (isLiked.value) Color.Red else Color.White,
                                          modifier =
                                              Modifier.size(48.dp)
                                                  .testTag(
                                                      if (isLiked.value) "LikedIcon_$page"
                                                      else "UnlikedIcon_$page"))
                                    }

                                // Comment button
                                IconButton(
                                    onClick = {
                                      selectedPublicationId = publication.id
                                      isCommentsVisible = true
                                      Log.d(
                                          "COMMENT",
                                          "Comment button clicked for publication: ${publication.id}")
                                    },
                                    modifier =
                                        Modifier.align(Alignment.CenterEnd)
                                            .offset(y = 128.dp)
                                            .padding(12.dp)
                                            .testTag("CommentButton_$page")) {
                                      Icon(
                                          imageVector = Icons.Default.Comment,
                                          contentDescription = "Comment",
                                          tint = Color.White,
                                          modifier = Modifier.size(48.dp))
                                    }

                                // Share button
                                IconButton(
                                    onClick = {
                                      handleShare(publication = publication, context = context)
                                      Log.d(
                                          "SHARE",
                                          "Share button clicked for publication: ${publication.id}")
                                    },
                                    modifier =
                                        Modifier.align(Alignment.CenterEnd)
                                            .offset(y = 192.dp)
                                            .padding(12.dp)
                                            .testTag("ShareButton_$page")) {
                                      Icon(
                                          imageVector = Icons.Default.Share,
                                          contentDescription = "Share",
                                          tint = Color.White,
                                          modifier = Modifier.size(48.dp))
                                    }
                              }
                        }

                    // Tab to switch between global feed and followed feed
                    TabRow(
                        selectedTabIndex = if (isPublicationGlobal) 0 else 1,
                        modifier = Modifier.align(Alignment.TopCenter),
                        backgroundColor = Color.Transparent,
                        contentColor = Color.White) {
                          Tab(
                              selected = isPublicationGlobal,
                              onClick = { isPublicationGlobal = true },
                              modifier = Modifier.testTag("globalFeed"),
                              text = { Text("Global") },
                              selectedContentColor = Color.White)
                          Tab(
                              selected = !isPublicationGlobal,
                              onClick = {
                                if (followedUsers.isEmpty()) {
                                  context.showToast("You are not following anyone")
                                } else if (followedPublications.isEmpty()) {
                                  context.showToast("No one you follow has posted anything yet")
                                } else {
                                  isPublicationGlobal = false
                                }
                              },
                              modifier = Modifier.testTag("followedFeed"),
                              text = { Text("Followed") },
                              selectedContentColor = Color.White)
                        }
                  }

                  // Load more publications when the user reaches the last visible page
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
}

@Composable
fun CommentsMenuContent(
    publicationId: String,
    commentsViewModel: CommentsViewModel,
    currentUserId: String,
    onDismiss: () -> Unit
) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(MaterialTheme.colorScheme.background)
              .testTag("CommentsMenuContent")) {
        CommentSection(publicationId, commentsViewModel, currentUserId)
      }
}

@Composable
fun CommentSection(
    publicationId: String,
    commentsViewModel: CommentsViewModel,
    currentUserId: String
) {
  val commentsState by commentsViewModel.commentsState.collectAsState()
  val context = LocalContext.current
  var newCommentText by remember { mutableStateOf("") }

  // Load comments when the component is launched or when publicationId changes
  LaunchedEffect(publicationId) { commentsViewModel.loadComments(publicationId) }

  Column(
      modifier =
          Modifier.fillMaxHeight(0.75f).fillMaxWidth().padding(16.dp).testTag("CommentsSection")) {
        Text(
            "Comments",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp))

        when (commentsState) {
          is CommentsUiState.Loading -> {
            // Display a loading indicator only during initial loading
            Box(
                modifier = Modifier.fillMaxSize().testTag("CommentsLoadingIndicator"),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator()
                }
          }
          is CommentsUiState.Success -> {
            val comments = (commentsState as CommentsUiState.Success).comments

            LazyColumn(modifier = Modifier.weight(1f).testTag("CommentsList")) {
              items(comments) { comment ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.padding(vertical = 4.dp)
                            .fillMaxWidth()
                            .testTag("CommentItem_${comment.id}")) {
                      // Display the profile image
                      if (!comment.profile?.profileImageUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = comment.profile?.profileImageUrl,
                            contentDescription = "Profile Image",
                            modifier =
                                Modifier.size(32.dp)
                                    .clip(CircleShape)
                                    .testTag("ProfileImage_${comment.id}"))
                      } else {
                        // Placeholder for users without a profile image
                        Box(
                            modifier =
                                Modifier.size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                                    .testTag("ProfileImagePlaceholder_${comment.id}"))
                      }

                      Spacer(modifier = Modifier.width(8.dp))

                      // Display the username and comment
                      Column(
                          modifier = Modifier.weight(1f).testTag("CommentContent_${comment.id}")) {
                            Text(
                                text = comment.profile?.username ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.testTag("CommentUsername_${comment.id}"))
                            Text(
                                text = comment.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("CommentText_${comment.id}"))
                          }

                      // Heart button to like the comment
                      Column(
                          horizontalAlignment = Alignment.CenterHorizontally,
                          modifier =
                              Modifier.padding(end = 8.dp)
                                  .testTag("LikeCommentSection_${comment.id}")) {
                            IconButton(
                                onClick = {
                                  commentsViewModel.likeComment(
                                      publicationId, comment.id, currentUserId)
                                },
                                modifier =
                                    Modifier.size(24.dp)
                                        .testTag("LikeCommentButton_${comment.id}")) {
                                  Icon(
                                      imageVector = Icons.Default.Favorite,
                                      contentDescription = "Like",
                                      tint =
                                          if (comment.likedBy.contains(currentUserId)) Color.Red
                                          else Color.Gray,
                                      modifier = Modifier.testTag("LikeCommentIcon_${comment.id}"))
                                }
                            Text(
                                text = comment.likes.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("CommentLikes_${comment.id}"))
                          }

                      // Trash button to delete the comment (visible only to the author)
                      if (comment.ownerId == currentUserId) {
                        IconButton(
                            onClick = {
                              commentsViewModel.deleteComment(publicationId, comment.id)
                            },
                            modifier =
                                Modifier.size(24.dp).testTag("DeleteCommentButton_${comment.id}")) {
                              Icon(
                                  imageVector = Icons.Default.Delete,
                                  contentDescription = "Delete",
                                  tint = Color.Gray,
                                  modifier = Modifier.testTag("DeleteCommentIcon_${comment.id}"))
                            }
                      }
                    }
              }
            }
          }
          is CommentsUiState.Error -> {
            val errorMessage = (commentsState as CommentsUiState.Error).message
            // Display the error message
            Box(
                modifier = Modifier.fillMaxSize().testTag("CommentsErrorIndicator"),
                contentAlignment = Alignment.Center) {
                  Text(text = errorMessage, color = Color.Red)
                }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input field and Post button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("NewCommentSection")) {
              TextField(
                  value = newCommentText,
                  onValueChange = { newCommentText = it },
                  modifier = Modifier.weight(1f).testTag("NewCommentTextField"),
                  placeholder = { Text("Write a comment...") })
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                  onClick = {
                    if (newCommentText.isNotBlank()) {
                      val ownerId = currentUserId.ifBlank { "anonymous" }
                      commentsViewModel.addComment(publicationId, ownerId, newCommentText)
                      newCommentText = "" // Clear the input field
                    } else {
                      Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                  },
                  modifier = Modifier.testTag("PostCommentButton")) {
                    Text("Post")
                  }
            }
      }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoItem(
    context: Context,
    mediaUrl: String,
    modifier: Modifier = Modifier.testTag("VideoItem"),
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
      modifier = modifier.fillMaxSize().background(Color.Black))
}

@Composable
fun PhotoItem(thumbnailUrl: String, modifier: Modifier = Modifier.testTag("PhotoItem")) {
  Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
    SubcomposeAsyncImage(
        model = thumbnailUrl,
        contentDescription = "Publication photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize())
  }
}

object ShareUtils {
  fun downloadBytes(url: String, client: OkHttpClient): ByteArray {
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()
    if (!response.isSuccessful) throw Exception("download failed: ${response.code}")
    return response.body?.bytes() ?: throw Exception("empty response body")
  }

  fun getFileExtension(mediaType: MediaType): String {
    return when (mediaType) {
      MediaType.PHOTO -> ".jpg"
      MediaType.VIDEO -> ".mp4"
    }
  }

  fun createMediaFile(context: Context, publication: Publication, bytes: ByteArray): File {
    val mediaDir =
        when (publication.mediaType) {
          MediaType.PHOTO -> File(context.cacheDir, "shared_images")
          MediaType.VIDEO -> File(context.filesDir, "shared_videos")
        }
    if (!mediaDir.exists()) mediaDir.mkdirs()

    val fileExtension = getFileExtension(publication.mediaType)
    val mediaFile = File(mediaDir, "shared_${publication.id}$fileExtension")

    FileOutputStream(mediaFile).use { it.write(bytes) }
    return mediaFile
  }

  fun createShareIntent(context: Context, publication: Publication, uri: Uri): Intent {
    val mimeType =
        when (publication.mediaType) {
          MediaType.PHOTO -> "image/jpeg"
          MediaType.VIDEO -> "video/mp4"
        }

    return Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_TEXT, "")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  fun handleShare(
      publication: Publication,
      context: Context,
      ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
      mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
      client: OkHttpClient = OkHttpClient()
  ) {
    CoroutineScope(ioDispatcher).launch {
      try {
        val bytes = downloadBytes(publication.mediaUrl, client)
        val mediaFile = createMediaFile(context, publication, bytes)
        val uri: Uri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mediaFile)
        val shareIntent = createShareIntent(context, publication, uri)
        withContext(mainDispatcher) {
          context.startActivity(Intent.createChooser(shareIntent, null))
          Toast.makeText(context, "Share launched", Toast.LENGTH_SHORT).show()
        }
      } catch (e: Exception) {
        e.printStackTrace()
        withContext(mainDispatcher) {
          Toast.makeText(context, "Error while sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
}
