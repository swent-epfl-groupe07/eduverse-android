package com.github.se.eduverse.model

import kotlinx.serialization.Serializable

@Serializable data class NotifAuthorizations(var eventEnabled: Boolean, var taskEnabled: Boolean)
