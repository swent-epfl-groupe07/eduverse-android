package com.github.se.eduverse.ui.others.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(onSaveClick: () -> Unit, onCancelClick: () -> Unit) {

  var name by remember { mutableStateOf("") }
  var school by remember { mutableStateOf("") }
  var coursesSelected by remember { mutableStateOf("") }
  var videosWatched by remember { mutableStateOf("") }
  var quizzesCompleted by remember { mutableStateOf("") }
  var studyTime by remember { mutableStateOf("") }
  var studyGoals by remember { mutableStateOf("") }

  Column(
      modifier =
          Modifier.fillMaxSize().padding(16.dp).testTag("profileColumn"), // Added test tag here
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
              OutlinedTextField(
                  value = name,
                  onValueChange = { name = it },
                  label = { Text(text = "Name") },
                  modifier = Modifier.fillMaxWidth().testTag("nameField"))
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = school,
                  onValueChange = { school = it },
                  label = { Text(text = "School") },
                  modifier = Modifier.fillMaxWidth().testTag("schoolInput"))
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = coursesSelected,
                  onValueChange = { coursesSelected = it },
                  label = { Text(text = "Courses Selected") },
                  modifier = Modifier.fillMaxWidth().testTag("coursesSelectedInput"))
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = videosWatched,
                  onValueChange = { videosWatched = it },
                  label = { Text(text = "# Videos Watched") },
                  modifier = Modifier.fillMaxWidth().testTag("videosWatchedInput"))
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = quizzesCompleted,
                  onValueChange = { quizzesCompleted = it },
                  label = { Text(text = "# Quizzes Completed") },
                  modifier = Modifier.fillMaxWidth().testTag("quizzesCompletedInput"))
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = studyTime,
                  onValueChange = { studyTime = it },
                  label = { Text(text = "Study Time Tracker") },
                  modifier = Modifier.fillMaxWidth().testTag("studyTimeInput"))
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = studyGoals,
                  onValueChange = { studyGoals = it },
                  label = { Text(text = "Study Goals") },
                  modifier = Modifier.fillMaxWidth().testTag("studyGoalsInput"))
            }

        Spacer(modifier = Modifier.height(16.dp))

        // Row for Save and Cancel buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
          Button(onClick = onSaveClick, modifier = Modifier.weight(1f).testTag("saveButton")) {
            Text(text = "Save")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(onClick = onCancelClick, modifier = Modifier.weight(1f).testTag("cancelButton")) {
            Text(text = "Cancel")
          }
        }
      }
}
