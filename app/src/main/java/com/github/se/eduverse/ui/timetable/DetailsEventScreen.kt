package com.github.se.eduverse.ui.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import com.github.se.eduverse.repository.TimeTableRepository
import com.github.se.eduverse.repository.TimeTableRepositoryImpl
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.TimeTableViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DetailsEventScreen(/*
    event: Scheduled,
    timeTableViewModel: TimeTableViewModel,
    navigationActions: NavigationActions*/
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "event.name",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("topBarTitle"))
                },
                modifier = Modifier.testTag("topBar"),
                navigationIcon = {
                    IconButton(
                        onClick = { },//navigationActions.goBack() },
                        modifier = Modifier.testTag("backButton")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            /*timeTableViewModel.deleteScheduled(event)
                            navigationActions.goBack()*/
                        },
                        modifier = Modifier.testTag("deleteButton")) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent))
        },
        bottomBar = {
            BottomNavigationMenu({ /*navigationActions.navigateTo(it)*/ }, LIST_TOP_LEVEL_DESTINATION, "")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Name of the event
                    Title("Name")
                    OutlinedTextField(
                        value = name,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .testTag("nameTextField"),
                        onValueChange = { name = it },
                        placeholder = { Text("Name the event") },
                        suffix = {})

                    Title("Description")
                    OutlinedTextField(
                        value = description,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .testTag("nameTextField"),
                        onValueChange = { name = it },
                        placeholder = { Text("No description provided") },
                        suffix = {},
                        minLines = 7)

                    DateAndTimePickers(context, Calendar.getInstance(), 0, 0, {}) { _, _ ->
                    }
                }
            }
        }
    }
}