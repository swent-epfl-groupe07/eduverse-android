package com.github.se.eduverse.ui.folder

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class CreateFolderTest {
  private lateinit var folderRepository: FolderRepository
  private lateinit var fileRepository: FileRepository
  private lateinit var navigationActions: NavigationActions
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var fileViewModel: FileViewModel

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    folderRepository = mock(FolderRepository::class.java)
    fileRepository = mock(FileRepository::class.java)
    fileViewModel = FileViewModel(fileRepository)
    navigationActions = mock(NavigationActions::class.java)

    `when`(folderRepository.getNewUid()).thenReturn("")
    `when`(fileRepository.getNewUid()).thenReturn("")
    `when`(fileRepository.savePdfFile(any(), any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(2)
      callback()
    }

    val uri = mock(Uri::class.java)
    `when`(uri.lastPathSegment).thenReturn("testFile.pdf")
    fileViewModel.createFile(uri)

    val auth = mock(FirebaseAuth::class.java)
    val currentUser = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(currentUser)
    `when`(currentUser.uid).thenReturn("uid")
    folderViewModel = FolderViewModel(folderRepository, auth)

    composeTestRule.setContent {
      CreateFolderScreen(navigationActions, folderViewModel, fileViewModel)
    }
  }

  @Test
  fun displayComponents() {
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("courseNameTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("courseNameField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("textFiles").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("file").assertCountEquals(1)
    composeTestRule.onNodeWithTag("addFile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderSave").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folderCancel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm").assertIsNotDisplayed()
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
    var test: Boolean
    `when`(navigationActions.navigateTo(any<TopLevelDestination>())).then {
      test = true
      null
    }
    LIST_TOP_LEVEL_DESTINATION.forEach {
      test = false

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

  @Test
  fun deleteFileWorkLikeExpected() {
    `when`(fileRepository.deleteFile(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }

    composeTestRule.onAllNodesWithTag("file").assertCountEquals(1)
    composeTestRule.onAllNodesWithTag("editButton").assertCountEquals(1)

    composeTestRule.onNodeWithTag("editButton").performClick()

    composeTestRule.onNodeWithTag("delete").assertIsDisplayed()
    composeTestRule.onNodeWithTag("delete").performClick()

    composeTestRule.onNodeWithTag("confirm").assertIsDisplayed()
    composeTestRule.onNodeWithTag("yes").assertIsDisplayed()
    composeTestRule.onNodeWithTag("no").assertIsDisplayed()

    composeTestRule.onNodeWithTag("no").performClick()

    composeTestRule.onAllNodesWithTag("file").assertCountEquals(1)
    composeTestRule.onAllNodesWithTag("editButton").assertCountEquals(1)

    composeTestRule.onNodeWithTag("editButton").performClick()
    composeTestRule.onNodeWithTag("delete").performClick()

    composeTestRule.onNodeWithTag("confirm").assertIsDisplayed()
    composeTestRule.onNodeWithTag("yes").assertIsDisplayed()
    composeTestRule.onNodeWithTag("no").assertIsDisplayed()

    composeTestRule.onNodeWithTag("yes").performClick()

    composeTestRule.onAllNodesWithTag("file").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("editButton").assertCountEquals(0)
  }

  @Test
  fun renameFileWorkLikeExpected() {
    composeTestRule.onAllNodesWithTag("editButton").assertCountEquals(1)

    composeTestRule.onNodeWithTag("editButton").performClick()

    composeTestRule.onNodeWithTag("rename").assertIsDisplayed()
    composeTestRule.onNodeWithTag("rename").performClick()

    composeTestRule.onNodeWithTag("renameDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("textField").assertIsDisplayed()

    composeTestRule.onNodeWithTag("textField").performTextInput("test_input")

    composeTestRule.onNodeWithTag("cancel").performClick()
    composeTestRule.onAllNodesWithText("test_input").assertCountEquals(0)

    composeTestRule.onNodeWithTag("editButton").performClick()
    composeTestRule.onNodeWithTag("rename").performClick()

    composeTestRule.onNodeWithTag("textField").performTextInput("test_input")

    composeTestRule.onNodeWithTag("confirm").performClick()
    composeTestRule.onAllNodesWithText("test_input").assertCountEquals(1)
  }

  @Test
  fun clickOnFileHaveExpectedBehavior_success() {
    var test = false

    `when`(fileRepository.accessFile(any(), any(), any())).then {
      test = true
      null
    }
    `when`(folderRepository.addFolder(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }

    composeTestRule.onNodeWithTag("file").performClick()
    assert(test)

    composeTestRule.onNodeWithTag("folderSave").performClick()
    composeTestRule.waitForIdle()

    assert(folderViewModel.folders.value[0].files[0].numberAccess == 1)
  }

  @Test
  fun clickOnFileHaveExpectedBehavior_failure() {
    var test = false

    `when`(fileRepository.accessFile(any(), any(), any())).then {
      val callback = it.getArgument<(Exception) -> Unit>(2)
      callback(Exception("message"))
      test = true
      null
    }

    composeTestRule.onNodeWithTag("file").performClick()
    assert(test)
  }
}
