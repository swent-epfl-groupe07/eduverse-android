package com.github.se.eduverse.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.github.se.eduverse.R
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.SearchProfileState

const val TAG_SEARCH_FIELD = "search_field"
const val TAG_LOADING_INDICATOR = "loading_indicator"
const val TAG_NO_RESULTS = "no_results"
const val TAG_IDLE_MESSAGE = "idle_message"
const val TAG_ERROR_MESSAGE = "error_message"
const val TAG_PROFILE_LIST = "profile_list"
const val TAG_PROFILE_ITEM = "profile_item"
const val TAG_PROFILE_IMAGE = "profile_image"
const val TAG_PROFILE_USERNAME = "profile_username"
const val TAG_PROFILE_STATS = "profile_stats"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchProfileScreen(navigationActions: NavigationActions, viewModel: ProfileViewModel) {
  var searchQuery by remember { mutableStateOf("") }
  val searchState by viewModel.searchState.collectAsState()

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
                                    MaterialTheme.colorScheme.primary))),
            title = {
              Text(
                  "Search Users",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onPrimary)
            },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("search_back_button")) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary)
                  }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent))
      }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
          TextField(
              value = searchQuery,
              onValueChange = {
                searchQuery = it
                viewModel.searchProfiles(it.lowercase())
              },
              modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag(TAG_SEARCH_FIELD),
              placeholder = { Text("Search profiles...") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
              singleLine = true)

          when (val state = searchState) {
            is SearchProfileState.Loading -> {
              Box(
                  modifier = Modifier.fillMaxSize().testTag(TAG_LOADING_INDICATOR),
                  contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                  }
            }
            is SearchProfileState.Success -> {
              if (state.profiles.isEmpty() && searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().testTag(TAG_NO_RESULTS),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = "No profiles found",
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
              } else {
                LazyColumn(
                    modifier = Modifier.testTag(TAG_PROFILE_LIST),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      items(items = state.profiles, key = { it.id }) { profile ->
                        ProfileSearchItem(
                            profile = profile,
                            onClick = { navigationActions.navigateToUserProfile(profile.id) })
                      }
                    }
              }
            }
            is SearchProfileState.Error -> {
              Box(
                  modifier = Modifier.fillMaxSize().testTag(TAG_ERROR_MESSAGE),
                  contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                  }
            }
            SearchProfileState.Idle -> {
              if (searchQuery.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().testTag(TAG_IDLE_MESSAGE),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = "Search for other users",
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
              }
            }
          }
        }
      }
}

@Composable
fun ProfileSearchItem(profile: Profile, onClick: () -> Unit) {
  ListItem(
      modifier =
          Modifier.clickable(onClick = onClick)
              .fillMaxWidth()
              .testTag("${TAG_PROFILE_ITEM}_${profile.id}"),
      headlineContent = {
        Text(
            text = profile.username,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("${TAG_PROFILE_USERNAME}_${profile.id}"))
      },
      supportingContent = {
        Row(
            modifier = Modifier.testTag("${TAG_PROFILE_STATS}_${profile.id}"),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              Text(
                  text = "${profile.followers} followers",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                  text = "${profile.following} following",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
      },
      leadingContent = {
        AsyncImage(
            model = profile.profileImageUrl,
            contentDescription = "Profile picture of ${profile.username}",
            modifier =
                Modifier.size(48.dp)
                    .clip(CircleShape)
                    .testTag("${TAG_PROFILE_IMAGE}_${profile.id}"),
            fallback = painterResource(R.drawable.eduverse_logo_alone))
      })
}
