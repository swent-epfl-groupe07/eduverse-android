package com.github.se.eduverse.ui.folder

//noinspection UsingMaterialAndMaterial3Libraries
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.github.se.eduverse.isNetworkAvailable
import com.github.se.eduverse.model.FilterTypes
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.ui.DeleteFileDialog
import com.github.se.eduverse.ui.EditFileMenu
import com.github.se.eduverse.ui.RenameFileDialog
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.theme.transparentButtonColor
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import java.util.Calendar

@Composable
fun FolderScreen(
    navigationActions: NavigationActions,
    folderViewModel: FolderViewModel,
    fileViewModel: FileViewModel,
    context: Context = LocalContext.current
) {
  val activeFolder by folderViewModel.activeFolder.collectAsState()

  var sorting by remember { mutableStateOf(false) }
  var deleteDialogOpen by remember { mutableStateOf(false) }
  var renameDialogOpen by remember { mutableStateOf(false) }
  var modifiedFile by remember { mutableStateOf<MyFile?>(null) }
  val validNewFile by fileViewModel.validNewFile.collectAsState()

  var trigger by remember { mutableIntStateOf(0) }

  if (validNewFile) activeFolder!!.files.add(fileViewModel.getNewFile()!!)

  folderViewModel.updateFolder(activeFolder!!)

  Scaffold(
      modifier = Modifier.testTag("scaffold"),
      topBar = {
        TopNavigationBar(
            navigationActions = navigationActions,
            screenTitle = activeFolder!!.name,
            actions = {
              IconButton(
                  onClick = {
                    if (context.isNetworkAvailable()) {
                      folderViewModel.archiveFolder(activeFolder!!)
                      navigationActions.goBack()
                    } else {
                      folderViewModel.showOfflineMessage(context)
                    }
                  },
                  modifier = Modifier.testTag("archive")) {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = "Archive")
                  }
            })
      },
      bottomBar = {
        BottomNavigationMenu(
            { navigationActions.navigateTo(it) },
            LIST_TOP_LEVEL_DESTINATION,
            "") // No item is selected, as it is not one of the screens on the bottom bar
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = {
              if (context.isNetworkAvailable()) {
                navigationActions.navigateTo(Screen.CREATE_FILE)
              } else {
                folderViewModel.showOfflineMessage(context)
              }
            },
            modifier = Modifier.testTag("createFile"),
            backgroundColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)) {
              Icon(Icons.Default.Add, contentDescription = "Create File")
            }
      }) { padding ->
        if (deleteDialogOpen) {
          DeleteFileDialog(
              onDismiss = {
                modifiedFile = null
                deleteDialogOpen = false
              },
              onConfirm = {
                fileViewModel.deleteFile(modifiedFile!!.fileId) {
                  folderViewModel.updateFolder(
                      activeFolder!!.apply { files.remove(modifiedFile!!) })
                  modifiedFile = null

                  // As the value of activeFolder is not modified but its content is,
                  // we need to force recomposition. This is the point of trigger.
                  trigger += 1
                }
                deleteDialogOpen = false
              })
        }

        if (renameDialogOpen) {
          RenameFileDialog(
              onDismiss = {
                modifiedFile = null
                renameDialogOpen = false
              },
              onConfirm = { newName ->
                modifiedFile?.name = newName
                folderViewModel.updateFolder(activeFolder!!)
                modifiedFile = null
                renameDialogOpen = false
              })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("column"),
        ) {
          item {
            // The text saying Files and the button to sort
            Row(
                modifier = Modifier.padding(20.dp, 15.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      "Files",
                      fontWeight = FontWeight.Bold,
                      fontSize = 24.sp,
                      modifier = Modifier.testTag("textFiles"))
                  Box {
                    Button(
                        onClick = { sorting = true },
                        modifier = Modifier.testTag("sortingButton"),
                        colors = transparentButtonColor(MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground)) {
                          Icon(Icons.AutoMirrored.Filled.List, "Sort files")
                        }
                    DropdownMenu(
                        expanded = sorting,
                        modifier = Modifier.width(IntrinsicSize.Min),
                        onDismissRequest = { sorting = false },
                        properties = PopupProperties(focusable = false)) {
                          DropdownMenuItem(
                              text = { Text("Alphabetic", modifier = Modifier.fillMaxWidth()) },
                              modifier = Modifier.fillMaxWidth().testTag("sortMode1"),
                              onClick = {
                                folderViewModel.sortBy(FilterTypes.NAME)
                                sorting = false
                              })
                          DropdownMenuItem(
                              text = { Text("Newest", modifier = Modifier.fillMaxWidth()) },
                              modifier = Modifier.fillMaxWidth().testTag("sortMode2"),
                              onClick = {
                                folderViewModel.sortBy(FilterTypes.CREATION_UP)
                                sorting = false
                              })
                          DropdownMenuItem(
                              text = { Text("Oldest", modifier = Modifier.fillMaxWidth()) },
                              modifier = Modifier.fillMaxWidth().testTag("sortMode3"),
                              onClick = {
                                folderViewModel.sortBy(FilterTypes.CREATION_DOWN)
                                sorting = false
                              })
                          DropdownMenuItem(
                              text = {
                                Text("Recently accessed", modifier = Modifier.fillMaxWidth())
                              },
                              modifier = Modifier.fillMaxWidth().testTag("sortMode4"),
                              onClick = {
                                folderViewModel.sortBy(FilterTypes.ACCESS_RECENT)
                                sorting = false
                              })
                          DropdownMenuItem(
                              text = { Text("Oldest access", modifier = Modifier.fillMaxWidth()) },
                              modifier = Modifier.fillMaxWidth().testTag("sortMode5"),
                              onClick = {
                                folderViewModel.sortBy(FilterTypes.ACCESS_OLD)
                                sorting = false
                              })
                          DropdownMenuItem(
                              text = { Text("Most accessed", modifier = Modifier.fillMaxWidth()) },
                              modifier = Modifier.fillMaxWidth().testTag("sortMode6"),
                              onClick = {
                                folderViewModel.sortBy(FilterTypes.ACCESS_MOST)
                                sorting = false
                              })
                          DropdownMenuItem(
                              text = { Text("Least accessed", modifier = Modifier.fillMaxWidth()) },
                              modifier = Modifier.fillMaxWidth().testTag("sortMode7"),
                              onClick = {
                                folderViewModel.sortBy(FilterTypes.ACCESS_LEAST)
                                sorting = false
                              })
                        }
                  }
                }

            // The files
            activeFolder?.files?.forEach {
              @Suppress("UNUSED_EXPRESSION")
              trigger // Force recomposition by adding dependency on a state flow

              Button(
                  onClick = {
                    it.lastAccess = Calendar.getInstance()
                    it.numberAccess += 1
                    folderViewModel.updateFolder(activeFolder!!)
                    fileViewModel.openFile(it.fileId, context)
                  },
                  modifier = Modifier.fillMaxWidth().padding(20.dp, 3.dp).testTag(it.name),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary,
                          contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart) {
                          Text(it.name)
                          EditFileMenu(
                              modifier = Modifier.align(Alignment.CenterEnd),
                              onRename = {
                                modifiedFile = it
                                renameDialogOpen = true
                              },
                              onDelete = {
                                modifiedFile = it
                                deleteDialogOpen = true
                              })
                        }
                  }
            }
          }
        }
      }
}
