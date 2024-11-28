package com.github.se.eduverse.ui.notifications

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.NotifAuthorizations
import com.github.se.eduverse.ui.navigation.NavigationActions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class NotificationsScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val notifAuthorizations = NotifAuthorizations(true, true)
  private lateinit var navigationActions: NavigationActions
  private lateinit var context: Context
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var editor: Editor

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    context = mock(Context::class.java)
    sharedPreferences = mock(SharedPreferences::class.java)
    editor = mock(Editor::class.java)

    `when`(context.getSharedPreferences(any(), any())).thenReturn(sharedPreferences)
    `when`(sharedPreferences.edit()).thenReturn(editor)
    `when`(editor.putString(any(), any())).thenReturn(editor)

    composeTestRule.setContent {
      NotificationsScreen(notifAuthorizations, navigationActions, context)
    }
  }

  @Test
  fun displayComponent() {
    composeTestRule.onNodeWithTag("topNavigationBar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("taskSwitch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventSwitch").assertIsDisplayed()
  }

  @Test
  fun goBackWorks() {
    var test = false
    `when`(navigationActions.goBack()).then {
      test = true
      null
    }

    composeTestRule.onNodeWithTag("goBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("goBackButton").performClick()
    assert(test)
  }

  @Test
  fun taskSwitchTest() {
    composeTestRule.onNodeWithTag("taskSwitch").performClick()
    assert(!notifAuthorizations.taskEnabled)

    composeTestRule.onNodeWithTag("taskSwitch").performClick()
    assert(notifAuthorizations.taskEnabled)

    verify(2) { editor.apply() }
  }

  @Test
  fun eventSwitchTest() {
    composeTestRule.onNodeWithTag("eventSwitch").performClick()
    assert(!notifAuthorizations.eventEnabled)

    composeTestRule.onNodeWithTag("eventSwitch").performClick()
    assert(notifAuthorizations.eventEnabled)

    verify(2) { editor.apply() }
  }
}
