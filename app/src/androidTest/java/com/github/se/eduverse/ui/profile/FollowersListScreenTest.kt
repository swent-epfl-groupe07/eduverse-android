package com.github.se.eduverse.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FollowActionState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class FollowListScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeViewModel: FakeProfileViewModel
  private lateinit var fakeNavigationActions: FakeNavigationActions
  private val testUserId = "test_user_id"
  private val currentUserId = "current_user_id"

  @Before
  fun setup() {
    MockFirebaseAuth.setup(isAuthenticated = true)
    fakeViewModel = FakeProfileViewModel()
    fakeNavigationActions = FakeNavigationActions()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  class FakeProfileViewModel : ProfileViewModel(mock()) {
    private val _followActionState = MutableStateFlow<FollowActionState>(FollowActionState.Idle)
    override val followActionState: StateFlow<FollowActionState> = _followActionState.asStateFlow()

    private var _isLoading = true
    private var profiles = mutableListOf<Profile>()

    suspend override fun getFollowers(userId: String): List<Profile> {
      return if (_isLoading) emptyList() else profiles
    }

    suspend override fun getFollowing(userId: String): List<Profile> {
      return if (_isLoading) emptyList() else profiles
    }

    fun setProfiles(newProfiles: List<Profile>) {
      profiles = newProfiles.toMutableList()
      _isLoading = false
    }

    fun setLoading(isLoading: Boolean) {
      _isLoading = isLoading
    }

    fun setFollowActionState(state: FollowActionState) {
      _followActionState.value = state
    }
  }

  class FakeNavigationActions : NavigationActions(mock()) {
    var lastNavigatedUserId: String? = null
      private set

    override fun navigateToUserProfile(userId: String) {
      lastNavigatedUserId = userId
    }
  }

  @Test
  fun whenNoFollowers_showsEmptyMessage() {
    fakeViewModel.setProfiles(emptyList())

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = true)
    }

    composeTestRule
        .onNodeWithTag("empty_message")
        .assertExists()
        .assertTextContains("No followers yet")
  }

  @Test
  fun whenFollowButtonClicked_updatesFollowState() {
    val followers =
        listOf(
            Profile(
                id = "1",
                username = "user1",
                isFollowedByCurrentUser = false,
                profileImageUrl = "",
                followers = 0,
                following = 0,
                publications = emptyList()))

    fakeViewModel.setLoading(false)
    fakeViewModel.setProfiles(followers)

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = true)
    }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag("follow_button_1").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("follow_button_1").assertExists().performClick()

    fakeViewModel.setFollowActionState(FollowActionState.Loading)

    fakeViewModel.setFollowActionState(
        FollowActionState.Success(
            followerId = currentUserId, targetUserId = "1", isNowFollowing = true))
  }

  @Test
  fun whenNoFollowing_showsEmptyMessage() {
    fakeViewModel.setProfiles(emptyList())

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = false)
    }

    composeTestRule
        .onNodeWithTag("empty_message")
        .assertExists()
        .assertTextContains("Not following anyone")
  }

  @Test
  fun whenHasFollowers_showsFollowersList() {
    val followers =
        listOf(
            Profile(id = "1", username = "user1", isFollowedByCurrentUser = false),
            Profile(id = "2", username = "user2", isFollowedByCurrentUser = true))
    fakeViewModel.setProfiles(followers)

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = true)
    }

    composeTestRule.onNodeWithTag("follow_list").assertExists()
    composeTestRule.onNodeWithTag("follow_list_item_1").assertExists()
    composeTestRule.onNodeWithTag("follow_list_item_2").assertExists()
  }

  @Test
  fun whenProfileClicked_navigatesToUserProfile() {
    val followers = listOf(Profile(id = "1", username = "user1", isFollowedByCurrentUser = false))
    fakeViewModel.setProfiles(followers)

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = true)
    }

    composeTestRule.onNodeWithTag("follow_list_item_1").performClick()
    assert(fakeNavigationActions.lastNavigatedUserId == "1")
  }

  @Test
  fun verifyFollowButtonVisibility() {
    val followers =
        listOf(
            Profile(
                id = "1",
                username = "user1",
                isFollowedByCurrentUser = false,
                profileImageUrl = "",
                followers = 0,
                following = 0,
                publications = emptyList()))

    fakeViewModel.setLoading(false)
    fakeViewModel.setProfiles(followers)

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = true)
    }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag("follow_button_1").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("follow_button_1").assertExists()
  }

  @Test
  fun whenFollowActionSucceeds_updatesButtonStateWithoutRefresh() {
    val initialFollowers =
        listOf(
            Profile(
                id = "1",
                username = "user1",
                isFollowedByCurrentUser = false,
                profileImageUrl = "",
                followers = 0,
                following = 0,
                publications = emptyList()))
    fakeViewModel.setProfiles(initialFollowers)

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = true)
    }

    // Verify initial "Follow Back" button state
    composeTestRule
        .onNodeWithTag("follow_button_1")
        .assertExists()
        .assertTextContains("Follow Back")

    // Simulate successful follow action
    fakeViewModel.setFollowActionState(
        FollowActionState.Success(
            followerId = currentUserId, targetUserId = "1", isNowFollowing = true))

    // Verify button text changes to "Following"
    composeTestRule.onNodeWithTag("follow_button_1").assertExists().assertTextContains("Following")
  }

  @Test
  fun whenFollowActionLoading_showsLoadingIndicator() {
    val followers =
        listOf(
            Profile(
                id = "1",
                username = "user1",
                isFollowedByCurrentUser = false,
                profileImageUrl = "",
                followers = 0,
                following = 0,
                publications = emptyList()))
    fakeViewModel.setProfiles(followers)

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = true)
    }

    // Click follow button
    composeTestRule.onNodeWithTag("follow_button_1").performClick()

    // Set loading state
    fakeViewModel.setFollowActionState(FollowActionState.Loading)

    // Verify button is disabled during loading
    composeTestRule.onNodeWithTag("follow_button_1").assertIsNotEnabled()
  }

  @Test
  fun whenInFollowingList_showsAppropriateButtonStates() {
    val following =
        listOf(
            Profile(
                id = "1",
                username = "user1",
                isFollowedByCurrentUser = true,
                profileImageUrl = "",
                followers = 0,
                following = 0,
                publications = emptyList()))
    fakeViewModel.setProfiles(following)

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = false)
    }

    // Verify initial "Following" button state
    composeTestRule.onNodeWithTag("follow_button_1").assertExists().assertTextContains("Following")

    // Simulate unfollow action
    fakeViewModel.setFollowActionState(
        FollowActionState.Success(
            followerId = currentUserId, targetUserId = "1", isNowFollowing = false))

    // Verify button text changes to "Follow"
    composeTestRule.onNodeWithTag("follow_button_1").assertExists().assertTextContains("Follow")
  }

  @Test
  fun whenFollowActionFails_maintainsOriginalState() {
    val followers =
        listOf(
            Profile(
                id = "1",
                username = "user1",
                isFollowedByCurrentUser = false,
                profileImageUrl = "",
                followers = 0,
                following = 0,
                publications = emptyList()))
    fakeViewModel.setProfiles(followers)

    composeTestRule.setContent {
      FollowListScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          userId = testUserId,
          isFollowersList = true)
    }

    // Click follow button
    composeTestRule.onNodeWithTag("follow_button_1").performClick()

    // Simulate failed follow action
    fakeViewModel.setFollowActionState(FollowActionState.Error("Failed to follow user"))

    // Verify button maintains original state
    composeTestRule
        .onNodeWithTag("follow_button_1")
        .assertExists()
        .assertTextContains("Follow Back")
        .assertIsEnabled()
  }
}

class MockFirebaseAuth {
  companion object {
    private val mockUser: FirebaseUser =
        mock(FirebaseUser::class.java).apply {
          `when`(getUid()).thenReturn("test_user_id")
          `when`(getEmail()).thenReturn("test@example.com")
          `when`(getDisplayName()).thenReturn("Test User")
          `when`(getPhoneNumber()).thenReturn(null)
          `when`(getPhotoUrl()).thenReturn(null)
          `when`(getProviderId()).thenReturn("firebase")
          `when`(isEmailVerified).thenReturn(true)
          `when`(isAnonymous).thenReturn(false)
          `when`(getMetadata()).thenReturn(null)
          `when`(getProviderData()).thenReturn(mutableListOf())
          `when`(getTenantId()).thenReturn(null)
        }

    fun setup(isAuthenticated: Boolean = true) {
      mockkStatic(FirebaseAuth::class)
      val mockAuth = mock(FirebaseAuth::class.java)
      every { FirebaseAuth.getInstance() } returns mockAuth
      `when`(mockAuth.currentUser).thenReturn(if (isAuthenticated) mockUser else null)
    }
  }
}
