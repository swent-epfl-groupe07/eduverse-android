package com.github.se.eduverse.model

/*
This class represent the data of a notification, if the app is opened by one. It's goal is to
be able to scale the notification system by adding new notifications channels, and to reduce
the amount of argument of EduverseApp (in comparison to just passing every field one by one)
 */
data class NotificationData(
    var isNotification: Boolean,
    val objectId: String? = null,
    var onOpen: suspend () -> Unit = {}
)
