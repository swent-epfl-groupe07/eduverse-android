package com.github.se.eduverse.ui.others.setting

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testPrivacySettingsInput() {
    composeTestRule.setContent { SettingsScreen(onSaveClick = {}, onCancelClick = {}) }

    // Test Privacy Settings input interaction
    composeTestRule.onNodeWithTag("privacySettingsInput").performTextInput("Private")
  }

  @Test
  fun testThemeDropdownInteraction() {
    composeTestRule.setContent { SettingsScreen(onSaveClick = {}, onCancelClick = {}) }

    // Test clicking theme dropdown
    composeTestRule.onNodeWithTag("themeSelectionBox").performClick()

    // Check that the dropdown menu is expanded
    composeTestRule.onNodeWithTag("themeDropdown").assertIsDisplayed()

    // Select a theme from the dropdown
    composeTestRule.onNodeWithTag("themeOption_Dark").performClick()

    // Verify the selected theme is "Dark"
    composeTestRule.onNodeWithTag("themeSelectionBox").assertTextContains("Dark")
  }

  @Test
  fun testLanguageDropdownInteraction() {
    composeTestRule.setContent { SettingsScreen(onSaveClick = {}, onCancelClick = {}) }

    // Test clicking language dropdown
    composeTestRule.onNodeWithTag("languageSelectionBox").performClick()

    // Check that the dropdown menu is expanded
    composeTestRule.onNodeWithTag("languageDropdown").assertIsDisplayed()

    // Select a language from the dropdown
    composeTestRule.onNodeWithTag("languageOption_Français").performClick()

    // Verify the selected language is "Français"
    composeTestRule.onNodeWithTag("languageSelectionBox").assertTextContains("Français")
  }

  @Test
  fun testSaveButton() {
    var isSaveClicked = false
    composeTestRule.setContent {
      SettingsScreen(onSaveClick = { isSaveClicked = true }, onCancelClick = {})
    }

    // Test Save button interaction
    composeTestRule.onNodeWithTag("saveButton").performClick()

    // Assert that save was clicked
    assert(isSaveClicked)
  }

  @Test
  fun testCancelButton() {
    var isCancelClicked = false
    composeTestRule.setContent {
      SettingsScreen(onSaveClick = {}, onCancelClick = { isCancelClicked = true })
    }

    // Test Cancel button interaction
    composeTestRule.onNodeWithTag("cancelButton").performClick()

    // Assert that cancel was clicked
    assert(isCancelClicked)
  }
}
