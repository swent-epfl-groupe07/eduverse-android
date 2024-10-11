package com.github.se.eduverse.ui.others.setting

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    // Here you can add state for each setting if needed
    var changePassword by remember { mutableStateOf("") }
    var privacySettings by remember { mutableStateOf("") }
    var themeSelection by remember { mutableStateOf("") }
    var languagePreferences by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = changePassword,
                onValueChange = { changePassword = it },
                label = { Text(text = "Change Password") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = privacySettings,
                onValueChange = { privacySettings = it },
                label = { Text(text = "Privacy Settings") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = themeSelection,
                onValueChange = { themeSelection = it },
                label = { Text(text = "Theme Selection") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = languagePreferences,
                onValueChange = { languagePreferences = it },
                label = { Text(text = "Language Preferences") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row for Save and Cancel buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onSaveClick, modifier = Modifier.weight(1f)) {
                Text(text = "Save")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onCancelClick, modifier = Modifier.weight(1f)) {
                Text(text = "Cancel")
            }
        }
    }
}