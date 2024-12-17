package com.github.se.eduverse.ui.saved

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.profile.PublicationDetailDialog
import com.github.se.eduverse.ui.profile.PublicationItem
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
        TopAppBar(
            title = { Text("Saved Posts") },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                  }
            },
            colors =
                TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary))
      }) { paddingValues ->
        if (favoritePublications.isEmpty()) {
          Box(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              contentAlignment = Alignment.Center) {
                Text(
                    text = "No saved posts yet",
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
                      publication = publication,
                      onClick = { selectedPublication = publication },
                      modifier = Modifier.testTag("saved_publication_${publication.id}"))
                }
              }

          // Show detail dialog when a publication is selected
          selectedPublication?.let { publication ->
            PublicationDetailDialog(
                publication = publication,
                profileViewModel = viewModel,
                currentUserId = userId,
                onDismiss = { selectedPublication = null })
          }
        }
      }
}
