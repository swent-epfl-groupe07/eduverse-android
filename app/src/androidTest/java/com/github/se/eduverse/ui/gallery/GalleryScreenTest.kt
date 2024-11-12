package com.github.se.eduverse.ui.gallery

import androidx.compose.ui.test.* // Import pour les tests UI
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.model.Photo
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.github.se.eduverse.viewmodel.PhotoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class GalleryScreenUiTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val mockNavigationActions = aliBouiri(navController = mock())
  private lateinit var fakePhotoViewModel: FakePhotoViewModel
  private lateinit var folderViewModel: FolderViewModel
  private lateinit var folderRepository: FolderRepository
  private lateinit var auth: FirebaseAuth

  val folder1 = Folder("uid", emptyList<MyFile>().toMutableList(), "folder1", "1", archived = false)
  val folder2 = Folder("uid", emptyList<MyFile>().toMutableList(), "folder2", "2", archived = false)

  @Before
  fun setUp() {
    fakePhotoViewModel =
        FakePhotoViewModel().apply {
          setPhotos(
              listOf(
                  Photo("testOwnerId", ByteArray(0), "path1"),
                  Photo("testOwnerId", ByteArray(0), "path2")))
        }

    folderRepository = mock(FolderRepository::class.java)
    auth = mock(FirebaseAuth::class.java)
    val user = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(user)
    `when`(user.uid).thenReturn("uid")

    `when`(
            folderRepository.getFolders(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any()))
        .then {
          val callback = it.getArgument<(List<Folder>) -> Unit>(1)
          callback(listOf(folder1, folder2))
        }
    folderViewModel = FolderViewModel(folderRepository, auth)
  }

  @Test
  fun testGalleryScreenDisplaysPhotos() {
    setupGalleryScreen()

    // Assure-toi que tu utilises le bon testTag avec la bonne partie du path
    composeTestRule.onNodeWithTag("PhotoItem_path1").assertExists()
    composeTestRule.onNodeWithTag("PhotoItem_path2").assertExists()

    // Si tu veux tester plusieurs éléments avec le même testTag :
    composeTestRule.onAllNodesWithTag("PhotoItem_path1").assertCountEquals(1)
    composeTestRule.onAllNodesWithTag("PhotoItem_path2").assertCountEquals(1)
  }

  @Test
  fun testGalleryScreenDisplaysNoPhotosMessage() {
    fakePhotoViewModel.setPhotos(emptyList()) // Simuler l'absence de photos
    setupGalleryScreen()

    composeTestRule.onNodeWithTag("NoPhotosText").assertExists()
    composeTestRule.onAllNodesWithTag("PhotoItem").assertCountEquals(0)
  }

  @Test
  fun testPhotoItemClick() {
    setupGalleryScreen()

    // Attendre que l'interface utilisateur soit complètement chargée
    composeTestRule.waitForIdle()

    // Effectuer le clic sur l'élément avec le testTag approprié
    composeTestRule.onNodeWithTag("PhotoItem_path1").performClick()

    // Vérifier que la boîte de dialogue s'affiche
    composeTestRule.onNodeWithTag("ImageDialogBox").assertExists()
  }

  @Test
  fun testImageDialogDismiss() {
    setupGalleryScreen()

    // Attendre que l'UI soit prête avant d'essayer de cliquer sur l'élément
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("PhotoItem_path1").performClick()
    composeTestRule.onNodeWithTag("ImageDialogBox").assertExists()

    composeTestRule.onNodeWithTag("CloseButton").performClick()
    composeTestRule.onNodeWithTag("ImageDialogBox").assertDoesNotExist()
  }

  @Test
  fun testImageDialogDownload() {
    setupGalleryScreen()

    // Attendre que l'UI soit prête
    composeTestRule.waitForIdle()

    // Tenter d'accéder à l'élément photo avec le testTag correspondant
    composeTestRule
        .onNodeWithTag("PhotoItem_path1") // Assure-toi que path1 est bien dans la liste des photos
        .performClick()

    // Vérifier que le dialogue d'image s'affiche
    composeTestRule.onNodeWithTag("ImageDialogBox").assertExists()

    // Effectuer un clic sur le bouton de téléchargement
    composeTestRule.onNodeWithTag("DownloadButton").performClick()
  }

  @Test
  fun testNavigationBack() {
    setupGalleryScreen()

    composeTestRule.onNodeWithTag("GoBackButton").performClick()
    // Vérifier que l'action de navigation a été appelée
    assert(mockNavigationActions.backCalled)
  }

  @Test
  fun testAddToFolder() {
    setupGalleryScreen()

    composeTestRule.onNodeWithTag("PhotoItem_path1").performClick()
    composeTestRule.onNodeWithTag("ImageDialogBox").assertExists()
    composeTestRule.onNodeWithTag("addToFolder").performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("button_container").assertIsDisplayed()

    composeTestRule.onNodeWithTag("folder_button1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("folder_button2").assertIsDisplayed()

    composeTestRule.onNodeWithTag("folder_button1").performClick()
    composeTestRule.onNodeWithTag("button_container").assertIsNotDisplayed()

    verify(folderRepository)
        .updateFolder(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())
    assertEquals(1, folder1.files.size)
  }

  private fun setupGalleryScreen() {
    composeTestRule.setContent {
      GalleryScreen(
          ownerId = "testOwnerId",
          photoViewModel = fakePhotoViewModel,
          folderViewModel = folderViewModel,
          navigationActions = mockNavigationActions)
    }
  }
}

// Classe factice qui simule le comportement de PhotoViewModel
// Classe factice qui simule le comportement de PhotoViewModel sans l'hériter
class FakePhotoViewModel : PhotoViewModel(mockRepository(), mock(FileRepository::class.java)) {

  // Simule une liste de photos
  override val _photos =
      MutableStateFlow(
          listOf(
              Photo(ownerId = "1", photo = ByteArray(0), path = "path1"),
              Photo(ownerId = "2", photo = ByteArray(0), path = "path2")))
  override val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

  private val _savePhotoState = MutableStateFlow(false)
  override val savePhotoState: StateFlow<Boolean> = _savePhotoState.asStateFlow()

  override fun getPhotosByOwner(ownerId: String) {
    // No-op pour les tests
  }

  override fun makeFileFromPhoto(photo: Photo, onSuccess: (String) -> Unit) {
    onSuccess("success")
  }

  fun setPhotos(newPhotos: List<Photo>) {
    _photos.value = newPhotos
  }
}

// Simule un repository pour PhotoViewModel
private fun mockRepository() =
    object : com.github.se.eduverse.model.repository.IPhotoRepository {
      override suspend fun savePhoto(photo: Photo): Boolean = true

      override suspend fun updatePhoto(photoId: String, photo: Photo): Boolean = true

      override suspend fun deletePhoto(photoId: String): Boolean = true

      override suspend fun getPhotosByOwner(ownerId: String): List<Photo> = emptyList()
    }

// Faux NavigationActions pour les tests de navigation
class aliBouiri(navController: NavHostController) : NavigationActions(navController) {
  var backCalled = false

  override fun goBack() {
    backCalled = true
  }
}
