package com.github.se.eduverse.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class SettingsRepository(private val firestore: FirebaseFirestore) {

  suspend fun setPrivacySettings(userId: String, value: Boolean) {
    try {
      val data = mapOf("privacySettings" to value)
      firestore.collection("user_settings").document(userId).set(data, SetOptions.merge()).await()
    } catch (e: Exception) {
      // Handle exceptions as needed from caller
      throw e
    }
  }

  suspend fun getPrivacySettings(userId: String): Boolean {
    return try {
      val documentSnapshot = firestore.collection("user_settings").document(userId).get().await()

      // Retrieve the "privacySettings" field, default to true if null
      documentSnapshot.getBoolean("privacySettings") ?: true
    } catch (e: Exception) {
      // Handle exceptions as needed
      throw e
    }
  }

  suspend fun getSelectedLanguage(userId: String): String {
    return try {
      val documentSnapshot = firestore.collection("user_settings").document(userId).get().await()

      // Retrieve the "selectedLanguage" field, default to "English" if null
      documentSnapshot.getString("selectedLanguage") ?: "English"
    } catch (e: Exception) {
      // Handle exceptions as needed
      throw e
    }
  }

  suspend fun setSelectedLanguage(userId: String, language: String) {
    try {
      val data = mapOf("selectedLanguage" to language)
      firestore.collection("user_settings").document(userId).set(data, SetOptions.merge()).await()
    } catch (e: Exception) {
      // Handle exceptions as needed
      throw e
    }
  }

  suspend fun getSelectedTheme(userId: String): String {
    return try {
      val documentSnapshot = firestore.collection("user_settings").document(userId).get().await()

      // Retrieve the "selectedTheme" field, default to "Light" if null
      documentSnapshot.getString("selectedTheme") ?: "Light"
    } catch (e: Exception) {
      // Handle exceptions as needed
      throw e
    }
  }

  suspend fun setSelectedTheme(userId: String, theme: String) {
    try {
      val data = mapOf("selectedTheme" to theme)
      firestore.collection("user_settings").document(userId).set(data, SetOptions.merge()).await()
    } catch (e: Exception) {
      // Handle exceptions as needed
      throw e
    }
  }
}
