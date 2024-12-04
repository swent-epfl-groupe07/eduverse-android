package com.github.se.eduverse.fake

import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import io.mockk.mockk

class FakeNavigationActions : NavigationActions(mockk(relaxed = true)) {

  private val navigationHistory = mutableListOf<String>()
  private var currentRoute: String = ""

  override fun navigateTo(destination: TopLevelDestination) {
    navigationHistory.add(destination.route)
    currentRoute = destination.route
  }

  override fun navigateTo(screen: String) {
    navigationHistory.add(screen)
    currentRoute = screen
  }

  override fun goBack() {
    if (navigationHistory.isNotEmpty()) {
      navigationHistory.removeAt(navigationHistory.lastIndex)
      currentRoute = navigationHistory.lastOrNull() ?: ""
    }
  }

  override fun currentRoute(): String {
    return currentRoute
  }

  fun getNavigationHistory(): List<String> = navigationHistory
}
