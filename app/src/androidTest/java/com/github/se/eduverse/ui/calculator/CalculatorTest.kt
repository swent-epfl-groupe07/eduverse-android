package com.github.se.eduverse.ui.calculator

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class CalculatorScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navigationActions: NavigationActions

  @Before
  fun setUp() {
    Intents.init()

    navController = Mockito.mock(NavHostController::class.java)
    navigationActions = NavigationActions(navController)

    composeTestRule.setContent { CalculatorScreen(navigationActions = navigationActions) }
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  private fun SemanticsNodeInteraction.assertDisplayTextEquals(expectedText: String) {
    val actualTextList = this.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)
    val actualText = actualTextList?.joinToString(separator = "") { it.text } ?: ""
    val actualTextWithoutCursor = actualText.replace("|", "")
    assertEquals(expectedText, actualTextWithoutCursor)
  }

  @Test
  fun testFullCalculatorFunctionality() {

    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed().assertHasClickAction()

    composeTestRule.onNodeWithTag("displayText").assertIsDisplayed().assertDisplayTextEquals("")

    composeTestRule.onNodeWithTag("button_1").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("1")

    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("1+")

    composeTestRule.onNodeWithTag("button_2").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("1+2")

    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("3")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("")

    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_6").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("30")

    composeTestRule.onNodeWithTag("button_1").performClick()
    composeTestRule.onNodeWithTag("button_0").performClick()
    composeTestRule.onNodeWithTag("button_/").performClick()
    composeTestRule.onNodeWithTag("button_2").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("1525")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("button_4").performClick()
    composeTestRule.onNodeWithTag("button_-").performClick()
    composeTestRule.onNodeWithTag("button_3").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("1")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("button_(").performClick()
    composeTestRule.onNodeWithTag("button_2").performClick()
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("button_3").performClick()
    composeTestRule.onNodeWithTag("button_)").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_4").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("20")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("button_4").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("45")
    composeTestRule.onNodeWithTag("backspaceButton").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("4")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("15")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("Undefined")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("")

    composeTestRule.onNodeWithTag("button_1").performClick()
    composeTestRule.onNodeWithTag("button_2").performClick()
    composeTestRule.onNodeWithTag("button_.").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("button_7").performClick()
    composeTestRule.onNodeWithTag("button_.").performClick()
    composeTestRule.onNodeWithTag("button_3").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("19.8")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("button_9").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_8").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_7").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_6").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_4").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_3").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_2").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_1").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("362880")

    // Test "Take Result"
    composeTestRule.onNodeWithTag("Take Result").performClick()
    composeTestRule.onNodeWithTag("button_-").performClick()
    composeTestRule.onNodeWithTag("button_1").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("362879")

    // Test cursor movement with left and right buttons
    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("button_1").performClick()
    composeTestRule.onNodeWithTag("button_2").performClick()
    composeTestRule.onNodeWithTag("button_3").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("123")

    // Move cursor left twice
    composeTestRule.onNodeWithContentDescription("Left Arrow").performClick()
    composeTestRule.onNodeWithContentDescription("Left Arrow").performClick()

    // Insert a "+" at cursor position
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("1+23")

    // Move cursor right once
    composeTestRule.onNodeWithContentDescription("Right Arrow").performClick()

    // Insert a "4" at cursor position
    composeTestRule.onNodeWithTag("button_4").performClick()
    composeTestRule.onNodeWithTag("displayText").assertDisplayTextEquals("1+243")

    // Evaluate the expression
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("244")

    // Test history functionality
    // Open history dialog
    composeTestRule.onNodeWithContentDescription("History").performClick()
    composeTestRule.onNodeWithText("History").assertIsDisplayed()
    composeTestRule.onNodeWithText("1+243 = 244").assertIsDisplayed()

    // Close history dialog
    composeTestRule.onNodeWithText("Close").performClick()
    composeTestRule.onNodeWithText("History").assertDoesNotExist()
  }
}
