package com.github.se.eduverse.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationMenu(
    onTabSelect: (TopLevelDestination) -> Unit,
    tabList: List<TopLevelDestination>,
    selectedItem: String
) {
  NavigationBar(
      modifier =
          Modifier.fillMaxWidth()
              .height(70.dp)
              .background(
                  Brush.horizontalGradient(
                      colors =
                          listOf(
                              MaterialTheme.colorScheme.primary,
                              MaterialTheme.colorScheme.secondary)))
              .testTag("bottomNavigationMenu"),
      containerColor = Color.Transparent) {
        tabList.forEach { tab ->
          val isSelected = selectedItem == tab.route
          val animatedColor by
              animateColorAsState(
                  if (isSelected) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface)
          NavigationBarItem(
              icon = { Icon(tab.icon, contentDescription = null, tint = animatedColor) },
              label = null,
              selected = isSelected,
              onClick = { onTabSelect(tab) },
              modifier = Modifier.clip(RoundedCornerShape(50.dp)).testTag(tab.textId))
        }
      }
}
