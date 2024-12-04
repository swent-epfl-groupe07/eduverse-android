// VideoScreen.kt
package com.github.se.eduverse.ui.videos

// Android Imports
// Accompanist Pager Imports
// Coil Image Loading
// Project-Specific Imports
// Pager Imports
// Firebase Import
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
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
import com.github.se.eduverse.viewmodel.CommentsUiState
import com.github.se.eduverse.viewmodel.CommentsViewModel
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.PublicationViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class)
@Composable
fun VideoScreen(
    navigationActions: NavigationActions,
    publicationViewModel: PublicationViewModel,
    profileViewModel: ProfileViewModel,
    commentsViewModel: CommentsViewModel,
    currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
) {
  val publications by publicationViewModel.publications.collectAsState()
  val publicationError by publicationViewModel.error.collectAsState()
  val pagerState = rememberPagerState()

  var isCommentsVisible by remember { mutableStateOf(false) }
  var selectedPublicationId by remember { mutableStateOf<String?>(null) }

  val sheetState =
      rememberModalBottomSheetState(
          initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

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
                  VerticalPager(
                      count = publications.size,
                      state = pagerState,
                      modifier =
                          Modifier.fillMaxSize().padding(paddingValues).testTag("VerticalPager")) {
                          page ->
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
                                    context = LocalContext.current, mediaUrl = publication.mediaUrl)
                              } else {
                                PhotoItem(thumbnailUrl = publication.thumbnailUrl)
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
  Box(modifier = Modifier.fillMaxWidth().background(Color.White).testTag("CommentsMenuContent")) {
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
            style = MaterialTheme.typography.h6,
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
                                style = MaterialTheme.typography.subtitle2,
                                color = Color.Black,
                                modifier = Modifier.testTag("CommentUsername_${comment.id}"))
                            Text(
                                text = comment.text,
                                style = MaterialTheme.typography.body2,
                                color = Color.Black,
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
                                style = MaterialTheme.typography.caption,
                                color = Color.Black,
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
        contentDescription = "Publication photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize())
  }
}
