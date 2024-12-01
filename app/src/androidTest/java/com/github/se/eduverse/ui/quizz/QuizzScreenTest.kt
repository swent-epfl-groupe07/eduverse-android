package com.github.se.eduverse.ui.quizz

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import com.github.se.eduverse.model.Question
import com.github.se.eduverse.repository.QuizzRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class QuizScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navigationActions: NavigationActions
  private lateinit var quizzRepository: QuizzRepository

  class FakeQuizzRepository(client: OkHttpClient = OkHttpClient(), apiKey: String = "dummyApiKey") :
      QuizzRepository(client, apiKey) {

    var sampleQuestions: List<Question> = emptyList()
    var shouldThrowException: Boolean = false

    override suspend fun getQuestionsFromGPT(
        topic: String,
        difficulty: String,
        numberOfQuestions: Int
    ): List<Question> {
      if (shouldThrowException) {
        throw Exception("Simulated error")
      }
      return sampleQuestions
    }
  }

  @Before
  fun setUp() {
    navController = Mockito.mock(NavHostController::class.java)
    navigationActions = NavigationActions(navController)
    quizzRepository = FakeQuizzRepository()

    composeTestRule.setContent {
      QuizScreen(navigationActions = navigationActions, quizzRepository = quizzRepository)
    }
  }

  @Test
  fun testInitialUIElementsAreDisplayed() {

    composeTestRule.onNodeWithTag("topicInput").assertIsDisplayed()
    composeTestRule.onNodeWithTag("generateQuizButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("difficultyButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("questionsButton").assertIsDisplayed()
  }

  @Test
  fun testTopicInputField() {
    composeTestRule.onNodeWithTag("topicInput").performTextInput("Math")
    composeTestRule.onNodeWithTag("topicInput").assertTextContains("Math")
  }

  @Test
  fun testDifficultySelection() {
    composeTestRule.onNodeWithTag("difficultyButton").performClick()
    composeTestRule.onNodeWithTag("difficultyOption_hard").performClick()
    composeTestRule.onNodeWithTag("difficultyButton").assertTextContains("Difficulty: hard")
  }

  @Test
  fun testNumberOfQuestionsSelection() {
    composeTestRule.onNodeWithTag("questionsButton").performClick()
    composeTestRule.onNodeWithTag("questionsOption_15").performClick()
    composeTestRule.onNodeWithTag("questionsButton").assertTextContains("Questions: 15")
  }

  @Test
  fun testGenerateQuizButtonFunctionality() {
    runBlocking {
      val sampleQuestions =
          listOf(
              Question(
                  text = "What is 2 + 2?",
                  answers = listOf("A. 3", "B. 4", "C. 5", "D. 6"),
                  correctAnswer = "B. 4"))
      (quizzRepository as FakeQuizzRepository).sampleQuestions = sampleQuestions

      composeTestRule.onNodeWithTag("topicInput").performTextInput("Math")
      composeTestRule.onNodeWithTag("generateQuizButton").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("questionsList").assertIsDisplayed()
      composeTestRule.onNodeWithTag("questionText_0").assertTextContains("What is 2 + 2?")
    }
  }

  @Test
  fun testAnswerSelection() {
    runBlocking {
      val sampleQuestions =
          listOf(
              Question(
                  text = "What is the capital of France?",
                  answers = listOf("A. Berlin", "B. Madrid", "C. Paris", "D. Rome"),
                  correctAnswer = "C. Paris"))
      (quizzRepository as FakeQuizzRepository).sampleQuestions = sampleQuestions

      composeTestRule.onNodeWithTag("topicInput").performTextInput("Geography")
      composeTestRule.onNodeWithTag("generateQuizButton").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("radioButton_0_2").performClick()

      composeTestRule.onNodeWithTag("radioButton_0_2").assertIsSelected()
    }
  }

  @Test
  fun testSubmitQuizButtonFunctionality() {
    runBlocking {
      val sampleQuestions =
          listOf(
              Question(
                  text = "What is 3 + 5?",
                  answers = listOf("A. 7", "B. 8", "C. 9", "D. 10"),
                  correctAnswer = "B. 8"))
      (quizzRepository as FakeQuizzRepository).sampleQuestions = sampleQuestions

      composeTestRule.onNodeWithTag("topicInput").performTextInput("Math")
      composeTestRule.onNodeWithTag("generateQuizButton").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("radioButton_0_0").performClick()

      composeTestRule.onNodeWithTag("submitQuizButton").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("scoreText").assertIsDisplayed()
      composeTestRule.onNodeWithTag("scoreText").assertTextContains("Your Score: 0 / 1")
    }
  }

  @Test
  fun testBackButtonFunctionality() {
    composeTestRule.onNodeWithTag("goBackButton").performClick()
    Mockito.verify(navController).popBackStack()
  }
}
