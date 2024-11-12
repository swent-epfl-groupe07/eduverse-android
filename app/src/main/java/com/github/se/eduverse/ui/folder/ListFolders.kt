package com.github.se.eduverse.ui.folder

//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListFoldersScreen(navigationActions: NavigationActions, folderViewModel: FolderViewModel) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
  folderViewModel.getUserFolders()
  val folders: List<Folder> by folderViewModel.folders.collectAsState()

  Scaffold(
      topBar = {
          CenterAlignedTopAppBar(
              modifier = Modifier.testTag("topAppBar"),
              title = {
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = "My Courses", modifier = Modifier.testTag("topBarText"))
                      IconButton(onClick = {}, modifier = Modifier.testTag("archive")) {
                          Icon(imageVector = Icons.Default.Archive, contentDescription = "Archive")
                      }
                  }
              },
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
        BottomNavigationMenu(
            { navigationActions.navigateTo(it) },
            LIST_TOP_LEVEL_DESTINATION,
            Route
                .LIST_FOLDERS) // No item is selected, as it is not one of the screens on the bottom
        // bar
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = { navigationActions.navigateTo(Screen.CREATE_FOLDER) },
            modifier = Modifier.testTag("createFolder")) {
              Icon(Icons.Default.Add, contentDescription = "Create Folder")
            }
      }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
          folders.forEach {
            Card(
                modifier =
                    Modifier.padding(8.dp)
                        .fillMaxWidth()
                        .clickable {
                          folderViewModel.selectFolder(it)
                          navigationActions.navigateTo(Screen.FOLDER)
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
