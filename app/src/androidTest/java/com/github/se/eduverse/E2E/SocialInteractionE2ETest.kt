package com.github.se.eduverse.E2E

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.se.eduverse.fake.FakeProfileRepository
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.ui.search.SearchProfileScreen
import com.github.se.eduverse.ui.search.TAG_PROFILE_ITEM
import com.github.se.eduverse.ui.search.TAG_PROFILE_LIST
import com.github.se.eduverse.ui.search.TAG_SEARCH_FIELD
import com.github.se.eduverse.ui.search.UserProfileScreen
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.SearchProfileState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.mockk.mockk
import io.mockk.unmockkAll
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class SocialInteractionE2ETest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var dashboardViewModel: FakeDashboardViewModel
  private lateinit var viewModel: FakeProfileViewModel
  private lateinit var navigationActions: FakeProfileNavigationActions

  @Before
  fun setup() {
    MockFirebaseAuth.setup()
    // Initialize all view models
    dashboardViewModel = FakeDashboardViewModel()
    viewModel = FakeProfileViewModel()
    navigationActions = FakeProfileNavigationActions()

    composeTestRule.setContent {
      TestNavigation2(
          dashboardViewModel = dashboardViewModel,
          viewModel = viewModel,
          navigationActions = navigationActions)
    }
  }

  @After
  fun tearDown() {
    unmockkAll() // Clean up all mockk mocks
  }

  @Test
  fun testSocialInteractionFlow() {
    composeTestRule.apply {
      // 1. Start from Dashboard and navigate to Search
      onNodeWithTag("search_button").performClick()
      navigationActions.navigateToSearch()
      waitForIdle()

      // 2. Search for a user
      onNodeWithTag(TAG_SEARCH_FIELD).performTextInput("test_user")
      waitForIdle()

      // Verify search results appear
      onNodeWithTag(TAG_PROFILE_LIST).assertExists()
      onNodeWithTag("${TAG_PROFILE_ITEM}_test_user_id").assertExists()

      // 3. Click on a user profile
      onNodeWithTag("${TAG_PROFILE_ITEM}_test_user_id").performClick()
      navigationActions.navigateToUserProfile("test_user_id")
      waitForIdle()

      // Verify user profile elements
      onNodeWithTag("user_profile_screen_container").assertExists()
      onNodeWithTag("user_profile_username").assertTextContains("TestUser")
      onNodeWithTag("followers_stat").assertExists()
      onNodeWithTag("following_stat").assertExists()

      // 4. Follow the user
      onNodeWithTag("follow_button").performClick()
      waitForIdle()

      // Verify follow button state changed
      onNodeWithTag("follow_button").assertTextContains("Unfollow")

      // 5. View user's publications
      onNodeWithTag("publications_tab").performClick()
      waitForIdle()

      // Verify publications grid exists
      onNodeWithTag("publications_grid").assertExists()

      // 6. Interact with a publication
      onNodeWithTag("publication_item_1").performClick()
      waitForIdle()

      // Verify publication detail dialog (using unmergedTree for dialog content)
      onNode(hasTestTag("publication_detail_dialog"), useUnmergedTree = true).assertExists()

      // 7. Like the publication (using unmergedTree for dialog elements)
      onNode(hasTestTag("like_button"), useUnmergedTree = true).performClick()
      waitForIdle()

      // Verify like status (using unmergedTree for dialog elements)
      onNode(hasTestTag("liked_icon"), useUnmergedTree = true).assertExists()

      // 8. Close publication detail (using unmergedTree for dialog elements)
      onNode(hasTestTag("close_button"), useUnmergedTree = true).performClick()
      waitForIdle()

      // 9. Unfollow user
      onNodeWithTag("follow_button").performClick()
      waitForIdle()

      // Verify follow button state reverted
      onNodeWithTag("follow_button").assertTextContains("Follow")

      // 10. Return to dashboard
      onNodeWithTag("back_button").performClick()
      navigationActions.goBack()
      waitForIdle()
    }
  }
}

