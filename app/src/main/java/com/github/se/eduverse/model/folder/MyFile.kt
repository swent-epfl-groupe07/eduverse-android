package com.github.se.eduverse.model.folder

import java.util.Calendar

data class MyFile(
    val name: String,
    val creationTime: Calendar,
    var lastAccess: Calendar,
    var numberAccess: Int
)
