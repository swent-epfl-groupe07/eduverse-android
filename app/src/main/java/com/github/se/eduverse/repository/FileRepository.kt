package com.github.se.eduverse.repository

import android.net.Uri

interface FileRepository {
  fun getNewUid(): String

  fun saveFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun modifiyFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun deleteFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun accessFile(fileId: String, onSuccess: (Uri) -> Unit, onFailure: (Exception) -> Unit)
}
