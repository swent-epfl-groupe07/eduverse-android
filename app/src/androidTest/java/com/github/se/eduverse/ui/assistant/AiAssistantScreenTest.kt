package com.github.se.eduverse.ui.assistant

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import com.github.se.eduverse.repository.AiAssistantRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

// UI tests for AiAssistantScreen.
class AiAssistantScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navigationActions: NavigationActions
  private lateinit var fakeRepo: FakeAiAssistantRepository

  class FakeAiAssistantRepository :
      AiAssistantRepository(client = okhttp3.OkHttpClient(), apiKey = "fake-key") {
    var shouldFail = false

    override suspend fun askAssistant(userQuestion: String): String {
      if (shouldFail) throw Exception("Fake error")
      return "Fake answer to '$userQuestion'"
    }
  }

  @Before
  fun setUp() {
    navController = Mockito.mock(NavHostController::class.java)
    navigationActions = NavigationActions(navController)
    fakeRepo = FakeAiAssistantRepository()

    composeTestRule.setContent {
      AiAssistantScreen(navigationActions = navigationActions, assistantRepository = fakeRepo)
    }
  }

  @Test
  fun testInitialUIElements() {
    // Check main layout
    composeTestRule.onNodeWithTag("aiAssistantChatScreenScaffold").assertIsDisplayed()
    composeTestRule.onNodeWithTag("aiAssistantChatScreen").assertIsDisplayed()

    // Top bar
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topBarBackButton").assertIsDisplayed().assertHasClickAction()

    // Message list
    composeTestRule.onNodeWithTag("aiAssistantMessageList").assertIsDisplayed()

    // Input field and related tags
    composeTestRule.onNodeWithTag("assistantInputRow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("assistantQuestionInput").assertIsDisplayed()
    // The placeholder should be visible initially
    composeTestRule.onNodeWithTag("assistantPlaceholder").assertIsDisplayed()
    composeTestRule.onNodeWithTag("askAssistantButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testBackButton() {
    // Clicking back button should pop back stack
    composeTestRule.onNodeWithTag("topBarBackButton").performClick()
    Mockito.verify(navController).popBackStack()
  }

  @Test
  fun testAskQuestionSuccess(): Unit = runBlocking {
    composeTestRule.onNodeWithTag("assistantQuestionInput").performTextInput("What is AI?")

    composeTestRule.onNodeWithTag("askAssistantButton").performClick()

    // Loading should appear first
    composeTestRule.onNodeWithTag("assistantLoadingRow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("assistantLoadingIndicator").assertIsDisplayed()

    // Wait for the answer to appear
    composeTestRule.waitUntil {
      composeTestRule.onAllNodesWithTag("messageItem").fetchSemanticsNodes().isNotEmpty()
    }

    // Loading disappears, message is shown
    composeTestRule.onAllNodesWithTag("assistantLoadingRow").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("messageItem").assertCountEquals(1)

    // Check the message content
    composeTestRule
        .onNodeWithTag("questionText")
        .assertIsDisplayed()
        .assertTextContains("What is AI?")
    composeTestRule
        .onNodeWithTag("answerText")
        .assertIsDisplayed()
        .assertTextContains("Fake answer to 'What is AI?'")
  }

  @Test
  fun testErrorMessage(): Unit = runBlocking {
    fakeRepo.shouldFail = true

    composeTestRule.onNodeWithTag("assistantQuestionInput").performTextInput("Will it fail?")

    composeTestRule.onNodeWithTag("askAssistantButton").performClick()

    // Wait for the error to appear
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
}
