package com.github.se.eduverse.model.folder

data class Folder(
    val ownerID: String,
    val files: MutableList<MyFile>,
    var name: String,
    val id: String,
    var filterType: FilterTypes = FilterTypes.CREATION_UP
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
