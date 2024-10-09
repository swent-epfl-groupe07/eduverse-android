package com.github.se.project.model.folder

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FolderViewModel(val repository: FolderRepository) : ViewModel() {
  // TODO
  /*companion object {
      val Factory: ViewModelProvider.Factory =
          object : ViewModelProvider.Factory {
              @Suppress("UNCHECKED_CAST")
              override fun <T : ViewModel> create(modelClass: Class<T>): T {
                  return FolderViewModel( ... ) as T
              }
          }
  }*/

  private val _existingFolders: MutableStateFlow<MutableList<Folder>> =
    MutableStateFlow(repository.getFolders(
      {}, {
        Log.e("FolderViewModel", "Exception $it while trying to load the folders")
      }
    ).toMutableList())
  val existingFolders: StateFlow<MutableList<Folder>> = _existingFolders

  var activeFolder: Folder? = null

  /**
   * Sort the array of files of the active folder.
   *
   * @param filter the filter to apply, as defined in enum FilterTypes in model/folder/Folder.kt
   */
  fun sortBy(filter: FilterTypes) {
    when (filter) {
      FilterTypes.NAME -> activeFolder?.pdfFiles?.value?.sortBy { it.name }
      FilterTypes.CREATION_UP ->
          activeFolder?.pdfFiles?.value?.sortBy { it.creationTime.timeInMillis }
      FilterTypes.CREATION_DOWN ->
          activeFolder?.pdfFiles?.value?.sortBy { -it.creationTime.timeInMillis }
      FilterTypes.ACCESS_RECENT ->
          activeFolder?.pdfFiles?.value?.sortBy { -it.lastAccess.timeInMillis }
      FilterTypes.ACCESS_OLD -> activeFolder?.pdfFiles?.value?.sortBy { it.lastAccess.timeInMillis }
      FilterTypes.ACCESS_MOST -> activeFolder?.pdfFiles?.value?.sortBy { -it.numberAccess }
      FilterTypes.ACCESS_LEAST -> activeFolder?.pdfFiles?.value?.sortBy { it.numberAccess }
      else -> throw NotImplementedError("The sort method is not up-to-date")
    }
  }

  /**
   * Add a folder to the list of existing folders.
   *
   * @param folder the folder to add
   */
  fun addFolder(folder: Folder) {
    _existingFolders.value.add(folder)
    repository.addFolder(folder,
      {}, {
        Log.e("FolderViewModel",
          "Exception $it while trying to add folder ${folder.name} to the repository")
      })
  }

  /**
   * Remove a folder from the list of existing folders.
   *
   * @param folder the folder to remove
   */
  fun deleteFolder(folder: Folder) {
    _existingFolders.value.remove(folder)
    if (activeFolder == folder) activeFolder = null
    repository.deleteFolder(folder,
      {}, {
        Log.e("FolderViewModel", "Exception $it while trying to delete folder ${folder.name}")
      })
  }

  /**
   * Update a folder in the list of existing folders.
   * If it doesn't exist, create it.
   *
   * @param folder the folder to update
   */
  fun updateFolder(folder: Folder) {
    try {
      _existingFolders.value[_existingFolders.value.indexOfFirst { it.id == folder.id }] = folder
      if(activeFolder?.id == folder.id) activeFolder = folder
      repository.updateFolder(folder,
        {}, {
          Log.e("FolderViewModel", "Exception $it while trying to update folder ${folder.name}")
        })
    } catch (_: IndexOutOfBoundsException) {
      addFolder(folder)
    }
  }

  /**
   * Get new ID for a folder.
   *
   */
  fun getNewUid(): String {
    return repository.getNewUid()
  }
}
