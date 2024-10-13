package com.github.se.eduverse.model.folder

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FolderViewModel(val repository: FolderRepository, val currentUser: FirebaseUser?) :
    ViewModel() {

  private var _existingFolders: MutableStateFlow<MutableList<Folder>> =
      MutableStateFlow(emptyList<Folder>().toMutableList())
  val existingFolders: StateFlow<MutableList<Folder>> = _existingFolders

  private var _activeFolder: MutableStateFlow<Folder?> = MutableStateFlow(null)
  val activeFolder: StateFlow<Folder?> = _activeFolder

  init {
    try {
      getUserFolders()
    } catch (e: Exception) {
      Log.e("FolderViewModel Initialisation", "Exception $e: ${e.message}")
    }
  }

  /**
   * Set the active folder
   *
   * @param folder the value to set
   */
  fun selectFolder(folder: Folder?) {
    _activeFolder.value = folder
  }

  /**
   * Sort the array of files of the active folder.
   *
   * @param filter the filter to apply, as defined in enum FilterTypes in model/folder/Folder.kt
   */
  fun sortBy(filter: FilterTypes) {
    activeFolder.value?.filterType = filter
    when (filter) {
      FilterTypes.NAME -> activeFolder.value?.files?.sortBy { it.name }
      FilterTypes.CREATION_UP -> activeFolder.value?.files?.sortBy { it.creationTime.timeInMillis }
      FilterTypes.CREATION_DOWN ->
          activeFolder.value?.files?.sortBy { -it.creationTime.timeInMillis }
      FilterTypes.ACCESS_RECENT -> activeFolder.value?.files?.sortBy { -it.lastAccess.timeInMillis }
      FilterTypes.ACCESS_OLD -> activeFolder.value?.files?.sortBy { it.lastAccess.timeInMillis }
      FilterTypes.ACCESS_MOST -> activeFolder.value?.files?.sortBy { -it.numberAccess }
      FilterTypes.ACCESS_LEAST -> activeFolder.value?.files?.sortBy { it.numberAccess }
      else -> throw NotImplementedError("The sort method is not up-to-date")
    }
  }

  /** Get the folders with owner id equivalent to the current user */
  fun getUserFolders() {
    repository.getFolders(
        { folders ->
          _existingFolders.value =
              folders.filter { it.ownerID == currentUser!!.uid }.toMutableList()
        },
        { Log.e("FolderViewModel", "Exception $it while trying to load the folders") })
  }

  /**
   * Add a folder to the list of existing folders.
   *
   * @param folder the folder to add
   */
  fun addFolder(folder: Folder) {
    _existingFolders.value.add(folder)
    repository.addFolder(
        folder,
        {},
        {
          Log.e(
              "FolderViewModel",
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
    if (activeFolder.value == folder) selectFolder(null)
    repository.deleteFolder(
        folder,
        {},
        { Log.e("FolderViewModel", "Exception $it while trying to delete folder ${folder.name}") })
  }

  /**
   * Update a folder in the list of existing folders. If it doesn't exist, create it.
   *
   * @param folder the folder to update
   */
  fun updateFolder(folder: Folder) {
    try {
      _existingFolders.value[_existingFolders.value.indexOfFirst { it.id == folder.id }] = folder
      if (activeFolder.value?.id == folder.id) selectFolder(folder)
      repository.updateFolder(
          folder,
          {},
          {
            Log.e("FolderViewModel", "Exception $it while trying to update folder ${folder.name}")
          })
    } catch (_: IndexOutOfBoundsException) {
      addFolder(folder)
    }
  }

  /** Get new ID for a folder. */
  fun getNewUid(): String {
    return repository.getNewUid()
  }

  /**
   * Add a new file to the active folder
   *
   * @param file the file to add
   */
  fun addFile(file: MyFile) {
    if (_activeFolder.value == null) return
    _activeFolder.value!!.files.add(file)
    sortBy(_activeFolder.value!!.filterType)
  }
}
