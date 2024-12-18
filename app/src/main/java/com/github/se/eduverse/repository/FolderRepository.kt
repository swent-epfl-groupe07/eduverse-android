package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Folder

interface FolderRepository {
  fun getFolders(
      userId: String,
      archived: Boolean = false,
      onSuccess: (List<Folder>) -> Unit,
      onFailure: (Exception) -> Unit
  )

  fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun updateFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  suspend fun deleteFolders(
      folders: List<Folder>,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  )

  fun getNewUid(): String
}
