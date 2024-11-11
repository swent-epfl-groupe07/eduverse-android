import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.MediaType
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.ui.VideoScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.profile.ProfileScreenTest.FakeNavigationActions
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

    private lateinit var fakeViewModell: com.github.se.eduverse.ui.profile.ProfileScreenTest.FakeProfileViewModel

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
            // Ne fait rien pour ce test
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
        // Création des NavigationActions factices
        val navigationActions = mockk<NavigationActions>(relaxed = true)
        every { navigationActions.currentRoute() } returns "video"

        // Utilisation du FakePublicationViewModel avec une liste vide
        val fakeViewModel = FakePublicationViewModel(emptyList())

        // Définition du contenu pour le test
        composeTestRule.setContent {
            VideoScreen(navigationActions = navigationActions, publicationViewModel = fakeViewModel, profileViewModel = fakeViewModell)
        }

        // Attendre que l'interface soit stabilisée
        composeTestRule.waitForIdle()

        // Vérifier que l'indicateur de chargement est affiché
        composeTestRule.onNodeWithTag("LoadingIndicator").assertExists().assertIsDisplayed()
    }

    @Test
    fun testVerticalPagerIsDisplayedWhenPublicationsAreNotEmpty() {
        // Création des NavigationActions factices
        val navigationActions = mockk<NavigationActions>(relaxed = true)
        every { navigationActions.currentRoute() } returns "video"

        // Liste de publications non vides
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

        // Utilisation du FakePublicationViewModel avec des publications non vides
        val fakeViewModel = FakePublicationViewModel(publications)

        // Définition du contenu pour le test
        composeTestRule.setContent {
            VideoScreen(navigationActions = navigationActions, publicationViewModel = fakeViewModel, profileViewModel = fakeViewModell)
        }

        // Attendre que l'interface soit stabilisée
        composeTestRule.waitForIdle()

        // Vérifier que le VerticalPager est affiché
        composeTestRule.onNodeWithTag("VerticalPager").assertExists().assertIsDisplayed()
    }

    @Test
    fun testCorrectDisplayOfPublications() {
        // Création des NavigationActions factices
        val navigationActions = mockk<NavigationActions>(relaxed = true)
        every { navigationActions.currentRoute() } returns "video"

        // Liste de publications avec différents types (vidéo et photo)
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

        // Utilisation du FakePublicationViewModel avec des publications non vides
        val fakeViewModel = FakePublicationViewModel(publications)

        // Définition du contenu pour le test
        composeTestRule.setContent {
            VideoScreen(navigationActions = navigationActions, publicationViewModel = fakeViewModel, profileViewModel = fakeViewModell)
        }

        // Attendre que l'interface soit stabilisée et que VideoScreen soit visible
        composeTestRule.onNodeWithTag("VideoScreen").assertExists().assertIsDisplayed()

        // Vérifier l'affichage correct du premier élément (vidéo)
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag("VideoItem").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("VideoItem").assertIsDisplayed()

        // Passer à la deuxième page (photo) en simulant un swipe
        composeTestRule.onNodeWithTag("VerticalPager").performTouchInput { swipeUp() }

        // Vérifier l'affichage correct du deuxième élément (photo)
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag("PhotoItem").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("PhotoItem").assertIsDisplayed()
    }

    @Test
    fun testPaginationLoadMorePublications() {
        // Création des NavigationActions factices
        val navigationActions = mockk<NavigationActions>(relaxed = true)
        every { navigationActions.currentRoute() } returns "video"

        // Liste initiale de publications avec quelques éléments
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

        // Création d'un mock pour PublicationViewModel
        val fakeViewModel = spyk(FakePublicationViewModel(initialPublications))

        // Définition du contenu pour le test
        composeTestRule.setContent {
            VideoScreen(navigationActions = navigationActions, publicationViewModel = fakeViewModel, profileViewModel = fakeViewModell)
        }

        // Attendre que l'interface soit stabilisée
        composeTestRule.onNodeWithTag("VideoScreen").assertExists().assertIsDisplayed()

        // Simuler un scroll jusqu'à la dernière publication pour déclencher le chargement
        composeTestRule.onNodeWithTag("VerticalPager").performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()

        // Vérifier que loadMorePublications() est appelé une fois que la dernière page est atteinte
        verify { runBlocking { fakeViewModel.loadMorePublications() } }
    }

    @Test
    fun testBottomNavigationMenuIsDisplayed() {
        // Création des NavigationActions factices
        val navigationActions = mockk<NavigationActions>(relaxed = true)
        every { navigationActions.currentRoute() } returns "video"

        // Création d'un mock pour le ViewModel avec quelques publications pour peupler l'écran
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

        // Définition du contenu pour le test
        composeTestRule.setContent {
            VideoScreen(navigationActions = navigationActions, publicationViewModel = fakeViewModel, profileViewModel = fakeViewModell)
        }

        // Attendre que l'interface soit stabilisée
        composeTestRule.onNodeWithTag("VideoScreen").assertExists().assertIsDisplayed()

        // Vérifier que la BottomNavigationMenu est affichée
        composeTestRule.onNodeWithTag("bottomNavigationMenu").assertExists().assertIsDisplayed()
    }

    @Test
    fun testErrorIndicatorIsDisplayedOnLoadingFailure() {
        // Création des NavigationActions factices
        val navigationActions = mockk<NavigationActions>(relaxed = true)
        every { navigationActions.currentRoute() } returns "video"

        // Création d'un ViewModel factice avec un état d'erreur initial
        val fakeViewModel =
            object : PublicationViewModel(mockk()) {
                override val publications = MutableStateFlow<List<Publication>>(emptyList())
                override val error = MutableStateFlow("Échec du chargement des publications")
            }

        // Définition du contenu pour le test
        composeTestRule.setContent {
            VideoScreen(navigationActions = navigationActions, publicationViewModel = fakeViewModel, profileViewModel = fakeViewModell)
        }

        // Attendre que l'interface soit stabilisée
        composeTestRule.waitForIdle()

        // Vérifier que l'indicateur d'erreur est affiché
        composeTestRule.onNodeWithTag("ErrorIndicator").assertExists().assertIsDisplayed()
    }

    @Test
    fun testPublicationsAreNotReloadedUnnecessarilyOnLifecycleChanges() {
        // Création des NavigationActions factices
        val navigationActions = mockk<NavigationActions>(relaxed = true)
        every { navigationActions.currentRoute() } returns "video"

        // Utilisation d'un mock de PublicationViewModel pour surveiller les appels à
        // loadMorePublications()
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

        // Définition du contenu pour le test
        composeTestRule.setContent {
            VideoScreen(navigationActions = navigationActions, publicationViewModel = fakeViewModel, profileViewModel = fakeViewModell)
        }

        // Attendre que l'interface soit stabilisée
        composeTestRule.waitForIdle()

        // Vérifier qu'un seul appel initial a été fait à loadMorePublications()
        verify(exactly = 1) { runBlocking { fakeViewModel.loadMorePublications() } }

        // Simuler un changement de cycle de vie (application en arrière-plan puis de retour au premier
        // plan)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        // Attendre la stabilisation de l'interface
        composeTestRule.waitForIdle()

        // Vérifier qu'aucun nouvel appel supplémentaire n'est fait à loadMorePublications()
        verify(exactly = 1) { runBlocking { fakeViewModel.loadMorePublications() } }
    }

    @Test
    fun testLikeButtonChangesStateCorrectly() {
        // Création des NavigationActions factices
        val navigationActions = mockk<NavigationActions>(relaxed = true)
        every { navigationActions.currentRoute() } returns "video"

        // Liste de publications avec un seul élément pour le test
        val publications = listOf(
            Publication(
                id = "1",
                userId = "user1",
                title = "Test Video",
                mediaType = MediaType.VIDEO,
                mediaUrl = "https://www.sample-videos.com/video123/mp4/480/asdasdas.mp4",
                thumbnailUrl = "",
                timestamp = System.currentTimeMillis(),
                likedBy = emptyList() // Initialement non liké
            )
        )

        // Utilisation du FakePublicationViewModel avec la publication
        val fakeViewModel = FakePublicationViewModel(publications)

        // Définition du contenu pour le test
        composeTestRule.setContent {
            VideoScreen(
                navigationActions = navigationActions,
                publicationViewModel = fakeViewModel,
                profileViewModel = fakeViewModell
            )
        }

        // Attendre que l'interface soit stabilisée
        composeTestRule.waitForIdle()

        // Vérifier que l'icône `UnlikedIcon` est présente initialement
        composeTestRule.onNodeWithTag("UnlikedIcon_0", useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()

        // Simuler un clic sur le bouton de like
        composeTestRule.onNodeWithTag("LikeButton_0", useUnmergedTree = true)
            .performClick()

        // Attendre la stabilisation de l'interface après l'interaction
        composeTestRule.waitForIdle()

        // Vérifier que l'icône est maintenant `LikedIcon`
        composeTestRule.onNodeWithTag("LikedIcon_0", useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("LikeButton_0", useUnmergedTree = true)
            .performClick()

        // Attendre la stabilisation de l'interface après l'interaction
        composeTestRule.waitForIdle()

        // Vérifier que l'icône est maintenant `UnlikedIcon`
        composeTestRule.onNodeWithTag("UnlikedIcon_0", useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
    }






}
