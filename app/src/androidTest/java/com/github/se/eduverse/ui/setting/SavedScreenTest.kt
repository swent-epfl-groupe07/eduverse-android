package com.github.se.eduverse.ui.saved

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.ProfileRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

class FakeProfileRepository : ProfileRepository {
  private var isFavorited = false
  private val favoritePublications = mutableListOf<Publication>()
  private val favoriteIds = mutableListOf<String>()

  override suspend fun isPublicationFavorited(userId: String, publicationId: String): Boolean {
    return isFavorited
  }

  override suspend fun getFavoritePublicationsIds(userId: String): List<String> {
    return favoriteIds
  }

  override suspend fun getFavoritePublications(favoriteIds: List<String>): List<Publication> {
    return favoritePublications
  }

  fun setFavoritePublications(publications: List<Publication>) {
    favoritePublications.clear()
    favoritePublications.addAll(publications)
    favoriteIds.clear()
    favoriteIds.addAll(publications.map { it.id })
  }

  // Rest of the interface methods with empty implementations
  override suspend fun getProfile(userId: String): Profile? = null

  override suspend fun updateProfile(userId: String, profile: Profile) {}

  override suspend fun addPublication(userId: String, publication: Publication) {}

  override suspend fun removePublication(publicationId: String) {}

  override suspend fun followUser(followerId: String, followedId: String) {}

  override suspend fun unfollowUser(followerId: String, followedId: String) {}

  override suspend fun uploadProfileImage(userId: String, imageUri: Uri): String = ""

  override suspend fun updateProfileImage(userId: String, imageUrl: String) {}

  override suspend fun searchProfiles(query: String, limit: Int): List<Profile> = emptyList()

  override suspend fun createProfile(
      userId: String,
      defaultUsername: String,
      photoUrl: String
  ): Profile = Profile(id = userId)

  override suspend fun updateUsername(userId: String, newUsername: String) {}

  override suspend fun doesUsernameExist(username: String): Boolean = false

  override suspend fun addToUserCollection(
      userId: String,
      collectionName: String,
      publicationId: String
  ) {}

  override suspend fun incrementLikes(publicationId: String, userId: String) {}

  override suspend fun removeFromLikedPublications(userId: String, publicationId: String) {}

  override suspend fun decrementLikesAndRemoveUser(publicationId: String, userId: String) {}

  override suspend fun getAllPublications(): List<Publication> = emptyList()

  override suspend fun getUserLikedPublicationsIds(userId: String): List<String> = emptyList()

  override suspend fun isFollowing(followerId: String, targetUserId: String): Boolean = false

  override suspend fun toggleFollow(followerId: String, targetUserId: String): Boolean = false

  override suspend fun updateFollowCounts(
      followerId: String,
      targetUserId: String,
      isFollowing: Boolean
  ) {}

  override suspend fun getFollowers(userId: String): List<Profile> = emptyList()

  override suspend fun getFollowing(userId: String): List<Profile> = listOf(Profile(id = "user2"))

  override suspend fun deletePublication(publicationId: String, userId: String): Boolean = true

  override suspend fun addToFavorites(userId: String, publicationId: String) {}

  override suspend fun removeFromFavorites(userId: String, publicationId: String) {}
}

