package com.github.se.eduverse.ui.others.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.repository.Profile
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class ProfileScreenUiTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeViewModel = FakeProfileViewModel()
  private val mockNavigationActions = FakeNavigationActions(navController = mock())

  @Test
  fun testProfileScreenDisplaysFields() {
    setupProfileScreen()

    composeTestRule.onNodeWithTag("profileColumn").assertExists()
    composeTestRule.onNodeWithTag("nameField").assertExists()
    composeTestRule.onNodeWithTag("schoolInput").assertExists()
    composeTestRule.onNodeWithTag("coursesSelectedInput").assertExists()
    composeTestRule.onNodeWithTag("videosWatchedInput").assertExists()
    composeTestRule.onNodeWithTag("quizzesCompletedInput").assertExists()
    composeTestRule.onNodeWithTag("studyTimeInput").assertExists()
    composeTestRule.onNodeWithTag("studyGoalsInput").assertExists()
  }

  @Test
  fun testProfileFieldsInput() {
    setupProfileScreen()

    // Simulate user input in fields
    composeTestRule.onNodeWithTag("nameField").performTextInput("John Doe")
    composeTestRule.onNodeWithTag("schoolInput").performTextInput("University of Test")
    composeTestRule.onNodeWithTag("coursesSelectedInput").performTextInput("5")
    composeTestRule.onNodeWithTag("videosWatchedInput").performTextInput("10")
    composeTestRule.onNodeWithTag("quizzesCompletedInput").performTextInput("7")
    composeTestRule.onNodeWithTag("studyTimeInput").performTextInput("15.5")
    composeTestRule.onNodeWithTag("studyGoalsInput").performTextInput("Complete all tests")

    // Assert inputs have been updated
    composeTestRule.onNodeWithTag("nameField").assertTextContains("John Doe")
    composeTestRule.onNodeWithTag("schoolInput").assertTextContains("University of Test")
    composeTestRule.onNodeWithTag("coursesSelectedInput").assertTextContains("5")
    composeTestRule.onNodeWithTag("videosWatchedInput").assertTextContains("10")
    composeTestRule.onNodeWithTag("quizzesCompletedInput").assertTextContains("7")
    composeTestRule.onNodeWithTag("studyTimeInput").assertTextContains("15.5")
    composeTestRule.onNodeWithTag("studyGoalsInput").assertTextContains("Complete all tests")
  }

  @Test
  fun testSaveButtonFunctionality() {
    setupProfileScreen()

    // Simulate user input
    composeTestRule.onNodeWithTag("nameField").performTextInput("John Doe")
    composeTestRule.onNodeWithTag("saveButton").performClick()

    // Verify the ViewModel's saveProfile function was called with updated inputs
    assert(fakeViewModel.savedProfile != null)
    assert(fakeViewModel.savedProfile!!.name == "John Doe")
  }

  @Test
  fun testCancelButtonFunctionality() {
    setupProfileScreen()

    // Assert the cancel button exists and performs the back action
    composeTestRule.onNodeWithTag("cancelButton").assertExists().performClick()
    // Add any additional verification for the navigation action, if necessary
  }

  private fun setupProfileScreen() {
    composeTestRule.setContent {
      ProfileScreen(
          viewModel = fakeViewModel,
          navigationActions = mockNavigationActions,
          userId = "testUserId")
    }
  }
}

class FakeProfileViewModel : ProfileViewModel(mock()) {
  private val _profileState =
      MutableStateFlow(
          Profile(
              name = "",
              school = "",
              coursesSelected = "",
              videosWatched = "",
              quizzesCompleted = "",
              studyTime = "",
              studyGoals = ""))

  override val profileState: StateFlow<Profile> = _profileState.asStateFlow()

  var savedProfile: Profile? = null

  override fun saveProfile(
      userId: String,
      name: String,
      school: String,
      coursesSelected: String,
      videosWatched: String,
      quizzesCompleted: String,
      studyTime: String,
      studyGoals: String
  ) {
    savedProfile =
        Profile(
            name, school, coursesSelected, videosWatched, quizzesCompleted, studyTime, studyGoals)
    _profileState.value = savedProfile!!
  }

  override fun loadProfile(userId: String) {
    // No-op for this test
  }
}

class FakeNavigationActions(navController: NavHostController) : NavigationActions(navController) {
  fun navigate(route: String) {
    // No-op for testing
  }
}
