package com.github.se.eduverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.github.se.project.model.folder.Folder
import com.github.se.project.model.folder.FolderRepository

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // setContent { EduverseTheme { Surface(modifier = Modifier.fillMaxSize()) {} } }
    /*val file1 =
      MyFile("name 1", Calendar.getInstance(), Calendar.getInstance(), 0)
    val file2 =
      MyFile("name 2", java.util.Calendar.getInstance(), java.util.Calendar.getInstance(), 0)
    val file3 =
      MyFile("name 3", java.util.Calendar.getInstance(), java.util.Calendar.getInstance(), 0)
    val f = Folder(
      MutableStateFlow(
        MutableList(3) {
          when (it) {
            1 -> file1
            2 -> file2
            else -> file3
          }
        }),
      "folder",
      "1",
      TimeTable()
    )
    val fvm = FolderViewModel(MockFolderRepository(f))
    fvm.activeFolder = f
    setContent { EduverseTheme { FolderScreen(NavigationActions(rememberNavController()),fvm) } }*/
  }
}

// Temporary, to use only until we have a folder repo
class MockFolderRepository(private val folder: Folder) : FolderRepository {

  override fun getFolders(
      onSuccess: (List<Folder>) -> Unit,
      onFailure: (Exception) -> Unit
  ): List<Folder> {
    return List(1) {
      return@List folder
    }
  }

  override fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {}

  override fun updateFolder(
      folder: Folder,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {}

  override fun deleteFolder(
      folder: Folder,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {}

  override fun getNewUid(): String {
    return "id test"
  }
}
