package com.github.se.eduverse.E2E

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.se.eduverse.fake.FakeProfileRepository
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.SettingsRepository
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.ui.profile.FollowListScreen
import com.github.se.eduverse.ui.search.SearchProfileScreen
import com.github.se.eduverse.ui.search.TAG_IDLE_MESSAGE
import com.github.se.eduverse.ui.search.TAG_NO_RESULTS
import com.github.se.eduverse.ui.search.TAG_PROFILE_ITEM
import com.github.se.eduverse.ui.search.TAG_PROFILE_LIST
import com.github.se.eduverse.ui.search.TAG_SEARCH_FIELD
import com.github.se.eduverse.ui.search.UserProfileScreen
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.SearchProfileState
import com.github.se.eduverse.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth
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
  private var settingViewModel = mock(SettingsViewModel::class.java)

    @Before
    fun setup() {
        // Setup MockFirebaseAuth first
        MockFirebaseAuth.setup()  // This will setup the Firebase mock with an authenticated user

        val mockAuth = mock(FirebaseAuth::class.java)
        val settingsRepository = FakeSettingsRepository()

        settingViewModel = SettingsViewModel(settingsRepository = settingsRepository, auth = mockAuth)
        dashboardViewModel = FakeDashboardViewModel()
        viewModel = FakeProfileViewModel()
        navigationActions = FakeProfileNavigationActions()

        composeTestRule.setContent {
            TestNavigation2(
                dashboardViewModel = dashboardViewModel,
                viewModel = viewModel,
                settingViewModel,
                navigationActions = navigationActions
            )
        }
    }

    @After
    fun tearDown() {
        MockFirebaseAuth.cleanup()  // Add this to clean up the mocks
        unmockkAll()
    }

    @Test
    fun testBasicSocialInteractionFlow() {
        composeTestRule.apply {
            navigateToUserProfile()
            performBasicProfileActions()
            returnToDashboard()
        }
    }

    @Test
    fun testFollowersInteractionFlow() {
        composeTestRule.apply {
            // Navigate to user profile
            navigateToUserProfile()

            // Click on followers count
            navigateToFollowersList()

            // Verify followers list screen
            onNodeWithTag("follow_list").assertExists()

            // Check follower items exist
            onNodeWithTag("follow_list_item_test_follower_1").assertExists()

            // Test follow back functionality
            onNodeWithTag("follow_button_test_follower_1").performClick()
            waitForIdle()
            onNodeWithTag("follow_button_test_follower_1")
                .assertTextContains("Follow Back")

            // Navigate to follower's profile
            onNodeWithTag("follow_list_item_test_follower_1").performClick()
            waitForIdle()

            // Verify on follower's profile
            onNodeWithTag("user_profile_username")
                .assertTextContains("TestUser")

            // Return to previous screen
            onNodeWithTag("back_button").performClick()
            waitForIdle()

        }
    }

    @Test
    fun testFollowingInteractionFlow() {
        composeTestRule.apply {
            // Navigate to user profile
            navigateToUserProfile()

            // Click on following count
            navigateToFollowingList()

            // Verify following list screen
            onNodeWithTag("follow_list").assertExists()

            // Check following items exist
            onNodeWithTag("follow_list_item_test_following_1").assertExists()

            // Test unfollow functionality
            onNodeWithTag("follow_button_test_following_1").performClick()
            waitForIdle()
            onNodeWithTag("follow_button_test_following_1")
                .assertTextContains("Following")

            // Navigate to following user's profile
            onNodeWithTag("follow_list_item_test_following_1").performClick()
            waitForIdle()

            // Verify on following user's profile
            onNodeWithTag("user_profile_username")
                .assertTextContains("TestUser")

            // Return to previous screen
            onNodeWithTag("back_button").performClick()
            waitForIdle()

        }
    }

    @Test
    fun testFollowUnfollowCycle() {
        composeTestRule.apply {
            // Navigate to user profile
            navigateToUserProfile()

            // Initial follow
            onNodeWithTag("follow_button").performClick()
            waitForIdle()
            onNodeWithTag("follow_button").assertTextContains("Unfollow")

            // Check followers count increased
            onNodeWithTag("followers_stat")
                .assertTextContains("101") // Initial 100 + 1

            // Unfollow
            onNodeWithTag("follow_button").performClick()
            waitForIdle()
            onNodeWithTag("follow_button").assertTextContains("Follow")

            // Check followers count decreased
            onNodeWithTag("followers_stat")
                .assertTextContains("100") // Back to initial value
        }
    }

    @Test
    fun testEmptyFollowLists() {
        composeTestRule.apply {
            // Set up view model with empty lists
            viewModel.clearFollowLists()

            // Navigate to user profile
            navigateToUserProfile()

            // Check empty followers list
            navigateToFollowersList()
            onNodeWithTag("empty_message").assertExists()
            onNodeWithTag("empty_message").assertTextContains("No followers yet")

            // Return to profile
            onNodeWithTag("goBackButton").performClick()
            waitForIdle()

            // Check empty following list
            navigateToFollowingList()
            onNodeWithTag("empty_message").assertExists()
            onNodeWithTag("empty_message").assertTextContains("Not following anyone")
        }
    }

    // Helper functions
    private fun ComposeTestRule.navigateToUserProfile() {
        onNodeWithTag("search_button").performClick()
        navigationActions.navigateToSearch()
        waitForIdle()

        onNodeWithTag(TAG_SEARCH_FIELD).performTextInput("test_user")
        waitForIdle()

        onNodeWithTag("${TAG_PROFILE_ITEM}_test_user_id").performClick()
        navigationActions.navigateToUserProfile("test_user_id")
        waitForIdle()
    }

    private fun ComposeTestRule.performBasicProfileActions() {
        onNodeWithTag("user_profile_screen_container").assertExists()
        onNodeWithTag("user_profile_username").assertTextContains("TestUser")
        onNodeWithTag("followers_stat").assertExists()
        onNodeWithTag("following_stat").assertExists()
    }

    private fun ComposeTestRule.returnToDashboard() {
        onNodeWithTag("back_button").performClick()
        navigationActions.goBack()
        waitForIdle()
    }
    private fun ComposeTestRule.navigateToFollowersList() {
        onNodeWithTag("followers_stat").performClick()
        navigationActions.navigateToFollowersList(navigationActions.currentUserId())
        waitForIdle()
    }

    private fun ComposeTestRule.navigateToFollowingList() {
        onNodeWithTag("following_stat").performClick()
        navigationActions.navigateToFollowingList(navigationActions.currentUserId())
        waitForIdle()
    }
}

