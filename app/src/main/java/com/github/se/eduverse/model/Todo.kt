package com.github.se.eduverse.model

import java.util.Date

data class Todo(
    val uid: String,
    val name: String,
    val timeSpent: Long = 0, // in minutes
    val status: TodoStatus,
    val ownerId: String,
    val creationTime: Date = Date() // used to sort the todos
)

enum class TodoStatus {
  ACTUAL,
  DONE
}