@RunWith(AndroidJUnit4::class)
class SavedScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeViewModel: FakeProfileViewModel
  private lateinit var fakeNavigationActions: FakeNavigationActions
  private lateinit var fakeRepository: FakeProfileRepository

  @Before
  fun setup() {

    fakeRepository = FakeProfileRepository()
    fakeViewModel = FakeProfileViewModel(fakeRepository)
    fakeNavigationActions = FakeNavigationActions()
  }

  class FakeProfileViewModel(override val repository: ProfileRepository) :
      ProfileViewModel(repository) {
    private val _favoritePublications = MutableStateFlow<List<Publication>>(emptyList())
    override val favoritePublications: StateFlow<List<Publication>> =
        _favoritePublications.asStateFlow()

    fun setFavoritePublications(publications: List<Publication>) {
      _favoritePublications.value = publications
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
  fun whenScreenLoads_showsTopBar() {
    composeTestRule.setContent {
      SavedScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Verify the top bar exists
    composeTestRule.onNodeWithTag("topNavigationBar", useUnmergedTree = true).assertExists()

    // Verify the title exists and contains correct text
    composeTestRule
        .onNodeWithTag("screenTitle", useUnmergedTree = true)
        .assertExists()
        .assertTextContains("Saved Posts")

    // Verify back button exists
    composeTestRule.onNodeWithTag("goBackButton", useUnmergedTree = true).assertExists()
  }

  @Test
  fun whenBackButtonClicked_navigatesBack() {
    composeTestRule.setContent {
      SavedScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Use the correct test tag from TopNavigationBar
    composeTestRule.onNodeWithTag("goBackButton", useUnmergedTree = true).performClick()

    assert(fakeNavigationActions.backClicked)
  }

  @Test
  fun whenNoSavedPosts_showsEmptyState() {
    fakeViewModel.setFavoritePublications(emptyList())

    composeTestRule.setContent {
      SavedScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule
        .onNodeWithTag("empty_saved_text")
        .assertExists()
        .assertTextContains("No saved posts yet")
  }

  @Test
  fun whenHasSavedPosts_showsPublicationsGrid() {
    val publications =
        listOf(
            Publication(
                id = "pub1",
                title = "Test Publication 1",
                mediaType = MediaType.PHOTO,
                thumbnailUrl = "https://example.com/thumb1.jpg"),
            Publication(
                id = "pub2",
                title = "Test Publication 2",
                mediaType = MediaType.VIDEO,
                thumbnailUrl = "https://example.com/thumb2.jpg"))

    // Set publications in both repository and viewModel
    fakeRepository.setFavoritePublications(publications)
    fakeViewModel.setFavoritePublications(publications)

    composeTestRule.setContent {
      SavedScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Verify both publications are displayed
    composeTestRule.onNodeWithTag("publication_content_pub1", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithTag("publication_content_pub2", useUnmergedTree = true).assertExists()
  }

  @Test
  fun whenPublicationClicked_showsDetailDialog() {
    val publication =
        Publication(
            id = "pub1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            thumbnailUrl = "https://example.com/thumb.jpg",
            userId = "testUser" // Add userId
            )

    fakeViewModel.setFavoritePublications(listOf(publication))

    composeTestRule.setContent {
      SavedScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publication_content_pub1", useUnmergedTree = true).performClick()
    composeTestRule
        .onNodeWithTag("publication_detail_dialog", useUnmergedTree = true)
        .assertExists()
    composeTestRule.onNodeWithTag("publication_title", useUnmergedTree = true).assertExists()
  }

  @Test
  fun whenDetailDialogClosed_returnsToGrid() {
    val publication =
        Publication(
            id = "pub1",
            title = "Test Publication",
            mediaType = MediaType.PHOTO,
            thumbnailUrl = "https://example.com/thumb.jpg",
            userId = "testUser" // Add userId
            )

    fakeViewModel.setFavoritePublications(listOf(publication))

    composeTestRule.setContent {
      SavedScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    composeTestRule.onNodeWithTag("publication_content_pub1", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("close_button", useUnmergedTree = true).performClick()

    composeTestRule
        .onNodeWithTag("publication_detail_dialog", useUnmergedTree = true)
        .assertDoesNotExist()
    composeTestRule.onNodeWithTag("publication_content_pub1", useUnmergedTree = true).assertExists()
  }

  @Test
  fun whenVideoPublication_showsVideoIndicator() {
    val videoPublication =
        Publication(
            id = "video1",
            title = "Test Video",
            mediaType = MediaType.VIDEO,
            thumbnailUrl = "https://example.com/thumb.jpg")

    fakeViewModel.setFavoritePublications(listOf(videoPublication))

    composeTestRule.setContent {
      SavedScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Verify video indicator is shown
    composeTestRule.onNodeWithTag("video_play_icon_video1", useUnmergedTree = true).assertExists()
  }

  @Test
  fun whenPhotoPublication_doesNotShowVideoIndicator() {
    val photoPublication =
        Publication(
            id = "photo1",
            title = "Test Photo",
            mediaType = MediaType.PHOTO,
            thumbnailUrl = "https://example.com/thumb.jpg")

    fakeViewModel.setFavoritePublications(listOf(photoPublication))

    composeTestRule.setContent {
      SavedScreen(navigationActions = fakeNavigationActions, viewModel = fakeViewModel)
    }

    // Use the correct test tag that matches your PublicationItem
    composeTestRule
        .onNodeWithTag("publication_content_photo1", useUnmergedTree = true)
        .assertExists()
        .onChildren()
        .filterToOne(hasContentDescription("Video"))
        .assertDoesNotExist()
  }
}
