package com.github.se.eduverse.E2E

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.se.eduverse.model.CommonWidgetType
import com.github.se.eduverse.model.Profile
import com.github.se.eduverse.model.Publication
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import com.github.se.eduverse.model.Widget
import com.github.se.eduverse.ui.Pomodoro.PomodoroScreen
import com.github.se.eduverse.ui.calculator.CalculatorScreen
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.ui.search.SearchProfileScreen
import com.github.se.eduverse.ui.search.TAG_PROFILE_ITEM
import com.github.se.eduverse.ui.search.TAG_PROFILE_LIST
import com.github.se.eduverse.ui.search.TAG_SEARCH_FIELD
import com.github.se.eduverse.ui.search.UserProfileScreen
import com.github.se.eduverse.viewmodel.DashboardViewModel
import com.github.se.eduverse.viewmodel.ProfileUiState
import com.github.se.eduverse.viewmodel.ProfileViewModel
import com.github.se.eduverse.viewmodel.SearchProfileState
import com.github.se.eduverse.viewmodel.TimerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class CalculatorWidgetE2ETest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: FakeDashboardViewModel
  private lateinit var navigationActions: FakeNavigationActions

  @Before
  fun setup() {
    MockFirebaseAuth.setup()
    viewModel = FakeDashboardViewModel()
    navigationActions = FakeNavigationActions()

    composeTestRule.setContent {
      TestNavigation(viewModel = viewModel, navigationActions = navigationActions)
    }
  }

  @After
  fun tearDown() {
    unmockkAll() // Clean up all mockk mocks
  }

  @Test
  fun testCalculatorWidgetFlow() {
    composeTestRule.apply {
      waitForIdle()

      // 1. Add Calculator Widget
      onNodeWithTag("add_widget_button").performClick()

      // Get button with Calculator text from CommonWidgetType
      val calculatorWidget = CommonWidgetType.CALCULATOR
      onNodeWithText(calculatorWidget.title).performClick()

      // Verify calculator widget appears on dashboard
      onNodeWithText(calculatorWidget.title).assertIsDisplayed()
      onNodeWithText(calculatorWidget.content).assertIsDisplayed()

      // 2. Open Calculator
      onNodeWithText("Calculator").performClick()
      navigationActions.navigateToCalculator()
      waitForIdle()

      // Verify calculator screen elements
      onNodeWithTag("display").assertExists()
      onNodeWithTag("displayText").assertExists()

      // 3. Perform Basic Calculation
      onNodeWithTag("button_7").performClick()
      onNodeWithTag("button_+").performClick()
      onNodeWithTag("button_3").performClick()
      onNodeWithTag("button_=").performClick()
      onNodeWithTag("resultText").assertTextContains("10")

      // 4. Return to Dashboard
      onNodeWithTag("goBackButton").performClick()
      navigationActions.navigateToDashboard()
      waitForIdle()

      // Re-verify the calculator widget is still there
      onNodeWithText("Calculator").assertIsDisplayed()

      // 5. Delete the Calculator widget
      onAllNodesWithTag("widget_card").onFirst().assertExists().performScrollTo()

      onAllNodesWithTag("delete_icon").onFirst().assertExists().performClick()

      // Verify widget is removed
      onNodeWithText("Calculator").assertDoesNotExist()
    }
  }
}

