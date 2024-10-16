package com.github.se.eduverse.ui.folder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.se.eduverse.model.folder.Folder
import com.github.se.eduverse.model.folder.FolderRepository
import com.github.se.eduverse.model.folder.FolderViewModel
import com.github.se.eduverse.model.folder.MyFile
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class DisplayFolderTest {
  private lateinit var folderRepository: FolderRepository
  private lateinit var navigationActions: NavigationActions
  private lateinit var folderViewModel: FolderViewModel

  val file1 = MyFile("", "name 1", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file2 = MyFile("", "name 2", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file3 = MyFile("", "name 3", Calendar.getInstance(), Calendar.getInstance(), 0)

  val folder =
      Folder(
          "",
          MutableList(3) {
            when (it) {
              1 -> file1
              2 -> file2
              else -> file3
            }
          },
          "folder",
          "1")

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    folderRepository = mock(FolderRepository::class.java)
    navigationActions = mock(NavigationActions::class.java)

    doAnswer {
          val callback = it.getArgument<(List<Folder>) -> Unit>(1)
          callback(listOf(folder))
          null
        }
        .whenever(folderRepository)
        .getFolders(any(), any(), any())

    val currentUser = mock(FirebaseUser::class.java)
    `when`(currentUser.uid).thenReturn("uid")
    folderViewModel = FolderViewModel(folderRepository, currentUser)
    folderViewModel.selectFolder(folder)

    composeTestRule.setContent { FolderScreen(navigationActions, folderViewModel) }
  }

  @Test
  fun displayComponents() {
    composeTestRule.onNodeWithTag("goBack").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topAppBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("createFile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("textFiles").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortingButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("scaffold").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topBarText").assertIsDisplayed()
    composeTestRule.onNodeWithTag("column").assertIsDisplayed()
    composeTestRule.onNodeWithTag(file1.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(file2.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(file3.name).assertIsDisplayed()
  }

  @Test
  fun goBackWorks() {
    var test = false
    `when`(navigationActions.goBack()).then {
      test = true
      null
    }

    composeTestRule.onNodeWithTag("goBack").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBack").performClick()
    assert(test)
  }

  @Test
  fun bottomBarWorks() {
    var test: Boolean
    `when`(navigationActions.navigateTo(any<TopLevelDestination>())).then {
      test = true
      null
    }
    LIST_TOP_LEVEL_DESTINATION.forEach {
      test = false

      composeTestRule.onNodeWithText(it.textId).assertIsDisplayed()
      composeTestRule.onNodeWithText(it.textId).performClick()

      assert(test)
    }
  }

  @Test
  fun addFileButtonWorks() {
    var test = false
    `when`(navigationActions.navigateTo(anyString())).then {
      test = true
      null
    }
    composeTestRule.onNodeWithTag("createFile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("createFile").performClick()

    assert(test)
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

    composeTestRule.onNodeWithTag("sortingButton").performClick()
    composeTestRule.onNodeWithTag("sortMode2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode2").performClick()
    composeTestRule.onNodeWithTag("sortMode2").assertIsNotDisplayed()

    composeTestRule.onNodeWithTag("sortingButton").performClick()
    composeTestRule.onNodeWithTag("sortMode3").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode3").performClick()
    composeTestRule.onNodeWithTag("sortMode3").assertIsNotDisplayed()

    composeTestRule.onNodeWithTag("sortingButton").performClick()
    composeTestRule.onNodeWithTag("sortMode4").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode4").performClick()
    composeTestRule.onNodeWithTag("sortMode4").assertIsNotDisplayed()

    composeTestRule.onNodeWithTag("sortingButton").performClick()
    composeTestRule.onNodeWithTag("sortMode5").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode5").performClick()
    composeTestRule.onNodeWithTag("sortMode5").assertIsNotDisplayed()

    composeTestRule.onNodeWithTag("sortingButton").performClick()
    composeTestRule.onNodeWithTag("sortMode6").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode6").performClick()
    composeTestRule.onNodeWithTag("sortMode6").assertIsNotDisplayed()

    composeTestRule.onNodeWithTag("sortingButton").performClick()
    composeTestRule.onNodeWithTag("sortMode7").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortMode7").performClick()
    composeTestRule.onNodeWithTag("sortMode7").assertIsNotDisplayed()
  }

  @Test
  fun clickOnFileHaveExpectedBehavior() {
    composeTestRule.onNodeWithTag("name 1").performClick()
    composeTestRule.onNodeWithTag("name 2").performClick()
    composeTestRule.onNodeWithTag("name 3").performClick()

    // click do nothing for now
  }
}
