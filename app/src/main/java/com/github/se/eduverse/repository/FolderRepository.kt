package com.github.se.eduverse.repository

import com.github.se.eduverse.model.folder.Folder

interface FolderRepository {
  fun getFolders(userId: String, onSuccess: (List<Folder>) -> Unit, onFailure: (Exception) -> Unit)

  fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun updateFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun deleteFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun getNewUid(): String
}
