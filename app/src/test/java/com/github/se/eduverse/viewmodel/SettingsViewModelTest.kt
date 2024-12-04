package com.github.se.eduverse.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.se.eduverse.repository.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  private val repository: SettingsRepository = mockk(relaxed = true)
  private lateinit var auth: FirebaseAuth
  private lateinit var viewModel: SettingsViewModel

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    auth = mockk()

    // Mock FirebaseUser
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    every { mockUser.uid } returns "user123"

    // Mock FirebaseAuth to return the mocked FirebaseUser
    every { auth.currentUser } returns mockUser

    // Mock repository methods
    coEvery { repository.getPrivacySettings("user123") } returns true
    coEvery { repository.getSelectedLanguage("user123") } returns "English"
    coEvery { repository.getSelectedTheme("user123") } returns "Light"

    Dispatchers.setMain(testDispatcher)

    viewModel = SettingsViewModel(repository, auth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun init_loadsSettings() = runTest {
    // Advance coroutine to complete initialization
    advanceUntilIdle()

    // Verify that repository methods were called
    coVerifySequence {
      repository.getPrivacySettings("user123")
      repository.getSelectedLanguage("user123")
      repository.getSelectedTheme("user123")
    }

    // Verify StateFlow values
    assertEquals(true, viewModel.privacySettings.value)
    assertEquals("English", viewModel.selectedLanguage.value)
    assertEquals("Light", viewModel.selectedTheme.value)
  }

  @Test
  fun updatePrivacySettings_updatesStateAndRepository() = runTest {
    coEvery { repository.setPrivacySettings("user123", false) } just Runs

    viewModel.updatePrivacySettings(false)

    // Verify StateFlow is updated
    assertEquals(false, viewModel.privacySettings.value)

    // Verify repository is called
    coVerify { repository.setPrivacySettings("user123", false) }
  }

  @Test
  fun updateSelectedLanguage_updatesStateAndRepository() = runTest {
    coEvery { repository.setSelectedLanguage("user123", "Français") } just Runs

    viewModel.updateSelectedLanguage("Français")

    // Verify StateFlow is updated
    assertEquals("Français", viewModel.selectedLanguage.value)

    // Verify repository is called
    coVerify { repository.setSelectedLanguage("user123", "Français") }
  }

  @Test
  fun updateSelectedTheme_updatesStateAndRepository() = runTest {
    coEvery { repository.setSelectedTheme("user123", "Dark") } just Runs

    viewModel.updateSelectedTheme("Dark")

    // Verify StateFlow is updated
    assertEquals("Dark", viewModel.selectedTheme.value)

    // Verify repository is called
    coVerify { repository.setSelectedTheme("user123", "Dark") }
  }

  @Test
  fun loadSettings_handlesException() = runTest {
    // Create a new mock repository that throws an exception
    val exceptionRepository: SettingsRepository = mockk(relaxed = true)
    coEvery { exceptionRepository.getPrivacySettings("user123") } throws
        Exception("Firestore error")

    // Create a new ViewModel with the exception-throwing repository
    val newViewModel = SettingsViewModel(exceptionRepository, auth)

    // Advance coroutine until idle to allow init block to execute
    advanceUntilIdle()

    // Verify StateFlow retains default values when exception occurs
    assertEquals(true, newViewModel.privacySettings.value)
    assertEquals("English", newViewModel.selectedLanguage.value)
    assertEquals("Light", newViewModel.selectedTheme.value)
  }

  @Test
  fun unauthenticated_user_doesNotLoadSettings() = runTest {
    // Mock FirebaseAuth to return null for currentUser
    val unauthenticatedAuth: FirebaseAuth = mockk()
    every { unauthenticatedAuth.currentUser } returns null

    // Create a new mock repository
    val repository: SettingsRepository = mockk(relaxed = true)

    // Create a new ViewModel with no authenticated user
    val newViewModel = SettingsViewModel(repository, unauthenticatedAuth)

    // Advance coroutine until idle
    advanceUntilIdle()

    // Verify that repository methods are not called
    coVerify(exactly = 0) {
      repository.getPrivacySettings(any())
      repository.getSelectedLanguage(any())
      repository.getSelectedTheme(any())
    }

    // Verify StateFlow retains default values
    assertEquals(true, newViewModel.privacySettings.value)
    assertEquals("English", newViewModel.selectedLanguage.value)
    assertEquals("Light", newViewModel.selectedTheme.value)
  }

  @Test
  fun updatePrivacySettings_handlesException() = runTest {
    coEvery { repository.setPrivacySettings("user123", false) } throws Exception("Firestore error")

    viewModel.updatePrivacySettings(false)

    // StateFlow is updated even if repository fails
    assertEquals(false, viewModel.privacySettings.value)

    // Verify repository is called
    coVerify { repository.setPrivacySettings("user123", false) }
  }

  @Test
  fun updateSelectedLanguage_handlesException() = runTest {
    // Arrange
    coEvery { repository.setSelectedLanguage("user123", "Français") } throws
        Exception("Firestore error")

    // Act
    viewModel.updateSelectedLanguage("Français")

    // Assert
    // StateFlow is updated even if repository fails
    assertEquals("Français", viewModel.selectedLanguage.value)

    // Verify repository is called
    coVerify { repository.setSelectedLanguage("user123", "Français") }
  }

  @Test
  fun updateSelectedTheme_handlesException() = runTest {
    // Arrange
    coEvery { repository.setSelectedTheme("user123", "Dark") } throws Exception("Firestore error")

    // Act
    viewModel.updateSelectedTheme("Dark")

    // Assert
    // StateFlow is updated even if repository fails
    assertEquals("Dark", viewModel.selectedTheme.value)

    // Verify repository is called
    coVerify { repository.setSelectedTheme("user123", "Dark") }
  }
}