class SocialInteractionE2ETest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var dashboardViewModel: FakeDashboardViewModel
  private lateinit var viewModel: FakeProfileViewModel
  private lateinit var navigationActions: FakeNavigationActions2

  @Before
  fun setup() {
    MockFirebaseAuth.setup()
    // Initialize all view models
    dashboardViewModel = FakeDashboardViewModel()
    viewModel = FakeProfileViewModel()
    navigationActions = FakeNavigationActions2()

    composeTestRule.setContent {
      TestNavigation2(
          dashboardViewModel = dashboardViewModel,
          viewModel = viewModel,
          navigationActions = navigationActions)
    }
  }

  @After
  fun tearDown() {
    unmockkAll() // Clean up all mockk mocks
  }

  @Test
  fun testSocialInteractionFlow() {
    composeTestRule.apply {
      // 1. Start from Dashboard and navigate to Search
      onNodeWithTag("search_button").performClick()
      navigationActions.navigateToSearch()
      waitForIdle()

      // 2. Search for a user
      onNodeWithTag(TAG_SEARCH_FIELD).performTextInput("test_user")
      waitForIdle()

      // Verify search results appear
      onNodeWithTag(TAG_PROFILE_LIST).assertExists()
      onNodeWithTag("${TAG_PROFILE_ITEM}_test_user_id").assertExists()

      // 3. Click on a user profile
      onNodeWithTag("${TAG_PROFILE_ITEM}_test_user_id").performClick()
      navigationActions.navigateToUserProfile("test_user_id")
      waitForIdle()

      // Verify user profile elements
      onNodeWithTag("user_profile_screen_container").assertExists()
      onNodeWithTag("user_profile_username").assertTextContains("TestUser")
      onNodeWithTag("followers_stat").assertExists()
      onNodeWithTag("following_stat").assertExists()

      // 4. Follow the user
      onNodeWithTag("follow_button").performClick()
      waitForIdle()

      // Verify follow button state changed
      onNodeWithTag("follow_button").assertTextContains("Unfollow")

      // 5. View user's publications
      onNodeWithTag("publications_tab").performClick()
      waitForIdle()

      // Verify publications grid exists
      onNodeWithTag("publications_grid").assertExists()

      // 6. Interact with a publication
      onNodeWithTag("publication_item_1").performClick()
      waitForIdle()

      // Verify publication detail dialog (using unmergedTree for dialog content)
      onNode(hasTestTag("publication_detail_dialog"), useUnmergedTree = true).assertExists()

      // 7. Like the publication (using unmergedTree for dialog elements)
      onNode(hasTestTag("like_button"), useUnmergedTree = true).performClick()
      waitForIdle()

      // Verify like status (using unmergedTree for dialog elements)
      onNode(hasTestTag("liked_icon"), useUnmergedTree = true).assertExists()

      // 8. Close publication detail (using unmergedTree for dialog elements)
      onNode(hasTestTag("close_button"), useUnmergedTree = true).performClick()
      waitForIdle()

      // 9. Unfollow user
      onNodeWithTag("follow_button").performClick()
      waitForIdle()

      // Verify follow button state reverted
      onNodeWithTag("follow_button").assertTextContains("Follow")

      // 10. Return to dashboard
      onNodeWithTag("back_button").performClick()
      navigationActions.goBack()
      waitForIdle()
    }
  }
}

@Composable
fun TestNavigation2(
    dashboardViewModel: FakeDashboardViewModel,
    viewModel: FakeProfileViewModel,
    navigationActions: FakeNavigationActions2
) {
  var currentScreen by remember { mutableStateOf("DASHBOARD") }

  // Listen for navigation changes
  if (navigationActions is FakeNavigationActions2) {
    currentScreen = navigationActions.currentRoute()
  }

  when (currentScreen) {
    "SEARCH" -> SearchProfileScreen(navigationActions = navigationActions, viewModel = viewModel)
    "USER_PROFILE" ->
        UserProfileScreen(
            navigationActions = navigationActions,
            viewModel = viewModel,
            userId = "test_user_id",
            currentUserId = "current_user_id")
    else -> DashboardScreen(viewModel = dashboardViewModel, navigationActions = navigationActions)
  }
}

@HiltViewModel
class FakeProfileViewModel @Inject constructor() : ProfileViewModel(mock()) {
  private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
  override val profileState: StateFlow<ProfileUiState> = _profileState

  private val _likedPublications = MutableStateFlow<List<Publication>>(emptyList())
  override val likedPublications: StateFlow<List<Publication>> = _likedPublications

  private val _searchState = MutableStateFlow<SearchProfileState>(SearchProfileState.Idle)
  override val searchState: StateFlow<SearchProfileState> = _searchState

  private val mockUser =
      Profile(
          id = "test_user_id",
          username = "TestUser",
          profileImageUrl = "",
          followers = 100,
          following = 50,
          publications =
              listOf(
                  Publication(
                      id = "1",
                      userId = "test_user_id",
                      title = "Test Publication",
                      mediaUrl = "",
                      thumbnailUrl = "",
                      likes = 10,
                      likedBy = listOf())),
          isFollowedByCurrentUser = false)

  override fun loadProfile(userId: String) {
    // Set the profile state with mock user data
    _profileState.value = ProfileUiState.Success(mockUser)
  }

  override fun loadLikedPublications(userId: String) {
    _likedPublications.value = mockUser.publications
  }

  override fun searchProfiles(query: String) {
    _searchState.value = SearchProfileState.Loading
    if (query.isNotEmpty()) {
      _searchState.value = SearchProfileState.Success(listOf(mockUser))
    } else {
      _searchState.value = SearchProfileState.Idle
    }
  }

