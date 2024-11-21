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
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.FollowActionState
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

    // Load profiles initially and after each successful follow/unfollow action
    LaunchedEffect(userId, isFollowersList, followActionState) {
        if (followActionState is FollowActionState.Success || isLoading) {
            isLoading = true
            profiles = if (isFollowersList) {
                viewModel.getFollowers(userId)
            } else {
                viewModel.getFollowing(userId)
            }
            isLoading = false
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .testTag("loading_indicator")
                        .align(Alignment.Center)
                )
            } else if (profiles.isEmpty()) {
                Text(
                    text = if (isFollowersList) "No followers yet" else "Not following anyone",
                    modifier = Modifier
                        .testTag("empty_message")
                        .align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .testTag("follow_list")
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = profiles,
                        key = { it.id }
                    ) { profile ->
                        var isFollowed by remember { mutableStateOf(profile.isFollowedByCurrentUser) }

                        LaunchedEffect(followActionState) {
                            if (followActionState is FollowActionState.Success) {
                                // Update local state based on follow action
                                if (currentUserId != null && profile.id == (followActionState as? FollowActionState.Success)?.targetUserId) {
                                    isFollowed = !isFollowed
                                }
                            }
                        }

                        FollowListItem(
                            profile = profile.copy(isFollowedByCurrentUser = isFollowed),
                            currentUserId = currentUserId,
                            onFollowClick = { profileId ->
                                viewModel.toggleFollow(currentUserId ?: "", profileId)
                            },
                            onProfileClick = { navigationActions.navigateToUserProfile(profile.id) }
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
    onFollowClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onProfileClick)
            .testTag("follow_list_item_${profile.id}"),
        headlineContent = {
            Text(
                text = profile.username,
                style = MaterialTheme.typography.titleMedium
            )
        },
        leadingContent = {
            AsyncImage(
                model = profile.profileImageUrl,
                contentDescription = "Profile picture of ${profile.username}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                fallback = painterResource(R.drawable.eduverse_logo_alone)
            )
        },
        trailingContent = {
            if (currentUserId != null && currentUserId != profile.id) {
                Button(
                    onClick = { onFollowClick(profile.id) },
                    modifier = Modifier.testTag("follow_button_${profile.id}")
                ) {
                    Text(if (profile.isFollowedByCurrentUser) "Following" else "Follow")
                }
            }
        }
    )
}