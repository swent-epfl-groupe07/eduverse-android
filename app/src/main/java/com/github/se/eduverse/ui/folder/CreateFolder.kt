package com.github.se.eduverse.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFolderScreen(
    navigationActions: NavigationActions,
    folderViewModel: FolderViewModel,
    fileViewModel: FileViewModel
) {
  val context = LocalContext.current
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
  var name by rememberSaveable { mutableStateOf("") }
  var files by rememberSaveable { mutableStateOf(emptyList<MyFile>()) }
  var dialogOpen by remember { mutableStateOf(false) }
  var suppressFile by remember { mutableStateOf<MyFile?>(null) }
  val validNewFile by fileViewModel.validNewFile.collectAsState()

  if (validNewFile) files += fileViewModel.getNewFile()!!

  val folder =
      Folder(
          ownerID = folderViewModel.auth.currentUser!!.uid,
          files = files.toMutableList(),
          name = name,
          id = folderViewModel.getNewUid())

  Scaffold(
      topBar = {
        MediumTopAppBar(
            modifier = Modifier.testTag("topAppBar"),
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            title = {
              Text(
                  text = "Create Course",
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("topBarText"))
            },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() }, modifier = Modifier.testTag("goBack")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back Arrow")
                  }
            },
            scrollBehavior = scrollBehavior)
      },
      bottomBar = {
        BottomNavigationMenu(
            { navigationActions.navigateTo(it) },
            LIST_TOP_LEVEL_DESTINATION,
            "") // No item is selected, as it is not one of the screens on the bottom bar
      }) { padding ->
        if (dialogOpen) {
          Dialog(
              onDismissRequest = {
                suppressFile = null
                dialogOpen = false
              }) {
                Column(
                    modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0F7FA))
                            .padding(16.dp)
                            .testTag("confirm")) {
                      Text("Are you sure you want to delete this file ?")
                      Row(
                          horizontalArrangement = Arrangement.SpaceBetween,
                          modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                  fileViewModel.deleteFile(suppressFile!!.fileId) {
                                    files -= suppressFile!!
                                    suppressFile = null
                                  }
                                  dialogOpen = false
                                },
                                modifier = Modifier.testTag("yes"),
                                colors =
                                    ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                                  Text("Yes")
                                }
                            Button(
                                onClick = {
                                  suppressFile = null
                                  dialogOpen = false
                                },
                                modifier = Modifier.testTag("no"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                  Text("No")
                                }
                          }
                    }
              }
        }
        Column(modifier = Modifier.padding(padding).padding(horizontal = 20.dp)) {
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
                  onClick = { fileViewModel.openFile(it.fileId, context) },
                  modifier = Modifier.fillMaxWidth().testTag("file")) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart) {
                          Text(it.name, modifier = Modifier.fillMaxWidth())
                          IconButton(
                              onClick = {
                                suppressFile = it
                                dialogOpen = true
                              },
                              modifier = Modifier.align(Alignment.TopEnd).testTag("delete_icon")) {
                                Icon(Icons.Default.Close, contentDescription = "Delete File")
                              }
                        }
                  }
            }
          }

          Button(
              onClick = { navigationActions.navigateTo(Screen.CREATE_FILE) },
              modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("addFile")) {
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
              modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("folderSave")) {
                Text("Save")
              }

          // Cancel the folder creation
          Button(
              onClick = {
                // Because we have created the folder in db when calling getNewFolderUid :
                folderViewModel.deleteFolder(folder)

                // Because files and name are saveable :
                files = emptyList()
                name = ""

                navigationActions.goBack()
              },
              modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("folderCancel")) {
                Text("Cancel")
              }
        }
      }
}
