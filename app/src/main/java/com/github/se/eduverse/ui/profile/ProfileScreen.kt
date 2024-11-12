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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
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
    var selectedTab by remember { mutableStateOf(0) }
    val uiState by viewModel.profileState.collectAsState()
    val likedPublications by viewModel.likedPublications.collectAsState(initial = emptyList())
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { viewModel.updateProfileImage(userId, it) }
        }

    LaunchedEffect(userId) {
        if (auth.currentUser == null) {
            navigationActions.navigateTo(Screen.AUTH)
            return@LaunchedEffect
        }

        viewModel.loadProfile(userId)
        viewModel.loadLikedPublications(userId) // Charger les publications likées
    }

    Scaffold(
        modifier = Modifier.testTag("profile_screen_container"),
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag("profile_top_bar"),
                title = {
                    when (uiState) {
                        is ProfileUiState.Success ->
                            Text(
                                text = (uiState as ProfileUiState.Success).profile.username,
                                modifier = Modifier.testTag("profile_username"))
                        else -> Text("Profile", modifier = Modifier.testTag("profile_title_default"))
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navigationActions.goBack() },
                        modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navigationActions.navigateTo(Screen.SETTING) },
                        modifier = Modifier.testTag("settings_button")) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                })
        },
        bottomBar = {
            BottomNavigationMenu(
                onTabSelect = { route -> navigationActions.navigateTo(route) },
                tabList = LIST_TOP_LEVEL_DESTINATION,
                selectedItem = navigationActions.currentRoute())
        }) { paddingValues ->
        Column(
            modifier =
            Modifier.testTag("profile_content_container")
                .fillMaxSize()
                .padding(paddingValues)) {
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
                        StatItem("Followers", profile.followers, Modifier.testTag("followers_stat"))
                        StatItem("Following", profile.following, Modifier.testTag("following_stat"))
                    }
                }
                else -> {}
            }

            TabRow(selectedTabIndex = selectedTab, modifier = Modifier.testTag("tabs_row")) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("publications_tab"),
                    text = { Text("Publications") },
                    icon = { Icon(Icons.Default.Article, contentDescription = null) })
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("favorites_tab"),
                    text = { Text("Favorites") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) })
            }

            when (uiState) {
                is ProfileUiState.Loading -> {
                    Box(
                        modifier = Modifier.testTag("loading_container").fillMaxSize(),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
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
                            likedPublications // Affichage des publications likées
                        }

                    PublicationsGrid(
                        publications = publications,
                        currentUserId = userId, // Passe l'ID de l'utilisateur actuel
                        profileViewModel = viewModel, // Passe le ViewModel pour gérer les interactions
                        onPublicationClick = { /* Handle publication click */ },
                        modifier = Modifier.testTag("publications_grid")
                    )

                }
            }
        }
    }
}


@Composable
private fun StatItem(label: String, count: Int, modifier: Modifier = Modifier) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
private fun PublicationItem(
    publication: Publication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
private fun PublicationDetailDialog(
    publication: Publication,
    profileViewModel: ProfileViewModel,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val isLiked = remember { mutableStateOf(publication.likedBy.contains(currentUserId)) }
    val likeCount = remember { mutableStateOf(publication.likes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
                SmallTopAppBar(
                    title = { Text(publication.title, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Black, titleContentColor = Color.White
                    )
                )

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (publication.mediaType) {
                        MediaType.VIDEO -> {
                            ExoVideoPlayer(
                                videoUrl = publication.mediaUrl, modifier = Modifier.fillMaxWidth()
                            )
                        }
                        MediaType.PHOTO -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(publication.mediaUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Publication media",
                                modifier = Modifier.fillMaxSize().testTag("detail_photo_view"),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // Icône du cœur et compteur de likes
                    Column(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                if (isLiked.value) {
                                    profileViewModel.removeLike(currentUserId, publication.id)
                                    isLiked.value = false
                                    likeCount.value -= 1
                                } else {
                                    profileViewModel.likeAndAddToFavorites(currentUserId, publication.id)
                                    isLiked.value = true
                                    likeCount.value += 1
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Like",
                                tint = if (isLiked.value) Color.Red else Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Text(
                            text = likeCount.value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.testTag("like_count_${publication.id}")
                        )
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
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No publications yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("empty_publications_text")
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(publications) { publication ->
                PublicationItem(
                    publication = publication,
                    onClick = { selectedPublication = publication },
                    modifier = Modifier.testTag("publication_item_${publication.id}")
                )
            }
        }

        // Show detail dialog when a publication is selected
        selectedPublication?.let { publication ->
            PublicationDetailDialog(
                publication = publication,
                profileViewModel = profileViewModel,
                currentUserId = currentUserId,
                onDismiss = { selectedPublication = null }
            )
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
