package com.github.se.eduverse.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeViewModel: FakeProfileViewModel
  private lateinit var fakeNavigationActions: FakeNavigationActions

  @Before
  fun setup() {
    fakeViewModel = FakeProfileViewModel()
    fakeNavigationActions = FakeNavigationActions()
  }

  class FakeNavigationActions : NavigationActions(mock()) {
    var backClicked = false
      private set

    var lastNavigatedRoute: String? = null
      private set

    override fun currentRoute(): String = "profile"

    override fun goBack() {
      backClicked = true
    }

    override fun navigateTo(route: String) {
      lastNavigatedRoute = route
    }
  }

  class FakeProfileViewModel : ProfileViewModel(mock()) {
    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    override val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    fun setState(state: ProfileUiState) {
      _profileState.value = state
    }
  }

  @Test
  fun whenScreenLoads_showsLoadingState() {
    fakeViewModel.setState(ProfileUiState.Loading)

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("loading_indicator").assertExists()
  }

  @Test
  fun whenProfileLoaded_showsProfileContent() {
    val testProfile =
        Profile(
            id = "test",
            username = "TestUser",
            followers = 100,
            following = 200,
            publications = listOf(Publication(id = "pub1", title = "Test Publication")))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("profile_image_container").assertExists()
    composeTestRule.onNodeWithTag("stats_row").assertExists()
    composeTestRule.onNodeWithTag("stat_count_Followers").assertTextContains("100")
    composeTestRule.onNodeWithTag("stat_count_Following").assertTextContains("200")
  }

  @Test
  fun whenError_showsErrorMessage() {
    fakeViewModel.setState(ProfileUiState.Error("Test error"))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("error_message").assertExists().assertTextContains("Test error")
  }

  @Test
  fun whenTabsClicked_switchesContent() {
    val testProfile =
        Profile(
            id = "test",
            username = "TestUser",
            publications = listOf(Publication(id = "pub1", title = "Test Publication")),
            favoritePublications = listOf(Publication(id = "fav1", title = "Favorite Publication")))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Check Publications tab
    composeTestRule.onNodeWithTag("publications_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_pub1").assertExists()

    // Switch to Favorites tab
    composeTestRule.onNodeWithTag("favorites_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertExists()
  }

  @Test
  fun whenNoPublications_showsEmptyState() {
    val testProfile =
        Profile(
            id = "test",
            username = "TestUser",
            publications = emptyList(),
            favoritePublications = emptyList())
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule
        .onNodeWithTag("empty_publications_text")
        .assertExists()
        .assertTextContains("No publications yet")
  }

  @Test
  fun whenBackButtonClicked_navigatesBack() {
    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("back_button").performClick()
    assertTrue(fakeNavigationActions.backClicked)
  }

  @Test
  fun whenProfileImageClicked_exists() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("profile_image").assertExists()
  }

  @Test
  fun verifyTopBarContent() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username").assertTextContains("TestUser")
  }

  @Test
  fun whenSettingsButtonClicked_navigatesToSettings() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("settings_button").assertExists()
    composeTestRule.onNodeWithTag("settings_button").performClick()
    assertTrue(fakeNavigationActions.lastNavigatedRoute == Screen.SETTING)
  }

  @Test
  fun whenVideoPublication_showsPlayIcon() {
    val videoPublication = Publication(
      id = "video1",
      title = "Test Video",
      mediaType = MediaType.VIDEO,
      thumbnailUrl = "https://example.com/thumb.jpg",
      mediaUrl = "https://example.com/video.mp4"
    )
    val testProfile = Profile(
      id = "test",
      username = "TestUser",
      publications = listOf(videoPublication)
    )
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("video_play_icon_video1", useUnmergedTree = true).assertExists()
  }

  @Test
  fun whenPhotoPublication_doesNotShowPlayIcon() {
    val photoPublication = Publication(
      id = "photo1",
      title = "Test Photo",
      mediaType = MediaType.PHOTO,
      thumbnailUrl = "https://example.com/thumb.jpg",
      mediaUrl = "https://example.com/photo.jpg"
    )
    val testProfile = Profile(
      id = "test",
      username = "TestUser",
      publications = listOf(photoPublication)
    )
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_photo1")
      .assertExists()
      .onChildren()
      .filterToOne(hasContentDescription("Video"))
      .assertDoesNotExist()
  }

  @Test
  fun whenPublicationClicked_showsDetailDialog() {
    val publication = Publication(
      id = "pub1",
      title = "Test Publication",
      mediaType = MediaType.PHOTO,
      thumbnailUrl = "https://example.com/thumb.jpg",
      mediaUrl = "https://example.com/photo.jpg"
    )
    val testProfile = Profile(
      id = "test",
      username = "TestUser",
      publications = listOf(publication)
    )
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").performClick()
    composeTestRule.onNodeWithText("Test Publication").assertExists()
  }

  @Test
  fun whenDetailDialogDisplayed_hasCloseButton() {
    val publication = Publication(
      id = "pub1",
      title = "Test Publication",
      mediaType = MediaType.PHOTO
    )
    val testProfile = Profile(
      id = "test",
      username = "TestUser",
      publications = listOf(publication)
    )
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").performClick()
    composeTestRule.onNodeWithContentDescription("Close").assertExists()
  }

  @Test
  fun whenPublicationHasNoThumbnail_showsFallback() {
    val publication = Publication(
      id = "pub1",
      title = "Test Publication",
      mediaType = MediaType.PHOTO,
      thumbnailUrl = ""
    )
    val testProfile = Profile(
      id = "test",
      username = "TestUser",
      publications = listOf(publication)
    )
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publication_thumbnail_pub1", useUnmergedTree = true).assertExists()
  }

  @Test
  fun whenSwitchingTabs_maintainsCorrectPublications() {
    val publication = Publication(id = "pub1", title = "Regular Publication")
    val favorite = Publication(id = "fav1", title = "Favorite Publication")
    val testProfile = Profile(
      id = "test",
      username = "TestUser",
      publications = listOf(publication),
      favoritePublications = listOf(favorite)
    )
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Check regular publications
    composeTestRule.onNodeWithTag("publication_item_pub1").assertExists()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertDoesNotExist()

    // Switch to favorites
    composeTestRule.onNodeWithTag("favorites_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_pub1").assertDoesNotExist()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertExists()
  }

  @Test
  fun whenDetailDialogDisplayed_showsCorrectMediaType() {
    val videoPublication = Publication(
      id = "video1",
      title = "Test Video",
      mediaType = MediaType.VIDEO,
      mediaUrl = "https://example.com/video.mp4"
    )
    val photoPublication = Publication(
      id = "photo1",
      title = "Test Photo",
      mediaType = MediaType.PHOTO,
      mediaUrl = "https://example.com/photo.jpg"
    )
    val testProfile = Profile(
      id = "test",
      username = "TestUser",
      publications = listOf(videoPublication, photoPublication)
    )
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Test video publication
    composeTestRule.onNodeWithTag("publication_item_video1").performClick()
    // Should find ExoPlayer for video
    composeTestRule.onAllNodesWithTag("exo_player_view").assertCountEquals(1)

    // Dismiss dialog
    composeTestRule.onNodeWithContentDescription("Close").performClick()

    // Test photo publication
    composeTestRule.onNodeWithTag("publication_item_photo1").performClick()
    // Should find AsyncImage for photo
    composeTestRule.onAllNodesWithTag("detail_photo_view").assertCountEquals(1)
  }
}
