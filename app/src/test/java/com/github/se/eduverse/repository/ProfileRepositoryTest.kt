package com.github.se.eduverse.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever

class ProfileRepositoryImplTest {

  private lateinit var repository: ProfileRepositoryImpl
  private val mockFirestore: FirebaseFirestore = mock(FirebaseFirestore::class.java)
  private val mockCollectionRef: CollectionReference = mock(CollectionReference::class.java)
  private val mockDocumentRef: DocumentReference = mock(DocumentReference::class.java)
  private val mockSnapshot: DocumentSnapshot = mock(DocumentSnapshot::class.java)

  @Before
  fun setUp() {
    whenever(mockFirestore.collection("profiles")).thenReturn(mockCollectionRef)

    repository = ProfileRepositoryImpl(mockFirestore)
  }

  @Test
  fun `saveProfile should save profile to Firestore`(): Unit = runBlocking {
    val userId = "testUser"
    val profile =
        Profile(
            name = "John Doe",
            school = "Test School",
            coursesSelected = "5",
            videosWatched = "10",
            quizzesCompleted = "3",
            studyTime = "20",
            studyGoals = "Graduate")

    // Mock Firestore interactions
    whenever(mockFirestore.collection("profiles")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.set(profile)).thenReturn(Tasks.forResult(null))

    // Call the saveProfile method
    repository.saveProfile(userId, profile)

    // Verify that the Firestore set method was called with the correct data
    verify(mockDocumentRef).set(profile)
  }

  @Test
  fun `getProfile should return profile from Firestore`(): Unit = runBlocking {
    val userId = "testUser"
    val expectedProfile =
        Profile(
            name = "John Doe",
            school = "Test School",
            coursesSelected = "5",
            videosWatched = "10",
            quizzesCompleted = "3",
            studyTime = "20",
            studyGoals = "Graduate")

    // Mock Firestore interactions
    whenever(mockFirestore.collection("profiles")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockSnapshot.toObject(Profile::class.java)).thenReturn(expectedProfile)

    // Call the getProfile method
    val result = repository.getProfile(userId)

    // Assert that the returned profile matches the expected profile
    assertEquals(expectedProfile, result)

    // Verify that Firestore get method was called
    verify(mockDocumentRef).get()
  }

  @Test
  fun `getProfile should return null if profile does not exist`(): Unit = runBlocking {
    val userId = "nonExistentUser"

    // Mock Firestore interactions for a non-existent profile
    whenever(mockFirestore.collection("profiles")).thenReturn(mockCollectionRef)
    whenever(mockCollectionRef.document(userId)).thenReturn(mockDocumentRef)
    whenever(mockDocumentRef.get()).thenReturn(Tasks.forResult(mockSnapshot))
    whenever(mockSnapshot.toObject(Profile::class.java)).thenReturn(null)

    // Call the getProfile method
    val result = repository.getProfile(userId)

    // Assert that the result is null
    assertNull(result)

    // Verify that Firestore get method was called
    verify(mockDocumentRef).get()
  }
}
