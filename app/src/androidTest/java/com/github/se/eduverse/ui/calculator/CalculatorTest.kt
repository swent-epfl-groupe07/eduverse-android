package com.github.se.eduverse.ui.calculator

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.ui.navigation.NavigationActions
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

  @Test
  fun testFullCalculatorFunctionality() {

    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed().assertHasClickAction()

    composeTestRule.onNodeWithTag("displayText").assertIsDisplayed().assertTextEquals("")

    composeTestRule.onNodeWithTag("button_1").performClick()
    composeTestRule.onNodeWithTag("displayText").assertTextContains("1")

    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("displayText").assertTextContains("1+")

    composeTestRule.onNodeWithTag("button_2").performClick()
    composeTestRule.onNodeWithTag("displayText").assertTextContains("1+2")

    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("3")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("displayText").assertTextEquals("")

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
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("5")

    composeTestRule.onNodeWithTag("button_4").performClick()
    composeTestRule.onNodeWithTag("button_-").performClick()
    composeTestRule.onNodeWithTag("button_3").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("1")

    composeTestRule.onNodeWithTag("button_(").performClick()
    composeTestRule.onNodeWithTag("button_2").performClick()
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("button_3").performClick()
    composeTestRule.onNodeWithTag("button_)").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_4").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("20")

    composeTestRule.onNodeWithTag("button_4").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("displayText").assertTextContains("45")
    composeTestRule.onNodeWithTag("backspaceButton").performClick()
    composeTestRule.onNodeWithTag("displayText").assertTextEquals("4")

    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("55")

    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_+").performClick()
    composeTestRule.onNodeWithTag("button_×").performClick()
    composeTestRule.onNodeWithTag("button_5").performClick()
    composeTestRule.onNodeWithTag("button_=").performClick()
    composeTestRule.onNodeWithTag("resultText").assertTextEquals("Undefined")

    composeTestRule.onNodeWithTag("clearButton").performClick()
    composeTestRule.onNodeWithTag("displayText").assertTextEquals("")

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
  }
}
