package com.github.se.eduverse.repository

import android.net.Uri
import com.google.firebase.storage.StorageReference

interface FileRepository {
  fun getNewUid(): String

  fun savePdfFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun modifiyFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun deleteFile(fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

  fun accessFile(
      fileId: String,
      onSuccess: (StorageReference, String) -> Unit,
      onFailure: (Exception) -> Unit
  )

  fun savePathToFirestore(path: String, suffix: String, fileId: String, onSuccess: () -> Unit)
}
