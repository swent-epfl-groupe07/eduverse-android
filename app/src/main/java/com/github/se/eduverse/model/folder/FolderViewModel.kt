package com.github.se.project.model.folder

import androidx.lifecycle.ViewModel

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

  val existingFolders: MutableList<Folder> = repository.getFolders().toMutableList()
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
    existingFolders.add(folder)
    repository.addFolder(folder)
  }

  /**
   * Remove a folder from the list of existing folders.
   *
   * @param folder the folder to remove
   */
  fun deleteFolder(folder: Folder) {
    existingFolders.remove(folder)
    if (activeFolder == folder) activeFolder = null
    repository.deleteFolder(folder)
  }

  /**
   * Update a folder in the list of existing folders.
   * If it doesn't exist, create it.
   *
   * @param folder the folder to update
   */
  fun updateFolder(folder: Folder) {
    try {
      existingFolders[existingFolders.indexOfFirst { it.id == folder.id }] = folder
      if(activeFolder?.id == folder.id) activeFolder = folder
      repository.updateFolder(folder)
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
