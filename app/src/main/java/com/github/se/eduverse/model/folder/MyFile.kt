package com.github.se.eduverse.model.folder

import java.util.Calendar

data class MyFile(
    val id: String, // The id of the MyFile in the folder
    val fileId: String, // The id of the file it reflects
    var name: String,
    val creationTime: Calendar,
    var lastAccess: Calendar,
    var numberAccess: Int
)
