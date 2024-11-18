import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.TimerState
import com.github.se.eduverse.model.TimerType
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.repository.TodoRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.pomodoro.PomodoroScreen
import com.github.se.eduverse.viewmodel.TimerViewModel
import com.github.se.eduverse.viewmodel.TodoListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

@RunWith(AndroidJUnit4::class)
class PomodoroScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockTodoRepository: TodoRepository
  private lateinit var todoListViewModel: TodoListViewModel
  private val fakeViewModel =
      FakeTimerViewModel(
          TimerState(
              remainingSeconds = 1500,
              isPaused = true,
              currentTimerType = TimerType.POMODORO,
              focusTime = 1500,
              shortBreakTime = 300,
              longBreakTime = 900,
              cycles = 4,
              currentCycle = 1))
  private val mockNavigationActions = FakeNavigationActions(navController = mock())

  private val sampleTodos =
      listOf(
          Todo("1", "Test Todo 1", 0, TodoStatus.ACTUAL, "uid"),
          Todo("2", "Test Todo 2", 0, TodoStatus.DONE, "uid"))
  private var todos = sampleTodos

  @Before
  fun setup() {
    mockTodoRepository = mock(TodoRepository::class.java)
    todoListViewModel = TodoListViewModel(mockTodoRepository)
    `when`(mockTodoRepository.getActualTodos(any(), any(), any())).then {
      it.getArgument<(List<Todo>) -> Unit>(1)(
          todos.filter { todo -> todo.status == TodoStatus.ACTUAL })
    }
    `when`(mockTodoRepository.getDoneTodos(any(), any(), any())).then {
      it.getArgument<(List<Todo>) -> Unit>(1)(
          todos.filter { todo -> todo.status == TodoStatus.DONE })
    }
    `when`(mockTodoRepository.deleteTodoById(any(), any(), any())).then {
      val todoToDeleteId = it.getArgument<String>(0)
      todos = todos.filter { todo -> todo.uid != todoToDeleteId }
      it.getArgument<() -> Unit>(1)()
    }
  }

  @Test
  fun testPomodoroScreenInitialState() {
    setupPomodoroScreen()

    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timerDisplay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timerText").assertTextEquals("25:00")
    composeTestRule.onNodeWithTag("cycleText").assertTextEquals("Cycle: 1/4")
    composeTestRule.onNodeWithTag("playPauseButton").assertIsDisplayed()
  }

  @Test
  fun testPlayPauseButton() {
    setupPomodoroScreen()

    // Simulate button click to start the timer
    composeTestRule.onNodeWithTag("playPauseButton").performClick()
    assert(!fakeViewModel.timerState.value.isPaused)

    // Simulate button click to stop the timer
    composeTestRule.onNodeWithTag("playPauseButton").performClick()
    assert(fakeViewModel.timerState.value.isPaused)
  }

  @Test
  fun testOpenAndCloseSettingsDialog() {
    setupPomodoroScreen()

    // Open the settings dialog
    composeTestRule.onNodeWithTag("settingsButton").performClick()
    composeTestRule.onNodeWithTag("settingsDialog").assertIsDisplayed()

    // Close the dialog
    composeTestRule.onNodeWithTag("settingsCancelButton").performClick()
    composeTestRule.onNodeWithTag("settingsDialog").assertDoesNotExist()
  }

  @Test
  fun testUpdateSettingsInDialog() {
    setupPomodoroScreen()

    // Open settings dialog
    composeTestRule.onNodeWithTag("settingsButton").performClick()

    // Interact with sliders
    composeTestRule.onNodeWithTag("Focus Time Slider").performGesture {
      this.swipeRight()
    } // 60 minutes
    composeTestRule.onNodeWithTag("Short Break Slider").performGesture {
      this.swipeRight()
    } // 30 minutes

    // Save settings
    composeTestRule.onNodeWithTag("settingsSaveButton").performClick()

    // Verify if ViewModel was updated
    assert(fakeViewModel.timerState.value.focusTime == 3600L)
    assert(fakeViewModel.timerState.value.shortBreakTime == 1800L)
  }

  @Test
  fun testResetAndSkipButtons() {
    setupPomodoroScreen()

    // Reset Timer
    composeTestRule.onNodeWithTag("resetButton").performClick()
    assert(fakeViewModel.timerState.value.remainingSeconds.toInt() == 1500)

    // Skip Timer
    composeTestRule.onNodeWithTag("skipButton").performClick()
    assert(fakeViewModel.timerState.value.currentTimerType == TimerType.SHORT_BREAK)
  }

  @Test
  fun testSelectTodoButtonIsCorrectlyDisplayed() {
    setupPomodoroScreen()
    todoListViewModel.currentUid = "uid"
    todoListViewModel.getActualTodos()
    composeTestRule.onNodeWithTag("selectTodoButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("selectTodoButton").assertHasClickAction()
    composeTestRule.onNodeWithTag("selectTodoButton").assertTextContains("Choose a Todo to work on")
    composeTestRule
        .onNodeWithTag("selectTodoButton")
        .assertIsEnabled() // Test button is enabled when actual todos list is not empty
    todos = emptyList()
    todoListViewModel.getActualTodos()
    composeTestRule
        .onNodeWithTag("selectTodoButton")
        .assertIsNotEnabled() // Test button is disabled when actual todos list is empty
  }

  @Test
  fun testSelectTodoDialogIsCorrectlyDisplayed() {
    setupPomodoroScreen()
    todoListViewModel.currentUid = "uid"
    todoListViewModel.getActualTodos()
    todoListViewModel.getDoneTodos()
    composeTestRule.onNodeWithTag("selectTodoButton").performClick()
    composeTestRule.onNodeWithTag("selectTodoDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("selectTodoDialogTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("selectTodoDialogTitle").assertTextEquals("Select Todo")
    composeTestRule.onNodeWithTag("selectTodoDismissButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOption_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("todoOption_1").assertTextContains("Test Todo 1")
    composeTestRule.onNodeWithTag("todoOption_2").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectTodoDismissButton").performClick()
    // Test that dismiss button is clicked the dialog is closed and no todo is selected
    composeTestRule.onNodeWithTag("selectTodoDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectTodoButton").assertIsDisplayed()
    assert(todoListViewModel.selectedTodo.value == null)
  }

  @Test
  fun testSelectedTodoIsCorrectlyDisplayed() {
    setupPomodoroScreen()
    todoListViewModel.currentUid = "uid"
    todoListViewModel.getActualTodos()
    todoListViewModel.getDoneTodos()
    composeTestRule.onNodeWithTag("selectTodoButton").performClick()
    composeTestRule.onNodeWithTag("todoOption_1").performClick()
    assert(todoListViewModel.selectedTodo.value == sampleTodos[0])
    composeTestRule.onNodeWithTag("selectTodoDialog").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("selectTodoButton").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("unselectTodoButton").assertIsDisplayed()

    // Test the unselect button on click behaviour
    composeTestRule.onNodeWithTag("unselectTodoButton").performClick()
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectTodoButton").assertIsDisplayed()
    assert(todoListViewModel.selectedTodo.value == null)
  }

  @Test
  fun testTodoIsNoMoreDisplayedWhenDeleted() {
    setupPomodoroScreen()

    todoListViewModel.currentUid = "uid"
    todoListViewModel.getActualTodos()
    todoListViewModel.getDoneTodos()
    todoListViewModel.selectTodo(sampleTodos[0])
    composeTestRule.onNodeWithTag("todoItem_1").assertIsDisplayed()
    todoListViewModel.deleteTodo(sampleTodos[0].uid)
    assert(todoListViewModel.selectedTodo.value == null)
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectTodoButton").assertIsDisplayed()
    todos = sampleTodos
  }

  @Test
  fun testTodoIsNoMoreDisplayedWhenSetToDone() {
    setupPomodoroScreen()

    `when`(mockTodoRepository.updateTodo(any(), any(), any())).then {
      todos -= it.getArgument<Todo>(0).copy(status = TodoStatus.ACTUAL)
      todos += it.getArgument<Todo>(0)
      it.getArgument<() -> Unit>(1)()
    }

    todoListViewModel.currentUid = "uid"
    todoListViewModel.getActualTodos()
    todoListViewModel.getDoneTodos()
    todoListViewModel.selectTodo(sampleTodos[0])
    composeTestRule.onNodeWithTag("todoDoneButton_1").performClick()
    assert(todoListViewModel.selectedTodo.value == null)
    composeTestRule.onNodeWithTag("todoItem_1").assertIsNotDisplayed()
    composeTestRule.onNodeWithTag("selectTodoButton").assertIsDisplayed()
    todos = sampleTodos
  }

  private fun setupPomodoroScreen() {
    composeTestRule.setContent {
      PomodoroScreen(
          navigationActions = mockNavigationActions,
          timerViewModel = fakeViewModel,
          todoListViewModel = todoListViewModel)
    }
  }
}

class FakeTimerViewModel(initialState: TimerState) : TimerViewModel() {
  private val _timerState = MutableStateFlow(initialState)
  override val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

  override fun startTimer() {
    _timerState.value = _timerState.value.copy(isPaused = false)
  }

  override fun stopTimer() {
    _timerState.value = _timerState.value.copy(isPaused = true)
  }

  override fun resetTimer() {
    _timerState.value = _timerState.value.copy(remainingSeconds = _timerState.value.focusTime)
  }

  override fun skipToNextTimer() {
    when (_timerState.value.currentTimerType) {
      TimerType.POMODORO ->
          _timerState.value = _timerState.value.copy(currentTimerType = TimerType.SHORT_BREAK)
      TimerType.SHORT_BREAK ->
          if (_timerState.value.currentCycle < _timerState.value.cycles) {
            _timerState.value =
                _timerState.value.copy(
                    currentTimerType = TimerType.POMODORO,
                    currentCycle = _timerState.value.currentCycle + 1)
          } else {
            _timerState.value =
                _timerState.value.copy(currentTimerType = TimerType.LONG_BREAK, currentCycle = 1)
          }
      TimerType.LONG_BREAK ->
          _timerState.value = _timerState.value.copy(currentTimerType = TimerType.POMODORO)
    }
  }

  override fun updateSettings(
      focusTime: Long,
      shortBreakTime: Long,
      longBreakTime: Long,
      cycles: Int
  ) {
    _timerState.value =
        _timerState.value.copy(
            focusTime = focusTime,
            shortBreakTime = shortBreakTime,
            longBreakTime = longBreakTime,
            cycles = cycles)
  }
}

class FakeNavigationActions(navController: NavHostController) : NavigationActions(navController) {
  fun navigate(route: String) {
    // No-op for testing
  }
}
