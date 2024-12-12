package com.github.se.eduverse.ui.folder

import androidx.compose.ui.test.assertIsDisplayed
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
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
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
}
