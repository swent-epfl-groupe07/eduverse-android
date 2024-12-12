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

// UI tests for AiAssistantScreen.
class AiAssistantScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navigationActions: NavigationActions
  private lateinit var fakeViewModel: FakeAiAssistantViewModel

  class FakeAiAssistantViewModel :
      AiAssistantViewModel(repository = Mockito.mock(AiAssistantRepository::class.java)) {
    val conversationFlow = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val isLoadingFlow = MutableStateFlow(false)
    val errorMessageFlow = MutableStateFlow<String?>(null)

    override val conversation = conversationFlow
    override val isLoading = isLoadingFlow
    override val errorMessage = errorMessageFlow

    fun simulateResponse(question: String, answer: String) {
      conversationFlow.value = conversationFlow.value + (question to answer)
    }

    fun simulateError(message: String) {
      errorMessageFlow.value = message
    }

    fun setLoading(loading: Boolean) {
      isLoadingFlow.value = loading
    }
  }

  @Before
  fun setUp() {
    navController = Mockito.mock(NavHostController::class.java)
    navigationActions = NavigationActions(navController)
    fakeViewModel = FakeAiAssistantViewModel()

    composeTestRule.setContent {
      AiAssistantScreen(navigationActions = navigationActions, viewModel = fakeViewModel)
    }
  }

  @Test
  fun testInitialUIElements() {
    // Check main layout
    composeTestRule.onNodeWithTag("aiAssistantChatScreenScaffold").assertIsDisplayed()
    composeTestRule.onNodeWithTag("aiAssistantChatScreen").assertIsDisplayed()

    // Message list
    composeTestRule.onNodeWithTag("aiAssistantMessageList").assertIsDisplayed()

    // Input field and related tags
    composeTestRule.onNodeWithTag("assistantInputRow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("assistantQuestionInput").assertIsDisplayed()

    composeTestRule.onNodeWithText("Ask a question").assertIsDisplayed()

    composeTestRule.onNodeWithTag("askAssistantButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testAskQuestionSuccess() = runBlockingTest {
    fakeViewModel.simulateResponse("What is AI?", "Artificial Intelligence is...")

    composeTestRule.onNodeWithTag("assistantQuestionInput").performTextInput("What is AI?")
    composeTestRule.onNodeWithTag("askAssistantButton").performClick()

    composeTestRule.waitUntil {
      composeTestRule.onAllNodesWithTag("messageItem").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify that the message appears correctly
    composeTestRule.onAllNodesWithTag("messageItem").assertCountEquals(1)
    composeTestRule.onNodeWithText("What is AI?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Artificial Intelligence is...").assertIsDisplayed()
  }

  @Test
  fun testErrorMessage() = runBlockingTest {
    fakeViewModel.simulateError("An error occurred. Please try again.")

    composeTestRule.waitUntil {
      composeTestRule
          .onAllNodesWithTag("assistantErrorMessageText")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Check the error message
    composeTestRule
        .onNodeWithTag("assistantErrorMessageText")
        .assertIsDisplayed()
        .assertTextContains("An error occurred. Please try again.")
  }

  @Test
  fun testLoadingIndicator() = runBlockingTest {
    fakeViewModel.setLoading(true)

    composeTestRule.onNodeWithTag("assistantLoadingIndicator").assertIsDisplayed()

    fakeViewModel.setLoading(false)

    composeTestRule.onNodeWithTag("assistantLoadingIndicator").assertDoesNotExist()
  }
}
