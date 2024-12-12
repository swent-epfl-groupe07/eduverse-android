package com.github.se.eduverse.ui.archive

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.isNetworkAvailable
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.viewmodel.FolderViewModel

@Composable
fun ArchiveScreen(
    navigationActions: NavigationActions,
    folderViewModel: FolderViewModel,
    context: Context = LocalContext.current
) {
  val folders: List<Folder> by folderViewModel.folders.collectAsState()

  LaunchedEffect(Unit) { folderViewModel.getArchivedUserFolders() }

  Scaffold(
      topBar = { TopNavigationBar(navigationActions) },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
          item {
            folders.forEach {
              Card(
                  modifier =
                      Modifier.padding(8.dp)
                          .fillMaxWidth()
                          .clickable {
                            if (context.isNetworkAvailable()) {
                              folderViewModel.unarchiveFolder(it)
                              navigationActions.navigateTo(Route.LIST_FOLDERS)
                            } else {
                              folderViewModel.showOfflineMessage(context)
                            }
                          }
                          .testTag("folderCard${it.id}"),
                  backgroundColor = MaterialTheme.colorScheme.surface,
                  elevation = 4.dp) {
                    Text(
                        text = it.name,
                        modifier = Modifier.padding(16.dp),
                        style = androidx.compose.material.MaterialTheme.typography.h6)
                  }
            }
          }
        }
      }
}