@Composable
fun TestNavigation2(
    dashboardViewModel: FakeDashboardViewModel,
    viewModel: FakeProfileViewModel,
    settingsViewModel: SettingsViewModel,
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
                settingsViewModel = settingsViewModel,
                userId = navigationActions.currentUserId(),
                currentUserId = "current_user_id")
        "FOLLOW_LIST" -> {
            if (navigationActions is FakeProfileNavigationActions) {
                FollowListScreen(
                    navigationActions = navigationActions,
                    viewModel = viewModel,
                    userId = navigationActions.currentUserId(),
                    isFollowersList = navigationActions.isFollowersList()
                )
            }
        }
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

    private var followers = mutableListOf(
        Profile(
            id = "test_follower_1",
            username = "TestFollower1",
            profileImageUrl = "",
            followers = 50,
            following = 30,
            publications = listOf(),
            isFollowedByCurrentUser = false
        )
    )

    private var following = mutableListOf(
        Profile(
            id = "test_following_1",
            username = "TestFollowing1",
            profileImageUrl = "",
            followers = 40,
            following = 20,
            publications = listOf(),
            isFollowedByCurrentUser = true
        )
    )

    override suspend fun getFollowers(userId: String): List<Profile> = followers

    override suspend fun getFollowing(userId: String): List<Profile> = following

    fun clearFollowLists() {
        followers.clear()
        following.clear()
    }
}

class FakeProfileNavigationActions : NavigationActions(mockk(relaxed = true)) {
    private var _currentRoute = mutableStateOf("DASHBOARD")
    private var _currentUserId = mutableStateOf("")
    private var _isFollowersList = mutableStateOf(false)

    fun navigateToSearch() {
        _currentRoute.value = "SEARCH"
    }

    override fun navigateToUserProfile(userId: String) {
        _currentRoute.value = "USER_PROFILE"
        _currentUserId.value = userId
    }

    override fun navigateToFollowersList(userId: String) {
        _currentRoute.value = "FOLLOW_LIST"
        _currentUserId.value = userId
        _isFollowersList.value = true
    }

    override fun navigateToFollowingList(userId: String) {
        _currentRoute.value = "FOLLOW_LIST"
        _currentUserId.value = userId
        _isFollowersList.value = false
    }

    override fun navigateTo(destination: TopLevelDestination) {
        _currentRoute.value = destination.route
    }

    override fun navigateTo(route: String) {
        _currentRoute.value = route
    }

    override fun goBack() {
        when (_currentRoute.value) {
            "FOLLOW_LIST" -> _currentRoute.value = "USER_PROFILE"
            "USER_PROFILE", "SEARCH" -> _currentRoute.value = "DASHBOARD"
        }
    }

    override fun currentRoute(): String = _currentRoute.value
    fun currentUserId(): String = _currentUserId.value
    fun isFollowersList(): Boolean = _isFollowersList.value
}

class FakeSettingsRepository : SettingsRepository(mock()) {

  private val userSettings = mutableMapOf<String, MutableMap<String, Any>>()

  override suspend fun setPrivacySettings(userId: String, value: Boolean) {
    val userSetting = userSettings[userId] ?: mutableMapOf()
    userSetting["privacySettings"] = value
    userSettings[userId] = userSetting
  }

  override suspend fun getPrivacySettings(userId: String): Boolean {
    return userSettings[userId]?.get("privacySettings") as? Boolean ?: false
  }

  override suspend fun getSelectedLanguage(userId: String): String {
    return userSettings[userId]?.get("selectedLanguage") as? String ?: "English"
  }

  override suspend fun setSelectedLanguage(userId: String, language: String) {
    val userSetting = userSettings[userId] ?: mutableMapOf()
    userSetting["selectedLanguage"] = language
    userSettings[userId] = userSetting
  }

  override suspend fun getSelectedTheme(userId: String): String {
    return userSettings[userId]?.get("selectedTheme") as? String ?: "Light"
  }

  override suspend fun setSelectedTheme(userId: String, theme: String) {
    val userSetting = userSettings[userId] ?: mutableMapOf()
    userSetting["selectedTheme"] = theme
    userSettings[userId] = userSetting
  }
}
