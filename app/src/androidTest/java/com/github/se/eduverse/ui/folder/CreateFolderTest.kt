package com.github.se.eduverse.ui.folder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class CreateFolderTest {
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
          "1")

  val folder2 = Folder("", emptyList<MyFile>().toMutableList(), "folder2", "2")

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    folderRepository = mock(FolderRepository::class.java)
    navigationActions = mock(NavigationActions::class.java)

    `when`(folderRepository.getNewUid()).thenReturn("")

    val auth = mock(FirebaseAuth::class.java)
    val currentUser = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(currentUser)
    `when`(currentUser.uid).thenReturn("uid")
    folderViewModel = FolderViewModel(folderRepository, auth)

    composeTestRule.setContent { CreateFolderScreen(navigationActions, folderViewModel) }
  }

  @Test
  fun displayComponents() {
    composeTestRule.onNodeWithTag("goBack").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topAppBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topBarText").assertIsDisplayed()
    composeTestRule.onNodeWithTag("courseNameTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("courseNameField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("textFiles").assertIsDisplayed()
    composeTestRule.onNodeWithTag("file").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("addFile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderSave").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderCancel").assertIsDisplayed()
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
  fun addFileWorks() {
    var test = false
    `when`(navigationActions.navigateTo(Screen.CREATE_FILE)).then {
      test = true
      null
    }

    composeTestRule.onNodeWithTag("addFile").performClick()
    assert(test)
    composeTestRule.onNodeWithTag("file").assertIsDisplayed()
  }

  @Test
  fun assertSaveWorks() {
    var test_add = false
    var test_nav = false

    `when`(folderRepository.addFolder(any(), any(), any())).then {
      test_add = true
      null
    }
    `when`(navigationActions.goBack()).then {
      test_nav = true
      null
    }

    composeTestRule.onNodeWithTag("folderSave").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderSave").performClick()
    assert(test_nav)
    assert(test_add)
  }

  @Test
  fun assertCancelWorks() {
    var test_del = false
    var test_nav = false

    `when`(folderRepository.deleteFolder(any(), any(), any())).then {
      test_del = true
      null
    }

    `when`(navigationActions.goBack()).then {
      test_nav = true
      null
    }

    composeTestRule.onNodeWithTag("folderCancel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderCancel").performClick()
    assert(test_nav)
    assert(test_del)
    assertEquals(folderViewModel.folders.value.size, 0)
  }
}
