package com.github.se.eduverse.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.se.eduverse.model.FilterTypes
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.repository.FolderRepositoryImpl
import com.github.se.eduverse.showToast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class FolderViewModel(
    val repository: FolderRepository,
    val auth: FirebaseAuth,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

  companion object {
    val Factory: ViewModelProvider.Factory =
        object : AbstractSavedStateViewModelFactory() {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(
              key: String,
              modelClass: Class<T>,
              handle: SavedStateHandle
          ): T {
            return FolderViewModel(
                FolderRepositoryImpl(Firebase.firestore), FirebaseAuth.getInstance(), handle)
                as T
          }
        }
  }

  private var _folders: MutableStateFlow<MutableList<Folder>> =
      MutableStateFlow(emptyList<Folder>().toMutableList())
  open val folders: StateFlow<MutableList<Folder>> = _folders

  private var _activeFolder: MutableStateFlow<Folder?> =
      MutableStateFlow(savedStateHandle["activeFolder"])
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
    savedStateHandle["activeFolder"] = folder
  }

  /**
   * Sort the array of files of the active folder.
   *
   * @param filter the filter to apply, as defined in enum FilterTypes in model/folder/Folder.kt
   */
  fun sortBy(filter: FilterTypes) {
    activeFolder.value!!.filterType = filter
    when (filter) {
      FilterTypes.NAME -> activeFolder.value!!.files.sortBy { it.name }
      FilterTypes.CREATION_UP -> activeFolder.value!!.files.sortBy { it.creationTime.timeInMillis }
      FilterTypes.CREATION_DOWN ->
          activeFolder.value!!.files.sortBy { -it.creationTime.timeInMillis }
      FilterTypes.ACCESS_RECENT -> activeFolder.value!!.files.sortBy { -it.lastAccess.timeInMillis }
      FilterTypes.ACCESS_OLD -> activeFolder.value!!.files.sortBy { it.lastAccess.timeInMillis }
      FilterTypes.ACCESS_MOST -> activeFolder.value!!.files.sortBy { -it.numberAccess }
      FilterTypes.ACCESS_LEAST -> activeFolder.value!!.files.sortBy { it.numberAccess }
      else -> throw NotImplementedError("The sort method is not up-to-date")
    }
  }

  /** Get the folders with owner id equivalent to the current user */
  fun getUserFolders() {
    repository.getFolders(
        auth.currentUser!!.uid,
        false,
        { _folders.value = it.toMutableList() },
        { Log.e("FolderViewModel", "Exception $it while trying to load the folders") })
  }

  /** Get the archived folders with owner id equivalent to the current user */
  fun getArchivedUserFolders() {
    repository.getFolders(
        auth.currentUser!!.uid,
        true,
        { _folders.value = it.toMutableList() },
        { Log.e("FolderViewModel", "Exception $it while trying to load the folders") })
  }

  /** Show a toast indicating that the device is offline */
  fun showOfflineMessage(context: Context) {
    context.showToast(
        "Your device is offline. Please connect to the internet to manage your folders")
  }

  /**
   * Add a folder to the list of existing folders.
   *
   * @param folder the folder to add
   */
  open fun addFolder(folder: Folder) {
    repository.addFolder(
        folder,
        { _folders.value.add(folder) },
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
    if (activeFolder.value == folder) selectFolder(null)
    repository.deleteFolder(
        folder,
        { _folders.value.remove(folder) },
        { Log.e("FolderViewModel", "Exception $it while trying to delete folder ${folder.name}") })
  }

  /**
   * Update a folder in the list of existing folders. If it doesn't exist, create it.
   *
   * @param folder the folder to update
   */
  fun updateFolder(folder: Folder) {
    repository.updateFolder(
        folder,
        {
          try {

            _folders.value[_folders.value.indexOfFirst { it.id == folder.id }] = folder
            if (activeFolder.value != null && activeFolder.value!!.id == folder.id)
                selectFolder(folder)
          } catch (_: IndexOutOfBoundsException) {
            Log.d("FolderViewModel", "Folder ${folder.name} is not in the active list of folder")
          }
        },
        { Log.e("FolderViewModel", "Exception $it while trying to update folder ${folder.name}") })
  }

  /** Get new ID for a folder. */
  fun getNewUid(): String {
    return repository.getNewUid()
  }

  /**
   * Archive a folder
   *
   * @param folder the folder to archive
   */
  fun archiveFolder(folder: Folder = activeFolder.value!!) {
    // If folders are not archived, remove the archived folder
    val index = _folders.value.indexOfFirst { it.id == folder.id }
    if (index != -1 && !_folders.value[index].archived) {
      _folders.value.removeAt(index)
    }
    folder.archived = true
    updateFolder(folder)
  }

  /**
   * Unarchive a folder
   *
   * @param folder the folder to unarchive
   */
  fun unarchiveFolder(folder: Folder = activeFolder.value!!) {
    // If folders are archived, remove the unarchived folder
    val index = _folders.value.indexOfFirst { it.id == folder.id }
    if (_folders.value[index].archived) {
      _folders.value.removeAt(index)
    }
    folder.archived = false
    updateFolder(folder)
  }

  /**
   * Rename a folder. If no argument is specified for the folder, rename the active folder.
   *
   * @param name the new name to assign
   * @param folder the folder to rename
   */
  fun renameFolder(name: String, folder: Folder = activeFolder.value!!) {
    folder.name = name
    updateFolder(folder)
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
    updateFolder(_activeFolder.value!!)
  }

  /**
   * Add a new file to the active folder
   *
   * @param file the file to add
   */
  fun deleteFile(file: MyFile) {
    if (_activeFolder.value == null) return
    _activeFolder.value!!.files.remove(file)
    updateFolder(_activeFolder.value!!)
  }

  /**
   * Create a new file in a folder.
   *
   * @param fileId the id of the file to add
   * @param name the name of the file to add
   * @param folder the folder in which to add
   */
  open fun createFileInFolder(fileId: String, name: String, folder: Folder) {
    val newFile =
        MyFile(
            id = "",
            fileId = fileId,
            name = name,
            creationTime = Calendar.getInstance(),
            lastAccess = Calendar.getInstance(),
            numberAccess = 0)
    folder.files.add(newFile)
    updateFolder(folder)
  }
}
