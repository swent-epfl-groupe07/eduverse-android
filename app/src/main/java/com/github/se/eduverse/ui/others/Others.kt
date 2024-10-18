package com.github.se.eduverse.ui.others

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions

@Composable
fun OthersScreen(navigationActions: NavigationActions) {
  Scaffold(
      modifier = Modifier.testTag("othersScreen"),
      bottomBar = {
        BottomNavigationMenu(
            onTabSelect = { route -> navigationActions.navigateTo(route) },
            tabList = LIST_TOP_LEVEL_DESTINATION,
            selectedItem = navigationActions.currentRoute())
      },
      content = { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top) {
              Spacer(modifier = Modifier.height(32.dp))

              // Settings button
              Button(
                  onClick = { /* Navigate to Settings */},
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(50.dp)
                          .testTag("settingsButton") // Test tag for the Settings button
                  ) {
                    Text(text = "Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Profile button
              Button(
                  onClick = { navigationActions.navigateToProfile() },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(50.dp)
                          .testTag("profileButton") // Test tag for the Profile button
                  ) {
                    Text(text = "Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // About button
              Button(
                  onClick = { /* Handle About Action */},
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(50.dp)
                          .testTag("aboutButton") // Test tag for the About button
                  ) {
                    Text(text = "About", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Field #4 button
              Button(
                  onClick = { /* Placeholder for Field #4 */},
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(50.dp)
                          .testTag("field4Button") // Test tag for Field #4 button
                  ) {
                    Text(text = "Field #4", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Field #5 button
              Button(
                  onClick = { /* Placeholder for Field #5 */},
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(50.dp)
                          .testTag("field5Button") // Test tag for Field #5 button
                  ) {
                    Text(text = "Field #5", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }
            }
      })
}
