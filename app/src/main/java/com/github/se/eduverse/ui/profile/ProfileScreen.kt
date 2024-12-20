package com.github.se.eduverse.ui.profile

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.gallery.PublicationItem
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.CommentsUiState
import com.github.se.eduverse.viewmodel.CommentsViewModel
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.UsernameUpdateState
import com.google.firebase.auth.FirebaseAuth

var auth = FirebaseAuth.getInstance()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@SuppressLint("SuspiciousIndentation")
@Composable
fun ProfileScreen(
    navigationActions: NavigationActions,
    viewModel: ProfileViewModel,
    commentsViewModel: CommentsViewModel,
    userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
) {
  var showUsernameDialog by remember { mutableStateOf(false) }
  val usernameState by viewModel.usernameState.collectAsState()
  var selectedTab by remember { mutableStateOf(0) }
  val uiState by viewModel.profileState.collectAsState()
  val likedPublications by viewModel.likedPublications.collectAsState(initial = emptyList())
  val launcher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        uri?.let { viewModel.updateProfileImage(userId, it) }
      }

  var isCommentsVisible by remember { mutableStateOf(false) }
  var selectedPublication by remember { mutableStateOf<Publication?>(null) }
  var selectedPublicationId by remember { mutableStateOf<String?>(null) }

  val sheetState =
      rememberModalBottomSheetState(
          initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

  // Handle opening and closing of the sheet
  LaunchedEffect(isCommentsVisible) {
    if (isCommentsVisible && selectedPublicationId != null) {
      sheetState.show()
      commentsViewModel.loadComments(selectedPublicationId!!)
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

  LaunchedEffect(userId) {
    if (auth.currentUser == null) {
      navigationActions.navigateTo(Screen.AUTH)
      return@LaunchedEffect
    }
    viewModel.loadProfile(userId)
    viewModel.loadLikedPublications(userId)
  }

  if (showUsernameDialog) {
    UsernameEditDialog(
        onDismiss = {
          showUsernameDialog = false
          viewModel.resetUsernameState()
        },
        onSubmit = { newUsername -> viewModel.updateUsername(userId, newUsername) },
        currentUsername = (uiState as? ProfileUiState.Success)?.profile?.username ?: "",
        usernameState = usernameState,
        onSuccess = {
          showUsernameDialog = false
          viewModel.resetUsernameState()
        })
  }

  ModalBottomSheetLayout(
      sheetContent = {
        if (isCommentsVisible && selectedPublicationId != null) {
          CommentsMenuContent(
              publicationId = selectedPublicationId!!,
              commentsViewModel = commentsViewModel,
              currentUserId = userId,
              onDismiss = {
                isCommentsVisible = false
                selectedPublicationId = null
              })
        } else {
          Spacer(modifier = Modifier.height(1.dp))
        }
      },
      sheetState = sheetState,
      scrimColor = Color.Black.copy(alpha = 0.32f)) {
        Scaffold(
            topBar = {
              TopAppBar(
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(
                              Brush.horizontalGradient(
                                  colors =
                                      listOf(
                                          MaterialTheme.colorScheme.secondary,
                                          MaterialTheme.colorScheme.primary)))
                          .testTag("topNavigationBar"),
                  title = {
                    when (uiState) {
                      is ProfileUiState.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showUsernameDialog = true }) {
                              Text(
                                  text = (uiState as ProfileUiState.Success).profile.username,
                                  style = MaterialTheme.typography.titleLarge,
                                  color = MaterialTheme.colorScheme.onPrimary,
                                  modifier = Modifier.testTag("profile_username"))
                              IconButton(
                                  onClick = { showUsernameDialog = true },
                                  modifier = Modifier.testTag("edit_username_button")) {
                                    Icon(
                                        imageVector =
                                            androidx.compose.material.icons.Icons.Default.Edit,
                                        contentDescription = "Edit username",
                                        tint = MaterialTheme.colorScheme.onPrimary)
                                  }
                            }
                      }
                      else -> {
                        Text(
                            "Profile",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("profile_title_default"))
                      }
                    }
                  },
                  colors =
                      TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
                  actions = {
                    IconButton(
                        onClick = { navigationActions.navigateTo(Screen.SETTING) },
                        modifier = Modifier.testTag("settings_button")) {
                          Icon(
                              imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                              contentDescription = "Settings",
                              tint = MaterialTheme.colorScheme.onPrimary)
                        }
                  })
            },
            bottomBar = {
              BottomNavigationMenu(
                  onTabSelect = { route -> navigationActions.navigateTo(route) },
                  tabList = LIST_TOP_LEVEL_DESTINATION,
                  selectedItem = Route.PROFILE)
            }) { paddingValues ->
              Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Column(modifier = Modifier.testTag("profile_content_container").fillMaxSize()) {
                  Spacer(modifier = Modifier.height(12.dp))

                  Box(
                      modifier =
                          Modifier.testTag("profile_image_container")
                              .fillMaxWidth()
                              .padding(top = 16.dp),
                      contentAlignment = Alignment.Center) {
                        ProfileImage(
                            imageUrl =
                                if (uiState is ProfileUiState.Success)
                                    (uiState as ProfileUiState.Success).profile.profileImageUrl
                                else "",
                            onImageClick = { launcher.launch("image/*") },
                            modifier = Modifier.testTag("profile_image"))
                      }

                  when (uiState) {
                    is ProfileUiState.Success -> {
                      val profile = (uiState as ProfileUiState.Success).profile
                      Row(
                          modifier = Modifier.testTag("stats_row").fillMaxWidth().padding(16.dp),
                          horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem(
                                "Followers",
                                profile.followers,
                                onClick = { navigationActions.navigateToFollowersList(profile.id) },
                                Modifier.testTag("followers_stat"))
                            StatItem(
                                "Following",
                                profile.following,
                                onClick = { navigationActions.navigateToFollowingList(profile.id) },
                                Modifier.testTag("following_stat"))
                          }
                    }
                    else -> {}
                  }

                  TabRow(selectedTabIndex = selectedTab, modifier = Modifier.testTag("tabs_row")) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                          Text(
                              "Publications",
                              style = MaterialTheme.typography.labelLarge,
                              color =
                                  if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurface)
                        },
                        icon = {
                          Icon(
                              Icons.Default.Article,
                              contentDescription = null,
                              tint =
                                  if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurface)
                        },
                        modifier = Modifier.testTag("publications_tab"))
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                          Text(
                              "Favorites",
                              style = MaterialTheme.typography.labelLarge,
                              color =
                                  if (selectedTab == 1) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurface)
                        },
                        icon = {
                          Icon(
                              Icons.Default.Favorite,
                              contentDescription = null,
                              tint =
                                  if (selectedTab == 1) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurface)
                        },
                        modifier = Modifier.testTag("favorites_tab"))
                  }

                  when (uiState) {
                    is ProfileUiState.Loading -> {
                      Box(
                          modifier = Modifier.testTag("loading_container").fillMaxSize(),
                          contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("loading_indicator"))
                          }
                    }
                    is ProfileUiState.Error ->
                        ErrorMessage(
                            message = (uiState as ProfileUiState.Error).message,
                            modifier = Modifier.testTag("error_container"))
                    is ProfileUiState.Success -> {
                      val profile = (uiState as ProfileUiState.Success).profile
                      val publications =
                          if (selectedTab == 0) {
                            profile.publications
                          } else {
                            likedPublications
                          }

                      LazyVerticalGrid(
                          columns = GridCells.Fixed(3),
                          modifier = Modifier.testTag("publications_grid").fillMaxSize(),
                          contentPadding = PaddingValues(1.dp),
                          horizontalArrangement = Arrangement.spacedBy(1.dp),
                          verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            items(publications) { publication ->
                              PublicationItem(
                                  publication.mediaType,
                                  publication.thumbnailUrl,
                                  onClick = { selectedPublication = publication },
                                  modifier = Modifier.testTag("publication_item_${publication.id}"))
                            }
                          }
                    }
                  }
                }

                selectedPublication?.let { publication ->
                  PublicationDetailView(
                      publication = publication,
                      profileViewModel = viewModel,
                      currentUserId = userId,
                      onDismiss = { selectedPublication = null },
                      onShowComments = { pubId ->
                        selectedPublicationId = pubId
                        isCommentsVisible = true
                      })
                }
              }
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicationDetailView(
    publication: Publication,
    profileViewModel: ProfileViewModel,
    currentUserId: String,
    onDismiss: () -> Unit,
    onShowComments: (String) -> Unit
) {
  val isLiked = remember { mutableStateOf(publication.likedBy.contains(currentUserId)) }
  val likeCount = remember { mutableStateOf(publication.likes) }
  var isFavorited by remember { mutableStateOf(false) }

  LaunchedEffect(publication.id) {
    isFavorited = profileViewModel.repository.isPublicationFavorited(currentUserId, publication.id)
  }

  Box(
      modifier =
          Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable {
            onDismiss()
          }) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp).background(Color.Black)) {
          SmallTopAppBar(
              title = {
                Text(
                    publication.title,
                    color = Color.White,
                    modifier = Modifier.testTag("publication_title"))
              },
              navigationIcon = {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_button")) {
                  Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
              },
              colors =
                  TopAppBarDefaults.smallTopAppBarColors(
                      containerColor = Color.Black, titleContentColor = Color.White))

          Box(
              modifier = Modifier.weight(1f).fillMaxWidth().testTag("media_container"),
              contentAlignment = Alignment.Center) {
                when (publication.mediaType) {
                  MediaType.VIDEO -> {
                    ExoVideoPlayer(
                        videoUrl = publication.mediaUrl,
                        modifier = Modifier.fillMaxWidth().testTag("video_player"))
                  }
                  MediaType.PHOTO -> {
                    AsyncImage(
                        model =
                            ImageRequest.Builder(LocalContext.current)
                                .data(publication.mediaUrl)
                                .crossfade(true)
                                .build(),
                        contentDescription = "Publication media",
                        modifier = Modifier.fillMaxSize().testTag("detail_photo_view"),
                        contentScale = ContentScale.Fit)
                  }
                }

                // Action Buttons Column (Like, Comment, Favorite)
                Column(
                    modifier =
                        Modifier.align(Alignment.CenterEnd)
                            .padding(16.dp)
                            .testTag("action_buttons"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                      // Like Button
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                              if (isLiked.value) {
                                profileViewModel.removeLike(currentUserId, publication.id)
                                isLiked.value = false
                                likeCount.value -= 1
                              } else {
                                profileViewModel.likeAndAddToFavorites(
                                    currentUserId, publication.id)
                                isLiked.value = true
                                likeCount.value += 1
                              }
                            },
                            modifier = Modifier.testTag("like_button")) {
                              Icon(
                                  imageVector = Icons.Default.Favorite,
                                  contentDescription = "Like",
                                  tint = if (isLiked.value) Color.Red else Color.White,
                                  modifier = Modifier.size(48.dp))
                            }
                        Text(
                            text = likeCount.value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.testTag("like_count_${publication.id}"))
                      }

                      // Comment Button
                      IconButton(
                          onClick = {
                            onShowComments(publication.id)
                            Log.d(
                                "COMMENT",
                                "Comment button clicked for publication: ${publication.id}")
                          },
                          modifier = Modifier.testTag("comment_button")) {
                            Icon(
                                painter = painterResource(id = R.drawable.comment),
                                contentDescription = "Comment",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp))
                          }

                      // Favorite Button
                      IconButton(
                          onClick = {
                            profileViewModel.toggleFavorite(currentUserId, publication.id)
                            isFavorited = !isFavorited
                          },
                          modifier = Modifier.testTag("favorite_button")) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Favorite",
                                tint = if (isFavorited) Color.Yellow else Color.White,
                                modifier = Modifier.size(48.dp))
                          }
                    }
              }
        }
      }
}

