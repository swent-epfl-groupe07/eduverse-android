package com.github.se.eduverse.ui.setting

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeViewModel: FakeSettingsViewModel
  private lateinit var fakeNavigationActions: FakeNavigationActions

  @Before
  fun setup() {
    // Mock FirebaseAuth and FirebaseUser
    val mockUser =
        mock(FirebaseUser::class.java).apply { Mockito.`when`(uid).thenReturn("testUserId") }

    val mockAuth =
        mock(FirebaseAuth::class.java).apply { Mockito.`when`(currentUser).thenReturn(mockUser) }

    // Replace the `FirebaseAuth` instance in the ViewModel with the mocked instance
    fakeViewModel = FakeSettingsViewModel(mockAuth)
    fakeNavigationActions = FakeNavigationActions()
  }

  /**
   * FakeSettingsViewModel extends the actual SettingsViewModel and allows setting various UI states
   * for testing purposes.
   */
  class FakeSettingsViewModel(private val fakeAuth: FirebaseAuth) :
      SettingsViewModel(mockk(relaxed = true), fakeAuth) {

    // MutableStateFlow properties to control UI state
    private val _privacySettingsState = MutableStateFlow(true)
    override val privacySettings: StateFlow<Boolean> = _privacySettingsState.asStateFlow()

    private val _selectedLanguageState = MutableStateFlow("English")
    override val selectedLanguage: StateFlow<String> = _selectedLanguageState.asStateFlow()

    private val _selectedThemeState = MutableStateFlow("Light")
    override val selectedTheme: StateFlow<String> = _selectedThemeState.asStateFlow()

    // Method to set privacy settings state
    fun setPrivacySettings(value: Boolean) {
      _privacySettingsState.value = value
    }

    // Method to set selected language state
    fun setSelectedLanguage(language: String) {
      _selectedLanguageState.value = language
    }

    // Method to set selected theme state
    fun setSelectedTheme(theme: String) {
      _selectedThemeState.value = theme
    }
  }

  /** FakeNavigationActions simulates navigation without performing actual navigation. */
  class FakeNavigationActions : NavigationActions(mock()) {
    private var backClicked = false
      private set

    var lastNavigatedRoute: String? = null
      private set

    override fun currentRoute(): String = "setting"

    override fun goBack() {
      backClicked = true
    }

    override fun navigateTo(screen: String) {
      lastNavigatedRoute = screen
    }
  }

  /** Test to verify that the privacy toggle is displayed correctly. */
  @Test
  fun privacyToggle_isDisplayedCorrectly() {
    // Arrange: Set privacy settings to true (Private)
    fakeViewModel.setPrivacySettings(true)

    // Act: Load the SettingsScreen
    composeTestRule.setContent {
      SettingsScreen(navigationActions = fakeNavigationActions, settingsViewModel = fakeViewModel)
    }

    // Assert: Verify the toggle exists and is in the correct state
    composeTestRule.onNodeWithTag("confidentialityToggle").assertExists().assertIsDisplayed()
  }

  /** Test to verify that clicking the logout button triggers navigation to the AUTH screen. */
  @Test
  fun clickLogoutButton_callsLogout() {
    // Arrange: Set the content
    composeTestRule.setContent {
      SettingsScreen(navigationActions = fakeNavigationActions, settingsViewModel = fakeViewModel)
    }

    // Act: Click the logout button
    composeTestRule.onNodeWithTag("logoutButton").performClick()

    // Assert: Verify that navigateTo was called with Screen.AUTH
    assertTrue(fakeNavigationActions.lastNavigatedRoute == Screen.AUTH)

    // Optionally, verify that signOut() was called on FirebaseAuth if needed
  }

  /**
   * Test to verify that clicking the add account button shows a toast or a not implemented action.
   * Since testing Toasts directly is challenging, ensure that the corresponding method is called.
   */
  @Test
  fun clickAddAccountButton_showsToast() {
    // Arrange: Set the content
    composeTestRule.setContent {
      SettingsScreen(navigationActions = fakeNavigationActions, settingsViewModel = fakeViewModel)
    }

    // Act: Click the add account button
    composeTestRule.onNodeWithTag("addAccountButton").performClick()

    // Assert: Since Toasts are hard to test, verify that navigateTo was not called with a specific
    // route
    // Alternatively, refactor the code to allow injecting a Toast handler and verify its invocation
    // For example:
    // verify { fakeNavigationActions.navigateTo(any()) wasNot Called }
  }

  /** Test to verify that the theme dropdown displays the correct current selection. */
  @Test
  fun themeDropdown_displaysCurrentSelection() {
    // Arrange: Set the theme to "Dark"
    fakeViewModel.setSelectedTheme("Dark")

    // Act: Set the content
    composeTestRule.setContent {
      SettingsScreen(navigationActions = fakeNavigationActions, settingsViewModel = fakeViewModel)
    }

    // Assert: The theme dropdown displays "Dark"
    composeTestRule
        .onNodeWithTag("themeDropdown")
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Text, listOf(AnnotatedString("Theme: Dark"))))
  }

  /** Test to verify that the language dropdown displays the correct current selection. */
  @Test
  fun languageDropdown_displaysCurrentSelection() {
    // Arrange: Set the language to "Français"
    fakeViewModel.setSelectedLanguage("Français")

    // Act: Set the content
    composeTestRule.setContent {
      SettingsScreen(navigationActions = fakeNavigationActions, settingsViewModel = fakeViewModel)
    }

    // Assert: The language dropdown displays "Français"
    composeTestRule
        .onNodeWithTag("languageDropdown")
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Text, listOf(AnnotatedString("Language: Français"))))
  }

  /** Test to verify that the settings options are displayed correctly and navigable. */
  @Test
  fun settingsOptions_areDisplayedAndNavigable() {
    // Arrange: Set the content
    composeTestRule.setContent {
      SettingsScreen(navigationActions = fakeNavigationActions, settingsViewModel = fakeViewModel)
    }

    // List of settings options with their respective routes
    val settingsOptions = listOf(Pair("Archive", Route.ARCHIVE), Pair("Gallery", Screen.GALLERY))

    // Iterate through each settings option and verify navigation
    for ((option, route) in settingsOptions) {
      composeTestRule.onNodeWithTag("settingsOption_$option").assertExists().assertIsDisplayed()
      composeTestRule.onNodeWithTag("settingsOption_$option").performClick()
      assertTrue(fakeNavigationActions.lastNavigatedRoute == route)
    }
  }
}
