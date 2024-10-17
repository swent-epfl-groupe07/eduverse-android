package com.github.se.eduverse.ui.folder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.model.folder.Folder
import com.github.se.eduverse.model.folder.MyFile
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFolderScreen(navigationActions: NavigationActions, folderViewModel: FolderViewModel,
                       fileViewModel: FileViewModel) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
  var name by rememberSaveable { mutableStateOf("") }
  var files by rememberSaveable { mutableStateOf(emptyList<MyFile>()) }

  if (fileViewModel.newFile != null) files += fileViewModel.newFile

  val folder =
      Folder(
          ownerID = "ownerId",//folderViewModel.auth.currentUser!!.uid,
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
              Button(onClick = {}, modifier = Modifier.fillMaxWidth().testTag("file")) {
                Text(it.name, modifier = Modifier.fillMaxWidth())
              }
            }
          }
          Button(
              onClick = {
                navigationActions.navigateTo(Screen.CREATE_FILE)
              },
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
