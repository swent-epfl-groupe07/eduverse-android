package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Event

interface EventRepository {
    fun getEvent(id: String, onSuccess: (Event) -> Unit, onFailure: (Exception) -> Unit)

    fun addEvent(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

    fun updateEvent(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)

    fun deleteEvent(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
}