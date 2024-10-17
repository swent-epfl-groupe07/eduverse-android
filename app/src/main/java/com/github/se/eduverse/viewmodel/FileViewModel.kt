package com.github.se.eduverse.viewmodel

import android.net.Uri
import android.util.Log
import com.github.se.eduverse.model.folder.MyFile
import com.github.se.eduverse.repository.FileRepository
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileViewModel(val fileRepository: FileRepository) {
  private var _newFile: MutableStateFlow<MyFile?> = MutableStateFlow(null)

  fun getNewFile(): MyFile? {
    val newFile = _newFile.value
    reset()
    return newFile
  }

  private var _validNewFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val validNewFile: StateFlow<Boolean> = _validNewFile

  fun createFile(uri: Uri) {
    _validNewFile.value = false
    _newFile.value = null // In case the creation fails, users will see it
    val uid = fileRepository.getNewUid()
    fileRepository.saveFile(
        uri,
        uid,
        {
          _validNewFile.value = true
          _newFile.value =
              MyFile(
                  id = "",
                  fileId = uid,
                  name = uri.lastPathSegment ?: uid,
                  /* If the uploaded file is non-null, it is the name, otherwise the uid is.
                  Anyway, this value should be changed using setName before the new file is
                  retrieved, this is only a fail-safe. */
                  creationTime = Calendar.getInstance(),
                  lastAccess = Calendar.getInstance(),
                  numberAccess = 0)
        },
        { Log.e("File Upload", "Uploading of file ${uri.lastPathSegment} failed: $it") })
  }

  fun setName(name: String) {
    _newFile.value?.name = name
  }

  fun reset() {
    _validNewFile.value = false
    _newFile.value = null
  }
}
