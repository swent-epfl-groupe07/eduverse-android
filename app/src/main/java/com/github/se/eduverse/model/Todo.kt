package com.github.se.eduverse.model

data class Todo(
    val uid: String,
    val name: String,
    val timeSpent: Long = 0, // in minutes
    val status: TodoStatus,
    val ownerId: String
)

enum class TodoStatus {
  ACTUAL,
  DONE
}
