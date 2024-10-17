package com.github.se.eduverse.viewmodel

import android.net.Uri
import android.util.Log
import com.github.se.eduverse.model.folder.MyFile
import com.github.se.eduverse.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class FileViewModel(val fileRepository: FileRepository) {
    private var _newFile: MutableStateFlow<MyFile?> = MutableStateFlow(null)

    // No need to put this in a state flow as it should only be accessed once
    val newFile: MyFile? = _newFile.value

    fun createFile(uri: Uri) {
        _newFile.value = null // In case the creation fails, users will see it
        val uid = fileRepository.getNewUid()
        fileRepository.saveFile(
            uri, uid,
            { _newFile.value = MyFile(
                id = "",
                fileId = uid,
                name = uri.lastPathSegment?:uid,
                /* If the uploaded file is non-null, it is the name, otherwise the uid is.
                Anyway, this value should be changed using setName before the new file is
                retrieved, this is only a fail-safe. */
                creationTime = Calendar.getInstance(),
                lastAccess = Calendar.getInstance(),
                numberAccess = 0
            ) },
            { Log.e("File Upload", "Uploading of file ${uri.lastPathSegment} failed: $it") }
        )
    }

    fun setName(name: String) {
        _newFile.value?.name = name
    }

    fun reset() {
        _newFile.value = null
    }
}