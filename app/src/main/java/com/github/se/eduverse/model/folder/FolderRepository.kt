package com.github.se.eduverse.model.folder

interface FolderRepository {
  fun getFolders(onSuccess: (List<Folder>) -> Unit, onFailure: (Exception) -> Unit): List<Folder>

  fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun updateFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun deleteFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun getNewUid(): String
}
