package com.github.se.eduverse.api

// Data class to encapsulate the fields of an open ai api request
data class OpenAiRequest(
    val model: String,
    val prompt: String,
    val max_tokens: Int,
    val temperature: Double = 0.7
)

// Data class to encapsulate the format of an open ai api response
data class OpenAiResponse(val choices: List<Choice>)

// Data class to encapsulate the choices in an open ai api response
data class Choice(val text: String)
