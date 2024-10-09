package com.github.se.project.model.folder

import com.github.se.project.model.widget.Widget
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow

class Folder(
    val pdfFiles: MutableStateFlow<MutableList<MyFile>>,
    val name: String,
    val id: String,
    val timeTable: TimeTable
) : Widget

class MyFile(
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
