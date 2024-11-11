import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.VideoScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.profile.ProfileScreenTest.FakeProfileViewModel
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.PublicationViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class VideoScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeViewModell:
      com.github.se.eduverse.ui.profile.ProfileScreenTest.FakeProfileViewModel

  @Before
  fun setup() {
    fakeViewModell = com.github.se.eduverse.ui.profile.ProfileScreenTest.FakeProfileViewModel()
  }

  class FakePublicationViewModel(initialPublications: List<Publication>) :
      PublicationViewModel(mockk(relaxed = true)) {
    private val _publications = MutableStateFlow(initialPublications)

    override val publications: StateFlow<List<Publication>>
      get() = _publications

    override suspend fun loadMorePublications() {
      // Does nothing for this test
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
  fun testLoadingIndicatorIsDisplayedWhenPublicationsAreEmpty() {
    // Create mock NavigationActions
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    every { navigationActions.currentRoute() } returns "video"

    // Use FakePublicationViewModel with an empty list
    val fakeViewModel = FakePublicationViewModel(emptyList())

    // Set content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = navigationActions,
          publicationViewModel = fakeViewModel,
          profileViewModel = fakeViewModell)
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Check that the loading indicator is displayed
    composeTestRule.onNodeWithTag("LoadingIndicator").assertExists().assertIsDisplayed()
  }

  @Test
  fun testVerticalPagerIsDisplayedWhenPublicationsAreNotEmpty() {
    // Create mock NavigationActions
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    every { navigationActions.currentRoute() } returns "video"

    // Non-empty list of publications
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://www.sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis()),
            Publication(
                id = "2",
                userId = "user2",
                title = "Test Photo",
                mediaType = MediaType.PHOTO,
                mediaUrl = "",
                thumbnailUrl = "https://via.placeholder.com/150",
                timestamp = System.currentTimeMillis()))

    // Use FakePublicationViewModel with non-empty publications
    val fakeViewModel = FakePublicationViewModel(publications)

    // Set content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = navigationActions,
          publicationViewModel = fakeViewModel,
          profileViewModel = fakeViewModell)
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Check that the VerticalPager is displayed
    composeTestRule.onNodeWithTag("VerticalPager").assertExists().assertIsDisplayed()
  }

  @Test
  fun testCorrectDisplayOfPublications() {
    // Create mock NavigationActions
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    every { navigationActions.currentRoute() } returns "video"

    // List of publications with different types (video and photo)
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://www.sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis()),
            Publication(
                id = "2",
                userId = "user2",
                title = "Test Photo",
                mediaType = MediaType.PHOTO,
                mediaUrl = "",
                thumbnailUrl = "https://via.placeholder.com/150",
                timestamp = System.currentTimeMillis()))

    // Use FakePublicationViewModel with non-empty publications
    val fakeViewModel = FakePublicationViewModel(publications)

    // Set content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = navigationActions,
          publicationViewModel = fakeViewModel,
          profileViewModel = fakeViewModell)
    }

    // Wait for the UI to stabilize and ensure VideoScreen is visible
    composeTestRule.onNodeWithTag("VideoScreen").assertExists().assertIsDisplayed()

    // Check the correct display of the first item (video)
    composeTestRule.waitUntil {
      composeTestRule.onAllNodesWithTag("VideoItem").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("VideoItem").assertIsDisplayed()

    // Swipe to the second page (photo)
    composeTestRule.onNodeWithTag("VerticalPager").performTouchInput { swipeUp() }

    // Check the correct display of the second item (photo)
    composeTestRule.waitUntil {
      composeTestRule.onAllNodesWithTag("PhotoItem").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("PhotoItem").assertIsDisplayed()
  }

  @Test
  fun testPaginationLoadMorePublications() {
    // Create mock NavigationActions
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    every { navigationActions.currentRoute() } returns "video"

    // Initial list of publications with a few items
    val initialPublications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Video 1",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://www.sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis()),
            Publication(
                id = "2",
                userId = "user2",
                title = "Video 2",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://www.sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis()))

    // Create a spy for PublicationViewModel
    val fakeViewModel = spyk(FakePublicationViewModel(initialPublications))

    // Set content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = navigationActions,
          publicationViewModel = fakeViewModel,
          profileViewModel = fakeViewModell)
    }

    // Wait for the UI to stabilize
    composeTestRule.onNodeWithTag("VideoScreen").assertExists().assertIsDisplayed()

    // Simulate scrolling to the last publication to trigger loading more
    composeTestRule.onNodeWithTag("VerticalPager").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()

    // Verify that loadMorePublications() is called when the last page is reached
    verify { runBlocking { fakeViewModel.loadMorePublications() } }
  }

  @Test
  fun testBottomNavigationMenuIsDisplayed() {
    // Create mock NavigationActions
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    every { navigationActions.currentRoute() } returns "video"

    // Create a mock ViewModel with some publications to populate the screen
    val fakeViewModel =
        FakePublicationViewModel(
            listOf(
                Publication(
                    id = "1",
                    userId = "user1",
                    title = "Test Video",
                    mediaType = MediaType.VIDEO,
                    mediaUrl = "https://www.sample-videos.com/video123/mp4/480/asdasdas.mp4",
                    thumbnailUrl = "",
                    timestamp = System.currentTimeMillis())))

    // Set content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = navigationActions,
          publicationViewModel = fakeViewModel,
          profileViewModel = fakeViewModell)
    }

    // Wait for the UI to stabilize
    composeTestRule.onNodeWithTag("VideoScreen").assertExists().assertIsDisplayed()

    // Check that the BottomNavigationMenu is displayed
    composeTestRule.onNodeWithTag("bottomNavigationMenu").assertExists().assertIsDisplayed()
  }

  @Test
  fun testErrorIndicatorIsDisplayedOnLoadingFailure() {
    // Create mock NavigationActions
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    every { navigationActions.currentRoute() } returns "video"

    // Create a mock ViewModel with an initial error state
    val fakeViewModel =
        object : PublicationViewModel(mockk()) {
          override val publications = MutableStateFlow<List<Publication>>(emptyList())
          override val error = MutableStateFlow("Failed to load publications")
        }

    // Set content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = navigationActions,
          publicationViewModel = fakeViewModel,
          profileViewModel = fakeViewModell)
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Check that the error indicator is displayed
    composeTestRule.onNodeWithTag("ErrorIndicator").assertExists().assertIsDisplayed()
  }

  @Test
  fun testPublicationsAreNotReloadedUnnecessarilyOnLifecycleChanges() {
    // Create mock NavigationActions
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    every { navigationActions.currentRoute() } returns "video"

    // Use a mock of PublicationViewModel to monitor calls to loadMorePublications()
    val fakeViewModel =
        spyk(
            FakePublicationViewModel(
                listOf(
                    Publication(
                        id = "1",
                        userId = "user1",
                        title = "Test Video",
                        mediaType = MediaType.VIDEO,
                        mediaUrl = "https://www.sample-videos.com/video123/mp4/480/asdasdas.mp4",
                        thumbnailUrl = "",
                        timestamp = System.currentTimeMillis()))))

    // Set content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = navigationActions,
          publicationViewModel = fakeViewModel,
          profileViewModel = fakeViewModell)
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Verify that an initial call to loadMorePublications() was made once
    verify(exactly = 1) { runBlocking { fakeViewModel.loadMorePublications() } }

    // Simulate a lifecycle change (app backgrounded then foregrounded)
    composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
    composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Verify that no additional calls to loadMorePublications() are made
    verify(exactly = 1) { runBlocking { fakeViewModel.loadMorePublications() } }
  }

  @Test
  fun testLikeButtonChangesStateCorrectly() {
    // Create mock NavigationActions
    val navigationActions = mockk<NavigationActions>(relaxed = true)
    every { navigationActions.currentRoute() } returns "video"

    // List of publications with a single item for the test
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://www.sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis(),
                likedBy = emptyList() // Initially not liked
                ))

    // Use FakePublicationViewModel with the publication
    val fakeViewModel = FakePublicationViewModel(publications)

    // Set content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = navigationActions,
          publicationViewModel = fakeViewModel,
          profileViewModel = fakeViewModell)
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Check that the `UnlikedIcon` is present initially
    composeTestRule
        .onNodeWithTag("UnlikedIcon_0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Simulate a click on the like button
    composeTestRule.onNodeWithTag("LikeButton_0", useUnmergedTree = true).performClick()

    // Wait for the UI to stabilize after the interaction
    composeTestRule.waitForIdle()

    // Check that the icon is now `LikedIcon`
    composeTestRule
        .onNodeWithTag("LikedIcon_0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag("LikeButton_0", useUnmergedTree = true).performClick()

    // Wait for the UI to stabilize after the interaction
    composeTestRule.waitForIdle()

    // Check that the icon is now `UnlikedIcon`
    composeTestRule
        .onNodeWithTag("UnlikedIcon_0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }
}
