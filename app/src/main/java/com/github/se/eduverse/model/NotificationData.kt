package com.github.se.eduverse.model

import androidx.lifecycle.ViewModel
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Route
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.viewmodel.TimeTableViewModel

/*
This class represent the data of a notification, if the app is opened by one. It's goal is to
be able to scale the notification system by adding new notifications channels, and to reduce
the amount of argument of EduverseApp (in comparison to just passing every field one by one)
 */
data class NotificationData(
    var isNotification: Boolean,
    val notificationType: NotificationType? = null,
    val objectId: String? = null,
    var viewModel: ViewModel? = null
) {
  /**
   * Navigate to the correct screen according to the data. If anything goes wrong, navigates to the
   * dashboard screen.
   */
  suspend fun open(navigationActions: NavigationActions) {
    when (notificationType) {
      NotificationType.SCHEDULED -> {
        if (viewModel == null) navigationActions.navigateTo(Route.DASHBOARD)
        val timeTableViewModel = viewModel as TimeTableViewModel

        var scheduled: Scheduled? = null
        if (objectId != null) {
          scheduled = timeTableViewModel.getScheduledById(objectId)
        }
        timeTableViewModel.opened = scheduled

        when (scheduled?.type) {
          ScheduledType.TASK -> {
            navigationActions.navigateTo(Screen.DETAILS_TASKS)
          }
          ScheduledType.EVENT -> {
            navigationActions.navigateTo(Screen.DETAILS_EVENT)
          }
          else -> { // scheduled is null
            navigationActions.navigateTo(Route.DASHBOARD)
          }
        }
      }
      NotificationType.DEFAULT,
      null -> {
        navigationActions.navigateTo(Route.DASHBOARD)
      }
    }
  }
}

enum class NotificationType {
  SCHEDULED, // Not TASK and EVENT because when creating the notification, we only have the object
             // ID
  DEFAULT // Will open the app on the dashboard menu
}
