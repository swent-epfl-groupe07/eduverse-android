package com.github.se.eduverse.ui.search

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.repository.ProfileRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
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
import org.mockito.kotlin.verify

class SearchProfileScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Mock private lateinit var mockRepository: ProfileRepository
  @Mock private lateinit var mockNavigationActions: NavigationActions

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
    mockNavigationActions = mock()
    viewModel = TestProfileViewModel(mockRepository, searchStateFlow)
  }

  @Test
  fun topAppBar_isDisplayed() {
    composeTestRule.setContent {
      SearchProfileScreen(navigationActions = mockNavigationActions, viewModel = viewModel)
    }

    composeTestRule.onNodeWithText("Search Users").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag("search_back_button").assertExists().assertIsDisplayed()
  }

  @Test
  fun backButton_triggersNavigation() {
    composeTestRule.setContent {
      SearchProfileScreen(navigationActions = mockNavigationActions, viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag("search_back_button").performClick()
    verify(mockNavigationActions).goBack()
  }

  @Test
  fun searchField_isDisplayed() {
    composeTestRule.setContent {
      SearchProfileScreen(navigationActions = mockNavigationActions, viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(TAG_SEARCH_FIELD).assertExists().assertIsDisplayed()
  }

  @Test
  fun idleState_showsSearchPrompt() {
    composeTestRule.setContent {
      SearchProfileScreen(navigationActions = mockNavigationActions, viewModel = viewModel)
    }

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
    composeTestRule.setContent {
      SearchProfileScreen(navigationActions = mockNavigationActions, viewModel = viewModel)
    }

    searchStateFlow.value = SearchProfileState.Loading
    composeTestRule.onNodeWithTag(TAG_LOADING_INDICATOR).assertExists().assertIsDisplayed()
  }

  @Test
  fun successState_withResults_showsProfiles() {
    composeTestRule.setContent {
      SearchProfileScreen(navigationActions = mockNavigationActions, viewModel = viewModel)
    }

    searchStateFlow.value = SearchProfileState.Success(testProfiles)

    composeTestRule.onNodeWithTag(TAG_PROFILE_LIST).assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag("${TAG_PROFILE_ITEM}_1").assertExists().assertIsDisplayed()

    composeTestRule
        .onNodeWithTag("${TAG_PROFILE_USERNAME}_1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag("${TAG_PROFILE_STATS}_1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .onChildren()
        .filterToOne(hasText("100 followers"))
        .assertExists()
  }

  @Test
  fun searchInput_convertsToLowerCase() {
    var searchQuery = ""
    val testViewModel =
        object : ProfileViewModel(mockRepository) {
          override fun searchProfiles(query: String) {
            searchQuery = query
          }
        }

    composeTestRule.setContent {
      SearchProfileScreen(navigationActions = mockNavigationActions, viewModel = testViewModel)
    }

    composeTestRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextInput("TestQuery")
    assert(searchQuery == "testquery") { "Search query should be converted to lowercase" }
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