  override fun toggleFollow(followerId: String, targetUserId: String) {
    val currentState = _profileState.value
    if (currentState is ProfileUiState.Success) {
      val updatedProfile =
          currentState.profile.copy(
              isFollowedByCurrentUser = !currentState.profile.isFollowedByCurrentUser,
              followers =
                  if (currentState.profile.isFollowedByCurrentUser)
                      currentState.profile.followers - 1
                  else currentState.profile.followers + 1)
      _profileState.value = ProfileUiState.Success(updatedProfile)
    }
  }

  override fun likeAndAddToFavorites(userId: String, publicationId: String) {
    // Update liked publication state
    val currentState = _profileState.value
    if (currentState is ProfileUiState.Success) {
      val updatedPublications =
          currentState.profile.publications.map { pub ->
            if (pub.id == publicationId) {
              pub.copy(likes = pub.likes + 1, likedBy = pub.likedBy + userId)
            } else pub
          }
      _profileState.value =
          ProfileUiState.Success(currentState.profile.copy(publications = updatedPublications))
    }
  }

  override fun removeLike(userId: String, publicationId: String) {
    // Update unliked publication state
    val currentState = _profileState.value
    if (currentState is ProfileUiState.Success) {
      val updatedPublications =
          currentState.profile.publications.map { pub ->
            if (pub.id == publicationId) {
              pub.copy(likes = maxOf(0, pub.likes - 1), likedBy = pub.likedBy - userId)
            } else pub
          }
      _profileState.value =
          ProfileUiState.Success(currentState.profile.copy(publications = updatedPublications))
    }
  }
}

class FakeNavigationActions2 : NavigationActions(mockk(relaxed = true)) {
  private var _currentRoute = mutableStateOf("DASHBOARD")

  fun navigateToSearch() {
    _currentRoute.value = "SEARCH"
  }

  override fun navigateToUserProfile(userId: String) {
    _currentRoute.value = "USER_PROFILE"
  }

  override fun navigateTo(destination: TopLevelDestination) {
    _currentRoute.value = destination.route
  }

  override fun navigateTo(route: String) {
    _currentRoute.value = route
  }

  override fun goBack() {
    _currentRoute.value = "DASHBOARD"
  }

  override fun currentRoute(): String = _currentRoute.value
}

@HiltViewModel
class FakeDashboardViewModel @Inject constructor() : DashboardViewModel(mock()) {
  // Keep track of widgets persistently
  private var widgets = mutableListOf<Widget>()

  override fun fetchWidgets(userId: String) {
    _widgetList.value = widgets
  }

  override fun addWidget(widget: Widget) {
    widgets.add(widget.copy(order = widgets.size))
    _widgetList.value = widgets.toList()
  }

  override fun removeWidgetAndUpdateOrder(widgetId: String, updatedWidgets: List<Widget>) {
    widgets = updatedWidgets.toMutableList()
    _widgetList.value = widgets.toList()
  }

  override fun updateWidgetOrder(reorderedWidgets: List<Widget>) {
    widgets =
        reorderedWidgets.mapIndexed { index, widget -> widget.copy(order = index) }.toMutableList()
    _widgetList.value = widgets.toList()
  }

  override fun getCommonWidgets(): List<Widget> {
    return CommonWidgetType.values().map { commonWidget ->
      Widget(
          widgetId = commonWidget.name,
          widgetType = commonWidget.name,
          widgetTitle = commonWidget.title,
          widgetContent = commonWidget.content,
          ownerUid = "",
          order = 0)
    }
  }
}

@Composable
fun TestNavigation(viewModel: FakeDashboardViewModel, navigationActions: FakeNavigationActions) {
  var currentScreen by remember { mutableStateOf("DASHBOARD") }

  // Listen for navigation changes
  if (navigationActions is FakeNavigationActions) {
    currentScreen = navigationActions.currentRoute()
  }

  when (currentScreen) {
    "CALCULATOR" -> CalculatorScreen(navigationActions = navigationActions)
    else -> DashboardScreen(viewModel = viewModel, navigationActions = navigationActions)
  }
}

class FakeNavigationActions : NavigationActions(mockk(relaxed = true)) {
  private var _currentRoute = mutableStateOf("DASHBOARD")

  fun navigateToCalculator() {
    _currentRoute.value = "CALCULATOR"
  }

  fun navigateToDashboard() {
    _currentRoute.value = "DASHBOARD"
  }

  override fun navigateTo(destination: TopLevelDestination) {
    _currentRoute.value = destination.route
  }

  override fun navigateTo(route: String) {
    _currentRoute.value = route
  }

  override fun goBack() {
    _currentRoute.value = "DASHBOARD"
  }

  override fun currentRoute(): String = _currentRoute.value
}

