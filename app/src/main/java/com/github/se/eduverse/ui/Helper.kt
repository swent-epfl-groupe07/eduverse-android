package com.github.se.eduverse.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
 * @param select the action to do when clicking on a folder
 */
fun showBottomMenu(context: Context, folderViewModel: FolderViewModel, select: (Folder) -> Unit) {
  // Create the BottomSheetDialog
  val bottomSheetDialog = BottomSheetDialog(context)

  // Define a list of Folder objects
  folderViewModel.getUserFolders()
  val folders = folderViewModel.folders.value

  // Set the inflated view as the content of the BottomSheetDialog
  bottomSheetDialog.setContentView(
      ComposeView(context).apply {
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
                  elevation = 4.dp) {
                    Text(
                        text = folder.name,
                        modifier = Modifier.padding(16.dp),
                        style = androidx.compose.material.MaterialTheme.typography.h6)
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
