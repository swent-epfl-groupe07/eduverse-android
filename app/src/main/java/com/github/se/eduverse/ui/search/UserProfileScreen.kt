package com.github.se.eduverse.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.github.se.eduverse.R
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.profile.PublicationDetailDialog
import com.github.se.eduverse.ui.profile.PublicationItem
import com.github.se.eduverse.viewmodel.FollowActionState
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navigationActions: NavigationActions,
    viewModel: ProfileViewModel,
    userId: String,
    currentUserId: String? =
        FirebaseAuth.getInstance().currentUser?.uid // Default for production code
) {
  var selectedTab by remember { mutableStateOf(0) }
  val uiState by viewModel.profileState.collectAsState()
  val likedPublications by viewModel.likedPublications.collectAsState(initial = emptyList())
  val followActionState by viewModel.followActionState.collectAsState()

  LaunchedEffect(userId) {
    viewModel.loadProfile(userId)
    viewModel.loadLikedPublications(userId)
  }

  Scaffold(
      modifier = Modifier.testTag("user_profile_screen_container"),
      topBar = {
        TopAppBar(
            modifier = Modifier.testTag("user_profile_top_bar"),
            title = {
              when (uiState) {
                is ProfileUiState.Success ->
                    Text(
                        text = (uiState as ProfileUiState.Success).profile.username,
                        modifier = Modifier.testTag("user_profile_username"))
                else -> Text("Profile", modifier = Modifier.testTag("user_profile_title_default"))
              }
            },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("back_button")) {
                    Icon(Icons.Default.ArrowBack, "Back")
                  }
            })
      },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.testTag("user_profile_content_container")
                    .fillMaxSize()
                    .padding(paddingValues)) {
              // Profile Image (non-editable)
              Box(
                  modifier =
                      Modifier.testTag("user_profile_image_container")
                          .fillMaxWidth()
                          .padding(top = 16.dp),
                  contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model =
                            if (uiState is ProfileUiState.Success)
                                (uiState as ProfileUiState.Success).profile.profileImageUrl
                            else "",
                        contentDescription = "Profile Image",
                        modifier = Modifier.size(96.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.eduverse_logo_alone))
                  }

              // Stats (Followers/Following)
              when (uiState) {
                is ProfileUiState.Success -> {
                  val profile = (uiState as ProfileUiState.Success).profile
                  Row(
                      modifier = Modifier.testTag("stats_row").fillMaxWidth().padding(16.dp),
                      horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("Followers", profile.followers, Modifier.testTag("followers_stat"))
                        StatItem("Following", profile.following, Modifier.testTag("following_stat"))
                      }

                  // Follow/Unfollow Button
                  if (currentUserId != null && currentUserId != userId) {
                    Button(
                        onClick = { viewModel.toggleFollow(currentUserId, userId) },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("follow_button"),
                        enabled = followActionState != FollowActionState.Loading) {
                          when (followActionState) {
                            FollowActionState.Loading -> {
                              CircularProgressIndicator(
                                  modifier = Modifier.size(24.dp),
                                  color = MaterialTheme.colorScheme.onTertiary)
                            }
                            else -> {
                              when (uiState) {
                                is ProfileUiState.Success -> {
                                  val profile = (uiState as ProfileUiState.Success).profile
                                  Text(
                                      if (profile.isFollowedByCurrentUser) "Unfollow" else "Follow")
                                }
                                else -> Text("Follow")
                              }
                            }
                          }
                        }

                    // Show error if follow action failed
                    if (followActionState is FollowActionState.Error) {
                      Text(
                          text = (followActionState as FollowActionState.Error).message,
                          color = MaterialTheme.colorScheme.error,
                          style = MaterialTheme.typography.bodySmall,
                          modifier = Modifier.padding(horizontal = 16.dp))
                    }
                  }
                }
                else -> {}
              }

              // Publications/Favorites Tabs
              TabRow(selectedTabIndex = selectedTab, modifier = Modifier.testTag("tabs_row")) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("publications_tab"),
                    text = { Text("Publications") },
                    icon = { Icon(Icons.Default.Article, contentDescription = null) },
                    selectedContentColor = MaterialTheme.colorScheme.secondary)
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("favorites_tab"),
                    text = { Text("Favorites") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    selectedContentColor = MaterialTheme.colorScheme.secondary)
              }

              // Content based on state
              when (uiState) {
                is ProfileUiState.Loading -> {
                  Box(
                      modifier = Modifier.testTag("loading_container").fillMaxSize(),
                      contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
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

                  PublicationsGrid(
                      publications = publications,
                      currentUserId = currentUserId ?: "",
                      profileViewModel = viewModel,
                      onPublicationClick = { /* Handle publication click */},
                      modifier = Modifier.testTag("publications_grid"))
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
