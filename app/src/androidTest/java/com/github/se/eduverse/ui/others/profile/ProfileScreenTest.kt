package com.github.se.eduverse.ui.others.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun profileScreen_displaysAllFields() {
    composeTestRule.setContent { ProfileScreen(onSaveClick = {}, onCancelClick = {}) }

    // Assert that all input fields are displayed with correct labels
    composeTestRule.onNodeWithText("Name").assertExists()
    composeTestRule.onNodeWithText("School").assertExists()
    composeTestRule.onNodeWithText("Courses Selected").assertExists()
    composeTestRule.onNodeWithText("# Videos Watched").assertExists()
    composeTestRule.onNodeWithText("# Quizzes Completed").assertExists()
    composeTestRule.onNodeWithText("Study Time Tracker").assertExists()
    composeTestRule.onNodeWithText("Study Goals").assertExists()
  }

  @Test
  fun profileScreen_inputFields_areEditable() {
    composeTestRule.setContent { ProfileScreen(onSaveClick = {}, onCancelClick = {}) }

    // Enter text in each field and assert the text is correctly entered
    composeTestRule.onNodeWithText("Name").performTextInput("John Doe")
    composeTestRule.onNodeWithText("John Doe").assertExists()

    composeTestRule.onNodeWithText("School").performTextInput("Harvard University")
    composeTestRule.onNodeWithText("Harvard University").assertExists()

    composeTestRule.onNodeWithText("Courses Selected").performTextInput("Math, Science")
    composeTestRule.onNodeWithText("Math, Science").assertExists()

    composeTestRule.onNodeWithText("# Videos Watched").performTextInput("10")
    composeTestRule.onNodeWithText("10").assertExists()

    composeTestRule.onNodeWithText("# Quizzes Completed").performTextInput("5")
    composeTestRule.onNodeWithText("5").assertExists()

    composeTestRule.onNodeWithText("Study Time Tracker").performTextInput("30 hours")
    composeTestRule.onNodeWithText("30 hours").assertExists()

    composeTestRule.onNodeWithText("Study Goals").performTextInput("Complete 2 courses")
    composeTestRule.onNodeWithText("Complete 2 courses").assertExists()
  }

  @Test
  fun profileScreen_buttons_areClickable() {
    var saveClicked = false
    var cancelClicked = false

    composeTestRule.setContent {
      ProfileScreen(onSaveClick = { saveClicked = true }, onCancelClick = { cancelClicked = true })
    }

    // Assert that Save button is clickable
    composeTestRule.onNodeWithText("Save").performClick()
    assert(saveClicked)

    // Assert that Cancel button is clickable
    composeTestRule.onNodeWithText("Cancel").performClick()
    assert(cancelClicked)
  }

  @Test
  fun profileScreen_layout_isCorrect() {
    composeTestRule.setContent { ProfileScreen(onSaveClick = {}, onCancelClick = {}) }

    // Assert that the Column layout is displayed correctly
    composeTestRule.onNode(hasTestTag("profileColumn")).assert(hasAnyChild(hasTestTag("nameField")))

    // Assert that buttons are horizontally aligned
    composeTestRule.onNodeWithText("Save").assert(hasAnySibling(hasText("Cancel")))
  }
}
