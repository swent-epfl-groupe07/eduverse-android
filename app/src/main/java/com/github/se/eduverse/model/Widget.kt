package com.github.se.eduverse.model

import com.github.se.eduverse.ui.navigation.Screen

data class Widget(
    val widgetId: String = "",
    val widgetType: String = "",
    val widgetTitle: String = "",
    val widgetContent: String = "",
    val ownerUid: String = "",
    val order: Int = 0
)

enum class CommonWidgetType(val title: String, val content: String, val route: String? = null) {
  TIMER("Study Timer", "Track your study sessions.", Screen.POMODORO),
  CALCULATOR("Calculator", "Perform basic calculations.", Screen.CALCULATOR),
  PDF_GENERATOR("PDF Generator", "Diverse tools to generate PDFs.", Screen.PDF_GENERATOR),
  FOLDERS("Folders", "Access your personal space", Screen.LIST_FOLDERS),
  TODO_LIST("Todo List", "Access your todo list", Screen.TODO_LIST),
  TIME_TABLE("Time table", "Plan your activities for the week", Screen.TIME_TABLE),
  QUIZZ("Quiz generator", "Generate a quiz on the topic of your choice", Screen.QUIZZ),
  ASSISTANT("Ai Assistant ", "Ask questions to your AI assistant", Screen.ASSISTANT),
  GALLERY("Gallery", "View your saved images and videos", Screen.GALLERY)
}
