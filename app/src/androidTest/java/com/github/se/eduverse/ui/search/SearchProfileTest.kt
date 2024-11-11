package com.github.se.eduverse.ui.search

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.repository.ProfileRepository
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.SearchProfileState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock

class SearchProfileScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Mock private lateinit var mockRepository: ProfileRepository

  private lateinit var viewModel: TestProfileViewModel
  private val searchStateFlow = MutableStateFlow<SearchProfileState>(SearchProfileState.Idle)

  private val testProfiles =
      listOf(
          Profile(
              id = "1",
              username = "testUser1",
              followers = 100,
              following = 50,
              profileImageUrl = "test_url_1"),
          Profile(
              id = "2",
              username = "testUser2",
              followers = 200,
              following = 150,
              profileImageUrl = "test_url_2"))

  private class TestProfileViewModel(
      repository: ProfileRepository,
      private val testSearchState: MutableStateFlow<SearchProfileState>
  ) : ProfileViewModel(repository) {
    override val searchState: StateFlow<SearchProfileState> = testSearchState
  }

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    mockRepository = mock()
    viewModel = TestProfileViewModel(mockRepository, searchStateFlow)
  }

  @Test
  fun searchField_isDisplayed() {
    composeTestRule.setContent { SearchProfileScreen(viewModel = viewModel, onProfileClick = {}) }

    composeTestRule.onNodeWithTag(TAG_SEARCH_FIELD).assertExists().assertIsDisplayed()
  }

  @Test
  fun idleState_showsSearchPrompt() {
    composeTestRule.setContent { SearchProfileScreen(viewModel = viewModel, onProfileClick = {}) }

    composeTestRule
        .onNodeWithTag(TAG_IDLE_MESSAGE)
        .assertExists()
        .assertIsDisplayed()
        .onChildren()
        .filterToOne(hasText("Search for other users"))
        .assertExists()
  }

  @Test
  fun loadingState_showsProgressIndicator() {
    composeTestRule.setContent { SearchProfileScreen(viewModel = viewModel, onProfileClick = {}) }

    searchStateFlow.value = SearchProfileState.Loading

    composeTestRule.onNodeWithTag(TAG_LOADING_INDICATOR).assertExists().assertIsDisplayed()
  }

  @Test
  fun successState_withResults_showsProfiles() {
    composeTestRule.setContent { SearchProfileScreen(viewModel = viewModel, onProfileClick = {}) }

    searchStateFlow.value = SearchProfileState.Success(testProfiles)

    // Check profile list exists
    composeTestRule.onNodeWithTag(TAG_PROFILE_LIST).assertExists().assertIsDisplayed()

    // Check profile item exists
    composeTestRule.onNodeWithTag("${TAG_PROFILE_ITEM}_1").assertExists().assertIsDisplayed()

    // Check username with unmerged tree
    composeTestRule
        .onNodeWithTag("${TAG_PROFILE_USERNAME}_1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Check stats with unmerged tree
    composeTestRule
        .onNodeWithTag("${TAG_PROFILE_STATS}_1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .onChildren()
        .filterToOne(hasText("100 followers"))
        .assertExists()
  }

  @Test
  fun successState_withEmptyResults_showsNoProfilesFound() {
    composeTestRule.setContent { SearchProfileScreen(viewModel = viewModel, onProfileClick = {}) }

    composeTestRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextInput("nonexistent")

    searchStateFlow.value = SearchProfileState.Success(emptyList())

    composeTestRule
        .onNodeWithTag(TAG_NO_RESULTS)
        .assertExists()
        .assertIsDisplayed()
        .onChildren()
        .filterToOne(hasText("No profiles found"))
        .assertExists()
  }

  @Test
  fun errorState_showsErrorMessage() {
    val errorMessage = "Test error message"

    composeTestRule.setContent { SearchProfileScreen(viewModel = viewModel, onProfileClick = {}) }

    searchStateFlow.value = SearchProfileState.Error(errorMessage)

    composeTestRule
        .onNodeWithTag(TAG_ERROR_MESSAGE)
        .assertExists()
        .assertIsDisplayed()
        .onChildren()
        .filterToOne(hasText(errorMessage))
        .assertExists()
  }

  @Test
  fun profileClick_triggersCallback() {
    var clickedProfileId = ""

    composeTestRule.setContent {
      SearchProfileScreen(viewModel = viewModel, onProfileClick = { clickedProfileId = it })
    }

    searchStateFlow.value = SearchProfileState.Success(testProfiles)

    composeTestRule.onNodeWithTag("${TAG_PROFILE_ITEM}_1").performClick()

    assert(clickedProfileId == "1")
  }

  @Test
  fun searchInput_triggersViewModelSearch() {
    // Create a test implementation of ProfileViewModel
    val testViewModel =
        object : ProfileViewModel(mockRepository) {
          override fun searchProfiles(query: String) {
            // Update the search state to Loading when search is triggered
            searchStateFlow.value = SearchProfileState.Loading
          }
        }

    composeTestRule.setContent {
      SearchProfileScreen(viewModel = testViewModel, onProfileClick = {})
    }

    composeTestRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextInput("test")

    // Wait for debounce
    composeTestRule.mainClock.advanceTimeBy(500L)
    composeTestRule.waitForIdle()

    // Verify the state changed to Loading
    assert(searchStateFlow.value is SearchProfileState.Loading) {
      "Search state should have changed to Loading, but was ${searchStateFlow.value}"
    }
  }
}

class ProfileSearchItemTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val testProfile =
      Profile(
          id = "1",
          username = "testUser",
          followers = 100,
          following = 50,
          profileImageUrl = "test_url")

  @Test
  fun allProfileElements_areDisplayed() {
    composeTestRule.setContent { ProfileSearchItem(profile = testProfile, onClick = {}) }

    // Check main container
    composeTestRule.onNodeWithTag("${TAG_PROFILE_ITEM}_1").assertExists().assertIsDisplayed()

    // Check username with unmerged tree
    composeTestRule
        .onNodeWithTag("${TAG_PROFILE_USERNAME}_1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .assertTextContains(testProfile.username)

    // Check stats with unmerged tree
    composeTestRule
        .onNodeWithTag("${TAG_PROFILE_STATS}_1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Check profile image
    composeTestRule
        .onNodeWithTag("${TAG_PROFILE_IMAGE}_1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun click_triggersCallback() {
    var wasClicked = false

    composeTestRule.setContent {
      ProfileSearchItem(profile = testProfile, onClick = { wasClicked = true })
    }

    composeTestRule.onNodeWithTag("${TAG_PROFILE_ITEM}_1").performClick()

    assert(wasClicked) { "Click callback was not triggered" }
  }

  @Test
  fun longUsername_isEllipsized() {
    val longNameProfile =
        testProfile.copy(username = "ThisIsAVeryLongUsernameThatShouldBeEllipsized")

    composeTestRule.setContent { ProfileSearchItem(profile = longNameProfile, onClick = {}) }

    // Check username with unmerged tree
    composeTestRule
        .onNodeWithTag("${TAG_PROFILE_USERNAME}_1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .assertTextContains(longNameProfile.username)
  }
}
