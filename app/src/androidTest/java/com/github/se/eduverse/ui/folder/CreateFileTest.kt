package com.github.se.eduverse.ui.folder

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.viewmodel.FileViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class CreateFileTest {
  private lateinit var fileRepository: FileRepository
  private lateinit var navigationActions: NavigationActions
  private lateinit var fileViewModel: FileViewModel

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    fileRepository = mock(FileRepository::class.java)
    navigationActions = mock(NavigationActions::class.java)
    fileViewModel = FileViewModel(fileRepository)

    `when`(fileRepository.getNewUid()).thenReturn("uid")
    `when`(fileRepository.savePdfFile(any(), any(), any(), any())).then {
      val callback = it.getArgument<() -> Unit>(2)
      callback()
    }

    composeTestRule.setContent { CreateFileScreen(navigationActions, fileViewModel) }
  }

  @Test
  fun displayComponents() {
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fileNameTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fileNameField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("uploadFileText").assertIsDisplayed()
    composeTestRule.onNodeWithTag("browseFile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("iconCheck").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("fileSave").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fileCancel").assertIsDisplayed()
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
  fun asserIconDisplayWhenShould() {
    composeTestRule.onNodeWithTag("iconCheck").assertIsNotDisplayed()

    fileViewModel.createFile(Uri.EMPTY)

    composeTestRule.onNodeWithTag("iconCheck").assertIsDisplayed()
  }

  @Test
  fun assertSaveWorks() {
    var test = false
    `when`(navigationActions.goBack()).then {
      test = true
      null
    }

    fileViewModel.createFile(Uri.EMPTY)

    composeTestRule.onNodeWithTag("fileSave").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fileSave").performClick()
    assert(test)
    assert(fileViewModel.validNewFile.value)
    assert(fileViewModel.getNewFile() != null)
  }

  @Test
  fun assertCancelWorks() {
    var test = false
    `when`(navigationActions.goBack()).then {
      test = true
      null
    }

    fileViewModel.createFile(Uri.EMPTY)

    composeTestRule.onNodeWithTag("fileCancel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fileCancel").performClick()
    assert(test)
    assert(!fileViewModel.validNewFile.value)
    assert(fileViewModel.getNewFile() == null)
  }
}
