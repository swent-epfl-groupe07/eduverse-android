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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.se.eduverse.R
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
  val launcher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        uri?.let { viewModel.updateProfileImage(userId, it) }
      }

  LaunchedEffect(userId) { viewModel.loadProfile(userId) }

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
                        profile.favoritePublications
                      }

                  PublicationsGrid(
                      publications = publications,
                      onPublicationClick = { /* Handle publication click */},
                      modifier = Modifier.testTag("publications_grid"))
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
private fun PublicationsGrid(
    publications: List<Publication>,
    onPublicationClick: (Publication) -> Unit,
    modifier: Modifier = Modifier
) {
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
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(publications) { publication ->
            PublicationItem(
                publication = publication,
                onClick = { onPublicationClick(publication) },
                modifier = Modifier.testTag("publication_item_${publication.id}"))
          }
        }
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
          Box(
              modifier =
                  Modifier.testTag("publication_thumbnail_${publication.id}")
                      .fillMaxSize()
                      .background(MaterialTheme.colorScheme.surfaceVariant))

          Box(
              modifier =
                  Modifier.testTag("publication_title_container_${publication.id}")
                      .fillMaxWidth()
                      .align(Alignment.BottomCenter)
                      .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                      .padding(8.dp)) {
                Text(
                    text = publication.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("publication_title_${publication.id}"))
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
