package com.github.se.eduverse.ui.others

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.github.se.eduverse.ui.navigation.Screen

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
                  onClick = { navigationActions.navigateToSetting() },
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

              // Pdf Converter
              Button(
                  onClick = { navigationActions.navigateTo(Screen.PDF_CONVERTER) },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(50.dp)
                          .testTag("pdfConverterButton") // Test tag for Field #4 button
                  ) {
                    Text(text = "Pdf Converter", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }

              Spacer(modifier = Modifier.height(16.dp))

              Button(
                  onClick = { navigationActions.navigateTo(Screen.POMODORO) },
                  modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pomodoroButton")) {
                    Text(text = "Pomodoro Timer", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // List Folders button
              Button(
                  onClick = { navigationActions.navigateTo(Screen.LIST_FOLDERS) },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(50.dp)
                          .testTag("field4Button") // Test tag for Field #4 button
                  ) {
                    Text(text = "Courses", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }
              Spacer(modifier = Modifier.height(16.dp))
              Button(
                  onClick = { navigationActions.navigateTo(Screen.CALCULATOR) },
                  modifier = Modifier.fillMaxWidth().height(50.dp).testTag("CalculatorButton")) {
                    Text(text = "Calculator", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }

              Spacer(modifier = Modifier.height(16.dp))

              Button(
                  onClick = { navigationActions.navigateTo(Screen.GALLERY) },
                  modifier = Modifier.fillMaxWidth().height(50.dp).testTag("GALLERYSCREENBTTN")) {
                    Text(text = "GALLERY", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                  }
            }
      })
}
