// VideoScreenTest.kt
package com.github.se.eduverse.ui.videos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.fake.FakeCommentsViewModel
import com.github.se.eduverse.fake.FakeNavigationActions
import com.github.se.eduverse.fake.FakeProfileViewModel
import com.github.se.eduverse.fake.FakePublicationRepository
import com.github.se.eduverse.fake.FakePublicationViewModel
import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class VideoScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeProfileViewModel: FakeProfileViewModel
  private lateinit var fakeCommentsViewModel: FakeCommentsViewModel
  private lateinit var fakePublicationViewModel: FakePublicationViewModel
  private lateinit var fakePublicationRepository: FakePublicationRepository
  private lateinit var fakeNavigationActions: FakeNavigationActions

  @Before
  fun setup() {
    fakeProfileViewModel = FakeProfileViewModel()
    fakeCommentsViewModel = FakeCommentsViewModel()
    fakePublicationRepository = FakePublicationRepository()
    fakePublicationViewModel = FakePublicationViewModel(fakePublicationRepository)
    fakeNavigationActions = FakeNavigationActions()
  }

  @Test
  fun testLoadingIndicatorIsDisplayedWhenPublicationsAreEmpty() {
    // Do not set publications to simulate absence of data
    fakePublicationViewModel.setPublications(emptyList())

    // Set the content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Verify that the loading indicator is displayed
    composeTestRule.onNodeWithTag("LoadingIndicator").assertExists().assertIsDisplayed()
  }

  @Test
  fun testVerticalPagerIsDisplayedWhenPublicationsAreNotEmpty() {
    // Non-empty list of publications
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://sample-videos.com/video123/mp4/480/asdasdas.mp4",
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

    // Set publications in the FakeRepository
    fakePublicationRepository.setPublications(publications)
    // Update publications in the ViewModel
    fakePublicationViewModel.setPublications(publications)

    // Set the content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Verify that the VerticalPager is displayed
    composeTestRule.onNodeWithTag("VerticalPager").assertExists().assertIsDisplayed()
  }

  @Test
  fun testCorrectDisplayOfPublications() {
    // Liste des publications avec différents types (vidéo et photo)
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://sample-videos.com/video123/mp4/480/asdasdas.mp4",
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

    // Configurer les publications dans le FakeRepository
    fakePublicationRepository.setPublications(publications)
    // Mettre à jour les publications dans le ViewModel
    fakePublicationViewModel.setPublications(publications)

    // Définir le contenu pour le test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    // Attendre que l'UI se stabilise et s'assurer que VideoScreen est visible
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("VideoScreen").assertExists().assertIsDisplayed()

    // Vérifier l'affichage correct du premier élément (vidéo)
    composeTestRule.onNodeWithTag("VideoItem_0").assertExists().assertIsDisplayed()

    // Faire défiler vers la deuxième page (photo)
    composeTestRule.onNodeWithTag("VerticalPager").performTouchInput { swipeUp() }

    // Attendre que l'UI se stabilise après le défilement
    composeTestRule.waitForIdle()

    // Vérifier l'affichage correct du deuxième élément (photo)
    composeTestRule.onNodeWithTag("PhotoItem_1").assertExists().assertIsDisplayed()
  }

  @Test
  fun testPaginationLoadMorePublications() = runTest {
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

    // Set publications in the FakeRepository
    fakePublicationRepository.setPublications(initialPublications)
    // Update publications in the ViewModel
    fakePublicationViewModel.setPublications(initialPublications)

    // Create a spy for the ViewModel
    val spyViewModel = spyk(fakePublicationViewModel)

    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = spyViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Wait until publications are loaded
    composeTestRule.waitUntil(timeoutMillis = 5000) { spyViewModel.publications.value.isNotEmpty() }

    // Verify that the VerticalPager is displayed
    composeTestRule.onNodeWithTag("VerticalPager").assertExists().assertIsDisplayed()

    // Simulate scrolling to the last page
    composeTestRule.onNodeWithTag("VerticalPager").performTouchInput { swipeUp() }
    composeTestRule.waitForIdle()

    // Verify that loadMorePublications() is called
    coVerify { spyViewModel.loadMorePublications() }
  }

  @Test
  fun testBottomNavigationMenuIsDisplayed() {
    // Set some publications
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis()))

    fakePublicationRepository.setPublications(publications)
    fakePublicationViewModel.setPublications(publications)

    // Set the content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("VideoScreen").assertExists().assertIsDisplayed()

    // Verify that the BottomNavigationMenu is displayed
    composeTestRule.onNodeWithTag("bottomNavigationMenu").assertExists().assertIsDisplayed()
  }

  @Test
  fun testErrorIndicatorIsDisplayedOnLoadingFailure() {
    // Simulate an error state in the ViewModel
    fakePublicationViewModel.apply {
      _error.value = "Failed to load publications"
      _publications.value = emptyList()
    }

    // Set the content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Verify that the error indicator is displayed
    composeTestRule.onNodeWithTag("ErrorIndicator").assertExists().assertIsDisplayed()
  }

  @Test
  fun testLikeButtonChangesStateCorrectly() {
    // List of publications with a single item for the test
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis(),
                likedBy = emptyList() // Initially not liked
                ))

    // Set publications
    fakePublicationRepository.setPublications(publications)
    fakePublicationViewModel.setPublications(publications)

    // Set the content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Verify that the `UnlikedIcon` is present initially
    composeTestRule
        .onNodeWithTag("UnlikedIcon_0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Simulate a click on the like button
    composeTestRule.onNodeWithTag("LikeButton_0", useUnmergedTree = true).performClick()

    // Wait for the UI to stabilize after interaction
    composeTestRule.waitForIdle()

    // Verify that the icon is now `LikedIcon`
    composeTestRule
        .onNodeWithTag("LikedIcon_0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Simulate another click to unlike
    composeTestRule.onNodeWithTag("LikeButton_0", useUnmergedTree = true).performClick()

    // Wait for the UI to stabilize after interaction
    composeTestRule.waitForIdle()

    // Verify that the icon is now `UnlikedIcon` again
    composeTestRule
        .onNodeWithTag("UnlikedIcon_0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun testCommentsSectionOpensOnCommentButtonClick() {
    // List of publications for the test
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis()))

    // Set publications
    fakePublicationRepository.setPublications(publications)
    fakePublicationViewModel.setPublications(publications)

    // Set the content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Verify that the comments section is not visible initially
    composeTestRule.onNodeWithTag("CommentsSection").assertDoesNotExist()

    // Click on the comment button
    composeTestRule.onNodeWithTag("CommentButton_0", useUnmergedTree = true).performClick()

    // Wait for the UI to stabilize after opening the modal
    composeTestRule.waitForIdle()

    // Verify that the comments section is now visible
    composeTestRule.onNodeWithTag("CommentsSection").assertExists().assertIsDisplayed()
  }

  @Test
  fun testAddCommentAndVerifyItAppearsInList() {
    // Initial setup
    val publicationId = "1"
    val publications =
        listOf(
            Publication(
                id = publicationId,
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis()))

    fakePublicationRepository.setPublications(publications)
    fakePublicationViewModel.setPublications(publications)

    // Prepare the CommentsViewModel
    val fakeComment =
        Comment(
            id = "comment1",
            ownerId = "user2",
            text = "Great video!",
            likes = 0,
            likedBy = emptyList(),
            profile = Profile(username = "TestUser", profileImageUrl = ""))
    fakeCommentsViewModel.setComments(publicationId, listOf(fakeComment))

    // Set the content for the test
    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Open the comments section
    composeTestRule.onNodeWithTag("CommentButton_0", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Verify that the initial comment is present
    composeTestRule
        .onNodeWithTag("CommentItem_${fakeComment.id}")
        .assertExists()
        .assertIsDisplayed()

    // Enter a new comment
    val newCommentText = "This is a new comment"
    composeTestRule.onNodeWithTag("NewCommentTextField").performTextInput(newCommentText)

    // Click on the "Post" button
    composeTestRule.onNodeWithTag("PostCommentButton").performClick()

    // Wait for the UI to stabilize
    composeTestRule.waitForIdle()

    // Verify that the new comment is added to the list
    val newCommentId =
        "new_comment_id" // Assume that the ID is generated like this in the FakeCommentsViewModel
    composeTestRule.onNodeWithTag("CommentItem_$newCommentId").assertExists().assertIsDisplayed()

    // Verify that the text of the new comment is correct
    composeTestRule
        .onNodeWithTag("CommentText_$newCommentId")
        .assertExists()
        .assertIsDisplayed()
        .assertTextEquals(newCommentText)
  }

  @Test
  fun testAuthorCanDeleteTheirComment() {
    val currentUserId = "user2"
    val publicationId = "1"
    val commentId = "comment1"

    val publications =
        listOf(
            Publication(
                id = publicationId,
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis()))

    fakePublicationRepository.setPublications(publications)
    fakePublicationViewModel.setPublications(publications)

    val fakeComment =
        Comment(
            id = commentId,
            ownerId = currentUserId,
            text = "Comment to be deleted",
            likes = 0,
            likedBy = emptyList(),
            profile = Profile(username = "TestUser", profileImageUrl = ""))
    fakeCommentsViewModel.setComments(publicationId, listOf(fakeComment))

    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          currentUserId = currentUserId)
    }

    composeTestRule.waitForIdle()

    // Open the comments section
    composeTestRule.onNodeWithTag("CommentButton_0", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Verify that the comment is displayed
    composeTestRule.onNodeWithTag("CommentItem_$commentId").assertExists().assertIsDisplayed()

    // Click on the delete button
    composeTestRule
        .onNodeWithTag("DeleteCommentButton_$commentId", useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    // Verify that the comment has been deleted
    composeTestRule.onNodeWithTag("CommentItem_$commentId").assertDoesNotExist()
  }

  @Test
  fun testShareButtonIsDisplayedForEachPublication() {
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://sample-videos.com/video123/mp4/480/asdasdas.mp4",
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

    fakePublicationRepository.setPublications(publications)
    fakePublicationViewModel.setPublications(publications)

    composeTestRule.setContent {
      VideoScreen(
          navigationActions = fakeNavigationActions,
          publicationViewModel = fakePublicationViewModel,
          profileViewModel = fakeProfileViewModel,
          commentsViewModel = fakeCommentsViewModel,
          "")
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("ShareButton_0").assertExists().assertIsDisplayed()

    composeTestRule.onNodeWithTag("VerticalPager").performTouchInput { swipeUp() }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("ShareButton_1").assertExists().assertIsDisplayed()
  }
}