// Update MockFirebaseAuth to use MockK consistently
class MockFirebaseAuth {
  companion object {
    private var mockAuth: FirebaseAuth? = null
    private var mockUser: FirebaseUser? = null

    fun setup(isAuthenticated: Boolean = true) {
      cleanup() // Clean up previous mocks

      mockkStatic(FirebaseAuth::class)
      mockAuth = mock(FirebaseAuth::class.java)
      mockUser =
          mock(FirebaseUser::class.java).apply {
            `when`(getUid()).thenReturn("test_user_id")
            `when`(getEmail()).thenReturn("test@example.com")
            `when`(getDisplayName()).thenReturn("Test User")
            `when`(getPhoneNumber()).thenReturn(null)
            `when`(getPhotoUrl()).thenReturn(null)
            `when`(getProviderId()).thenReturn("firebase")
            `when`(isEmailVerified).thenReturn(true)
            `when`(isAnonymous).thenReturn(false)
            `when`(getMetadata()).thenReturn(null)
            `when`(getProviderData()).thenReturn(mutableListOf())
            `when`(getTenantId()).thenReturn(null)
          }

      every { FirebaseAuth.getInstance() } returns mockAuth!!
      `when`(mockAuth!!.currentUser).thenReturn(if (isAuthenticated) mockUser else null)
    }

    fun cleanup() {
      unmockkAll()
      mockAuth = null
      mockUser = null
    }
  }
}

class PomodoroTimerE2ETest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: FakeDashboardViewModel
  private lateinit var timerViewModel: FakeTimerViewModel
  private lateinit var navigationActions: FakePomodoroNavigationActions

  @Before
  fun setup() {
    MockFirebaseAuth.setup()
    viewModel = FakeDashboardViewModel()
    timerViewModel = FakeTimerViewModel()
    navigationActions = FakePomodoroNavigationActions()

    composeTestRule.setContent {
      TestPomodoroNavigation(
          dashboardViewModel = viewModel,
          timerViewModel = timerViewModel,
          navigationActions = navigationActions)
    }
  }

  @After
  fun tearDown() {
    unmockkAll() // Clean up all mockk mocks
  }

  @Test
  fun testPomodoroTimerFlow() {
    composeTestRule.apply {
      waitForIdle()

      // 1. Add Timer Widget
      onNodeWithTag("add_widget_button").performClick()
      val timerWidget = CommonWidgetType.TIMER
      onNodeWithText(timerWidget.title).performClick()

      // Verify timer widget appears on dashboard
      onNodeWithText(timerWidget.title).assertIsDisplayed()
      onNodeWithText(timerWidget.content).assertIsDisplayed()

      // 2. Open Timer
      onNodeWithText("Study Timer").performClick()
      navigationActions.navigateToPomodoro()
      waitForIdle()
      waitForIdle()
      waitForIdle()

      // Verify initial state
      onNodeWithTag("timerText").assertTextContains("25:00")
      onNodeWithTag("cycleText").assertTextContains("Cycle: 1/4")
      onNodeWithTag("focusIcon").assertExists()

      // 3. Test timer controls
      // Get initial timer text
      val initialTime =
          onNodeWithTag("timerText")
              .fetchSemanticsNode()
              .config
              .first { it.key.name == "Text" }
              .value
              .toString()

      // Start timer
      onNodeWithTag("playPauseButton").performClick()
      waitForIdle()

      // Verify timer started (text should be different from initial)
      val afterStartTime =
          onNodeWithTag("timerText")
              .fetchSemanticsNode()
              .config
              .first { it.key.name == "Text" }
              .value
              .toString()
      assert(initialTime != afterStartTime) { "Timer value should change after starting" }

      // Pause timer
      onNodeWithTag("playPauseButton").performClick()
      waitForIdle()

      // 4. Test timer settings
      onNodeWithTag("settingsButton").performClick()
      waitForIdle()

      // If your Slider has SemanticProperties.ProgressBarRangeInfo set
      onNode(hasTestTag("Focus Time Slider"), useUnmergedTree = true).performSemanticsAction(
          SemanticsActions.SetProgress) {
            it(1.8f) // Set to 2 min
      }

      waitForIdle()

      onNode(hasTestTag("settingsSaveButton"), useUnmergedTree = true).performClick()
      waitForIdle()

      // 5. Test timer type changes
      onNodeWithTag("skipButton").performClick()
      waitForIdle()

      // Verify short break mode
      onNodeWithTag("shortBreakIcon").assertExists()
      onNodeWithTag("timerText").assertTextContains("5:00")

      // 6. Test reset
      onNodeWithTag("resetButton").performClick()
      waitForIdle()

      // Verify reset state
      onNodeWithTag("timerText").assertTextContains("2:00")
      onNodeWithTag("focusIcon").assertExists()

      // 7. Return to dashboard
      onNodeWithTag("backButton").performClick()
      navigationActions.goBack()
      waitForIdle()

      // 8. Clean up
      onAllNodesWithTag("delete_icon").onFirst().performScrollTo().performClick()

      // Verify widget removed
      onNodeWithTag("widget_card").assertDoesNotExist()
    }
  }
}

