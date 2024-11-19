package com.github.se.eduverse.api

// Data class to encapsulate the fields of an open ai api request
data class OpenAiRequest(val model: String, val messages: List<Message>, val temperature: Double)

// Data class to encapsulate the format of an open ai api response
data class OpenAiResponse(val choices: List<Choice>)

// Data class to encapsulate the choices in an open ai api response
data class Choice(val message: Message)

// Data class to encapsulate the fields of a message
data class Message(val role: String, val content: String)
