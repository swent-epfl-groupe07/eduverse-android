package com.github.se.eduverse.ui.folder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.github.se.eduverse.model.FilterTypes
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    navigationActions: NavigationActions,
    folderViewModel: FolderViewModel,
    fileViewModel: FileViewModel
) {
  val activeFolder by folderViewModel.activeFolder.collectAsState()

    val context = LocalContext.current
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
  var sorting by remember { mutableStateOf(false) }
  val validNewFile by fileViewModel.validNewFile.collectAsState()

  if (validNewFile) activeFolder!!.files.add(fileViewModel.getNewFile()!!)

  Scaffold(
      modifier = Modifier.testTag("scaffold"),
      topBar = {
        MediumTopAppBar(
            modifier = Modifier.testTag("topAppBar"),
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            title = { Text(text = activeFolder!!.name, modifier = Modifier.testTag("topBarText")) },
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
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = { navigationActions.navigateTo(Screen.CREATE_FILE) },
            modifier = Modifier.testTag("createFile")) {
              Icon(Icons.Default.Add, contentDescription = "Create File")
            }
      }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("column"),
        ) {
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
                      colors =
                          ButtonColors(
                              Color.Transparent, Color.Black, Color.Transparent, Color.Transparent),
                      border = BorderStroke(1.dp, Color.Black)) {
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
          activeFolder!!.files.forEach {
            Button(
                onClick = { fileViewModel.openFile(it.fileId, context) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag(it.name)) {
                  Text(it.name, modifier = Modifier.fillMaxWidth())
                }
          }
        }
      }
}
