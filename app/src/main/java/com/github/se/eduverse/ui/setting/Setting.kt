package com.github.se.eduverse.ui.setting

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.theme.Theme
import com.github.se.eduverse.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(
    navigationActions: NavigationActions,
    settingsViewModel: SettingsViewModel,
    systemTheme: String = if (isSystemInDarkTheme()) Theme.DARK else Theme.LIGHT
) {
  val context = LocalContext.current

  val privacySettings by settingsViewModel.privacySettings.collectAsState()
  val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
  val selectedLanguage by settingsViewModel.selectedLanguage.collectAsState()

  var isThemeDropdownExpanded by remember { mutableStateOf(false) }
  var isLanguageDropdownExpanded by remember { mutableStateOf(false) }

  Scaffold(topBar = { TopNavigationBar(navigationActions, screenTitle = null) }) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        verticalArrangement = Arrangement.SpaceBetween) {

          // Confidentiality Toggle Section
          Card(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(
                      text = "Confidentiality:",
                      fontSize = 20.sp,
                      color = MaterialTheme.colorScheme.primary,
                      style =
                          MaterialTheme.typography.headlineMedium.copy(
                              fontWeight = FontWeight.Bold),
                      modifier = Modifier.padding(bottom = 16.dp))

                  Column(
                      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                              Text(
                                  text = if (privacySettings) "Private" else "Public",
                                  color = MaterialTheme.colorScheme.primary,
                                  style = MaterialTheme.typography.bodyLarge,
                                  modifier = Modifier.testTag("confidentialityToggleState"))
                              Switch(
                                  checked = privacySettings,
                                  onCheckedChange = { settingsViewModel.updatePrivacySettings(it) },
                                  colors =
                                      SwitchDefaults.colors(
                                          checkedThumbColor = MaterialTheme.colorScheme.primary,
                                          checkedTrackColor = MaterialTheme.colorScheme.secondary),
                                  modifier = Modifier.testTag("confidentialityToggle"))
                            }

                        Text(
                            text =
                                if (privacySettings) {
                                  "Only you and your followers can see your profile and posts."
                                } else {
                                  "Your profile and posts are visible to everyone."
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                      }
                }
              }

          // Notifications, Saved, Archive, and Gallery Fields
          Card(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(4.dp)) {
                Column {
                  SettingsOption(
                      "Notifications",
                      Icons.Default.Notifications,
                      navigationActions,
                      Screen.NOTIFICATIONS,
                      context)
                  SettingsOption("Saved", Icons.Default.Bookmark, navigationActions, null, context)
                  SettingsOption(
                      "Archive", Icons.Default.Archive, navigationActions, Route.ARCHIVE, context)
                  SettingsOption(
                      "Gallery",
                      Icons.Default.PhotoLibrary,
                      navigationActions,
                      Screen.GALLERY,
                      context)
                }
              }

          // Theme and Language Dropdowns
          Card(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(4.dp)) {
                Column {
                  SettingsDropdown(
                      label = "Theme",
                      selectedOption = selectedTheme,
                      options = listOf(Theme.LIGHT, Theme.DARK, "System Default"),
                      onOptionSelected = {
                        if (it == "System Default") {
                          settingsViewModel.updateSelectedTheme(systemTheme)
                        } else {
                          settingsViewModel.updateSelectedTheme(it)
                        }
                      },
                      isExpanded = isThemeDropdownExpanded,
                      onExpandChange = { isThemeDropdownExpanded = it },
                      modifier = Modifier.padding(16.dp).testTag("themeDropdown"))

                  SettingsDropdown(
                      label = "Language",
                      selectedOption = selectedLanguage,
                      options = listOf("Français", "English"),
                      onOptionSelected = { settingsViewModel.updateSelectedLanguage(it) },
                      isExpanded = isLanguageDropdownExpanded,
                      onExpandChange = { isLanguageDropdownExpanded = it },
                      modifier = Modifier.padding(16.dp).testTag("languageDropdown"))
                }
              }

          // Add Account and Log Out Buttons
          Card(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Button(
                      onClick = { showNotImplementedToast(context) },
                      modifier = Modifier.fillMaxWidth().testTag("addAccountButton"),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MaterialTheme.colorScheme.secondary)) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Account",
                            modifier = Modifier.padding(end = 8.dp),
                            tint = Color.White)
                        Text(text = "Add Account", color = Color.White)
                      }
                  Spacer(modifier = Modifier.height(8.dp))
                  Button(
                      onClick = { logout(navigationActions) },
                      modifier = Modifier.fillMaxWidth().testTag("logoutButton"),
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4DEE8))) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Log Out",
                            modifier = Modifier.padding(end = 8.dp),
                            tint = Color.Black)
                        Text(text = "Log Out", color = Color.Black)
                      }
                }
              }
        }
  }
}

@Composable
fun SettingsOption(
    title: String,
    icon: ImageVector,
    navigationActions: NavigationActions,
    route: String?,
    context: android.content.Context
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clickable {
                if (route != null) {
                  navigationActions.navigateTo(route)
                } else {
                  showNotImplementedToast(context)
                }
              }
              .padding(16.dp)
              .testTag("settingsOption_$title"),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp))
        Text(title, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary)
      }
}

@Composable
fun SettingsDropdown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dropdownwidth: Dp = 380.dp
) {
  Box(modifier = modifier.width(dropdownwidth).clickable { onExpandChange(!isExpanded) }) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "$label: $selectedOption",
              modifier = Modifier.weight(1f),
              color = MaterialTheme.colorScheme.primary)
          Icon(
              imageVector = Icons.Filled.ArrowDropDown,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary)
        }
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { onExpandChange(false) },
        modifier = Modifier.width(dropdownwidth)) {
          options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                  onOptionSelected(option)
                  onExpandChange(false)
                },
                modifier = Modifier.testTag("dropdownOption_$label$option"))
          }
        }
  }
}

fun logout(navigationActions: NavigationActions) {
  FirebaseAuth.getInstance().signOut()
  navigationActions.navigateTo(Screen.AUTH)
}

// Helper function to show a toast
fun showNotImplementedToast(context: android.content.Context) {
  Toast.makeText(context, "Functionality not yet implemented", Toast.LENGTH_SHORT).show()
}
