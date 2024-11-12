package com.github.se.eduverse.ui.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

  @Before
  fun setup() {
    fakeViewModel = FakeProfileViewModel()
    fakeNavigationActions = FakeNavigationActions()
  }

  class FakeProfileViewModel : ProfileViewModel(mock()) {
    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    override val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    private val _likedPublications = MutableStateFlow<List<Publication>>(emptyList())
    override val likedPublications: StateFlow<List<Publication>> = _likedPublications.asStateFlow()

    fun setState(state: ProfileUiState) {
      _profileState.value = state
    }

    fun setLikedPublications(publications: List<Publication>) {
      _likedPublications.value = publications
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
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
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
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
    }

    composeTestRule.onNodeWithTag("user_profile_image_container").assertExists()
    composeTestRule.onNodeWithTag("stats_row").assertExists()
    composeTestRule.onNodeWithTag("stat_count_Followers").assertTextContains("100")
    composeTestRule.onNodeWithTag("stat_count_Following").assertTextContains("200")
  }

  @Test
  fun whenError_showsErrorMessage() {
    fakeViewModel.setState(ProfileUiState.Error("Test error"))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
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
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
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
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
    }

    composeTestRule.onNodeWithTag("back_button").performClick()
    assert(fakeNavigationActions.backClicked)
  }

  @Test
  fun whenNoPublications_showsEmptyState() {
    val testProfile =
        Profile(
            id = testUserId,
            username = "TestUser",
            publications = emptyList(),
            favoritePublications = emptyList())
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      UserProfileScreen(
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
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
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
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
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
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
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
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
          navigationActions = fakeNavigationActions, viewModel = fakeViewModel, userId = testUserId)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").assertExists()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertDoesNotExist()

    composeTestRule.onNodeWithTag("favorites_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_pub1").assertDoesNotExist()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertExists()
  }
}
