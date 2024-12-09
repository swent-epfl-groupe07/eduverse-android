package com.github.se.eduverse.ui.folder

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.Calendar
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DisplayFolderTest {
  private lateinit var folderRepository: FolderRepository
  private lateinit var fileRepository: FileRepository
  private lateinit var navigationActions: NavigationActions
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var fileViewModel: FileViewModel

  val file1 = MyFile("", "", "name 1", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file2 = MyFile("", "", "name 2", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file3 = MyFile("", "", "name 3", Calendar.getInstance(), Calendar.getInstance(), 0)

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
          "1",
          archived = false)

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    folderRepository = mock(FolderRepository::class.java)
    fileRepository = mock(FileRepository::class.java)
    fileViewModel = FileViewModel(fileRepository)
    navigationActions = mock(NavigationActions::class.java)

    doAnswer {
          val callback = it.getArgument<(List<Folder>) -> Unit>(2)
          callback(listOf(folder))
          null
        }
        .whenever(folderRepository)
        .getFolders(any(), any(), any(), any())

    val auth = mock(FirebaseAuth::class.java)
    val currentUser = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(currentUser)
    `when`(currentUser.uid).thenReturn("uid")
    folderViewModel = FolderViewModel(folderRepository, auth)
    folderViewModel.selectFolder(folder)

    composeTestRule.setContent { FolderScreen(navigationActions, folderViewModel, fileViewModel) }
  }

  @Test
  fun displayComponents() {
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("createFile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("textFiles").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sortingButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("scaffold").assertIsDisplayed()
    composeTestRule.onNodeWithTag("centerImage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("column").assertIsDisplayed()
    composeTestRule.onNodeWithTag("archive").assertIsDisplayed()
    composeTestRule.onNodeWithTag(file1.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(file2.name).assertIsDisplayed()
    composeTestRule.onNodeWithTag(file3.name).assertIsDisplayed()
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
  fun clickOnFileHaveExpectedBehavior_success() {
    var test = false

    `when`(fileRepository.accessFile(any(), any(), any())).then {
      test = true
      null
    }

    composeTestRule.onNodeWithTag("name 1").performClick()

    assert(test)
    assert(file1.numberAccess == 1)
  }

  @Test
  fun clickOnFileHaveExpectedBehavior_failureAccess() {
    var test = false

    `when`(fileRepository.accessFile(any(), any(), any())).then {
      val callback = it.getArgument<(Exception) -> Unit>(2)
      callback(Exception("message"))
      test = true
      null
    }

    composeTestRule.onNodeWithTag("name 1").performClick()

    assert(test)
  }

  @Test
  fun clickOnFileHaveExpectedBehavior_failureOpen() {
    val ref = mock(StorageReference::class.java)
    val task = mock(FileDownloadTask::class.java)
    var test = false

    `when`(fileRepository.accessFile(any(), any(), any())).then {
      val callback = it.getArgument<(StorageReference, String) -> Unit>(1)
      callback(ref, ".pdf")
    }
    `when`(ref.getFile(any<File>())).thenReturn(task)
    `when`(task.addOnSuccessListener(any())).thenReturn(task)
    `when`(task.addOnFailureListener(any())).then {
      val callback = it.getArgument<OnFailureListener>(0)
      callback.onFailure(Exception("message"))
      task
    }
    `when`(ref.name).then {
      test = true
      "name"
    }

    composeTestRule.onNodeWithTag("name 1").performClick()

    assert(test)
  }

  @Test
  fun deleteFileWorkLikeExpected() {
    `when`(fileRepository.deleteFile(any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(1)
      callback()
    }

    composeTestRule.onAllNodesWithTag("editButton").assertCountEquals(3)

    composeTestRule.onAllNodesWithTag("editButton").onFirst().performClick()

    composeTestRule.onNodeWithTag("delete").assertIsDisplayed()
    composeTestRule.onNodeWithTag("delete").performClick()

    composeTestRule.onNodeWithTag("confirm").assertIsDisplayed()
    composeTestRule.onNodeWithTag("yes").assertIsDisplayed()
    composeTestRule.onNodeWithTag("no").assertIsDisplayed()

    composeTestRule.onNodeWithTag("yes").performClick()

    composeTestRule.onAllNodesWithTag("editButton").assertCountEquals(2)

    composeTestRule.onAllNodesWithTag("editButton").onFirst().performClick()
    composeTestRule.onNodeWithTag("delete").performClick()

    composeTestRule.onNodeWithTag("confirm").assertIsDisplayed()
    composeTestRule.onNodeWithTag("yes").assertIsDisplayed()
    composeTestRule.onNodeWithTag("no").assertIsDisplayed()

    composeTestRule.onNodeWithTag("no").performClick()

    composeTestRule.onAllNodesWithTag("editButton").assertCountEquals(2)
    composeTestRule.onNodeWithTag("delete").assertIsNotDisplayed()
  }

  @Test
  fun renameFileWorkLikeExpected() {
    composeTestRule.onAllNodesWithTag("editButton").assertCountEquals(3)

    composeTestRule.onAllNodesWithTag("editButton").onFirst().performClick()

    composeTestRule.onNodeWithTag("rename").assertIsDisplayed()
    composeTestRule.onNodeWithTag("rename").performClick()

    composeTestRule.onNodeWithTag("renameDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("textField").assertIsDisplayed()

    composeTestRule.onNodeWithTag("textField").performTextInput("test_input")

    composeTestRule.onNodeWithTag("cancel").performClick()
    composeTestRule.onAllNodesWithText("test_input").assertCountEquals(0)

    composeTestRule.onAllNodesWithTag("editButton").onFirst().performClick()
    composeTestRule.onNodeWithTag("rename").performClick()

    composeTestRule.onNodeWithTag("textField").performTextInput("test_input")

    composeTestRule.onNodeWithTag("confirm").performClick()
    composeTestRule.onAllNodesWithText("test_input").assertCountEquals(1)
  }

  @Test
  fun archiveButtonTest() {
    composeTestRule.onNodeWithTag("archive").performClick()
    verify(2) { folderRepository.updateFolder(any(), any(), any()) }
    verify(navigationActions).goBack()

    assert(folder.archived)
  }
}
