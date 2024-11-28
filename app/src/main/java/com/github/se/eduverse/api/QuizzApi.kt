package com.github.se.eduverse.api

// Data class representing a single quiz question
data class Question(
    val text: String, // The text of the question
    val answers: List<String>, // A list of possible answer options
    val correctAnswer: String // The correct answer to the question
)