@HiltViewModel
class FakeTimerViewModel @Inject constructor() : TimerViewModel(mock()) {
  private val _timerState =
      MutableStateFlow(
          TimerState(
              isPaused = true,
              remainingSeconds = DEFAULT_FOCUS_TIME,
              currentTimerType = TimerType.POMODORO,
              focusTime = DEFAULT_FOCUS_TIME,
              shortBreakTime = DEFAULT_SHORT_BREAK_TIME,
              longBreakTime = DEFAULT_LONG_BREAK_TIME,
              cycles = DEFAULT_CYCLES,
              currentCycle = 1))
  override val timerState: StateFlow<TimerState> = _timerState

  override fun startTimer() {
    _timerState.value = _timerState.value.copy(isPaused = false)
    // Simulate time passing
    _timerState.value =
        _timerState.value.copy(
            remainingSeconds =
                _timerState.value.remainingSeconds - 60 // Decrease by 1 minute for clear testing
            )
  }

  override fun stopTimer() {
    _timerState.value = _timerState.value.copy(isPaused = true)
  }

  override fun resetTimer() {
    val currentState = _timerState.value
    _timerState.value =
        currentState.copy(
            remainingSeconds =
                currentState.focusTime, // Use focus time as we're resetting to POMODORO
            isPaused = true,
            currentTimerType = TimerType.POMODORO, // Always reset to POMODORO
            currentCycle = 1)
  }

  override fun skipToNextTimer() {
    val currentState = _timerState.value
    val (newType, newTime) =
        when (currentState.currentTimerType) {
          TimerType.POMODORO -> TimerType.SHORT_BREAK to currentState.shortBreakTime
          TimerType.SHORT_BREAK -> TimerType.LONG_BREAK to currentState.longBreakTime
          TimerType.LONG_BREAK -> TimerType.POMODORO to currentState.focusTime
        }
    _timerState.value =
        currentState.copy(currentTimerType = newType, remainingSeconds = newTime, isPaused = true)
  }

  override fun updateSettings(focusTime: Long, shortBreak: Long, longBreak: Long, cycles: Int) {
    _timerState.value =
        _timerState.value.copy(
            focusTime = focusTime,
            shortBreakTime = shortBreak,
            longBreakTime = longBreak,
            cycles = cycles,
            remainingSeconds = focusTime, // Set remaining seconds to new focus time
            isPaused = true,
            currentTimerType = TimerType.POMODORO)
  }

  companion object {
    private const val DEFAULT_FOCUS_TIME = 1500L // 25 minutes
    private const val DEFAULT_SHORT_BREAK_TIME = 300L // 5 minutes
    private const val DEFAULT_LONG_BREAK_TIME = 900L // 15 minutes
    private const val DEFAULT_CYCLES = 4
  }
}

class FakePomodoroNavigationActions : NavigationActions(mockk(relaxed = true)) {
  private var _currentRoute = mutableStateOf("DASHBOARD")

  fun navigateToPomodoro() {
    _currentRoute.value = "POMODORO"
  }

  override fun navigateTo(destination: TopLevelDestination) {
    _currentRoute.value = destination.route
  }

  override fun navigateTo(route: String) {
    _currentRoute.value = route
  }

  override fun goBack() {
    _currentRoute.value = "DASHBOARD"
  }

  override fun currentRoute(): String = _currentRoute.value
}

@Composable
fun TestPomodoroNavigation(
    dashboardViewModel: FakeDashboardViewModel,
    timerViewModel: FakeTimerViewModel,
    navigationActions: FakePomodoroNavigationActions
) {
  var currentScreen by remember { mutableStateOf("DASHBOARD") }

  if (navigationActions is FakePomodoroNavigationActions) {
    currentScreen = navigationActions.currentRoute()
  }

  when (currentScreen) {
    "POMODORO" ->
        PomodoroScreen(navigationActions = navigationActions, timerViewModel = timerViewModel)
    else -> DashboardScreen(viewModel = dashboardViewModel, navigationActions = navigationActions)
  }
}
