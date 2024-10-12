package com.github.se.eduverse.model

data class Widget(
    val widgetId: String = "",
    val widgetType: String = "",
    val widgetTitle: String = "",
    val widgetContent: String = "",
    val ownerUid: String = ""
)

enum class CommonWidgetType(val title: String, val content: String) {
  TIMER("Study Timer", "Track your study sessions."),
  CALCULATOR("Calculator", "Perform basic calculations."),
  PDF_CONVERTER("PDF Converter", "Convert images to PDFs."),
  WEEKLY_PLANNER("Weekly Planner", "Plan your weekly schedule.")
}
