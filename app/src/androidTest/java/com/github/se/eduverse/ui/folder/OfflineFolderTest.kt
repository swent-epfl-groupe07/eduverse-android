package com.github.se.eduverse.ui.folder

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class OfflineFolderTest {
  private lateinit var navigationActions: NavigationActions
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var fileViewModel: FileViewModel
  private lateinit var context: Context

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    folderViewModel = mock(FolderViewModel::class.java)
    fileViewModel = mock(FileViewModel::class.java)
    context = mock(Context::class.java)

    val connectivityManager = mock(ConnectivityManager::class.java)

    `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
    `when`(connectivityManager.activeNetwork).thenReturn(null)

    `when`(folderViewModel.activeFolder)
        .thenReturn(
            MutableStateFlow(
                Folder("", emptyList<MyFile>().toMutableList(), "folder", "1", archived = false)))
    `when`(folderViewModel.updateFolder(any())).then {}
    `when`(folderViewModel.folders)
        .thenReturn(MutableStateFlow(emptyList<Folder>().toMutableList()))
    `when`(folderViewModel.getUserFolders()).then {}

    `when`(fileViewModel.validNewFile).thenReturn(MutableStateFlow(false))
  }

  @Test
  fun folderScreenCantArchiveWhenOfflineTest() {
    composeTestRule.setContent {
      FolderScreen(navigationActions, folderViewModel, fileViewModel, context)
    }

    composeTestRule.onNodeWithTag("archive").performClick()
    verify(0) { navigationActions.goBack() }
  }

  @Test
  fun folderScreenCantCreateFileWhenOfflineTest() {
    composeTestRule.setContent {
      FolderScreen(navigationActions, folderViewModel, fileViewModel, context)
    }

    composeTestRule.onNodeWithTag("createFile").performClick()
    verify(0) { navigationActions.navigateTo(anyString()) }
  }

  @Test
  fun listFolderCantCreateFolderWhenOfflineTest() {
    composeTestRule.setContent { ListFoldersScreen(navigationActions, folderViewModel, context) }

    composeTestRule.onNodeWithTag("createFolder").performClick()
    verify(0) { navigationActions.navigateTo(anyString()) }
  }
}
