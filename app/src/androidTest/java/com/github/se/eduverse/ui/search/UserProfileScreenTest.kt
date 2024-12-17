package com.github.se.eduverse.ui.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.SettingsRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FollowActionState
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.SettingsViewModel
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class UserProfileScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeViewModel: FakeProfileViewModel
  private lateinit var fakeNavigationActions: FakeNavigationActions
  private val testUserId = "test_user_id"
  private lateinit var settingsViewModel: SettingsViewModel

  @Before
  fun setup() {
    fakeViewModel = FakeProfileViewModel()
    fakeNavigationActions = FakeNavigationActions()

    val fakeSettingsRepository =
        FakeSettingsRepository().apply {
          runBlocking { setPrivacySettings(testUserId, false) } // Configure the repository
        }

    settingsViewModel = SettingsViewModel(fakeSettingsRepository, mock())
  }

  class FakeProfileViewModel : ProfileViewModel(mock()) {
    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    override val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    private val _likedPublications = MutableStateFlow<List<Publication>>(emptyList())
    override val likedPublications: StateFlow<List<Publication>> = _likedPublications.asStateFlow()

    // Add these lines
    private val _followActionState = MutableStateFlow<FollowActionState>(FollowActionState.Idle)
    override val followActionState: StateFlow<FollowActionState> = _followActionState.asStateFlow()

    fun setState(state: ProfileUiState) {
      _profileState.value = state
    }

    fun setLikedPublications(publications: List<Publication>) {
      _likedPublications.value = publications
    }

    // Add this function
    fun setFollowActionState(state: FollowActionState) {
      _followActionState.value = state
    }
  }

  class FakeSettingsRepository : SettingsRepository(mock()) {

    private val userSettings = mutableMapOf<String, Any>()

    override suspend fun setPrivacySettings(userId: String, value: Boolean) {
      userSettings["$userId-privacy"] = value
    }

    override suspend fun getPrivacySettings(userId: String): Boolean {
      return userSettings["$userId-privacy"] as? Boolean ?: false
    }

    override suspend fun getSelectedLanguage(userId: String): String {
      return userSettings["$userId-language"] as? String ?: "English"
    }

    override suspend fun setSelectedLanguage(userId: String, language: String) {
      userSettings["$userId-language"] = language
    }

    override suspend fun getSelectedTheme(userId: String): String {
      return userSettings["$userId-theme"] as? String ?: "Light"
    }

    override suspend fun setSelectedTheme(userId: String, theme: String) {
      userSettings["$userId-theme"] = theme
    }
  }

  class FakeNavigationActions : NavigationActions(mock()) {
    var backClicked = false
      private set

    override fun goBack() {
      backClicked = true
    }
  }

  @Test
  fun whenScreenLoads_showsLoadingState() {
    fakeViewModel.setState(ProfileUiState.Loading)

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule.onNodeWithTag("loading_indicator").assertExists()
  }

  @Test
  fun whenProfileLoaded_showsProfileContent() {
    val testProfile =
        Profile(
            id = testUserId,
            username = "TestUser",
            followers = 100,
            following = 200,
            publications = listOf(Publication(id = "pub1", title = "Test Publication")))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule
        .onNodeWithTag("user_profile_image_container", useUnmergedTree = true)
        .assertExists()
    composeTestRule.onNodeWithTag("stats_row", useUnmergedTree = true).assertExists()
    composeTestRule
        .onNodeWithTag("stat_count_Followers", useUnmergedTree = true)
        .assertTextContains("100")
    composeTestRule
        .onNodeWithTag("stat_count_Following", useUnmergedTree = true)
        .assertTextContains("200")
  }

  @Test
  fun whenError_showsErrorMessage() {
    fakeViewModel.setState(ProfileUiState.Error("Test error"))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule.onNodeWithTag("error_message").assertExists().assertTextContains("Test error")
  }

  @Test
  fun whenTabsClicked_switchesContent() {
    val testProfile =
        Profile(
            id = testUserId,
            username = "TestUser",
            publications = listOf(Publication(id = "pub1", title = "Test Publication")))
    val likedPublications =
        listOf(Publication(id = "fav1", title = "Favorite Publication", userId = "testUser"))

    fakeViewModel.setState(ProfileUiState.Success(testProfile))
    fakeViewModel.setLikedPublications(likedPublications)

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule.onNodeWithTag("publications_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_pub1").assertExists()

    composeTestRule.onNodeWithTag("favorites_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertExists()
  }

  @Test
  fun whenBackButtonClicked_callsNavigationGoBack() {
    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule.onNodeWithTag("back_button").performClick()
    assert(fakeNavigationActions.backClicked)
  }

  @Test
  fun whenNoPublications_showsEmptyState() {
    val testProfile = Profile(id = testUserId, username = "TestUser", publications = emptyList())
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule
        .onNodeWithTag("empty_publications_text")
        .assertExists()
        .assertTextContains("No publications yet")
  }

  @Test
  fun verifyTopBarContent() {
    val testProfile = Profile(id = testUserId, username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule.onNodeWithTag("user_profile_username").assertTextContains("TestUser")
  }

  @Test
  fun whenVideoPublication_showsPlayIcon() {
    val videoPublication =
        Publication(
            id = "video1",
            title = "Test Video",
            mediaType = MediaType.VIDEO,
            thumbnailUrl = "https://example.com/thumb.jpg",
            mediaUrl = "https://example.com/video.mp4")
    val testProfile =
        Profile(id = testUserId, username = "TestUser", publications = listOf(videoPublication))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule.onNodeWithTag("video_play_icon_video1", useUnmergedTree = true).assertExists()
  }

  @Test
  fun whenPublicationClicked_showsDetailDialog() {
    val publication =
        Publication(
            id = "pub1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            thumbnailUrl = "https://example.com/thumb.jpg",
            mediaUrl = "https://example.com/photo.jpg")
    val testProfile =
        Profile(id = testUserId, username = "TestUser", publications = listOf(publication))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").performClick()
    composeTestRule.onNodeWithText("Test Publication").assertExists()
  }

  @Test
  fun whenSwitchingTabs_maintainsCorrectPublications() {
    val publication = Publication(id = "pub1", title = "Regular Publication", userId = testUserId)
    val likedPublication =
        Publication(id = "fav1", title = "Favorite Publication", userId = testUserId)

    val testProfile =
        Profile(id = testUserId, username = "TestUser", publications = listOf(publication))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))
    fakeViewModel.setLikedPublications(listOf(likedPublication))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = testUserId)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").assertExists()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertDoesNotExist()

    composeTestRule.onNodeWithTag("favorites_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_pub1").assertDoesNotExist()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertExists()
  }

  @Test
  fun verifyFollowButtonStates() {
    val testProfile =
        Profile(id = "other_user_id", username = "TestUser", isFollowedByCurrentUser = false)

    fakeViewModel.setState(ProfileUiState.Success(testProfile))
    fakeViewModel.setFollowActionState(FollowActionState.Idle)

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = "other_user_id",
          currentUserId = "current_user_id")
    }

    // Test initial Follow button state
    composeTestRule
        .onNodeWithTag("follow_button")
        .assertExists()
        .assertTextContains("Follow")
        .assertIsEnabled()

    // Test loading state
    fakeViewModel.setFollowActionState(FollowActionState.Loading)
    composeTestRule.onNodeWithTag("follow_button").assertExists().assertIsNotEnabled()

    // Test error state
    fakeViewModel.setFollowActionState(FollowActionState.Error("Follow failed"))
    composeTestRule.onNodeWithText("Follow failed").assertExists()

    // Test success state with the new parameters
    val updatedProfile = testProfile.copy(isFollowedByCurrentUser = true)
    fakeViewModel.setState(ProfileUiState.Success(updatedProfile))
    fakeViewModel.setFollowActionState(
        FollowActionState.Success(
            followerId = "current_user_id", targetUserId = "other_user_id", isNowFollowing = true))
    composeTestRule.onNodeWithTag("follow_button").assertExists().assertTextContains("Unfollow")
  }

  @Test
  fun whenFollowButtonClicked_showsLoadingAndUpdatesState() {
    val testProfile =
        Profile(id = "other_user_id", username = "TestUser", isFollowedByCurrentUser = false)

    fakeViewModel.setState(ProfileUiState.Success(testProfile))
    fakeViewModel.setFollowActionState(FollowActionState.Idle)

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel,
          userId = "other_user_id",
          currentUserId = "current_user_id")
    }

    composeTestRule
        .onNodeWithTag("follow_button")
        .assertExists()
        .assertTextContains("Follow")
        .performClick()

    fakeViewModel.setFollowActionState(FollowActionState.Loading)
    composeTestRule.onNodeWithTag("follow_button").assertExists().assertIsNotEnabled()

    // Update with new Success state parameters
    fakeViewModel.setFollowActionState(
        FollowActionState.Success(
            followerId = "current_user_id", targetUserId = "other_user_id", isNowFollowing = true))

    // Test error state
    fakeViewModel.setFollowActionState(FollowActionState.Error("Failed to follow"))
    composeTestRule.onNodeWithText("Failed to follow").assertExists()
  }

  @Test
  fun whenProfileIsPrivate_followersStatShowsToast() {
    // Arrange
    val testProfile =
        Profile(id = testUserId, username = "TestUser", followers = 50, following = 20)
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel = settingsViewModel,
          userId = testUserId,
          currentUserId = "another_user_id" // Simulate non-owner access
          )
    }

    // Act: Click on "Followers" stat
    composeTestRule.onNodeWithTag("followers_stat").performClick()

    // Assert: Restricted message is displayed (toast can't be directly verified)
    composeTestRule.onRoot().printToLog("TestTag")
    composeTestRule.onNodeWithTag("followers_stat").assertHasClickAction()
  }

  @Test
  fun whenProfileIsPrivate_followingStatShowsToast() {
    // Arrange
    val testProfile =
        Profile(id = testUserId, username = "TestUser", followers = 50, following = 20)
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel = settingsViewModel,
          userId = testUserId,
          currentUserId = "another_user_id" // Simulate non-owner access
          )
    }

    // Act: Click on "Following" stat
    composeTestRule.onNodeWithTag("following_stat").performClick()

    // Assert: Restricted message is displayed (toast can't be directly verified)
    composeTestRule.onNodeWithTag("following_stat").assertHasClickAction()
  }

  @Test
  fun whenProfileIsPrivate_showsPrivateProfileMessage() {
    // Arrange
    val testProfile = Profile(id = testUserId, username = "TestUser", publications = emptyList())
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    // Force the profile to be private
    runBlocking {
      (settingsViewModel.settingsRepository as FakeSettingsRepository).setPrivacySettings(
          testUserId, true)
    }

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          settingsViewModel = settingsViewModel,
          userId = testUserId,
          currentUserId = "another_user_id" // Simulate non-owner access
          )
    }

    // Assert: Check that private profile message is displayed
    composeTestRule.onNodeWithTag("private_profile_message").assertExists()
    composeTestRule
        .onNodeWithText("This profile is private. You cannot access publications or favorites.")
        .assertExists()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }
}
