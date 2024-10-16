package com.github.se.eduverse.model

data class Photo(val ownerId: String, val photo: ByteArray, val path: String = "")
