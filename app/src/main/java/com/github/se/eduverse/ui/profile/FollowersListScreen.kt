package com.github.se.eduverse.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.github.se.eduverse.R
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.viewmodel.FollowActionState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun FollowListScreen(
    navigationActions: NavigationActions,
    viewModel: ProfileViewModel,
    userId: String,
    isFollowersList: Boolean
) {
  val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
  val followActionState by viewModel.followActionState.collectAsState()
  var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }

  // Initial load
  LaunchedEffect(userId, isFollowersList) {
    loadProfiles(viewModel, userId, isFollowersList) { newProfiles ->
      profiles = newProfiles
      isLoading = false
    }
  }

  // React to follow action state changes
  LaunchedEffect(followActionState) {
    when (followActionState) {
      is FollowActionState.Success -> {
        // Reload the list after any successful follow/unfollow action
        isLoading = true
        loadProfiles(viewModel, userId, isFollowersList) { newProfiles ->
          profiles = newProfiles
          isLoading = false
        }
      }
      else -> {
        /* Handle other states if needed */
      }
    }
  }

  Scaffold(
      topBar = {
        TopNavigationBar(
            screenTitle = if (isFollowersList) "Followers" else "Following",
            navigationActions = navigationActions)
      }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.testTag("loading_indicator").align(Alignment.Center))
          } else if (profiles.isEmpty()) {
            Text(
                text = if (isFollowersList) "No followers yet" else "Not following anyone",
                modifier = Modifier.testTag("empty_message").align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
            LazyColumn(
                modifier = Modifier.testTag("follow_list").padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(items = profiles, key = { it.id }) { profile ->
                    FollowListItem(
                        profile = profile,
                        currentUserId = currentUserId,
                        isFollowersList = isFollowersList,
                        onFollowClick = { profileId ->
                          viewModel.toggleFollow(currentUserId ?: "", profileId)
                        },
                        onProfileClick = { navigationActions.navigateToUserProfile(profile.id) })
                  }
                }
          }
        }
      }
}

// Extracted helper function for loading profiles
private suspend fun loadProfiles(
    viewModel: ProfileViewModel,
    userId: String,
    isFollowersList: Boolean,
    onResult: (List<Profile>) -> Unit
) {
  val newProfiles =
      if (isFollowersList) {
        viewModel.getFollowers(userId)
      } else {
        viewModel.getFollowing(userId).map { it.copy(isFollowedByCurrentUser = true) }
      }
  onResult(newProfiles)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowListItem(
    profile: Profile,
    currentUserId: String?,
    isFollowersList: Boolean,
    onFollowClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
  ListItem(
      modifier =
          Modifier.clickable(onClick = onProfileClick).testTag("follow_list_item_${profile.id}"),
      headlineContent = {
        Text(text = profile.username, style = MaterialTheme.typography.titleMedium)
      },
      leadingContent = {
        AsyncImage(
            model = profile.profileImageUrl,
            contentDescription = "Profile picture of ${profile.username}",
            modifier = Modifier.size(48.dp).clip(CircleShape),
            fallback = painterResource(R.drawable.eduverse_logo_alone))
      },
      trailingContent = {
        if (currentUserId != null && currentUserId != profile.id) {
          if (isFollowersList && !profile.isFollowedByCurrentUser) {
            Button(
                onClick = { onFollowClick(profile.id) },
                modifier = Modifier.testTag("follow_button_${profile.id}")) {
                  Text("Follow")
                }
          } else if (!isFollowersList) {
            Button(
                onClick = { onFollowClick(profile.id) },
                modifier = Modifier.testTag("follow_button_${profile.id}")) {
                  Text("Following")
                }
          }
        }
      })
}
