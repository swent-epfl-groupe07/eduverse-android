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
import com.github.se.eduverse.ui.theme.md_theme_light_onTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationBar(
    navigationActions: NavigationActions,
    actions: @Composable() (RowScope.() -> Unit) = {}
) {
  TopAppBar(
      modifier =
          Modifier.fillMaxWidth()
              .background(
                  Brush.horizontalGradient(
                      colors =
                          listOf(
                              MaterialTheme.colorScheme.secondary,
                              MaterialTheme.colorScheme.primary))),
      title = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Image(
              painter = painterResource(id = R.drawable.eduverse_logo_png),
              contentDescription = "Logo",
              modifier = Modifier.size(140.dp).testTag("centerImage"))
        }
      },
      navigationIcon = {
        IconButton(
            onClick = { navigationActions.goBack() }, modifier = Modifier.testTag("goBackButton")) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = null,
                  tint = md_theme_light_onTertiary)
            }
      },
      actions = actions,
      colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
}
