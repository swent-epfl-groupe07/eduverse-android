package com.github.se.eduverse.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.se.eduverse.R
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.gallery.PublicationItem
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.profile.CommentSection
import com.github.se.eduverse.ui.profile.CommentsMenuContent
import com.github.se.eduverse.ui.profile.ExoVideoPlayer
import com.github.se.eduverse.viewmodel.CommentsViewModel
import com.github.se.eduverse.viewmodel.DeletePublicationState
import com.github.se.eduverse.viewmodel.FollowActionState
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun UserProfileScreen(
    navigationActions: NavigationActions,
    viewModel: ProfileViewModel,
    userId: String,
    commentsViewModel: CommentsViewModel,
    currentUserId: String? =
        FirebaseAuth.getInstance().currentUser?.uid // Default for production code
) {
  var selectedTab by remember { mutableStateOf(0) }
  val uiState by viewModel.profileState.collectAsState()
  val likedPublications by viewModel.likedPublications.collectAsState(initial = emptyList())
  val followActionState by viewModel.followActionState.collectAsState()

  var isCommentsVisible by remember { mutableStateOf(false) }
  var selectedPublicationId by remember { mutableStateOf<String?>(null) }

  val sheetState =
      rememberModalBottomSheetState(
          initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

  // Show/hide comments sheet when isCommentsVisible changes
  LaunchedEffect(isCommentsVisible) {
    if (isCommentsVisible && selectedPublicationId != null) {
      sheetState.show()
      commentsViewModel.loadComments(selectedPublicationId!!)
    } else {
      sheetState.hide()
    }
  }

  // Reset states when sheet is hidden
  LaunchedEffect(sheetState.currentValue) {
    if (sheetState.currentValue == ModalBottomSheetValue.Hidden) {
      isCommentsVisible = false
      selectedPublicationId = null
    }
  }

  LaunchedEffect(userId) {
    viewModel.loadProfile(userId)
    viewModel.loadLikedPublications(userId)
  }

  ModalBottomSheetLayout(
      sheetContent = {
        if (isCommentsVisible && selectedPublicationId != null) {
          CommentsMenuContent(
              publicationId = selectedPublicationId!!,
              commentsViewModel = commentsViewModel,
              currentUserId = currentUserId ?: "",
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
            modifier = Modifier.testTag("user_profile_screen_container"),
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
                          .testTag("user_profile_top_bar"),
                  title = {
                    when (uiState) {
                      is ProfileUiState.Success -> {
                        Text(
                            text = (uiState as ProfileUiState.Success).profile.username,
                            modifier = Modifier.testTag("user_profile_username"),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary)
                      }
                      else -> {
                        Text(
                            "Profile",
                            modifier = Modifier.testTag("user_profile_title_default"),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary)
                      }
                    }
                  },
                  navigationIcon = {
                    IconButton(
                        onClick = { navigationActions.goBack() },
                        modifier = Modifier.testTag("back_button")) {
                          Icon(
                              imageVector = Icons.Default.ArrowBack,
                              contentDescription = "Back",
                              tint = MaterialTheme.colorScheme.onPrimary)
                        }
                  },
                  colors =
                      TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
              )
            },
            bottomBar = {
              BottomNavigationMenu(
                  onTabSelect = { navigationActions.navigateTo(it) },
                  tabList = LIST_TOP_LEVEL_DESTINATION,
                  selectedItem = "")
            }) { paddingValues ->
              Column(
                  modifier =
                      Modifier.testTag("user_profile_content_container")
                          .fillMaxSize()
                          .padding(paddingValues)) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Profile Image
                    Box(
                        modifier =
                            Modifier.testTag("user_profile_image_container")
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                        contentAlignment = Alignment.Center) {
                          val profileUrl =
                              if (uiState is ProfileUiState.Success)
                                  (uiState as ProfileUiState.Success).profile.profileImageUrl
                              else ""

                          AsyncImage(
                              model = profileUrl,
                              contentDescription = "Profile Image",
                              modifier = Modifier.size(96.dp).clip(CircleShape),
                              contentScale = ContentScale.Crop,
                              placeholder = painterResource(R.drawable.eduverse_logo_alone))
                        }

                    // Stats & Follow Button
                    when (uiState) {
                      is ProfileUiState.Success -> {
                        val profile = (uiState as ProfileUiState.Success).profile
                        Row(
                            modifier = Modifier.testTag("stats_row").fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                              StatItem(
                                  "Followers",
                                  profile.followers,
                                  onClick = {
                                    navigationActions.navigateToFollowersList(profile.id)
                                  },
                                  Modifier.testTag("followers_stat"))
                              StatItem(
                                  "Following",
                                  profile.following,
                                  onClick = {
                                    navigationActions.navigateToFollowingList(profile.id)
                                  },
                                  Modifier.testTag("following_stat"))
                            }

                        if (currentUserId != null && currentUserId != userId) {
                          val followActionStateValue = followActionState
                          Button(
                              onClick = { viewModel.toggleFollow(currentUserId, userId) },
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .padding(horizontal = 16.dp)
                                      .testTag("follow_button"),
                              enabled = followActionStateValue != FollowActionState.Loading) {
                                when (followActionStateValue) {
                                  FollowActionState.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onTertiary)
                                  }
                                  else -> {
                                    val profile = (uiState as ProfileUiState.Success).profile
                                    Text(
                                        if (profile.isFollowedByCurrentUser) "Unfollow"
                                        else "Follow")
                                  }
                                }
                              }

                          if (followActionStateValue is FollowActionState.Error) {
                            Text(
                                text = followActionStateValue.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp))
                          }
                        }
                      }
                      else -> {}
                    }

                    // Tabs (Publications/Favorites)
                    TabRow(
                        selectedTabIndex = selectedTab, modifier = Modifier.testTag("tabs_row")) {
                          Tab(
                              selected = selectedTab == 0,
                              onClick = { selectedTab = 0 },
                              modifier = Modifier.testTag("publications_tab"),
                              text = { Text("Publications") },
                              icon = { Icon(Icons.Default.Article, contentDescription = null) },
                              selectedContentColor =
                                  if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurface)
                          Tab(
                              selected = selectedTab == 1,
                              onClick = { selectedTab = 1 },
                              modifier = Modifier.testTag("favorites_tab"),
                              text = { Text("Favorites") },
                              icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                              selectedContentColor =
                                  if (selectedTab == 1) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurface)
                        }

                    // Content
                    when (uiState) {
                      is ProfileUiState.Loading -> {
                        Box(
                            modifier = Modifier.testTag("loading_container").fillMaxSize(),
                            contentAlignment = Alignment.Center) {
                              CircularProgressIndicator(
                                  modifier = Modifier.testTag("loading_indicator"))
                            }
                      }
                      is ProfileUiState.Error -> {
                        ErrorMessage(
                            message = (uiState as ProfileUiState.Error).message,
                            modifier = Modifier.testTag("error_container"))
                      }
                      is ProfileUiState.Success -> {
                        val profile = (uiState as ProfileUiState.Success).profile
                        val publications =
                            if (selectedTab == 0) {
                              profile.publications
                            } else {
                              likedPublications
                            }

                        var localSelectedPublication by remember {
                          mutableStateOf<Publication?>(null)
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.testTag("publications_grid").fillMaxSize(),
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)) {
                              items(publications) { publication ->
                                PublicationItem(
                                    mediaType = publication.mediaType,
                                    thumbnailUrl = publication.thumbnailUrl,
                                    onClick = { localSelectedPublication = publication },
                                    modifier =
                                        Modifier.testTag("publication_item_${publication.id}"))
                              }
                            }

                        // If a publication is selected, show detail dialog with comment features
                        localSelectedPublication?.let { pub ->
                          PublicationDetailDialog(
                              publication = pub,
                              profileViewModel = viewModel,
                              currentUserId = currentUserId ?: "",
                              commentsViewModel = commentsViewModel,
                              onDismiss = { localSelectedPublication = null },
                          )
                        }
                      }
                    }
                  }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicationDetailDialog(
    publication: Publication,
    profileViewModel: ProfileViewModel,
    commentsViewModel: CommentsViewModel,
    currentUserId: String,
    onDismiss: () -> Unit
) {
  val isLiked = remember { mutableStateOf(publication.likedBy.contains(currentUserId)) }
  val likeCount = remember { mutableStateOf(publication.likes) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  val deleteState by profileViewModel.deletePublicationState.collectAsState()

  var isFavorited by remember { mutableStateOf(false) }
  LaunchedEffect(publication.id) {
    isFavorited = profileViewModel.repository.isPublicationFavorited(currentUserId, publication.id)
  }

  var errorMessage by remember { mutableStateOf<String?>(null) }

  // State to show comments like a bottom sheet overlay in the dialog
  var showComments by remember { mutableStateOf(false) }

  LaunchedEffect(showComments) {
    if (showComments) {
      commentsViewModel.loadComments(publication.id)
    }
  }

  LaunchedEffect(deleteState) {
    when (deleteState) {
      is DeletePublicationState.Success -> {
        errorMessage = null
        onDismiss()
        profileViewModel.resetDeleteState()
      }
      is DeletePublicationState.Error -> {
        errorMessage = (deleteState as DeletePublicationState.Error).message
        profileViewModel.resetDeleteState()
      }
      else -> {
        errorMessage = null
      }
    }
  }

  // Confirmation dialog for delete action
  if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text("Delete Publication") },
        text = {
          Text("Are you sure you want to delete this publication? This action cannot be undone.")
        },
        confirmButton = {
          Button(
              onClick = {
                profileViewModel.deletePublication(publication.id, currentUserId)
                showDeleteDialog = false
              },
              colors =
                  ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
              }
        },
        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } })
  }

  Dialog(
      onDismissRequest = onDismiss,
      properties =
          DialogProperties(
              usePlatformDefaultWidth = false,
              dismissOnBackPress = true,
              dismissOnClickOutside = false)) {
        Box(modifier = Modifier.fillMaxSize().testTag("publication_detail_dialog")) {
          Column(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
              Column(modifier = Modifier.fillMaxSize()) {
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
                    actions = {
                      if (publication.userId == currentUserId) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("delete_button")) {
                              Icon(
                                  imageVector = Icons.Default.Delete,
                                  contentDescription = "Delete publication",
                                  tint = Color.White)
                            }
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

                            // Comment Button - show comments in a style similar to ProfileScreen
                            IconButton(
                                onClick = { showComments = true },
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

                      // If we are showing comments, replicate the bottom sheet style here
                      if (showComments) {
                        // Overlay a surface that looks like CommentsMenuContent
                        Box(
                            modifier =
                                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.BottomCenter) {
                              Surface(
                                  modifier =
                                      Modifier.fillMaxWidth()
                                          .heightIn(min = 200.dp)
                                          .clip(
                                              RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                          .background(MaterialTheme.colorScheme.background),
                                  color = MaterialTheme.colorScheme.background) {
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
                                                onClick = { showComments = false },
                                                modifier =
                                                    Modifier.testTag("close_comments_button")) {
                                                  Icon(
                                                      Icons.Default.Close,
                                                      contentDescription = "Close comments")
                                                }
                                          }
                                      Divider(modifier = Modifier.padding(vertical = 8.dp))
                                      CommentSection(
                                          publication.id, commentsViewModel, currentUserId)
                                    }
                                  }
                            }
                      }
                    }
              }
            }
          }
        }
      }
}
