package com.github.se.project.model.folder

interface FolderRepository {
  fun getFolders(): List<Folder>
  fun addFolder(folder: Folder)
  fun updateFolder(folder: Folder)
  fun deleteFolder(folder: Folder)
  fun getNewUid(): String
}
