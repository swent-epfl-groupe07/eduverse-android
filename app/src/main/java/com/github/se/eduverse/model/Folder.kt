package com.github.se.eduverse.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Folder(
    val ownerID: String,
    var files: MutableList<MyFile>,
    var name: String,
    val id: String,
    var filterType: FilterTypes = FilterTypes.CREATION_UP,
    var archived: Boolean
) : Parcelable

enum class FilterTypes {
  NAME,
  CREATION_UP,
  CREATION_DOWN,
  ACCESS_RECENT,
  ACCESS_OLD,
  ACCESS_MOST,
  ACCESS_LEAST
}
