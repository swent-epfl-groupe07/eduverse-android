import com.github.se.eduverse.model.Comment
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import com.github.se.eduverse.viewmodel.PublicationViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkClass
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class PublicationViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var viewModel: PublicationViewModel
  private val mockRepository = mockkClass(PublicationRepository::class, relaxed = true)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    viewModel = PublicationViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun test_initial_load_of_publications() = runTest {
    val publications =
        listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaUrl = "https://...",
                thumbnailUrl = "https://...",
                mediaType = MediaType.VIDEO,
                timestamp = System.currentTimeMillis()),
            Publication(
                id = "2",
                userId = "user2",
                title = "Test Photo",
                mediaUrl = "https://...",
                thumbnailUrl = "https://...",
                mediaType = MediaType.PHOTO,
                timestamp = System.currentTimeMillis()))

    // Setup MockK to return the list when calling the repository function
    coEvery { mockRepository.loadRandomPublications(any()) } returns publications

    // Load the publications in the view model
    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    // Instead of comparing lists, compare the content of individual elements
    val result = viewModel.publications.value
    assertEquals(publications.size, result.size)
    assertEquals(publications[0].id, result[0].id)
    assertEquals(publications[1].id, result[1].id)
  }

  @Test
  fun `test loading more publications when some are already present`() = runTest {
    val initialPublications =
        listOf(
            Publication(
                id = "1", userId = "user1", title = "Test Video", mediaType = MediaType.VIDEO))
    val morePublications =
        listOf(
            Publication(
                id = "2", userId = "user2", title = "Test Photo", mediaType = MediaType.PHOTO))

    coEvery { mockRepository.loadRandomPublications(any()) } returns
        initialPublications andThen
        morePublications

    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    val expectedIds = (initialPublications + morePublications).map { it.id }.toSet()
    val actualIds = viewModel.publications.first().map { it.id }.toSet()

    assertEquals(expectedIds, actualIds)
  }

  @Test
  fun `test error handling when loading publications fails`() = runTest {
    coEvery { mockRepository.loadRandomPublications(any()) } throws Exception("Network Error")

    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Échec du chargement des publications", viewModel.error.first())
  }

  @Test
  fun `test loading empty list of publications`() = runTest {
    coEvery { mockRepository.loadRandomPublications(any()) } returns emptyList()

    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(emptyList<Publication>(), viewModel.publications.first())
    assertNull(viewModel.error.first())
  }

  @Test
  fun `test loadPublications is called with correct limit`() = runTest {
    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { mockRepository.loadRandomPublications(20) }
  }

  @Test
  fun `test loading comments for a publication`() = runTest {
    val publicationId = "testPublication"
    val comments =
        listOf(
            Comment(
                id = "comment1",
                publicationId = publicationId,
                ownerId = "user1",
                text = "Great!",
                likes = 5),
            Comment(
                id = "comment2",
                publicationId = publicationId,
                ownerId = "user2",
                text = "Amazing!",
                likes = 3))

    // Mock repository to return the comments
    coEvery { mockRepository.getComments(publicationId) } returns comments

    // Act
    viewModel.loadComments(publicationId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    assertEquals(comments, viewModel.comments.first())
    assertNull(viewModel.error.first())
  }

  @Test
  fun `test adding a comment to a publication`() = runTest {
    val publicationId = "testPublication"
    val ownerId = "user1"
    val text = "This is a comment"

    // Prepare a captor for the Comment object
    val commentCaptor = slot<Comment>()

    // Mock repository to succeed when adding a comment
    coEvery { mockRepository.addComment(publicationId, capture(commentCaptor)) } returns Unit
    coEvery { mockRepository.getComments(publicationId) } returns
        listOf(
            Comment(
                id = "capturedId",
                publicationId = publicationId,
                ownerId = ownerId,
                text = text,
                likes = 0))

    // Act
    viewModel.addComment(publicationId, ownerId, text)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    // Verify that addComment was called with a captured Comment object
    coVerify { mockRepository.addComment(publicationId, any()) }
    assertEquals(publicationId, commentCaptor.captured.publicationId)
    assertEquals(ownerId, commentCaptor.captured.ownerId)
    assertEquals(text, commentCaptor.captured.text)

    // Verify that comments were reloaded after adding
    assertEquals(1, viewModel.comments.first().size)
    assertEquals(text, viewModel.comments.first()[0].text)
  }

  @Test
  fun `test liking a comment`() = runTest {
    val publicationId = "testPublication"
    val commentId = "comment1"

    // Mock repository to succeed when liking a comment
    coEvery { mockRepository.likeComment(publicationId, commentId) } returns Unit
    coEvery { mockRepository.getComments(publicationId) } returns emptyList()

    // Act
    viewModel.likeComment(publicationId, commentId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    coVerify { mockRepository.likeComment(publicationId, commentId) }
    coVerify { mockRepository.getComments(publicationId) }
    assertNull(viewModel.error.first())
  }

  @Test
  fun `test deleting a comment`() = runTest {
    val publicationId = "testPublication"
    val commentId = "comment1"

    // Mock repository to succeed when deleting a comment
    coEvery { mockRepository.deleteComment(publicationId, commentId) } returns Unit
    coEvery { mockRepository.getComments(publicationId) } returns emptyList()

    // Act
    viewModel.deleteComment(publicationId, commentId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    coVerify { mockRepository.deleteComment(publicationId, commentId) }
    coVerify { mockRepository.getComments(publicationId) }
    assertEquals(emptyList<Comment>(), viewModel.comments.first())
    assertNull(viewModel.error.first())
  }
}
