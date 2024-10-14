package com.github.se.eduverse.model.folder

interface FolderRepository {
  fun getFolders(userId: String, onSuccess: (List<Folder>) -> Unit, onFailure: (Exception) -> Unit)

  fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun updateFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun deleteFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun getNewFolderUid(): String

  fun getNewFileUid(folder: Folder): String
}
