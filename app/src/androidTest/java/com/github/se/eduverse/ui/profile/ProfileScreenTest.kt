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
import com.github.se.eduverse.viewmodel.DeletePublicationState
import com.github.se.eduverse.viewmodel.ImageUploadState
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.SearchProfileState
import com.github.se.eduverse.viewmodel.UsernameUpdateState
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

  private lateinit var fakeProfileRepository: FakeProfileRepository
  private lateinit var fakeViewModel: FakeProfileViewModel
  private lateinit var fakeNavigationActions: FakeNavigationActions

  @Before
  fun setup() {
    fakeProfileRepository = FakeProfileRepository()
    fakeViewModel = FakeProfileViewModel(fakeProfileRepository)
    fakeNavigationActions = FakeNavigationActions()
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

    // Method to set the profile state for testing
    fun setProfileState(state: ProfileUiState) {
      _profileState.value = state
    }

    // Method to set the liked publications for testing
    fun setLikedPublications(publications: List<Publication>) {
      _likedPublications.value = publications
    }

    // Method to set the image upload state for testing
    fun setImageUploadState(state: ImageUploadState) {
      _imageUploadState.value = state
    }

    fun setState(state: ProfileUiState) {
      _profileState.value = state
    }

    // Method to set the search state for testing
    fun setSearchState(state: SearchProfileState) {
      _searchState.value = state
    }

    // Method to set the username state for testing
    fun setUsernameState(state: UsernameUpdateState) {
      _usernameState.value = state
    }

    // Method to simulate an error for testing
    fun setError(message: String?) {
      _error.value = message
    }

    var wasResetCalled = false
      private set

    override fun resetUsernameState() {
      wasResetCalled = true
      _usernameState.value = UsernameUpdateState.Idle
    }

    fun setDeletePublicationState(state: DeletePublicationState) {
      _deletePublicationState.value = state
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

  fun isProgressIndicator() =
      SemanticsMatcher("is progress indicator") { node ->
        node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) != null
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
            publications = listOf(Publication(id = "pub1", title = "Test Publication")))
    val likedPublications =
        listOf(Publication(id = "fav1", title = "Favorite Publication", userId = "testUser"))

    fakeViewModel.setProfileState(ProfileUiState.Success(testProfile))
    fakeViewModel.setLikedPublications(likedPublications)

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publications_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_pub1").assertExists()

    composeTestRule.onNodeWithTag("favorites_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertExists()
  }

  @Test
  fun whenNoPublications_showsEmptyState() {
    val testProfile = Profile(id = "test", username = "TestUser", publications = emptyList())
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

    composeTestRule
        .onNodeWithTag("profile_username", useUnmergedTree = true)
        .assertTextContains("TestUser")
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
    val videoPublication =
        Publication(
            id = "video1",
            title = "Test Video",
            mediaType = MediaType.VIDEO,
            thumbnailUrl = "https://example.com/thumb.jpg",
            mediaUrl = "https://example.com/video.mp4")
    val testProfile =
        Profile(id = "test", username = "TestUser", publications = listOf(videoPublication))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("video_play_icon_video1", useUnmergedTree = true).assertExists()
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
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule
        .onNodeWithTag("publication_item_photo1")
        .assertExists()
        .onChildren()
        .filterToOne(hasContentDescription("Video"))
        .assertDoesNotExist()
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
        Profile(id = "test", username = "TestUser", publications = listOf(publication))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").performClick()
    composeTestRule.onNodeWithText("Test Publication").assertExists()
  }

  @Test
  fun whenDetailDialogDisplayed_hasCloseButton() {
    val publication =
        Publication(id = "pub1", title = "Test Publication", mediaType = MediaType.PHOTO)
    val testProfile =
        Profile(id = "test", username = "TestUser", publications = listOf(publication))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").performClick()
    composeTestRule.onNodeWithContentDescription("Close").assertExists()
  }

  @Test
  fun whenPublicationHasNoThumbnail_showsFallback() {
    val publication =
        Publication(
            id = "pub1", title = "Test Publication", mediaType = MediaType.PHOTO, thumbnailUrl = "")
    val testProfile =
        Profile(id = "test", username = "TestUser", publications = listOf(publication))
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule
        .onNodeWithTag("publication_thumbnail_pub1", useUnmergedTree = true)
        .assertExists()
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
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publication_item_pub1").assertExists()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertDoesNotExist()

    composeTestRule.onNodeWithTag("favorites_tab").performClick()
    composeTestRule.onNodeWithTag("publication_item_pub1").assertDoesNotExist()
    composeTestRule.onNodeWithTag("publication_item_fav1").assertExists()
  }

  @Test
  fun whenDetailDialogDisplayed_showsCorrectMediaType() {
    val videoPublication =
        Publication(
            id = "video1",
            title = "Test Video",
            mediaType = MediaType.VIDEO,
            mediaUrl = "https://example.com/video.mp4")
    val photoPublication =
        Publication(
            id = "photo1",
            title = "Test Photo",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg")
    val testProfile =
        Profile(
            id = "test",
            username = "TestUser",
            publications = listOf(videoPublication, photoPublication))
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

  @Test
  fun testHeartIconAndLikeCounterPresence() {
    // Créer une publication simulée
    val publication =
        Publication(
            id = "pub1",
            userId = "user1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            thumbnailUrl = "",
            likedBy = listOf("user1"), // Initialement liké par l'utilisateur
            likes = 10)

    // Créer une instance simulée du ViewModel
    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "user1",
          onDismiss = {})
    }

    // Attendre que l'interface se stabilise
    composeTestRule.waitForIdle()

    // Vérifier que l'icône de like est présente
    composeTestRule
        .onNodeWithTag("like_button", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Vérifier que le compteur de likes est présent et affiche la bonne valeur
    composeTestRule
        .onNodeWithTag("like_count_pub1", useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("10")
  }

  @Test
  fun testInitialLikeStateAndCount() {
    // Create a publication that is already liked by the current user
    val publication =
        Publication(
            id = "pub1",
            userId = "user1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            thumbnailUrl = "",
            likedBy = listOf("currentUser"),
            likes = 42)

    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {})
    }
    composeTestRule.waitForIdle()
    // Check initial liked state
    composeTestRule
        .onNodeWithTag("liked_icon", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Verify like count
    composeTestRule
        .onNodeWithTag("like_count_pub1", useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("42")
  }

  @Test
  fun testLikeButtonClickWhenInitiallyNotLiked() {
    // Create a publication that is not liked initially
    val publication =
        Publication(
            id = "pub1",
            userId = "user1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            thumbnailUrl = "",
            likedBy = emptyList(),
            likes = 10)

    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {})
    }

    composeTestRule.waitForIdle()

    // Verify initial unlike state
    composeTestRule
        .onNodeWithTag("unliked_icon", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Click the like button
    composeTestRule.onNodeWithTag("like_button").performClick()

    composeTestRule.waitForIdle()

    // Verify the icon changed to liked state
    composeTestRule
        .onNodeWithTag("liked_icon", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Verify like count increased
    composeTestRule
        .onNodeWithTag("like_count_pub1", useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("11")
  }

  @Test
  fun testLikeButtonClickWhenInitiallyLiked() {
    // Create a publication that is already liked
    val publication =
        Publication(
            id = "pub1",
            userId = "user1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            thumbnailUrl = "",
            likedBy = listOf("currentUser"),
            likes = 10)

    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {})
    }

    composeTestRule.waitForIdle()

    // Verify initial liked state
    composeTestRule
        .onNodeWithTag("liked_icon", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Click the like button
    composeTestRule.onNodeWithTag("like_button", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify the icon changed to unliked state
    composeTestRule
        .onNodeWithTag("unliked_icon", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Verify like count decreased
    composeTestRule
        .onNodeWithTag("like_count_pub1", useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("9")
  }

  @Test
  fun testLikeCountUpdatesCorrectly() {
    // Create a publication with initial likes
    val publication =
        Publication(
            id = "pub1",
            userId = "user1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            thumbnailUrl = "",
            likedBy = emptyList(),
            likes = 5)

    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {})
    }

    composeTestRule.waitForIdle()

    // Verify initial count
    composeTestRule
        .onNodeWithTag("like_count_pub1", useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("5")

    // Like the publication
    composeTestRule.onNodeWithTag("like_button", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify count increased
    composeTestRule
        .onNodeWithTag("like_count_pub1", useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("6")

    // Unlike the publication
    composeTestRule.onNodeWithTag("like_button", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify count decreased
    composeTestRule
        .onNodeWithTag("like_count_pub1", useUnmergedTree = true)
        .assertExists()
        .assertTextEquals("5")
  }

  @Test
  fun testLikeButtonIconColor() {
    // Create a publication
    val publication =
        Publication(
            id = "pub1",
            userId = "user1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://example.com/photo.jpg",
            thumbnailUrl = "",
            likedBy = listOf("currentUser"),
            likes = 10)

    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {})
    }

    composeTestRule.waitForIdle()

    // Find the like button icon and verify its color when liked
    composeTestRule
        .onNodeWithTag("liked_icon", useUnmergedTree = true)
        .assertExists()
        .assertHasNoClickAction()

    // Click to unlike
    composeTestRule.onNodeWithTag("like_button", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify icon color changed for unliked state
    composeTestRule
        .onNodeWithTag("unliked_icon", useUnmergedTree = true)
        .assertExists()
        .assertHasNoClickAction()
  }

  private fun hasSetTextAction() =
      SemanticsMatcher("has SetText action") { node ->
        node.config.getOrNull(SemanticsProperties.EditableText) != null
      }

  private fun targetOutlinedTextFieldValue() =
      SemanticsMatcher("OutlinedTextField with value property") { node ->
        node.config.getOrNull(SemanticsProperties.EditableText) != null &&
            node.config.getOrNull(SemanticsProperties.TextSelectionRange) != null
      }

  @Test
  fun whenUsernameClicked_showsEditDialog() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Click on username row
    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()

    // Verify dialog appears
    composeTestRule.onNodeWithTag("username_edit_dialog", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Edit Username", useUnmergedTree = true).assertExists()
  }

  @Test
  fun usernameDialog_showsCurrentUsername() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Open dialog
    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()

    // Find the input field specifically
    composeTestRule
        .onNode(hasTestTag("username_edit_dialog"))
        .onChildren()
        .filterToOne(hasSetTextAction())
        .assert(hasText("TestUser"))
  }

  @Test
  fun usernameDialog_initialSaveButtonDisabled() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("submit_button", useUnmergedTree = true).assertIsNotEnabled()
  }

  @Test
  fun whenUsernameChanged_enablesSaveButton() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()

    // Find and interact with the input field specifically
    composeTestRule
        .onNode(hasTestTag("username_edit_dialog"))
        .onChildren()
        .filterToOne(hasSetTextAction())
        .performTextInput("NewUsername")

    // Verify save button is enabled
    composeTestRule.onNodeWithTag("submit_button", useUnmergedTree = true).assertIsEnabled()
  }

  @Test
  fun whenLoadingState_showsProgressIndicator() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()

    // Set loading state
    fakeViewModel.setUsernameState(UsernameUpdateState.Loading)

    // Verify loading indicator is shown and save button is disabled
    composeTestRule
        .onNode(hasTestTag("username_edit_dialog"))
        .onChildren()
        .filterToOne(isProgressIndicator())
        .assertExists()
    composeTestRule.onNodeWithTag("submit_button").assertIsNotEnabled()
  }

  @Test
  fun whenErrorState_showsErrorMessage() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()

    // Set error state
    fakeViewModel.setUsernameState(UsernameUpdateState.Error("Username already taken"))

    // Verify error message is shown
    composeTestRule.onNodeWithText("Username already taken", useUnmergedTree = true).assertExists()
  }

  @Test
  fun whenSuccessState_dialogDismisses() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()

    // Verify dialog is shown
    composeTestRule.onNodeWithTag("username_edit_dialog", useUnmergedTree = true).assertExists()

    // Set success state
    fakeViewModel.setUsernameState(UsernameUpdateState.Success)

    // Verify dialog is dismissed
    composeTestRule
        .onNodeWithTag("username_edit_dialog", useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun whenCancelClicked_dialogDismissesAndResetsState() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("cancel_button", useUnmergedTree = true).performClick()

    // Verify dialog is dismissed
    composeTestRule
        .onNodeWithTag("username_edit_dialog", useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun whenDialogDismissed_resetsUsernameState() {
    val testProfile = Profile(id = "test", username = "TestUser")
    fakeViewModel.setState(ProfileUiState.Success(testProfile))

    composeTestRule.setContent {
      ProfileScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Open dialog and set error state
    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()

    // Verify dialog is shown
    composeTestRule.onNodeWithTag("username_edit_dialog", useUnmergedTree = true).assertExists()

    // Set error state
    fakeViewModel.setUsernameState(UsernameUpdateState.Error("Some error"))

    // Wait for state update
    composeTestRule.waitForIdle()

    // Verify error is shown
    composeTestRule.onNodeWithText("Some error", useUnmergedTree = true).assertExists()

    // Close dialog
    composeTestRule.onNodeWithTag("cancel_button", useUnmergedTree = true).performClick()

    // Wait for dialog to close
    composeTestRule.waitForIdle()

    // Verify reset was called
    assertTrue("resetUsernameState should have been called", fakeViewModel.wasResetCalled)

    // Verify the dialog is closed
    composeTestRule
        .onNodeWithTag("username_edit_dialog", useUnmergedTree = true)
        .assertDoesNotExist()

    // Reopen dialog
    composeTestRule.onNodeWithTag("profile_username", useUnmergedTree = true).performClick()

    // Verify new dialog is in clean state
    composeTestRule.onNodeWithText("Some error", useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun whenUserOwnsPublication_showsDeleteButton() {
    val publication =
        Publication(
            id = "pub1",
            userId = "currentUser", // Same as currentUserId to test owner case
            title = "Test Publication",
            mediaType = MediaType.PHOTO)
    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {})
    }

    // Verify delete button is shown for owner
    composeTestRule.onNodeWithTag("delete_button").assertExists()
  }

  @Test
  fun whenUserDoesNotOwnPublication_hideDeleteButton() {
    val publication =
        Publication(
            id = "pub1",
            userId = "otherUser", // Different from currentUserId
            title = "Test Publication",
            mediaType = MediaType.PHOTO)
    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {})
    }

    // Verify delete button is not shown for non-owner
    composeTestRule.onNodeWithTag("delete_button").assertDoesNotExist()
  }

  @Test
  fun whenDeleteButtonClicked_showsConfirmationDialog() {
    val publication =
        Publication(
            id = "pub1",
            userId = "currentUser",
            title = "Test Publication",
            mediaType = MediaType.PHOTO)
    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {})
    }

    // Click delete button
    composeTestRule.onNodeWithTag("delete_button").performClick()

    // Verify confirmation dialog appears
    composeTestRule.onNodeWithText("Delete Publication").assertExists()
    composeTestRule
        .onNodeWithText(
            "Are you sure you want to delete this publication? This action cannot be undone.")
        .assertExists()
    composeTestRule.onAllNodesWithText("Delete")[0].assertExists()
    composeTestRule.onAllNodesWithText("Cancel")[0].assertExists()
  }

  @Test
  fun whenDeleteConfirmed_callsDeletePublication() {
    val publication =
        Publication(
            id = "pub1",
            userId = "currentUser",
            title = "Test Publication",
            mediaType = MediaType.PHOTO)
    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = {})
    }

    // Click delete button and confirm
    composeTestRule.onNodeWithTag("delete_button").performClick()
    composeTestRule.onAllNodesWithText("Delete")[0].performClick()

    // Verify deletePublication was called
    assertTrue(fakeViewModel.deletePublicationCalled)
  }

  @Test
  fun whenDeleteSucceeds_dismissesDialog() {
    val publication =
        Publication(
            id = "pub1",
            userId = "currentUser",
            title = "Test Publication",
            mediaType = MediaType.PHOTO)
    val fakeViewModel = FakeProfileViewModel(fakeProfileRepository)
    var dialogDismissed = false

    composeTestRule.setContent {
      PublicationDetailDialog(
          publication = publication,
          profileViewModel = fakeViewModel,
          currentUserId = "currentUser",
          onDismiss = { dialogDismissed = true })
    }

    // Click delete button and confirm
    composeTestRule.onNodeWithTag("delete_button").performClick()
    composeTestRule.onAllNodesWithText("Delete")[0].performClick()

    // Simulate successful deletion and wait for UI update
    fakeViewModel.setDeletePublicationState(DeletePublicationState.Success)
    composeTestRule.waitForIdle()

    // Verify dialog was dismissed
    assertTrue(dialogDismissed)
  }
}
