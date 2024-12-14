package com.github.se.eduverse.ui.folder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.theme.transparentButtonColor
import com.github.se.eduverse.viewmodel.FileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFileScreen(navigationActions: NavigationActions, fileViewModel: FileViewModel) {

  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
  var name by rememberSaveable { mutableStateOf("") }
  val validNewFile by fileViewModel.validNewFile.collectAsState()

  val filePickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
          // Handle the selected file URI (e.g., update ViewModel or state)
          fileViewModel.createFile(it)
        }
      }

  Scaffold(
      topBar = { TopNavigationBar(navigationActions, screenTitle = "Add File") },
      bottomBar = {
        BottomNavigationMenu(
            { navigationActions.navigateTo(it) },
            LIST_TOP_LEVEL_DESTINATION,
            "") // No item is selected, as it is not one of the screens on the bottom bar
      }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 20.dp).fillMaxSize()) {
          // Give a name to the course
          Text(
              "File Name",
              fontWeight = FontWeight.Bold,
              fontSize = 24.sp,
              modifier = Modifier.padding(vertical = 15.dp).testTag("fileNameTitle"))
          OutlinedTextField(
              value = name,
              modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).testTag("fileNameField"),
              onValueChange = { name = it },
              placeholder = { Text("Name of the file") })

          Row(
              modifier = Modifier.padding(vertical = 15.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      "Upload file",
                      fontWeight = FontWeight.Bold,
                      fontSize = 24.sp,
                      modifier = Modifier.testTag("uploadFileText"))
                  if (validNewFile)
                      Icon(
                          Icons.Default.Check,
                          contentDescription = "File created",
                          modifier = Modifier.testTag("iconCheck"),
                          tint = Color.Green)
                }
                Button(
                    onClick = { filePickerLauncher.launch("application/pdf") },
                    modifier = Modifier.testTag("browseFile"),
                    colors = transparentButtonColor(MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)) {
                      Icon(Icons.AutoMirrored.Filled.InsertDriveFile, "Browse file")
                    }
              }
          Spacer(modifier = Modifier.fillMaxHeight(0.72f))
          Button(
              onClick = {
                if (name.isNotEmpty()) fileViewModel.setName(name)
                navigationActions.goBack()
              },
              enabled = validNewFile,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(vertical = 2.dp)
                      .align(Alignment.End)
                      .testTag("fileSave"),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary)) {
                Text("Save")
              }
          Button(
              onClick = {
                fileViewModel.reset()
                navigationActions.goBack()
              },
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(vertical = 2.dp)
                      .align(Alignment.End)
                      .testTag("fileCancel"),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary)) {
                Text("Cancel")
              }
        }
      }
}
