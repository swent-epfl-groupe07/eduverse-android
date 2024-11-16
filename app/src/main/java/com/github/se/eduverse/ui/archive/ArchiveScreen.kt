package com.github.se.eduverse.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.viewmodel.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(navigationActions: NavigationActions, folderViewModel: FolderViewModel) {
  val folders: List<Folder> by folderViewModel.folders.collectAsState()

  LaunchedEffect(Unit) { folderViewModel.getArchivedUserFolders() }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            modifier = Modifier.testTag("topAppBar"),
            title = { Text(text = "Archived Courses", modifier = Modifier.testTag("topBarText")) },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() }, modifier = Modifier.testTag("goBack")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back Arrow")
                  }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent))
      },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
          folders.forEach {
            Card(
                modifier =
                    Modifier.padding(8.dp)
                        .fillMaxWidth()
                        .clickable {
                          folderViewModel.unarchiveFolder(it)
                          navigationActions.navigateTo(Route.LIST_FOLDERS)
                        }
                        .testTag("folderCard${it.id}"),
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