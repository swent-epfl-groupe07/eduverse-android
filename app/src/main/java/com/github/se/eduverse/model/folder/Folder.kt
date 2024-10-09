package com.github.se.project.model.folder

import com.github.se.eduverse.model.folder.MyFile
import kotlinx.coroutines.flow.MutableStateFlow

data class Folder(
    val files: MutableStateFlow<MutableList<MyFile>>,
    val name: String,
    val id: String,
    val timeTable: TimeTable
)

enum class FilterTypes {
  NAME,
  CREATION_UP,
  CREATION_DOWN,
  ACCESS_RECENT,
  ACCESS_OLD,
  ACCESS_MOST,
  ACCESS_LEAST
}
