package com.github.se.eduverse.ui.assistant

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import com.github.se.eduverse.repository.AiAssistantRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.AiAssistantViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

/**
 * UI tests for the AiAssistantScreen. These tests validate the visual and interactive components of
 * the AI Assistant screen.
 */
class AiAssistantScreenTest {

  /** Rule for managing Compose testing lifecycle. */
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navigationActions: NavigationActions
  private lateinit var fakeViewModel: FakeAiAssistantViewModel

  /**
   * A fake ViewModel implementation for testing purposes. Allows the simulation of various
   * ViewModel states.
   */
  class FakeAiAssistantViewModel :
      AiAssistantViewModel(repository = Mockito.mock(AiAssistantRepository::class.java)) {

    val conversationFlow = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val isLoadingFlow = MutableStateFlow(false)
    val errorMessageFlow = MutableStateFlow<String?>(null)

    override val conversation = conversationFlow
    override val isLoading = isLoadingFlow
    override val errorMessage = errorMessageFlow

    /**
     * Simulates a new response from the AI assistant.
     *
     * @param question The user's question.
     * @param answer The assistant's response.
     */
    fun simulateResponse(question: String, answer: String) {
      conversationFlow.value = conversationFlow.value + (question to answer)
    }

    /**
     * Simulates an error state with a given message.
     *
     * @param message The error message to simulate.
     */
    fun simulateError(message: String) {
      errorMessageFlow.value = message
    }

    /**
     * Simulates the loading state.
     *
     * @param loading Whether the screen should display a loading indicator.
     */
    fun setLoading(loading: Boolean) {
      isLoadingFlow.value = loading
    }
  }

  /** Sets up the test environment before each test. */
  @Before
  fun setUp() {
    navController = Mockito.mock(NavHostController::class.java)
    navigationActions = NavigationActions(navController)
    fakeViewModel = FakeAiAssistantViewModel()

    composeTestRule.setContent {
      AiAssistantScreen(navigationActions = navigationActions, viewModel = fakeViewModel)
    }
  }

  /** Verifies the initial state and visibility of UI elements. */
  @Test
  fun testInitialUIElements() {
    composeTestRule.onNodeWithTag("aiAssistantChatScreenScaffold").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed().assertHasClickAction()
    composeTestRule.onNodeWithTag("screenTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("aiAssistantChatScreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("aiAssistantMessageList").assertIsDisplayed()
    composeTestRule.onNodeWithTag("assistantInputRow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("assistantQuestionInput").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ask a question").assertIsDisplayed()
    composeTestRule.onNodeWithTag("askAssistantButton").assertIsDisplayed().assertHasClickAction()
  }

  /** Verifies the correct display of a question and answer in the conversation list. */
  @Test
  fun testAskQuestionSuccess() = runBlockingTest {
    fakeViewModel.simulateResponse("What is AI?", "Artificial Intelligence is...")

    composeTestRule.onNodeWithTag("assistantQuestionInput").performTextInput("What is AI?")
    composeTestRule.onNodeWithTag("askAssistantButton").performClick()

    composeTestRule.waitUntil {
      composeTestRule.onAllNodesWithTag("messageItem").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onAllNodesWithTag("messageItem").assertCountEquals(1)
    composeTestRule.onNodeWithText("What is AI?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Artificial Intelligence is...").assertIsDisplayed()
  }

  /** Verifies that an error message is displayed in an AlertDialog when an error occurs. */
  @Test
  fun testErrorMessage() = runBlockingTest {
    fakeViewModel.simulateError("An error occurred. Please try again.")

    // Wait for the AlertDialog to appear
    composeTestRule.waitUntil {
      composeTestRule
          .onAllNodesWithText("An error occurred. Please try again.")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify the AlertDialog is displayed with the correct error message
    composeTestRule.onNodeWithText("Error").assertIsDisplayed()
    composeTestRule.onNodeWithText("An error occurred. Please try again.").assertIsDisplayed()

    // Verify the confirm button is displayed and can dismiss the dialog
    composeTestRule.onNodeWithText("OK").assertIsDisplayed().performClick()

    // Verify that the dialog is dismissed after clicking the button
    composeTestRule.onNodeWithText("An error occurred. Please try again.").assertDoesNotExist()
  }

  /** Verifies that the loading indicator is displayed when loading and disappears after. */
  @Test
  fun testLoadingIndicator() = runBlockingTest {
    fakeViewModel.setLoading(true)
    composeTestRule.onNodeWithTag("assistantLoadingIndicator").assertIsDisplayed()

    fakeViewModel.setLoading(false)
    composeTestRule.onNodeWithTag("assistantLoadingIndicator").assertDoesNotExist()
  }
}
