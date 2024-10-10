package com.github.se.eduverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.github.se.project.model.folder.Folder
import com.github.se.project.model.folder.FolderRepository

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { EduverseTheme { Surface(modifier = Modifier.fillMaxSize()) {} } }
  }
}
