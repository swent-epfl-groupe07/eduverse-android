package com.github.se.eduverse.viewmodel

import com.github.se.eduverse.repository.Profile
import com.github.se.eduverse.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

  // TestCoroutineDispatcher is replaced by StandardTestDispatcher (Kotlin 1.6+)
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var profileViewModel: ProfileViewModel
  private val mockRepository: ProfileRepository = mock(ProfileRepository::class.java)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher) // Set test dispatcher for coroutines
    profileViewModel = ProfileViewModel(mockRepository)
  }

  @Test
  fun `loadProfile updates profileState when repository returns a profile`() = runTest {
    val testProfile =
        Profile(
            name = "John Doe",
            school = "Test School",
            coursesSelected = "5",
            videosWatched = "10",
            quizzesCompleted = "3",
            studyTime = "20.0",
            studyGoals = "Graduate")

    // Mock the repository's getProfile method to return the test profile
    val userId = "testUser"
    `when`(mockRepository.getProfile(userId)).thenReturn(testProfile)

    // Call the loadProfile method
    profileViewModel.loadProfile(userId)

    // Execute pending coroutine actions
    advanceUntilIdle()

    // Verify that the profileState is updated with the test profile
    val result = profileViewModel.profileState.first()
    assertEquals(testProfile, result)
  }

  @Test
  fun `saveProfile updates profileState and saves profile to repository`() = runTest {
    val userId = "testUser"
    val name = "John Doe"
    val school = "Test School"
    val coursesSelected = "5"
    val videosWatched = "10" // String as per your signature
    val quizzesCompleted = "3" // String as per your signature
    val studyTime = "20.0" // Double as per your signature
    val studyGoals = "Graduate"

    // Call saveProfile method with the input values
    profileViewModel.saveProfile(
        userId = userId,
        name = name,
        school = school,
        coursesSelected = coursesSelected,
        videosWatched = videosWatched,
        quizzesCompleted = quizzesCompleted,
        studyTime = studyTime,
        studyGoals = studyGoals)

    // Execute pending coroutine actions
    advanceUntilIdle()

    // Verify that the profileState is updated correctly
    val updatedProfile = profileViewModel.profileState.first()

    assertEquals(name, updatedProfile.name)
    assertEquals(school, updatedProfile.school)
    assertEquals(coursesSelected, updatedProfile.coursesSelected)
    assertEquals(videosWatched, updatedProfile.videosWatched)
    assertEquals(quizzesCompleted, updatedProfile.quizzesCompleted)
    assertEquals(studyTime, updatedProfile.studyTime)
    assertEquals(studyGoals, updatedProfile.studyGoals)

    // Verify that the repository's saveProfile method was called with the correct data
    verify(mockRepository).saveProfile(userId, updatedProfile)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain() // Reset the main dispatcher
  }
}
