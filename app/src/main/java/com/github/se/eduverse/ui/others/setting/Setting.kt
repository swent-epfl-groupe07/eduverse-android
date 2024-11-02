package com.github.se.eduverse.ui.others.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth

// Define theme colors
private val HeaderColor = Color(0xFF4bace5)
private val SecondaryColor = Color(0xFF217384)
private val LightBackgroundColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navigationActions: NavigationActions) {
  var privacySettings by remember { mutableStateOf(true) }
  var selectedTheme by remember { mutableStateOf("Light") }
  var selectedLanguage by remember { mutableStateOf("English") }
  var isThemeDropdownExpanded by remember { mutableStateOf(false) }
  var isLanguageDropdownExpanded by remember { mutableStateOf(false) }

  Scaffold(
      topBar = {
        MediumTopAppBar(
            colors =
                TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary),
            title = {
              Text(
                  text = "Settings",
                  style = MaterialTheme.typography.titleLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("topBarText"))
            },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("backButton")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                  }
            })
      }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(LightBackgroundColor).padding(padding)) {
              // Confidentiality Toggle Section
              Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Confidentiality:",
                    fontSize = 20.sp,
                    color = SecondaryColor,
                    modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("confidentialityToggle"),
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = if (privacySettings) "Private" else "Public",
                          modifier = Modifier.weight(1f).testTag("confidentialityToggleState"),
                          color = SecondaryColor)
                      Switch(
                          checked = privacySettings,
                          onCheckedChange = { privacySettings = it },
                          colors = SwitchDefaults.colors(checkedThumbColor = HeaderColor))
                    }
                Text(
                    text =
                        if (privacySettings)
                            "Only you and your followers can see your profile and posts."
                        else "Your profile and posts are visible to everyone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryColor.copy(alpha = 0.6f))
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Notifications, Saved, Archive, and Gallery Fields
              SettingsOption(
                  "Notifications",
                  Icons.Default.Notifications,
                  navigationActions,
                  "NotificationsScreen")
              SettingsOption("Saved", Icons.Default.Bookmark, navigationActions, "SavedScreen")
              SettingsOption("Archive", Icons.Default.Archive, navigationActions, "ArchiveScreen")
              SettingsOption(
                  "Gallery", Icons.Default.PhotoLibrary, navigationActions, Screen.GALLERY)

              Spacer(modifier = Modifier.height(16.dp))

              // Theme and Language Dropdowns
              SettingsDropdown(
                  label = "Theme",
                  selectedOption = selectedTheme,
                  options = listOf("Light", "Dark", "System Default"),
                  onOptionSelected = { selectedTheme = it },
                  isExpanded = isThemeDropdownExpanded,
                  onExpandChange = { isThemeDropdownExpanded = it },
                  modifier = Modifier.padding(horizontal = 16.dp).testTag("themeDropdown"))

              Spacer(modifier = Modifier.height(16.dp))

              SettingsDropdown(
                  label = "Language",
                  selectedOption = selectedLanguage,
                  options = listOf("Français", "English"),
                  onOptionSelected = { selectedLanguage = it },
                  isExpanded = isLanguageDropdownExpanded,
                  onExpandChange = { isLanguageDropdownExpanded = it },
                  modifier = Modifier.padding(horizontal = 16.dp).testTag("languageDropdown"))

              Spacer(modifier = Modifier.height(144.dp))

              // Add Account and Log Out Buttons
              Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = { /* Add account functionality here */},
                    modifier = Modifier.fillMaxWidth().testTag("addAccountButton"),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)) {
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

// Other helper functions (SettingsOption and SettingsDropdown) remain the same

// Other helper functions (SettingsOption and SettingsDropdown) remain the same

@Composable
fun SettingsOption(
    title: String,
    icon: ImageVector,
    navigationActions: NavigationActions,
    route: String
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clickable { navigationActions.navigateTo(route) }
              .padding(16.dp)
              .testTag("settingsOption_$title"),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SecondaryColor,
            modifier = Modifier.padding(end = 16.dp))
        Text(title, modifier = Modifier.weight(1f), color = SecondaryColor)
        Icon(
            imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = HeaderColor)
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
              color = SecondaryColor)
          Icon(
              imageVector = Icons.Filled.ArrowDropDown,
              contentDescription = null,
              tint = HeaderColor)
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

private fun logout(navigationActions: NavigationActions) {
  FirebaseAuth.getInstance().signOut()
  navigationActions.navigateTo(Screen.AUTH) // Replace "LoginScreen" with your login screen route
}
