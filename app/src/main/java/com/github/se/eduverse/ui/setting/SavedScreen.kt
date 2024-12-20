package com.github.se.eduverse.ui.saved

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.R
import com.github.se.eduverse.ui.gallery.PublicationItem
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.profile.PublicationDetailView
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    navigationActions: NavigationActions,
    viewModel: ProfileViewModel,
    userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
) {
  val favoritePublications by viewModel.favoritePublications.collectAsState()
  var selectedPublication by remember {
    mutableStateOf<com.github.se.eduverse.model.Publication?>(null)
  }

  LaunchedEffect(userId) { viewModel.loadFavoritePublications(userId) }

  Scaffold(
      topBar = {
        TopNavigationBar(
            navigationActions, screenTitle = stringResource(R.string.saved_posts_title))
      }) { paddingValues ->
        if (favoritePublications.isEmpty()) {
          Box(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.saved_posts_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("empty_saved_text"))
              }
        } else {
          LazyVerticalGrid(
              columns = GridCells.Fixed(3),
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              contentPadding = PaddingValues(1.dp),
              horizontalArrangement = Arrangement.spacedBy(1.dp),
              verticalArrangement = Arrangement.spacedBy(1.dp)) {
                items(favoritePublications) { publication ->
                  PublicationItem(
                      publication.mediaType,
                      publication.thumbnailUrl,
                      onClick = { selectedPublication = publication },
                      modifier = Modifier.testTag("saved_publication_${publication.id}"))
                }
              }

          // Show detail dialog when a publication is selected
          selectedPublication?.let { publication ->
            PublicationDetailView(
                publication = publication,
                profileViewModel = viewModel,
                currentUserId = userId,
                onDismiss = { selectedPublication = null },
                onShowComments = {})
          }
        }
      }
}
