package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Location
import com.github.se.eduverse.repository.LocationRepository
import com.github.se.eduverse.repository.NominatimLocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class LocationViewModel(private val repository: LocationRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _locationSuggestions = MutableStateFlow<List<Location>>(emptyList())
    val locationSuggestions: StateFlow<List<Location>> = _locationSuggestions.asStateFlow()

    fun setQuery(newQuery: String) {
        _query.value = newQuery
        searchLocation(newQuery)
    }

    private fun searchLocation(query: String) {
        viewModelScope.launch {
            repository.search(
                query,
                onSuccess = { locationList -> _locationSuggestions.value = locationList },
                onFailure = { exception ->
                    Log.e("LocationViewModel", "Error searching location", exception)
                })
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LocationViewModel(NominatimLocationRepository(OkHttpClient())) as T
                }
            }
    }
}