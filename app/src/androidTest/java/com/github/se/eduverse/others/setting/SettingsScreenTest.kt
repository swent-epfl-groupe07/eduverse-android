package com.github.se.eduverse.others.setting

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.setting.SettingsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Mutable states to track button clicks
  private var isSaveClicked = mutableStateOf(false)
  private var isCancelClicked = mutableStateOf(false)

  // Fake NavigationActions class to include state tracking
  private class FakeNavigationActions(
      navController: NavHostController,
      private val onSave: () -> Unit = {},
      private val onCancel: () -> Unit = {}
  ) : NavigationActions(navController)

  @Test
  fun testPrivacySettingsInput() {
    composeTestRule.setContent { SettingsScreen(navigationActions = FakeNavigationActions(mock())) }

    // Test Privacy Settings input interaction
    composeTestRule.onNodeWithTag("privacySettingsInput").performTextInput("Private")
  }

  @Test
  fun testThemeDropdownInteraction() {
    composeTestRule.setContent { SettingsScreen(navigationActions = FakeNavigationActions(mock())) }

    composeTestRule.onNodeWithTag("themeSelectionBox").assertHasClickAction()

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
    composeTestRule.setContent { SettingsScreen(navigationActions = FakeNavigationActions(mock())) }

    composeTestRule.onNodeWithTag("languageSelectionBox").assertHasClickAction()
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
    // Reset the save click variable

    // Set the content with the FakeNavigationActions for tracking save action
    composeTestRule.setContent { SettingsScreen(navigationActions = FakeNavigationActions(mock())) }

    // Test Save button interaction
    composeTestRule.onNodeWithTag("saveButton").performClick()

    // Assert that save was clicked
  }

  @Test
  fun testCancelButton() {
    // Reset the cancel click variable

    // Set the content with the FakeNavigationActions for tracking cancel action
    composeTestRule.setContent { SettingsScreen(navigationActions = FakeNavigationActions(mock())) }

    // Test Cancel button interaction
    composeTestRule.onNodeWithTag("cancelButton").performClick()

    // Assert that cancel was clicked
  }
}
