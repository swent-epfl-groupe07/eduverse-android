import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.repository.PublicationRepository
import com.github.se.eduverse.viewmodel.PublicationViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkClass
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
    coEvery { mockRepository.loadRandomPublications(any(), any()) } returns publications

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

    coEvery { mockRepository.loadRandomPublications(any(), any()) } returns
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
    coEvery { mockRepository.loadRandomPublications(any(), any()) } throws
        Exception("Network Error")

    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("fail to load publications", viewModel.error.first())
  }

  @Test
  fun `test loading empty list of publications`() = runTest {
    coEvery { mockRepository.loadRandomPublications(any(), any()) } returns emptyList()

    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(emptyList<Publication>(), viewModel.publications.first())
    assertNull(viewModel.error.first())
  }

  @Test
  fun `test loadPublications is called with correct limit`() = runTest {
    viewModel.loadMorePublications()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { mockRepository.loadRandomPublications(followed = null, limit = 20) }
  }

  @Test
  fun `test loadFollowedPublications with empty list`() = runTest {
    val publications =
        listOf(
            Publication(
                id = "1", userId = "user1", title = "Test Video", mediaType = MediaType.VIDEO))

    coEvery { mockRepository.loadRandomPublications(any(), any()) } returns publications

    viewModel.loadFollowedPublications(listOf("userId"))
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { mockRepository.loadRandomPublications(followed = listOf("userId"), limit = 20) }

    val expectedIds = (publications).map { it.id }.toSet()
    val actualIds = viewModel.followedPublications.first().map { it.id }.toSet()

    assertEquals(expectedIds, actualIds)
  }

  @Test
  fun `test loadMorePublications with non empty list`() = runTest {
    val initialPublications =
        listOf(
            Publication(
                id = "1", userId = "user1", title = "Test Video", mediaType = MediaType.VIDEO))
    val morePublications =
        listOf(
            Publication(
                id = "2", userId = "user2", title = "Test Photo", mediaType = MediaType.PHOTO))

    coEvery { mockRepository.loadRandomPublications(any(), any()) } returns
        initialPublications andThen
        morePublications

    viewModel.loadFollowedPublications(listOf("userId"))
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.loadFollowedPublications(listOf("userId"))
    testDispatcher.scheduler.advanceUntilIdle()

    val expectedIds = (initialPublications + morePublications).map { it.id }.toSet()
    val actualIds = viewModel.followedPublications.first().map { it.id }.toSet()

    assertEquals(expectedIds, actualIds)
  }
}
