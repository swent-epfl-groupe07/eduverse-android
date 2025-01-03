package com.github.se.eduverse.ui.folder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.showToast
import com.github.se.eduverse.ui.DeleteFileDialog
import com.github.se.eduverse.ui.EditFileMenu
import com.github.se.eduverse.ui.RenameFileDialog
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFolderScreen(
    navigationActions: NavigationActions,
    folderViewModel: FolderViewModel,
    fileViewModel: FileViewModel
) {
  val context = LocalContext.current
  var name by rememberSaveable { mutableStateOf("") }
  var files by rememberSaveable { mutableStateOf(emptyList<MyFile>()) }
  var deleteDialogOpen by remember { mutableStateOf(false) }
  var renameDialogOpen by remember { mutableStateOf(false) }
  var modifiedFile by remember { mutableStateOf<MyFile?>(null) }
  val validNewFile by fileViewModel.validNewFile.collectAsState()

  if (validNewFile) files += fileViewModel.getNewFile()!!

  val folder =
      Folder(
          ownerID = folderViewModel.auth.currentUser!!.uid,
          files = files.toMutableList(),
          name = name,
          id = folderViewModel.getNewUid(),
          archived = false)

  Scaffold(
      topBar = { TopNavigationBar(navigationActions, screenTitle = "Create Course") },
      bottomBar = {
        BottomNavigationMenu(
            { navigationActions.navigateTo(it) },
            LIST_TOP_LEVEL_DESTINATION,
            "") // No item is selected, as it is not one of the screens on the bottom bar
      }) { padding ->
        if (deleteDialogOpen) {
          DeleteFileDialog(
              onDismiss = {
                modifiedFile = null
                deleteDialogOpen = false
              },
              onConfirm = {
                fileViewModel.deleteFile(modifiedFile!!.fileId) {
                  files -= modifiedFile!!
                  modifiedFile = null
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
              onConfirm = { newName -> modifiedFile?.name = newName })
        }

        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 20.dp)) {
          item {
            // Give a name to the course
            Text(
                "Course Name",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(vertical = 15.dp).testTag("courseNameTitle"))
            OutlinedTextField(
                value = name,
                modifier =
                    Modifier.fillMaxWidth().padding(vertical = 10.dp).testTag("courseNameField"),
                onValueChange = { name = it },
                placeholder = { Text("Name of the course") })

            // Add file to the course
            Text(
                "Files",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(vertical = 15.dp).testTag("textFiles"))

            Column(modifier = Modifier.fillMaxHeight(0.65f)) {
              files.forEach {
                Button(
                    onClick = {
                      it.lastAccess = Calendar.getInstance()
                      it.numberAccess += 1
                      folderViewModel.updateFolder(folder)
                      fileViewModel.openFile(it.fileId, context)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).testTag("file"),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary)) {
                      Box(
                          modifier = Modifier.fillMaxWidth(),
                          contentAlignment = Alignment.CenterStart) {
                            Text(it.name, modifier = Modifier.fillMaxWidth())
                            EditFileMenu(
                                modifier = Modifier.align(Alignment.TopEnd),
                                onDelete = {
                                  modifiedFile = it
                                  deleteDialogOpen = true
                                },
                                onDownload = { fileViewModel.downloadFile(it.id, context) },
                                onRename = {
                                  modifiedFile = it
                                  renameDialogOpen = true
                                })
                          }
                    }
              }
            }

            Button(
                onClick = { navigationActions.navigateTo(Screen.CREATE_FILE) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("addFile"),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary)) {
                  Text("Add file")
                }

            // Create the folder
            Button(
                onClick = {
                  folderViewModel.addFolder(folder)
                  files = emptyList()
                  name = ""
                  navigationActions.goBack()
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("folderSave"),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary)) {
                  Text("Save")
                }

            // Cancel the folder creation
            Button(
                onClick = {
                  // Because we have created the folder in db when calling getNewFolderUid :
                  folderViewModel.deleteFolders(listOf(folder)) {
                    context.showToast("Couldn't properly delete created folder")
                  }

                  // Because files and name are saveable :
                  files = emptyList()
                  name = ""

                  navigationActions.goBack()
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("folderCancel"),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary)) {
                  Text("Cancel")
                }
          }
        }
      }
}
