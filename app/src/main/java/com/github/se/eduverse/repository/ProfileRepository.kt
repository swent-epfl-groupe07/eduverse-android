package com.github.se.eduverse.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Profile(
    val name: String = "",
    val school: String = "",
    val coursesSelected: String = "",
    val videosWatched: String = "",
    val quizzesCompleted: String = "",
    val studyTime: String = "",
    val studyGoals: String = ""
)

interface ProfileRepository {
  suspend fun saveProfile(userId: String, profile: Profile)

  suspend fun getProfile(userId: String): Profile?
}

class ProfileRepositoryImpl(private val firestore: FirebaseFirestore) : ProfileRepository {
  private val profilesCollection = firestore.collection("profiles")

  override suspend fun saveProfile(userId: String, profile: Profile) {
    profilesCollection
        .document(userId) // You can use a dynamic ID for each user
        .set(profile)
        .await() // This makes sure it's a suspend function and works with coroutines
  }

  override suspend fun getProfile(userId: String): Profile? {
    val snapshot = profilesCollection.document(userId).get().await()
    return snapshot.toObject(Profile::class.java)
  }
}
