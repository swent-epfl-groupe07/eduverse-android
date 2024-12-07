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

    // Message list
    composeTestRule.onNodeWithTag("aiAssistantMessageList").assertIsDisplayed()

    // Input field and related tags
    composeTestRule.onNodeWithTag("assistantInputRow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("assistantQuestionInput").assertIsDisplayed()

    composeTestRule.onNodeWithText("Ask a question").assertIsDisplayed()

    composeTestRule.onNodeWithTag("askAssistantButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testBackButton() {
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun testAskQuestionSuccess(): Unit = runBlocking {
    composeTestRule.onNodeWithTag("assistantQuestionInput").performTextInput("What is AI?")
    composeTestRule.onNodeWithTag("askAssistantButton").performClick()

    composeTestRule.waitUntil {
      composeTestRule.onAllNodesWithTag("messageItem").fetchSemanticsNodes().isNotEmpty()
    }

    // Vérifier que le message apparaît bien
    composeTestRule.onAllNodesWithTag("messageItem").assertCountEquals(1)
    composeTestRule.onNodeWithText("What is AI?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Fake answer to 'What is AI?'").assertIsDisplayed()
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
