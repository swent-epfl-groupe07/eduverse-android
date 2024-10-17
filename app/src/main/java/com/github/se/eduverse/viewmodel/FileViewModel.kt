package com.github.se.eduverse.viewmodel

import android.net.Uri
import android.util.Log
import com.github.se.eduverse.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileViewModel(val fileRepository: FileRepository) {
    private var _newFileId: MutableStateFlow<String?> = MutableStateFlow(null)
    val newFileId: StateFlow<String?> = _newFileId

    fun createFile(uri: Uri) {
        _newFileId.value = null // In case the creation fails, users will see it
        val uid = fileRepository.getNewUid()
        fileRepository.saveFile(
            uri, uid,
            { _newFileId.value = uid },
            { Log.e("File Upload", "Uploading of file ${uri.lastPathSegment} failed: $it") }
        )
    }
}