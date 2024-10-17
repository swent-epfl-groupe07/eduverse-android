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

  private var _validNewFile: MutableStateFlow<Boolean> = MutableStateFlow(false)

  /** A state flow containing the information if a new file was created or not */
  val validNewFile: StateFlow<Boolean> = _validNewFile

  /**
   * Access the new file and immediately reset the view model, so that it is ready to create another
   * file
   *
   * @return The new file or null if there wasn't any
   */
  fun getNewFile(): MyFile? {
    val newFile = _newFile.value
    reset()
    return newFile
  }

  /**
   * Create a new file in the database containing the file present locally at a certain uri
   *
   * @param uri the uri of the file to add to the database
   */
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

  /**
   * Set the name of the created file, if any, to a new value
   *
   * @param name the new name
   */
  fun setName(name: String) {
    _newFile.value?.name = name
  }

  /** Cancel the file in creation */
  fun reset() {
    _validNewFile.value = false
    _newFile.value = null
  }
}
