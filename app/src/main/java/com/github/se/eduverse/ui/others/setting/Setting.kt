package com.github.se.eduverse.ui.others.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.ui.navigation.NavigationActions

@Composable
fun SettingsScreen(navigationActions: NavigationActions) {
  var privacySettings by remember { mutableStateOf("") }
  var selectedTheme by remember { mutableStateOf("Light") }
  var selectedLanguage by remember { mutableStateOf("English") }
  var isThemeDropdownExpanded by remember { mutableStateOf(false) }
  var isLanguageDropdownExpanded by remember { mutableStateOf(false) }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
              OutlinedTextField(
                  value = privacySettings,
                  onValueChange = { privacySettings = it },
                  label = { Text(text = "Privacy Settings") },
                  modifier = Modifier.fillMaxWidth().testTag("privacySettingsInput"))
              Spacer(modifier = Modifier.height(8.dp))

              // Dropdown for Theme Selection
              Box(
                  modifier =
                      Modifier.fillMaxWidth().testTag("themeSelectionBox").clickable {
                        isThemeDropdownExpanded = !isThemeDropdownExpanded
                      } // Click handling here
                  ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                          Text(text = selectedTheme, modifier = Modifier.weight(1f))
                          Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                  }

              DropdownMenu(
                  expanded = isThemeDropdownExpanded,
                  onDismissRequest = { isThemeDropdownExpanded = false },
                  modifier = Modifier.testTag("themeDropdown")) {
                    listOf("Light", "Dark", "System Default").forEach { theme ->
                      DropdownMenuItem(
                          text = { Text(theme) },
                          onClick = {
                            selectedTheme = theme
                            isThemeDropdownExpanded = false
                          },
                          modifier = Modifier.testTag("themeOption_$theme"))
                    }
                  }

              Spacer(modifier = Modifier.height(8.dp))
              Box(
                  modifier =
                      Modifier.fillMaxWidth().testTag("languageSelectionBox").clickable {
                        isLanguageDropdownExpanded = !isLanguageDropdownExpanded
                      } // Click handling here
                  ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                          Text(text = selectedLanguage, modifier = Modifier.weight(1f))
                          Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                  }

              DropdownMenu(
                  expanded = isLanguageDropdownExpanded,
                  onDismissRequest = { isLanguageDropdownExpanded = false },
                  modifier = Modifier.testTag("languageDropdown")) {
                    listOf("Français", "English").forEach { language ->
                      DropdownMenuItem(
                          text = { Text(language) },
                          onClick = {
                            selectedLanguage = language
                            isLanguageDropdownExpanded = false
                          },
                          modifier = Modifier.testTag("languageOption_$language"))
                    }
                  }
            }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().testTag("settingsButtons"),
            horizontalArrangement = Arrangement.SpaceEvenly) {
              Button(onClick = {navigationActions.goBack()}, modifier = Modifier.weight(1f).testTag("saveButton")) {
                Text(text = "Save")
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                  onClick = {navigationActions.goBack()}, modifier = Modifier.weight(1f).testTag("cancelButton")) {
                    Text(text = "Cancel")
                  }
            }
      }
}
