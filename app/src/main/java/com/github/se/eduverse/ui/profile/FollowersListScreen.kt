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

    // React to follow action state changes but don't reload the list
    LaunchedEffect(followActionState) {
        when (followActionState) {
            is FollowActionState.Success -> {
                // Update the specific profile's follow status instead of reloading the entire list
                val updatedProfiles = profiles.map { profile ->
                    if (profile.id == (followActionState as FollowActionState.Success).targetUserId) {
                        profile.copy(isFollowedByCurrentUser = (followActionState as FollowActionState.Success).isNowFollowing)
                    } else {
                        profile
                    }
                }
                profiles = updatedProfiles
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
                navigationActions = navigationActions
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag("loading_indicator").align(Alignment.Center)
                )
            } else if (profiles.isEmpty()) {
                Text(
                    text = if (isFollowersList) "No followers yet" else "Not following anyone",
                    modifier = Modifier.testTag("empty_message").align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.testTag("follow_list").padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = profiles, key = { it.id }) { profile ->
                        FollowListItem(
                            profile = profile,
                            currentUserId = currentUserId,
                            isFollowersList = isFollowersList,
                            onFollowClick = { profileId ->
                                viewModel.toggleFollow(currentUserId ?: "", profileId)
                            },
                            onProfileClick = { navigationActions.navigateToUserProfile(profile.id) },
                            followActionState = followActionState,
                            targetUserId = (followActionState as? FollowActionState.Success)?.targetUserId
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowListItem(
    profile: Profile,
    currentUserId: String?,
    isFollowersList: Boolean,
    onFollowClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    followActionState: FollowActionState,
    targetUserId: String?
) {
    val isLoading = followActionState is FollowActionState.Loading && targetUserId == profile.id

    ListItem(
        modifier = Modifier
            .clickable(onClick = onProfileClick)
            .testTag("follow_list_item_${profile.id}"),
        headlineContent = {
            Text(text = profile.username, style = MaterialTheme.typography.titleMedium)
        },
        leadingContent = {
            AsyncImage(
                model = profile.profileImageUrl,
                contentDescription = "Profile picture of ${profile.username}",
                modifier = Modifier.size(48.dp).clip(CircleShape),
                fallback = painterResource(R.drawable.eduverse_logo_alone)
            )
        },
        trailingContent = {
            if (currentUserId != null && currentUserId != profile.id) {
                val buttonEnabled = !isLoading && followActionState !is FollowActionState.Loading

                Button(
                    onClick = { onFollowClick(profile.id) },
                    modifier = Modifier.testTag("follow_button_${profile.id}"),
                    enabled = buttonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (profile.isFollowedByCurrentUser)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.primary,
                        disabledContainerColor = if (profile.isFollowedByCurrentUser)
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            when {
                                profile.isFollowedByCurrentUser -> "Following"
                                isFollowersList -> "Follow Back"
                                else -> "Follow"
                            }
                        )
                    }
                }
            }
        }
    )
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


