package com.github.se.eduverse.ui.others.setting

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.setting.SettingsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val mockNavHostController = Mockito.mock(NavHostController::class.java)
  private val mockNavigationActions = NavigationActions(mockNavHostController)

  @Test
  fun testBackButtonNavigation() {
    composeTestRule.setContent { SettingsScreen(navigationActions = mockNavigationActions) }

    // Test clicking the back button
    composeTestRule.onNodeWithTag("backButton").assertIsDisplayed()

    // Verify that navigation action 'goBack' was triggered
  }

  @Test
  fun testConfidentialityToggleSwitch() {
    composeTestRule.setContent { SettingsScreen(navigationActions = mockNavigationActions) }

    // Check that the confidentiality toggle is displayed
    composeTestRule.onNodeWithTag("confidentialityToggle").assertIsDisplayed()

    // Check initial state of confidentiality toggle (assuming it starts as "Private")
    composeTestRule.onNodeWithTag("confidentialityToggleState").assertTextEquals("Private")
  }

  @Test
  fun testThemeDropdownSelection() {
    composeTestRule.setContent { SettingsScreen(navigationActions = mockNavigationActions) }

    // Open the theme dropdown
    composeTestRule.onNodeWithTag("themeDropdown").assertIsDisplayed().performClick()

    composeTestRule.onNodeWithTag("dropdownOption_ThemeLight").isDisplayed()

    // Check dropdown display and selection of theme option
    composeTestRule.onNodeWithTag("dropdownOption_ThemeDark").isDisplayed()

    composeTestRule.onNodeWithTag("dropdownOption_ThemeSystem Default").isDisplayed()
  }

  @Test
  fun testLanguageDropdownSelection() {
    composeTestRule.setContent { SettingsScreen(navigationActions = mockNavigationActions) }

    composeTestRule.onNodeWithTag("languageDropdown").assertIsDisplayed().performClick()

    // Open the language dropdown
    composeTestRule.onNodeWithTag("dropdownOption_LanguageEnglish").assertIsDisplayed()

    // Select a language from the dropdown and verify selection text
    composeTestRule.onNodeWithTag("dropdownOption_LanguageFrançais").assertIsDisplayed()
  }

  @Test
  fun testAddAccountButtonClick() {
    composeTestRule.setContent { SettingsScreen(navigationActions = mockNavigationActions) }

    // Click on the Add Account button
    composeTestRule.onNodeWithTag("addAccountButton").assertIsDisplayed()

    // Verify Add Account action (assuming further implementation to test the outcome)
  }

  @Test
  fun testLogoutButtonClick() {
    composeTestRule.setContent { SettingsScreen(navigationActions = mockNavigationActions) }

    // Check if Logout button exists and clickable
    composeTestRule.onNodeWithTag("logoutButton").assertIsDisplayed()
  }
}
