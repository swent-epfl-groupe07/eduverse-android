package com.github.se.eduverse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.isAppInDarkMode
import com.github.se.eduverse.repository.SettingsRepository
import com.github.se.eduverse.ui.theme.Theme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class SettingsViewModel(
    val settingsRepository: SettingsRepository,
    private val auth: FirebaseAuth,
    private val setAppThemeIsDarkMode: (Boolean) -> Unit = { isAppInDarkMode = it }
) : ViewModel() {

  private val _privacySettings = MutableStateFlow(true)
  open val privacySettings: StateFlow<Boolean>
    get() = _privacySettings

  private val _selectedLanguage = MutableStateFlow("English")
  open val selectedLanguage: StateFlow<String>
    get() = _selectedLanguage

  private val _selectedTheme = MutableStateFlow(Theme.LIGHT)
  open val selectedTheme: StateFlow<String>
    get() = _selectedTheme

  val userId: String?
    get() = auth.currentUser?.uid

  init {
    viewModelScope.launch {
      val uid = auth.currentUser?.uid
      if (uid == null) {
        Log.e("SettingsViewModel", "User is not authenticated while trying to load the settings")
        return@launch // Exit early if no user is authenticated
      }

      try {
        _privacySettings.value = settingsRepository.getPrivacySettings(uid)
        _selectedLanguage.value = settingsRepository.getSelectedLanguage(uid)
        _selectedTheme.value = settingsRepository.getSelectedTheme(uid)
        setAppThemeIsDarkMode(selectedTheme.value == Theme.DARK)
      } catch (e: Exception) {
        Log.e("SettingsViewModel", "Exception $e while trying to load the settings")
        // Set defaults on exception
        _privacySettings.value = true
        _selectedLanguage.value = "English"
        _selectedTheme.value = Theme.LIGHT
      }
    }
  }

  fun updatePrivacySettings(value: Boolean) {
    _privacySettings.value = value
    val uid = userId
    if (uid != null) {
      viewModelScope.launch {
        try {
          settingsRepository.setPrivacySettings(uid, value)
          _privacySettings.value = value
        } catch (e: Exception) {
          Log.e("SettingsViewModel", "Exception $e while updating privacy settings")
        }
      }
    } else {
      Log.e("SettingsViewModel", "User is not authenticated while updating privacy settings")
    }
  }

  fun updateSelectedLanguage(value: String) {
    _selectedLanguage.value = value
    val uid = userId
    if (uid != null) {
      viewModelScope.launch {
        try {
          settingsRepository.setSelectedLanguage(uid, value)
        } catch (e: Exception) {
          Log.e("SettingsViewModel", "Exception $e while updating selected language")
        }
      }
    } else {
      Log.e("SettingsViewModel", "User is not authenticated while updating selected language")
    }
  }

  open fun updateSelectedTheme(value: String) {
    _selectedTheme.value = value
    setAppThemeIsDarkMode(value == Theme.DARK)
    val uid = userId
    if (uid != null) {
      viewModelScope.launch {
        try {
          settingsRepository.setSelectedTheme(uid, value)
        } catch (e: Exception) {
          Log.e("SettingsViewModel", "Exception $e while updating selected theme")
        }
      }
    } else {
      Log.e("SettingsViewModel", "User is not authenticated while updating selected theme")
    }
  }

  suspend fun getPrivacySettingsDirect(userId: String): Boolean {
    return settingsRepository.getPrivacySettings(userId)
  }
}