@Composable
fun TestNavigation2(
    dashboardViewModel: FakeDashboardViewModel,
    viewModel: FakeProfileViewModel,
    navigationActions: FakeProfileNavigationActions
) {
  var currentScreen by remember { mutableStateOf("DASHBOARD") }

  // Listen for navigation changes
  if (navigationActions is FakeProfileNavigationActions) {
    currentScreen = navigationActions.currentRoute()
  }

  when (currentScreen) {
    "SEARCH" -> SearchProfileScreen(navigationActions = navigationActions, viewModel = viewModel)
    "USER_PROFILE" ->
        UserProfileScreen(
            navigationActions = navigationActions,
            viewModel = viewModel,
            userId = "test_user_id",
            currentUserId = "current_user_id")
    else -> DashboardScreen(viewModel = dashboardViewModel, navigationActions = navigationActions)
  }
}

@HiltViewModel
class FakeProfileViewModel @Inject constructor() : ProfileViewModel(FakeProfileRepository()) {
  private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
  override val profileState: StateFlow<ProfileUiState> = _profileState

  private val _likedPublications = MutableStateFlow<List<Publication>>(emptyList())
  override val likedPublications: StateFlow<List<Publication>> = _likedPublications

  private val _searchState = MutableStateFlow<SearchProfileState>(SearchProfileState.Idle)
  override val searchState: StateFlow<SearchProfileState> = _searchState

  private val mockUser =
      Profile(
          id = "test_user_id",
          username = "TestUser",
          profileImageUrl = "",
          followers = 100,
          following = 50,
          publications =
              listOf(
                  Publication(
                      id = "1",
                      userId = "test_user_id",
                      title = "Test Publication",
                      mediaUrl = "",
                      thumbnailUrl = "",
                      likes = 10,
                      likedBy = listOf())),
          isFollowedByCurrentUser = false)

  override fun loadProfile(userId: String) {
    // Set the profile state with mock user data
    _profileState.value = ProfileUiState.Success(mockUser)
  }

  override fun loadLikedPublications(userId: String) {
    _likedPublications.value = mockUser.publications
  }

  override fun searchProfiles(query: String) {
    _searchState.value = SearchProfileState.Loading
    if (query.isNotEmpty()) {
      _searchState.value = SearchProfileState.Success(listOf(mockUser))
    } else {
      _searchState.value = SearchProfileState.Idle
    }
  }

  override fun toggleFollow(followerId: String, targetUserId: String) {
    val currentState = _profileState.value
    if (currentState is ProfileUiState.Success) {
      val updatedProfile =
          currentState.profile.copy(
              isFollowedByCurrentUser = !currentState.profile.isFollowedByCurrentUser,
              followers =
                  if (currentState.profile.isFollowedByCurrentUser)
                      currentState.profile.followers - 1
                  else currentState.profile.followers + 1)
      _profileState.value = ProfileUiState.Success(updatedProfile)
    }
  }

  override fun likeAndAddToFavorites(userId: String, publicationId: String) {
    // Update liked publication state
    val currentState = _profileState.value
    if (currentState is ProfileUiState.Success) {
      val updatedPublications =
          currentState.profile.publications.map { pub ->
            if (pub.id == publicationId) {
              pub.copy(likes = pub.likes + 1, likedBy = pub.likedBy + userId)
            } else pub
          }
      _profileState.value =
          ProfileUiState.Success(currentState.profile.copy(publications = updatedPublications))
    }
  }

  override fun removeLike(userId: String, publicationId: String) {
    // Update unliked publication state
    val currentState = _profileState.value
    if (currentState is ProfileUiState.Success) {
      val updatedPublications =
          currentState.profile.publications.map { pub ->
            if (pub.id == publicationId) {
              pub.copy(likes = maxOf(0, pub.likes - 1), likedBy = pub.likedBy - userId)
            } else pub
          }
      _profileState.value =
          ProfileUiState.Success(currentState.profile.copy(publications = updatedPublications))
    }
  }
}

class FakeProfileNavigationActions : NavigationActions(mockk(relaxed = true)) {
  private var _currentRoute = mutableStateOf("DASHBOARD")

  fun navigateToSearch() {
    _currentRoute.value = "SEARCH"
  }

  override fun navigateToUserProfile(userId: String) {
    _currentRoute.value = "USER_PROFILE"
  }

  override fun navigateTo(destination: TopLevelDestination) {
    _currentRoute.value = destination.route
  }

  override fun navigateTo(route: String) {
    _currentRoute.value = route
  }

  override fun goBack() {
    _currentRoute.value = "DASHBOARD"
  }

  override fun currentRoute(): String = _currentRoute.value
}
