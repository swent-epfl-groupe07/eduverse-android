package com.github.se.eduverse.ui.folder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.se.eduverse.model.folder.Folder
import com.github.se.eduverse.model.folder.FolderRepository
import com.github.se.eduverse.model.folder.FolderViewModel
import com.github.se.eduverse.model.folder.MyFile
import com.github.se.eduverse.model.folder.TimeTable
import com.github.se.eduverse.ui.navigation.NavigationActions
import java.util.Calendar
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class DisplayFolderTest {
  private lateinit var folderRepository: FolderRepository
  private lateinit var navigationActions: NavigationActions
  private lateinit var folderViewModel: FolderViewModel

  val file1 = MyFile("name 1", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file2 =
      MyFile("name 2", java.util.Calendar.getInstance(), java.util.Calendar.getInstance(), 0)
  val file3 =
      MyFile("name 3", java.util.Calendar.getInstance(), java.util.Calendar.getInstance(), 0)

  val folder =
      Folder(
          MutableList(3) {
            when (it) {
              1 -> file1
              2 -> file2
              else -> file3
            }
          },
          "folder",
          "1",
          TimeTable())

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    folderRepository = mock(FolderRepository::class.java)
    navigationActions = mock(NavigationActions::class.java)

    `when`(folderRepository.getFolders(any(), any())).thenReturn(listOf(folder))
    folderViewModel = FolderViewModel(folderRepository)
    folderViewModel.activeFolder.value = folder

    composeTestRule.setContent { FolderScreen(navigationActions, folderViewModel) }
  }

  @Test
  fun displayComponents() {
    composeTestRule.onNodeWithTag("topAppBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("createFile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timeTable").assertIsDisplayed()
    composeTestRule.onNodeWithTag("textFiles").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortingButton").assertIsDisplayed()
  }

  @Test
  fun dropdownMenuWorks() {
    composeTestRule.onNodeWithTag("sortMode1").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("sortingButton").assertIsDisplayed()

    composeTestRule.onNodeWithTag("sortingButton").performClick()
    composeTestRule.onNodeWithTag("sortMode1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode3").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode4").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode5").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode6").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode7").assertIsDisplayed()

    composeTestRule.onNodeWithTag("sortMode1").performClick()
    composeTestRule.onNodeWithTag("sortMode1").assertIsNotDisplayed()
  }
}
