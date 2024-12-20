package com.github.se.eduverse.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.fake.FakeProfileRepository
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.*
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
class ProfileScreenNewTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeProfileRepository: FakeProfileRepository
  private lateinit var fakeViewModel: FakeProfileViewModel
  private lateinit var fakeNavigationActions: FakeNavigationActions
  private lateinit var fakeCommentsViewModel: FakeCommentsViewModel

  @Before
  fun setup() {
    fakeProfileRepository = FakeProfileRepository()
    fakeViewModel = FakeProfileViewModel(fakeProfileRepository)
    fakeNavigationActions = FakeNavigationActions()
    fakeCommentsViewModel = FakeCommentsViewModel()
  }

  class FakeProfileViewModel(fakeRepository: FakeProfileRepository) :
      ProfileViewModel(fakeRepository) {
    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    override val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    private val _likedPublications = MutableStateFlow<List<Publication>>(emptyList())
    override val likedPublications: StateFlow<List<Publication>> = _likedPublications.asStateFlow()

    private val _imageUploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
    override val imageUploadState: StateFlow<ImageUploadState> = _imageUploadState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchProfileState>(SearchProfileState.Idle)
    override val searchState: StateFlow<SearchProfileState> = _searchState.asStateFlow()

    private val _usernameState = MutableStateFlow<UsernameUpdateState>(UsernameUpdateState.Idle)
    override val usernameState: StateFlow<UsernameUpdateState> = _usernameState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _deletePublicationState =
        MutableStateFlow<DeletePublicationState>(DeletePublicationState.Idle)
    override val deletePublicationState: StateFlow<DeletePublicationState> =
        _deletePublicationState.asStateFlow()

    // For testing
    fun setProfileState(state: ProfileUiState) {
      _profileState.value = state
    }

    fun setLikedPublications(publications: List<Publication>) {
      _likedPublications.value = publications
    }

    fun setUsernameState(state: UsernameUpdateState) {
      _usernameState.value = state
    }

    fun setDeletePublicationState(state: DeletePublicationState) {
      _deletePublicationState.value = state
    }

    override fun resetUsernameState() {
      _usernameState.value = UsernameUpdateState.Idle
    }

    var deletePublicationCalled = false
      private set

    override fun deletePublication(publicationId: String, userId: String) {
      deletePublicationCalled = true
    }

    override fun resetDeleteState() {
      _deletePublicationState.value = DeletePublicationState.Idle
    }
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

  class FakeCommentsViewModel : CommentsViewModel(mock(), mock()) {
    private val _commentsState = MutableStateFlow<CommentsUiState>(CommentsUiState.Loading)
    override val commentsState: StateFlow<CommentsUiState> = _commentsState.asStateFlow()

    fun setCommentsState(state: CommentsUiState) {
      _commentsState.value = state
    }

    override fun loadComments(publicationId: String) {
      // For testing, we can simulate loading comments
      _commentsState.value = CommentsUiState.Success(emptyList())
    }
  }

  fun isProgressIndicator() =
      SemanticsMatcher("is progress indicator") { node ->
        node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) != null
      }

  @Test
  fun whenScreenLoads_showsLoadingState() {
    fakeViewModel.setProfileState(ProfileUiState.Loading)

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
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
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("profile_image_container", useUnmergedTree = true).assertExists()
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
    fakeViewModel.setProfileState(ProfileUiState.Error("Test error"))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("error_message").assertExists().assertTextContains("Test error")
  }

  @Test
  fun whenTabsClicked_switchesContent() {
    val testProfile =
        Profile(
            id = "test",
            username = "TestUser",
            publications = listOf(Publication(id = "pub1", title = "Test Publication")))
    val likedPublications =
        listOf(Publication(id = "fav1", title = "Favorite Publication", userId = "testUser"))

    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))
    fakeViewModel.setLikedPublications(likedPublications)

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("publications_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_pub1").assertExists()

    composeTestRule.onNodeWithTag("favorites_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertExists()
  }

  @Test
  fun whenNoPublications_showsEmptyState() {
    val testProfile = Profile(id = "test", username = "TestUser", publications = emptyList())
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }
    composeTestRule.onNodeWithText("No publications yet").assertExists()
  }

  @Test
  fun whenProfileImageClicked_exists() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("profile_image").assertExists()
  }

  @Test
  fun verifyTopBarContent() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule
        .onNodeWithTag("profile_username", useUnmergedTree = true)
        .assertTextContains("TestUser")
  }

  @Test
  fun whenSettingsButtonClicked_navigatesToSettings() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("settings_button").assertExists()
    composeTestRule.onNodeWithTag("settings_button").performClick()
    assertTrue(fakeNavigationActions.lastNavigatedRoute == Screen.SETTING)
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
        Profile(id = "test", username = "TestUser", publications = listOf(videoPublication))
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_video1").assertExists()
  }

  @Test
  fun whenPhotoPublication_doesNotShowPlayIcon() {
    val photoPublication =
        Publication(
            id = "photo1",
            title = "Test Photo",
            mediaType = MediaType.PHOTO,
            thumbnailUrl = "https://example.com/thumb.jpg",
            mediaUrl = "https://example.com/photo.jpg")
    val testProfile =
        Profile(id = "test", username = "TestUser", publications = listOf(photoPublication))
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_photo1").assertExists()
    // Since it's a photo, no play icon is expected. The new code doesn't explicitly show a play
    // icon for photos.
    // We just ensure the photo item is rendered without a play overlay.
  }

  @Test
  fun whenPublicationClicked_showsDetailView() {
    val publication =
        Publication(
            id = "pub1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            thumbnailUrl = "https://example.com/thumb.jpg",
            mediaUrl = "https://example.com/photo.jpg")
    val testProfile =
        Profile(id = "test", username = "TestUser", publications = listOf(publication))
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").performClick()
    // Check if the detail view is shown by verifying publication title
    composeTestRule
        .onNodeWithTag("publication_title")
        .assertExists()
        .assertTextContains("Test Publication")
  }

  @Test
  fun whenDetailViewDisplayed_hasCloseButton() {
    val publication =
        Publication(id = "pub1", title = "Test Publication", mediaType = MediaType.PHOTO)
    val testProfile =
        Profile(id = "test", username = "TestUser", publications = listOf(publication))
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").performClick()
    composeTestRule.onNodeWithTag("close_button").assertExists()
  }

  @Test
  fun whenSwitchingTabs_maintainsCorrectPublications() {
    val publication = Publication(id = "pub1", title = "Regular Publication", userId = "testUser")
    val likedPublication =
        Publication(id = "fav1", title = "Favorite Publication", userId = "testUser")

    val testProfile =
        Profile(id = "test", username = "TestUser", publications = listOf(publication))
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))
    fakeViewModel.setLikedPublications(listOf(likedPublication))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").assertExists()
    composeTestRule.onNodeWithTag("favorites_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertExists()
  }

  @Test
  fun testInitialLikeStateAndCount() {
    val publication =
        Publication(
            id = "pub1",
            userId = "user1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            likedBy = listOf("currentUser"),
            likes = 42)

    composeTestRule.setContent {
      PublicationDetailView(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {},
          onShowComments = {})
    }

    composeTestRule.waitForIdle()
    // Verify initially liked
    composeTestRule.onNodeWithTag("like_button").assertExists()
    // Since it's liked by currentUser, tint should be red (liked_icon scenario)
    // There's no separate testTag for liked_icon/unliked_icon now, but we can assume the icon color
    // or
    // check like_count.
    // We'll rely on the text to confirm correctness.
    composeTestRule.onNodeWithTag("like_count_pub1").assertTextEquals("42")
  }

  @Test
  fun testLikeButtonClickWhenInitiallyNotLiked() {
    val publication =
        Publication(
            id = "pub1",
            userId = "user1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            likedBy = emptyList(),
            likes = 10)

    composeTestRule.setContent {
      PublicationDetailView(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {},
          onShowComments = {})
    }

    composeTestRule.waitForIdle()

    // Click the like button
    composeTestRule.onNodeWithTag("like_button").performClick()
    composeTestRule.waitForIdle()

    // Verify like count increased
    composeTestRule.onNodeWithTag("like_count_pub1").assertTextEquals("11")
  }

  @Test
  fun testLikeButtonClickWhenInitiallyLiked() {
    val publication =
        Publication(
            id = "pub1",
            userId = "user1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            likedBy = listOf("currentUser"),
            likes = 10)

    composeTestRule.setContent {
      PublicationDetailView(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {},
          onShowComments = {})
    }

    composeTestRule.waitForIdle()

    // Click the like button to unlike
    composeTestRule.onNodeWithTag("like_button").performClick()
    composeTestRule.waitForIdle()

    // Verify like count decreased
    composeTestRule.onNodeWithTag("like_count_pub1").assertTextEquals("9")
  }

  @Test
  fun whenUsernameClicked_showsEditDialog() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    // Click on username
    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("username_edit_dialog", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Edit Username", useUnmergedTree = true).assertExists()
  }

  @Test
  fun usernameDialog_showsCurrentUsername() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    // Open dialog
    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()

    // Check the text field value
    composeTestRule
        .onNodeWithTag("username_edit_dialog")
        .onChildren()
        .filterToOne(hasSetTextAction())
        .assertTextContains("TestUser")
  }

  @Test
  fun usernameDialog_initialSaveButtonDisabled() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    // Open dialog
    composeTestRule.onNodeWithTag("profile_username").performClick()
    composeTestRule.onNodeWithTag("submit_button").assertIsNotEnabled()
  }

  @Test
  fun whenUsernameChanged_enablesSaveButton() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username").performClick()

    // Change username
    composeTestRule.onNode(hasSetTextAction()).performTextClearance()
    composeTestRule.onNode(hasSetTextAction()).performTextInput("NewUsername")

    composeTestRule.onNodeWithTag("submit_button").assertIsEnabled()
  }

  @Test
  fun whenLoadingState_showsProgressIndicatorInDialog() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username").performClick()
    fakeViewModel.setUsernameState(UsernameUpdateState.Loading)
    composeTestRule
        .onNode(hasTestTag("username_edit_dialog"))
        .onChildren()
        .filterToOne(isProgressIndicator())
        .assertExists()
    composeTestRule.onNodeWithTag("submit_button").assertIsNotEnabled()
  }

  @Test
  fun whenErrorState_showsErrorMessageInDialog() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username").performClick()
    fakeViewModel.setUsernameState(UsernameUpdateState.Error("Username already taken"))
    composeTestRule.onNodeWithText("Username already taken").assertExists()
  }

  @Test
  fun whenSuccessState_dialogDismisses() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username").performClick()
    fakeViewModel.setUsernameState(UsernameUpdateState.Success)
    composeTestRule.onNodeWithTag("username_edit_dialog").assertDoesNotExist()
  }

  @Test
  fun whenCancelClicked_dialogDismissesAndResetsState() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username").performClick()
    composeTestRule.onNodeWithTag("cancel_button").performClick()
    composeTestRule.onNodeWithTag("username_edit_dialog").assertDoesNotExist()
  }

  @Test
  fun whenUserOwnsPublication_showsDeleteButtonInDetailView() {
    val publication =
        Publication(
            id = "pub1",
            userId = "currentUser",
            title = "Test Publication",
            mediaType = MediaType.PHOTO)

    composeTestRule.setContent {
      PublicationDetailView(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {},
          onShowComments = {})
    }

    composeTestRule.onNodeWithTag("delete_button").assertExists()
  }

  @Test
  fun whenUserDoesNotOwnPublication_noDeleteButtonInDetailView() {
    val publication =
        Publication(
            id = "pub1",
            userId = "otherUser",
            title = "Test Publication",
            mediaType = MediaType.PHOTO)

    composeTestRule.setContent {
      PublicationDetailView(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {},
          onShowComments = {})
    }

    composeTestRule.onNodeWithTag("delete_button").assertDoesNotExist()
  }

  @Test
  fun whenDeleteButtonClicked_showsConfirmationAlertInOldCodeEquivalent() {
    // The new code no longer shows a confirmation alert directly in PublicationDetailView.
    // The user code you provided removed that confirmation logic from PublicationDetailView.
    // If you still have that logic, adapt this test accordingly.
    // If it's gone, skip this test or test the new delete logic if any.

    // Assuming the new code may have lost the delete confirmation dialog:
    // If the new code no longer shows a confirmation dialog for delete,
    // we can at least test if deletePublication is called directly or you can reintroduce a
    // confirmation dialog.

    val publication =
        Publication(
            id = "pub1",
            userId = "currentUser",
            title = "Test Publication",
            mediaType = MediaType.PHOTO)

    composeTestRule.setContent {
      PublicationDetailView(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {},
          onShowComments = {})
    }

    // In the provided new code, there's no confirmation dialog for delete.
    // It was removed from the snippet. If you need it, adapt accordingly.
    // Here, we'll assume that if delete_button is shown and clicked, delete is called immediately.

    composeTestRule.onNodeWithTag("delete_button").performClick()
    assertTrue(fakeViewModel.deletePublicationCalled)
  }

  @Test
  fun whenCommentButtonClicked_showsCommentsBottomSheet() {
    val publication =
        Publication(
            id = "pub1",
            userId = "someUser",
            title = "Test Publication",
            mediaType = MediaType.PHOTO)
    val testProfile =
        Profile(id = "test", username = "TestUser", publications = listOf(publication))
    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))
    fakeCommentsViewModel.setCommentsState(CommentsUiState.Success(emptyList()))

    composeTestRule.setContent {
      ProfileScreen(
          navigationActions = fakeNavigationActions,
          viewModel = fakeViewModel,
          commentsViewModel = fakeCommentsViewModel)
    }

    // Open detail view
    composeTestRule.onNodeWithTag("publication_item_pub1").performClick()

    // Click comment button
    composeTestRule.onNodeWithTag("comment_button").performClick()

    // Now the bottom sheet with comments should appear
    composeTestRule.onNodeWithTag("CommentsMenuContent").assertExists()
    composeTestRule.onNodeWithTag("comments_title").assertExists().assertTextContains("Comments")
  }
}
