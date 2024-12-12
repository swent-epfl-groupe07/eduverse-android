package com.github.se.eduverse.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationBar(
    navigationActions: NavigationActions,
    actions: @Composable() (RowScope.() -> Unit) = {},
    screenTitle: String?
) {
  TopAppBar(
      modifier =
          Modifier.fillMaxWidth()
              .background(
                  Brush.horizontalGradient(
                      colors =
                          listOf(
                              MaterialTheme.colorScheme.secondary,
                              MaterialTheme.colorScheme.primary)))
              .testTag("topNavigationBar"),
      title = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          if (screenTitle == null) {
            // Display the app logo if no title is provided
            Image(
                painter = painterResource(id = R.drawable.eduverse_logo_png),
                contentDescription = "Logo",
                modifier = Modifier.size(140.dp).testTag("screenTitle"))
          } else {
            // Display the screen title if provided
            Text(
                text = screenTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("screenTitle"))
          }
        }
      },
      navigationIcon = {
        IconButton(
            onClick = { navigationActions.goBack() }, modifier = Modifier.testTag("goBackButton")) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onTertiary)
            }
      },
      actions = actions,
      colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
}
