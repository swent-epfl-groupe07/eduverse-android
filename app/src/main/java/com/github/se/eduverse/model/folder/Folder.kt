package com.github.se.project.model.folder

import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow

data class Folder(
    val pdfFiles: MutableStateFlow<MutableList<MyFile>>,
    val name: String,
    val id: String,
    val timeTable: TimeTable
)

data class MyFile(
    val name: String,
    val creationTime: Calendar,
    var lastAccess: Calendar,
    var numberAccess: Int
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
