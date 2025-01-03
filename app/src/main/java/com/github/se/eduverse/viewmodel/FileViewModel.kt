package com.github.se.eduverse.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.github.se.eduverse.BuildConfig
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.showToast
import java.io.File
import java.nio.file.Files
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class FileViewModel(val fileRepository: FileRepository) {
  private var _newFile: MutableStateFlow<MyFile?> = MutableStateFlow(null)

  private var _validNewFile: MutableStateFlow<Boolean> = MutableStateFlow(false)

  /** A state flow containing the information if a new file was created or not */
  open val validNewFile: StateFlow<Boolean> = _validNewFile

  /**
   * Access the new file and immediately reset the view model, so that it is ready to create another
   * file
   *
   * @return The new file or null if there wasn't any
   */
  open fun getNewFile(): MyFile? {
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
    fileRepository.savePdfFile(
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

  /**
   * Open a pdf file using an specialized app on the device
   *
   * @param fileId the id of the file as stored in firestore
   * @param context the context in which this method is used
   */
  fun openFile(fileId: String, context: Context) {
    fileRepository.accessFile(
        fileId = fileId,
        onSuccess = { storageRef, suffix ->
          val localFile = File.createTempFile("tempFile", suffix)
          try {
            storageRef
                .getFile(localFile)
                .addOnSuccessListener { displayFile(localFile, context) }
                .addOnFailureListener {
                  Log.e("Open File", "Opening of file ${storageRef.name} failed: $it")
                  Toast.makeText(context, "Can't open file", Toast.LENGTH_SHORT).show()
                }
          } finally {
            localFile.delete()
          }
        },
        onFailure = {
          Log.e("Access File", "Access of file at $fileId failed: $it")
          Toast.makeText(context, "Can't access file", Toast.LENGTH_SHORT).show()
        })
  }

  /**
   * Open a pdf file. Parameters intent and uri are there to make the function testable
   *
   * @param file the file to open
   * @param context the context in which the file is opened
   * @param intent the intent that is created and then executed with an appropriate activity
   * @param uri the uri of the file in specified context
   */
  fun displayFile(
      file: File,
      context: Context,
      intent: Intent = Intent(Intent.ACTION_VIEW),
      uri: Uri =
          FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
  ) {
    intent.setDataAndType(uri, Files.probeContentType(file.toPath()))
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    if (intent.resolveActivity(context.packageManager) != null) {
      context.startActivity(intent)
    } else {
      Toast.makeText(context, "No application to open ${file.extension}", Toast.LENGTH_SHORT).show()
    }
  }

  /**
   * Delete a file from firebase
   *
   * @param fileId the id of the file to delete
   * @param onSuccess some code to execute upon deletion, typically suppressing the file
   *   representation in the caller
   */
  fun deleteFile(fileId: String, onSuccess: () -> Unit) {
    fileRepository.deleteFile(
        fileId = fileId,
        onSuccess = onSuccess,
        onFailure = { Log.e("Delete File", "Can't delete file at $fileId: $it") })
  }

  /**
   * Download a file from firebase
   *
   * @param fileId the id of the file to download
   * @param context the context in which we download
   */
  fun downloadFile(fileId: String, context: Context) {
    fileRepository.accessFile(
        fileId = fileId,
        onSuccess = { storageRef, suffix ->
          try {
            val directory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = fileId + suffix
            val localFile = File(directory, fileName)
            storageRef
                .getFile(localFile)
                .addOnSuccessListener {
                  context.showToast("Successfully downloaded file $fileName")
                }
                .addOnFailureListener {
                  localFile.delete()
                  context.showToast("Can't access file")
                }
          } catch (_: Exception) {
            context.showToast("Failed to download file")
          }
        },
        onFailure = { context.showToast("Can't access file") })
  }
}
