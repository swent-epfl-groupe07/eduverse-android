package com.github.se.eduverse.ui.archive

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.isNetworkAvailable
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.ui.DeleteFoldersDialog
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.viewmodel.FolderViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    navigationActions: NavigationActions,
    folderViewModel: FolderViewModel,
    context: Context = LocalContext.current
) {
  val folders: List<Folder> by folderViewModel.folders.collectAsState()
  var isSelectMode by remember { mutableStateOf(false) }
  var selected by remember { mutableStateOf(emptyList<Int>()) }
  var deleteDialogOpen by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { folderViewModel.getArchivedUserFolders() }

  Scaffold(
      topBar = {
        TopNavigationBar("Archived Courses", navigationActions) {
          if (isSelectMode) {
            IconButton(
                onClick = {
                  if (context.isNetworkAvailable()) {
                    deleteDialogOpen = true
                  } else {
                    folderViewModel.showOfflineMessage(context)
                  }
                },
                modifier = Modifier.testTag("delete")) {
                  Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
          }
        }
      },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      }) { padding ->
        if (deleteDialogOpen) {
          DeleteFoldersDialog(
              number = selected.size,
              onDismiss = { deleteDialogOpen = false },
              onConfirm = {
                deleteDialogOpen = false
                isSelectMode = false
                selected.forEach { folderViewModel.deleteFolder(folders[it]) }
                selected = emptyList()
              })
        }

        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
          if (isSelectMode) {
            item {
              Row(
                  modifier =
                      Modifier.clickable {
                            isSelectMode = false
                            selected = emptyList()
                          }
                          .testTag("cancel"),
                  verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cancel, contentDescription = "Cancel Selection")
                    Text("Cancel selection", style = MaterialTheme.typography.titleLarge)
                  }
            }
          }

          items(folders.size) {
            Card(
                modifier =
                    Modifier.padding(8.dp)
                        .fillMaxWidth()
                        .combinedClickable(
                            onLongClick = {
                              if (!isSelectMode) {
                                isSelectMode = true
                                if (!selected.contains(it)) {
                                  selected = selected + it
                                }
                              }
                            },
                            onClick = {
                              if (context.isNetworkAvailable()) {
                                folderViewModel.unarchiveFolder(folders[it])
                                navigationActions.navigateTo(Route.LIST_FOLDERS)
                              } else {
                                folderViewModel.showOfflineMessage(context)
                              }
                            })
                        .testTag("folderCard${folders[it].id}"),
                elevation = 4.dp) {
                  Row(
                      modifier = Modifier.padding(16.dp).fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = folders[it].name, style = MaterialTheme.typography.headlineSmall)
                        if (isSelectMode) {
                          if (selected.contains(it)) {
                            IconButton(
                                modifier = Modifier.size(25.dp).testTag("checked"),
                                onClick = { selected = selected - it }) {
                                  Icon(Icons.Default.CheckBox, contentDescription = "Checked")
                                }
                          } else {
                            IconButton(
                                modifier = Modifier.size(25.dp).testTag("unchecked"),
                                onClick = { selected = selected + it }) {
                                  Icon(
                                      Icons.Default.CheckBoxOutlineBlank,
                                      contentDescription = "Not Checked")
                                }
                          }
                        }
                      }
                }
          }
        }
      }
}