@Composable
fun ProfileImage(imageUrl: String, onImageClick: () -> Unit, modifier: Modifier = Modifier) {
  Box(modifier = modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
        contentDescription = "Profile Image",
        modifier =
            Modifier.testTag("profile_image_async")
                .size(96.dp)
                .clip(CircleShape)
                .clickable(onClick = onImageClick),
        contentScale = ContentScale.Crop,
        placeholder = painterResource(R.drawable.eduverse_logo_alone))

    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Edit",
        modifier =
            Modifier.testTag("profile_image_edit_icon")
                .align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(4.dp))
  }
}

@Composable
private fun ErrorMessage(message: String, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("error_message"))
  }
}

@Composable
private fun UsernameEditDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    currentUsername: String,
    usernameState: UsernameUpdateState,
    onSuccess: () -> Unit
) {
  var username by remember { mutableStateOf(currentUsername) }
  var isError by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }

  // Handle success state
  LaunchedEffect(usernameState) {
    if (usernameState is UsernameUpdateState.Success) {
      onSuccess()
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface) {
          Column(
              modifier = Modifier.padding(16.dp).testTag("username_edit_dialog"),
              verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Edit Username", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                      username = it
                      isError = false
                      errorMessage = ""
                    },
                    label = { Text("Username") },
                    isError = isError || usernameState is UsernameUpdateState.Error,
                    supportingText =
                        when {
                          isError -> {
                            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                          }
                          usernameState is UsernameUpdateState.Error -> {
                            { Text(usernameState.message, color = MaterialTheme.colorScheme.error) }
                          }
                          else -> null
                        },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth())

                when (usernameState) {
                  is UsernameUpdateState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                  }
                  else -> {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                      TextButton(
                          onClick = onDismiss, modifier = Modifier.testTag("cancel_button")) {
                            Text("Cancel")
                          }
                      Spacer(modifier = Modifier.width(8.dp))
                      Button(
                          onClick = { onSubmit(username) },
                          modifier = Modifier.testTag("submit_button"),
                          enabled =
                              username != currentUsername &&
                                  username.isNotBlank() &&
                                  usernameState !is UsernameUpdateState.Loading) {
                            Text("Save")
                          }
                    }
              }
        }
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

  // Load comments when this UI is shown or when publicationId changes
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
            Box(
                modifier = Modifier.fillMaxSize().testTag("CommentsLoadingIndicator"),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator()
                }
          }
          is CommentsUiState.Success -> {
            val comments = (commentsState as CommentsUiState.Success).comments
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f).testTag("CommentsList")) {
                  items(comments) { comment ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.padding(vertical = 4.dp)
                                .fillMaxWidth()
                                .testTag("CommentItem_${comment.id}")) {
                          // Profile image
                          if (!comment.profile?.profileImageUrl.isNullOrBlank()) {
                            coil.compose.SubcomposeAsyncImage(
                                model = comment.profile?.profileImageUrl,
                                contentDescription = "Profile Image",
                                modifier =
                                    Modifier.size(32.dp)
                                        .clip(CircleShape)
                                        .testTag("ProfileImage_${comment.id}"))
                          } else {
                            Box(
                                modifier =
                                    Modifier.size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray)
                                        .testTag("ProfileImagePlaceholder_${comment.id}"))
                          }

                          Spacer(modifier = Modifier.width(8.dp))

                          Column(
                              modifier =
                                  Modifier.weight(1f).testTag("CommentContent_${comment.id}")) {
                                Text(
                                    text = comment.profile?.username ?: "Unknown",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.testTag("CommentUsername_${comment.id}"))
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("CommentText_${comment.id}"))
                              }

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
                                          modifier =
                                              Modifier.testTag("LikeCommentIcon_${comment.id}"))
                                    }
                                Text(
                                    text = comment.likes.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.testTag("CommentLikes_${comment.id}"))
                              }

                          if (comment.ownerId == currentUserId) {
                            IconButton(
                                onClick = {
                                  commentsViewModel.deleteComment(publicationId, comment.id)
                                },
                                modifier =
                                    Modifier.size(24.dp)
                                        .testTag("DeleteCommentButton_${comment.id}")) {
                                  Icon(
                                      imageVector =
                                          androidx.compose.material.icons.Icons.Default.Delete,
                                      contentDescription = "Delete",
                                      tint = Color.Gray,
                                      modifier =
                                          Modifier.testTag("DeleteCommentIcon_${comment.id}"))
                                }
                          }
                        }
                  }
                }
          }
          is CommentsUiState.Error -> {
            val errorMessage = (commentsState as CommentsUiState.Error).message
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
                      android.widget.Toast.makeText(
                              context, "Comment cannot be empty", android.widget.Toast.LENGTH_SHORT)
                          .show()
                    }
                  },
                  modifier = Modifier.testTag("PostCommentButton")) {
                    Text("Post")
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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Comments",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("comments_title"))
                IconButton(
                    onClick = onDismiss, modifier = Modifier.testTag("close_comments_button")) {
                      Icon(Icons.Default.Close, contentDescription = "Close comments")
                    }
              }
          Divider(modifier = Modifier.padding(vertical = 8.dp))
          CommentSection(publicationId, commentsViewModel, currentUserId)
        }
      }
}

@Composable
private fun StatItem(
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.clickable(onClick = onClick).testTag("stat_${label.lowercase()}"),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("stat_count_$label"))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("stat_label_$label"))
      }
}
