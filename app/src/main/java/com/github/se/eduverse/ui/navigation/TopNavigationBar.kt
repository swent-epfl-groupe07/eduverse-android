package com.github.se.eduverse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationBar(screenTitle: String, navigationActions: NavigationActions) {
  TopAppBar(
      modifier = Modifier.testTag("topNavigationBar"),
      title = { Text(screenTitle, Modifier.testTag("screenTitle")) },
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag("goBackButton"), onClick = { navigationActions.goBack() }) {
              Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
      })
}
