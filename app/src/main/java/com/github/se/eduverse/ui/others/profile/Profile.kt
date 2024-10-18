package com.github.se.eduverse.ui.others.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.eduverse.ui.dashboard.auth
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.ProfileViewModel

@SuppressLint("SuspiciousIndentation")
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navigationActions: NavigationActions,
    userId: String = auth.currentUser!!.uid
) {

  val profile = viewModel.profileState.collectAsState().value

  var name by rememberSaveable { mutableStateOf(profile.name) }
  var school by rememberSaveable { mutableStateOf(profile.school) }
  var coursesSelected by rememberSaveable { mutableStateOf(profile.coursesSelected) }
  var videosWatched by rememberSaveable { mutableStateOf(profile.videosWatched) }
  var quizzesCompleted by rememberSaveable { mutableStateOf(profile.quizzesCompleted) }
  var studyTime by rememberSaveable { mutableStateOf(profile.studyTime) }
  var studyGoals by rememberSaveable { mutableStateOf(profile.studyGoals) }

  LaunchedEffect(userId) { viewModel.loadProfile(userId) }
  LaunchedEffect(profile) {
    name = profile.name
    school = profile.school
    coursesSelected = profile.coursesSelected
    videosWatched = profile.videosWatched
    quizzesCompleted = profile.quizzesCompleted
    studyTime = profile.studyTime
    studyGoals = profile.studyGoals
  }

  // Update the local state when the profile state is updated from the ViewModel
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
                  keyboardOptions =
                      KeyboardOptions(
                          keyboardType =
                              KeyboardType.Number), // Ensures only numbers can be entered
                  modifier = Modifier.fillMaxWidth().testTag("coursesSelectedInput"))
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = videosWatched,
                  onValueChange = { videosWatched = it },
                  label = { Text(text = "# Videos Watched") },
                  keyboardOptions =
                      KeyboardOptions(
                          keyboardType =
                              KeyboardType.Number), // Ensures only numbers can be entered
                  modifier = Modifier.fillMaxWidth().testTag("videosWatchedInput"))
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = quizzesCompleted,
                  onValueChange = { quizzesCompleted = it },
                  label = { Text(text = "# Quizzes Completed") },
                  keyboardOptions =
                      KeyboardOptions(
                          keyboardType =
                              KeyboardType.Number), // Ensures only numbers can be entered
                  modifier = Modifier.fillMaxWidth().testTag("quizzesCompletedInput"))
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                  value = studyTime,
                  onValueChange = { studyTime = it },
                  label = { Text(text = "Study Time Tracker") },
                  keyboardOptions =
                      KeyboardOptions(
                          keyboardType =
                              KeyboardType.Number), // Ensures only numbers can be entered
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
          Button(
              onClick = {
                viewModel.saveProfile(
                    userId = userId,
                    name = name,
                    school = school,
                    coursesSelected = coursesSelected,
                    videosWatched = videosWatched,
                    quizzesCompleted = quizzesCompleted,
                    studyTime = studyTime,
                    studyGoals = studyGoals)
                viewModel.loadProfile(userId)
                navigationActions.goBack()
              },
              modifier = Modifier.weight(1f).testTag("saveButton")) {
                Text(text = "Save")
              }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
              onClick = { navigationActions.goBack() },
              modifier = Modifier.weight(1f).testTag("cancelButton")) {
                Text(text = "Cancel")
              }
        }
      }
}
