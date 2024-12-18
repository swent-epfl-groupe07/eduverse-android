package com.github.se.eduverse.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Create a bottom menu with the list of the folders of the active user
 *
 * @param context the context in which the dialog must be opened
 * @param folderViewModel the folder view model of the app
 * @param color the color of the menu
 * @param select the action to do when clicking on a folder
 */
fun showBottomMenu(
    context: Context,
    folderViewModel: FolderViewModel,
    backgroundColor: Color,
    surfaceColor: Color,
    contentColor: Color,
    select: (Folder) -> Unit
) {
  // Create the BottomSheetDialog
  val bottomSheetDialog = BottomSheetDialog(context)

  // Define a list of Folder objects
  folderViewModel.getUserFolders()
  val folders = folderViewModel.folders.value

  // Set the inflated view as the content of the BottomSheetDialog
  bottomSheetDialog.setContentView(
      ComposeView(context).apply {
        setBackgroundColor(backgroundColor.toArgb())
        setContent {
          Column(modifier = Modifier.padding(16.dp).fillMaxWidth().testTag("button_container")) {
            folders.forEach { folder ->
              Card(
                  modifier =
                      Modifier.padding(8.dp)
                          .fillMaxWidth()
                          .clickable {
                            select(folder)
                            bottomSheetDialog.dismiss()
                          }
                          .testTag("folder_button${folder.id}"),
                  backgroundColor = surfaceColor,
                  contentColor = contentColor,
                  elevation = 4.dp) {
                    Text(
                        text = folder.name,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.h6)
                  }
            }
          }
        }
      })

  // Show the dialog
  bottomSheetDialog.show()

  // Dismiss the dialog on lifecycle changes
  if (context is LifecycleOwner) {
    @Suppress("DEPRECATION")
    val lifecycleObserver =
        object : LifecycleObserver {
          @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
          fun onStop() {
            if (bottomSheetDialog.isShowing) {
              bottomSheetDialog.dismiss()
            }
          }

          @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
          fun onDestroy() {
            // Remove observer to prevent memory leaks
            (context as LifecycleOwner).lifecycle.removeObserver(this)
          }
        }

    (context as LifecycleOwner).lifecycle.addObserver(lifecycleObserver)
  }
}

@Composable
fun DeleteFoldersDialog(number: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .testTag("confirm")) {
          Text("Are you sure you want to delete $number folder(s) ?")
          Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.testTag("yes"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                      Text("Yes")
                    }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("no"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                      Text("No")
                    }
              }
        }
  }
}

@Composable
fun DeleteFileDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .testTag("confirm")) {
          Text("Are you sure you want to delete this file ?")
          Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.testTag("yes"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                      Text("Yes")
                    }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("no"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                      Text("No")
                    }
              }
        }
  }
}

@Composable
fun RenameFileDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
  var name by remember { mutableStateOf("") }

  AlertDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(onClick = { onConfirm(name) }, modifier = Modifier.testTag("confirm")) {
          Text("Confirm")
        }
      },
      modifier = Modifier.testTag("renameDialog"),
      dismissButton = {
        TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel")) { Text("Cancel") }
      },
      title = { Text("Rename file") },
      text = {
        OutlinedTextField(
            value = name,
            modifier = Modifier.testTag("textField"),
            onValueChange = { name = it },
            label = { Text("Enter new name") })
      },
      backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background)
}

@Composable
fun EditFileMenu(
    modifier: Modifier,
    onRename: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier) {
    IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("editButton")) {
      Icon(Icons.Default.Edit, contentDescription = "Edit File")
    }

    DropdownMenu(
        expanded = expanded,
        modifier = Modifier.width(IntrinsicSize.Min),
        onDismissRequest = { expanded = false },
        properties = PopupProperties(focusable = false)) {
          DropdownMenuItem(
              text = { Text("Rename", modifier = Modifier.fillMaxWidth()) },
              modifier = Modifier.fillMaxWidth().testTag("rename"),
              onClick = {
                onRename()
                expanded = false
              })
          DropdownMenuItem(
              text = { Text("Download", modifier = Modifier.fillMaxWidth()) },
              modifier = Modifier.fillMaxWidth().testTag("download"),
              onClick = {
                onDownload()
                expanded = false
              })
          DropdownMenuItem(
              text = { Text("Delete", modifier = Modifier.fillMaxWidth()) },
              modifier = Modifier.fillMaxWidth().testTag("delete"),
              onClick = {
                onDelete()
                expanded = false
              })
        }
  }
}

@Composable
fun SaveIcon(onClick: () -> Unit, isEnabled: () -> Boolean) {
  Icon(
      imageVector = Icons.Default.Save,
      contentDescription = "Delete",
      modifier = Modifier.clickable(enabled = isEnabled()) { onClick() }.testTag("saveIcon"),
      tint = if (isEnabled()) MaterialTheme.colors.primary else Color.Gray)
}
