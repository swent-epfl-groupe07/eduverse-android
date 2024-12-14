package com.github.se.eduverse.ui.folder

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ListFoldersTest {
  private lateinit var folderRepository: FolderRepository
  private lateinit var navigationActions: NavigationActions
  private lateinit var folderViewModel: FolderViewModel

  val file1 = MyFile("", "", "name 1", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file2 = MyFile("", "", "name 2", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file3 = MyFile("", "", "name 3", Calendar.getInstance(), Calendar.getInstance(), 0)

  val folder1 =
      Folder(
          "",
          MutableList(3) {
            when (it) {
              1 -> file1
              2 -> file2
              else -> file3
            }
          },
          "folder1",
          "1",
          archived = false)

  val folder2 = Folder("", emptyList<MyFile>().toMutableList(), "folder2", "2", archived = false)

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    folderRepository = mock(FolderRepository::class.java)
    navigationActions = mock(NavigationActions::class.java)

    doAnswer {
          val callback = it.getArgument<(List<Folder>) -> Unit>(2)
          callback(listOf(folder1, folder2))
          null
        }
        .whenever(folderRepository)
        .getFolders(any(), any(), any(), any())

    val auth = mock(FirebaseAuth::class.java)
    val currentUser = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(currentUser)
    `when`(currentUser.uid).thenReturn("uid")
    folderViewModel = FolderViewModel(folderRepository, auth)

    composeTestRule.setContent { ListFoldersScreen(navigationActions, folderViewModel) }
  }

  @Test
  fun displayComponents() {
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("archive").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("createFolder").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderCard1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderCard2").assertIsDisplayed()
  }

  @Test
  fun goBackWorks() {
    var test = false
    `when`(navigationActions.goBack()).then {
      test = true
      null
    }

    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").performClick()
    assert(test)
  }

  @Test
  fun bottomBarWorks() {
    composeTestRule.onNodeWithTag("bottomNavigationMenu").assertIsDisplayed()

    var test: Boolean
    `when`(navigationActions.navigateTo(any<TopLevelDestination>())).then {
      test = true
      null
    }

    LIST_TOP_LEVEL_DESTINATION.forEach { tab ->
      test = false
      composeTestRule.onNodeWithTag(tab.textId).assertExists().performClick()
      assert(test)
    }
  }

  @Test
  fun accessFolderWorks() {
    var test = false
    `when`(navigationActions.navigateTo(Screen.FOLDER)).then {
      test = true
      null
    }

    composeTestRule.onNodeWithTag("folderCard1").performClick()
    assert(test)
    assertSame(folderViewModel.activeFolder.value, folder1)

    test = false
    composeTestRule.onNodeWithTag("folderCard2").performClick()
    assert(test)
    assertSame(folderViewModel.activeFolder.value, folder2)
  }

  @Test
  fun createFolderWorks() {
    var test = false
    `when`(navigationActions.navigateTo(Screen.CREATE_FOLDER)).then {
      test = true
      null
    }

    composeTestRule.onNodeWithTag("createFolder").performClick()
    assert(test)
  }

  @Test
  fun deleteDialogWorks() {
    composeTestRule.onNodeWithTag("delete").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("cancel").assertIsNotDisplayed()
    composeTestRule.onAllNodesWithTag("checked").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("unchecked").assertCountEquals(0)

    composeTestRule.onNodeWithTag("folderCard1").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("delete").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancel").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("checked").assertCountEquals(1)
    composeTestRule.onAllNodesWithTag("unchecked").assertCountEquals(1)

    composeTestRule.onNodeWithTag("checked").performClick()
    composeTestRule.onNodeWithTag("delete").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("checked").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("unchecked").assertCountEquals(2)

    composeTestRule.onNodeWithTag("cancel").performClick()
    composeTestRule.onNodeWithTag("delete").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("cancel").assertIsNotDisplayed()
    composeTestRule.onAllNodesWithTag("checked").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("unchecked").assertCountEquals(0)

    composeTestRule.onNodeWithTag("folderCard1").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("delete").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("checked").assertCountEquals(1)
    composeTestRule.onAllNodesWithTag("unchecked").assertCountEquals(1)

    composeTestRule.onNodeWithTag("unchecked").performClick()
    composeTestRule.onNodeWithTag("delete").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("checked").assertCountEquals(2)
    composeTestRule.onAllNodesWithTag("unchecked").assertCountEquals(0)

    composeTestRule.onNodeWithTag("confirm").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("delete").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm").assertIsDisplayed()

    composeTestRule.onNodeWithTag("no").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm").assertIsNotDisplayed()
    composeTestRule.onAllNodesWithTag("checked").assertCountEquals(2)

    verify(0) { folderRepository.deleteFolder(any(), any(), any()) }

    composeTestRule.onNodeWithTag("delete").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("yes").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("confirm").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("delete").assertIsNotDisplayed()
    composeTestRule.onAllNodesWithTag("checked").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("unchecked").assertCountEquals(0)

    verify(1) { folderRepository.deleteFolder(any(), any(), any()) }
  }
}
