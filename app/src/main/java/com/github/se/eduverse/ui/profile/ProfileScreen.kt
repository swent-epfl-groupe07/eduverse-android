package com.github.se.eduverse.ui.profile

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.se.eduverse.R
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.DeletePublicationState
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.UsernameUpdateState
import com.google.firebase.auth.FirebaseAuth

var auth = FirebaseAuth.getInstance()

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SuspiciousIndentation")
@Composable
fun ProfileScreen(
    navigationActions: NavigationActions,
    viewModel: ProfileViewModel,
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
                                  imageVector = Icons.Default.Edit,
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
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
            actions = {
              IconButton(
                  onClick = { navigationActions.navigateTo(Screen.SETTING) },
                  modifier = Modifier.testTag("settings_button")) {
                    Icon(
                        imageVector = Icons.Default.Settings,
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
        Column(
            modifier =
                Modifier.testTag("profile_content_container")
                    .fillMaxSize()
                    .padding(paddingValues)) {
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

                  PublicationsGrid(
                      publications = publications,
                      currentUserId = userId,
                      profileViewModel = viewModel,
                      onPublicationClick = {},
                      modifier = Modifier.testTag("publications_grid"))
                }
              }
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

@Composable
fun PublicationItem(publication: Publication, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier.aspectRatio(1f).clickable(onClick = onClick),
      shape = RoundedCornerShape(8.dp)) {
        Box(modifier = Modifier.testTag("publication_content_${publication.id}").fillMaxSize()) {
          AsyncImage(
              model =
                  ImageRequest.Builder(LocalContext.current)
                      .data(publication.thumbnailUrl)
                      .crossfade(true)
                      .listener(
                          onError = { _, _ ->
                            println("Failed to load thumbnail for ${publication.id}")
                          },
                          onSuccess = { _, _ ->
                            println("Successfully loaded thumbnail for ${publication.id}")
                          })
                      .build(),
              contentDescription = "Publication thumbnail",
              modifier = Modifier.testTag("publication_thumbnail_${publication.id}").fillMaxSize(),
              contentScale = ContentScale.Crop,
              error = painterResource(R.drawable.eduverse_logo_alone),
              fallback = painterResource(R.drawable.eduverse_logo_alone),
          )

          // Video indicator overlay
          if (publication.mediaType == MediaType.VIDEO) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Video",
                modifier =
                    Modifier.size(48.dp)
                        .align(Alignment.Center)
                        .testTag("video_play_icon_${publication.id}"), // Add this testTag
                tint = Color.White)
          }
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicationDetailDialog(
    publication: Publication,
    profileViewModel: ProfileViewModel,
    currentUserId: String,
    onDismiss: () -> Unit
) {
  val isLiked = remember { mutableStateOf(publication.likedBy.contains(currentUserId)) }
  val likeCount = remember { mutableStateOf(publication.likes) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  val deleteState by profileViewModel.deletePublicationState.collectAsState()
  val favoriteState by profileViewModel.favoriteActionState.collectAsState()

  // Track if publication is favorited
  var isFavorited by remember { mutableStateOf(false) }

  // Check if publication is favorited on initial load
  LaunchedEffect(publication.id) {
    isFavorited = profileViewModel.repository.isPublicationFavorited(currentUserId, publication.id)
  }

  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Handle delete state changes
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

                      // Action Buttons Column
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
                                        modifier =
                                            Modifier.size(48.dp)
                                                .testTag(
                                                    if (isLiked.value) "liked_icon"
                                                    else "unliked_icon"))
                                  }
                              Text(
                                  text = likeCount.value.toString(),
                                  style = MaterialTheme.typography.bodyMedium,
                                  color = Color.White,
                                  modifier = Modifier.testTag("like_count_${publication.id}"))
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
                                      modifier =
                                          Modifier.size(48.dp)
                                              .testTag(
                                                  if (isFavorited) "favorited_icon"
                                                  else "unfavorited_icon"))
                                }
                          }
                    }
              }
            }
          }
        }
      }
}

@Composable
private fun PublicationsGrid(
    publications: List<Publication>,
    currentUserId: String,
    profileViewModel: ProfileViewModel,
    onPublicationClick: (Publication) -> Unit,
    modifier: Modifier = Modifier
) {
  var selectedPublication by remember { mutableStateOf<Publication?>(null) }

  if (publications.isEmpty()) {
    Box(
        modifier = Modifier.testTag("empty_publications_container").fillMaxSize(),
        contentAlignment = Alignment.Center) {
          Text(
              text = "No publications yet",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("empty_publications_text"))
        }
  } else {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)) {
          items(publications) { publication ->
            PublicationItem(
                publication = publication,
                onClick = { selectedPublication = publication },
                modifier = Modifier.testTag("publication_item_${publication.id}"))
          }
        }

    // Show detail dialog when a publication is selected
    selectedPublication?.let { publication ->
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = profileViewModel,
          currentUserId = currentUserId,
          onDismiss = { selectedPublication = null })
    }
  }
}

@Composable
private fun VideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
  ExoVideoPlayer(videoUrl = videoUrl, modifier = modifier)
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
