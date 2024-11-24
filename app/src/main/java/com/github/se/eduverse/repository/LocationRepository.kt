package com.github.se.eduverse.repository

import com.github.se.eduverse.model.Location

interface LocationRepository {
    fun search(query: String, onSuccess: (List<Location>) -> Unit, onFailure: (Exception) -> Unit)
}