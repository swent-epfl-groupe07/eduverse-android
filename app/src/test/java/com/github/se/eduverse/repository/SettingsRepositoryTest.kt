package com.github.se.eduverse.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.mockk.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

  // Rule to set Main dispatcher to a TestCoroutineDispatcher
  private lateinit var firestore: FirebaseFirestore
  private lateinit var documentReference: DocumentReference
  private lateinit var repository: SettingsRepository

  @Before
  fun setup() {
    // Mock static methods first
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    // Initialize mocks
    firestore = mockk()
    documentReference = mockk()
    repository = SettingsRepository(firestore)

    // Stub firestore.collection("user_settings").document(userId)
    every { firestore.collection("user_settings") } returns
        mockk { every { document(any()) } returns documentReference }

    // Stub documentReference.set(any(), any())
    val mockSetTask: Task<Void> = mockk {
      // Stub the await() call
      coEvery { await() } returns mockk()
    }
    every { documentReference.set(any(), any()) } returns mockSetTask

    // Stub documentReference.get()
    val mockGetTask: Task<DocumentSnapshot> = mockk {
      // Stub the await() call
      coEvery { await() } returns mockk(relaxed = true)
    }
    every { documentReference.get() } returns mockGetTask
  }

  @After
  fun tearDown() {
    // Unmock all to avoid interference with other tests
    unmockkAll()
  }

  @Test
  fun `getPrivacySettings returns correct value when privacySettings is false`() = runTest {
    // Arrange
    val userId = "user123"
    val documentSnapshot =
        mockk<DocumentSnapshot>(relaxed = true) {
          every { getBoolean("privacySettings") } returns false
        }

    // Mock the get() call to return the mocked documentSnapshot
    coEvery { documentReference.get().await() } returns documentSnapshot

    // Act
    val result = repository.getPrivacySettings(userId)

    // Assert
    assertFalse(result)
    coVerify(exactly = 1) { documentReference.get().await() }
  }

  @Test
  fun `getPrivacySettings returns true when privacySettings is null`() = runTest {
    // Arrange
    val userId = "user123"
    val documentSnapshot =
        mockk<DocumentSnapshot>(relaxed = true) {
          every { getBoolean("privacySettings") } returns null
        }

    coEvery { documentReference.get().await() } returns documentSnapshot

    // Act
    val result = repository.getPrivacySettings(userId)

    // Assert
    assertTrue(result) // Default value is true
    coVerify(exactly = 1) { documentReference.get().await() }
  }

  @Test
  fun `setPrivacySettings successfully updates Firestore`() = runTest {
    // Arrange
    val userId = "testUser"
    val expectedValue = true
    val data = mapOf("privacySettings" to expectedValue)

    // Mock the Firestore interactions
    coEvery { documentReference.set(data, SetOptions.merge()).await() } returns null

    // Act
    repository.setPrivacySettings(userId, expectedValue)

    // Assert
    coVerify(exactly = 1) { documentReference.set(data, SetOptions.merge()).await() }
  }

  @Test
  fun `getSelectedLanguage returns correct value`() = runTest {
    // Arrange
    val userId = "user123"
    val expectedLanguage = "Français"
    val documentSnapshot =
        mockk<DocumentSnapshot>(relaxed = true) {
          every { getString("selectedLanguage") } returns expectedLanguage
        }

    coEvery { documentReference.get().await() } returns documentSnapshot

    // Act
    val result = repository.getSelectedLanguage(userId)

    // Assert
    assertEquals(expectedLanguage, result)
    coVerify(exactly = 1) { documentReference.get().await() }
  }

  @Test
  fun `getSelectedLanguage returns default value when null`() = runTest {
    // Arrange
    val userId = "user123"
    val documentSnapshot =
        mockk<DocumentSnapshot>(relaxed = true) {
          every { getString("selectedLanguage") } returns null
        }

    coEvery { documentReference.get().await() } returns documentSnapshot

    // Act
    val result = repository.getSelectedLanguage(userId)

    // Assert
    assertEquals("English", result) // Default value
    coVerify(exactly = 1) { documentReference.get().await() }
  }

  @Test
  fun `setSelectedLanguage successfully updates Firestore`() = runTest {
    // Arrange
    val userId = "user123"
    val newLanguage = "Français"
    val data = mapOf("selectedLanguage" to newLanguage)

    // Mock the Firestore interactions
    coEvery { documentReference.set(data, SetOptions.merge()).await() } returns null

    // Act
    repository.setSelectedLanguage(userId, newLanguage)

    // Assert
    coVerify(exactly = 1) { documentReference.set(data, SetOptions.merge()).await() }
  }

  @Test
  fun `getSelectedTheme returns correct value`() = runTest {
    // Arrange
    val userId = "user123"
    val expectedTheme = "Dark"
    val documentSnapshot =
        mockk<DocumentSnapshot>(relaxed = true) {
          every { getString("selectedTheme") } returns expectedTheme
        }

    coEvery { documentReference.get().await() } returns documentSnapshot

    // Act
    val result = repository.getSelectedTheme(userId)

    // Assert
    assertEquals(expectedTheme, result)
    coVerify(exactly = 1) { documentReference.get().await() }
  }

  @Test
  fun `getSelectedTheme returns default value when null`() = runTest {
    // Arrange
    val userId = "user123"
    val documentSnapshot =
        mockk<DocumentSnapshot>(relaxed = true) {
          every { getString("selectedTheme") } returns null
        }

    coEvery { documentReference.get().await() } returns documentSnapshot

    // Act
    val result = repository.getSelectedTheme(userId)

    // Assert
    assertEquals("Light", result) // Default value
    coVerify(exactly = 1) { documentReference.get().await() }
  }

  @Test
  fun `setSelectedTheme successfully updates Firestore`() = runTest {
    // Arrange
    val userId = "user123"
    val newTheme = "Dark"
    val data = mapOf("selectedTheme" to newTheme)

    // Mock the Firestore interactions
    coEvery { documentReference.set(data, SetOptions.merge()).await() } returns null

    // Act
    repository.setSelectedTheme(userId, newTheme)

    // Assert
    coVerify(exactly = 1) { documentReference.set(data, SetOptions.merge()).await() }
  }

  @Test
  fun `getPrivacySettings handles exceptions`() = runTest {
    // Arrange
    val userId = "user123"
    coEvery { documentReference.get().await() } throws Exception("Firestore error")

    // Act & Assert
    try {
      repository.getPrivacySettings(userId)
      fail("Expected exception was not thrown")
    } catch (e: Exception) {
      assertEquals("Firestore error", e.message)
    }

    coVerify(exactly = 1) { documentReference.get().await() }
  }
}
