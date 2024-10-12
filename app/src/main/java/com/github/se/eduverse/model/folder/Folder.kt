package com.github.se.eduverse.model.folder

data class Folder(
    val files: MutableList<MyFile>,
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
