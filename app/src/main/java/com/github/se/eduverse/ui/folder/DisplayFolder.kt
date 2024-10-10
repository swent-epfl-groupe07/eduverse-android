package com.github.se.project.ui

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.github.se.eduverse.ui.folder.DisplayTimeTable
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.project.model.folder.FolderViewModel
import com.github.se.project.model.folder.TimeTable

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FolderScreen(navigationActions: NavigationActions, folderViewModel: FolderViewModel) {
  if (folderViewModel.activeFolder == null) throw IllegalArgumentException(
    "There is no active folder, select one before going to FolderScreen")
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

  Scaffold(
    topBar = {
      MediumTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
          Text(
            text = folderViewModel.activeFolder!!.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
          IconButton(
            onClick = { navigationActions.goBack() }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back Arrow")
          }
        },
        scrollBehavior = scrollBehavior
      )
    },
    bottomBar = {
      BottomNavigationMenu(
        { navigationActions.navigateTo(it) },
        LIST_TOP_LEVEL_DESTINATION,
        "" //No item is selected, as it is not one of the screens on the bottom bar
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { navigationActions.navigateTo(Screen.CREATE_FOLDER) }) {
        Icon(Icons.Default.Add, contentDescription = "Create Folder")
      }
    }
  ) {
    DisplayTimeTable(folderViewModel.activeFolder!!.timeTable)
  }
}
