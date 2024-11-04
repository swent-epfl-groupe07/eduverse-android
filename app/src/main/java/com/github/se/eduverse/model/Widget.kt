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
  PDF_CONVERTER("PDF Converter", "Convert images to PDFs.", Screen.PDF_CONVERTER),
  FOLDERS("Folders", "Access your personal space", Screen.LIST_FOLDERS)
}
