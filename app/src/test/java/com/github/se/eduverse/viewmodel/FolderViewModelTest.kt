package com.github.se.eduverse.viewmodel

import android.os.Looper
import com.github.se.eduverse.model.FilterTypes
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.repository.FolderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class FolderViewModelTest {
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var folderRepository: FolderRepository
  private lateinit var auth: FirebaseAuth
  private lateinit var currentUser: FirebaseUser
  private lateinit var folderViewModel: FolderViewModel

  lateinit var folder: Folder
  lateinit var archivedFolder: Folder
  lateinit var folder2: Folder

  val file1 = MyFile("", "", "name 1", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file2 = MyFile("", "", "name 2", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file3 = MyFile("", "", "name 3", Calendar.getInstance(), Calendar.getInstance(), 0)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    file1.creationTime.set(3, 1)
    file2.creationTime.set(3, 2)
    file3.creationTime.set(3, 3)

    file1.lastAccess.set(3, 2)
    file2.lastAccess.set(3, 3)
    file3.lastAccess.set(3, 1)

    file1.numberAccess = 4
    file3.numberAccess = 1

    folder =
        Folder(
            "uid",
            MutableList(3) {
              when (it) {
                1 -> file1
                2 -> file2
                else -> file3
              }
            },
            "folder",
            "1",
            archived = false)
    archivedFolder =
        Folder(
            "uid",
            MutableList(3) {
              when (it) {
                1 -> file1
                2 -> file2
                else -> file3
              }
            },
            "archived",
            "1",
            archived = true)
    folder2 = Folder("uid", emptyList<MyFile>().toMutableList(), "folder2", "2", archived = false)

    folderRepository = MockFolderRepository(folder, archivedFolder)
    auth = mock(FirebaseAuth::class.java)
    currentUser = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(currentUser)
    `when`(currentUser.uid).thenReturn("uid")
    folderViewModel = FolderViewModel(folderRepository, auth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun addFolderTest() {
    folderViewModel.addFolder(folder2)
    assertEquals(folderViewModel.folders.value.size, 2)
    assertSame(folderViewModel.folders.value[0], folder)
    assertSame(folderViewModel.folders.value[1], folder2)
  }

  @Test
  fun removeFolderTest() = runTest {
    folderViewModel.addFolder(folder2)
    folderViewModel.selectFolder(folder)

    folderViewModel.deleteFolders(listOf(folder))
    shadowOf(Looper.getMainLooper()).idle()
    delay(100) // Because the first async task start a second one
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(folderViewModel.folders.value.size, 1)
    assertSame(folderViewModel.folders.value[0], folder2)
    assertNull(folderViewModel.activeFolder.value)

    folderViewModel.deleteFolders(listOf(folder2))
    shadowOf(Looper.getMainLooper()).idle()
    delay(100) // Because the first async task start a second one
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(folderViewModel.folders.value.size, 0)
  }

  @Test
  fun removeManyFoldersTest() = runTest {
    folderViewModel.addFolder(folder2)

    folderViewModel.deleteFolders(listOf(folder, folder2))
    shadowOf(Looper.getMainLooper()).idle()
    delay(100) // Because the first async task start a second one
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(folderViewModel.folders.value.size, 0)
  }

  @Test
  fun updateFolderTest() {
    folderViewModel.selectFolder(folder)
    val folder3 =
        Folder("uid", emptyList<MyFile>().toMutableList(), "folder3", "1", archived = false)
    folderViewModel.updateFolder(folder3)
    assertSame(folderViewModel.activeFolder.value, folder3)
    assertEquals(folderViewModel.folders.value.size, 1)
    assertSame(folderViewModel.folders.value[0], folder3)

    val folder4 =
        Folder(
            "uid",
            emptyList<MyFile>().toMutableList(),
            "folder4",
            folderViewModel.getNewUid(),
            archived = false)
    assertEquals(folder4.id, "id test")
    folderViewModel.updateFolder(folder4)
    assertEquals(folderViewModel.folders.value.size, 1)
  }

  @Test
  fun sortFolderTest() {
    folderViewModel.selectFolder(folder)

    folderViewModel.sortBy(FilterTypes.NAME)
    assertSame(folder.files[0], file1)
    assertSame(folder.files[1], file2)
    assertSame(folder.files[2], file3)

    folderViewModel.sortBy(FilterTypes.CREATION_UP)
    assertSame(folder.files[0], file1)
    assertSame(folder.files[1], file2)
    assertSame(folder.files[2], file3)

    folderViewModel.sortBy(FilterTypes.CREATION_DOWN)
    assertSame(folder.files[0], file3)
    assertSame(folder.files[1], file2)
    assertSame(folder.files[2], file1)

    folderViewModel.sortBy(FilterTypes.ACCESS_RECENT)
    assertSame(folder.files[0], file2)
    assertSame(folder.files[1], file1)
    assertSame(folder.files[2], file3)

    folderViewModel.sortBy(FilterTypes.ACCESS_OLD)
    assertSame(folder.files[0], file3)
    assertSame(folder.files[1], file1)
    assertSame(folder.files[2], file2)

    folderViewModel.sortBy(FilterTypes.ACCESS_MOST)
    assertSame(folder.files[0], file1)
    assertSame(folder.files[1], file3)
    assertSame(folder.files[2], file2)

    folderViewModel.sortBy(FilterTypes.ACCESS_LEAST)
    assertSame(folder.files[0], file2)
    assertSame(folder.files[1], file3)
    assertSame(folder.files[2], file1)
  }

  @Test
  fun addFileTest() {
    val file4 = MyFile("", "", "name 4", Calendar.getInstance(), Calendar.getInstance(), 0)

    folderViewModel.addFile(file4)
    assertEquals(folder.files.count { it == file4 }, 0)

    folderViewModel.selectFolder(folder)
    folderViewModel.addFile(file4)
    assertEquals(folder.files.count { it == file4 }, 1)
  }

  @Test
  fun deleteFileTest() {
    folderViewModel.deleteFile(file3)
    assertEquals(folder.files.count { it == file3 }, 1)
    assertEquals(folder.files.size, 3)

    folderViewModel.selectFolder(folder)
    folderViewModel.deleteFile(file3)
    assertEquals(folder.files.count { it == file3 }, 0)
    assertEquals(folder.files.size, 2)
  }

  @Test
  fun renameFolderTest() {
    folderViewModel.selectFolder(folder)
    folderViewModel.renameFolder("test 1", folder)
    assertEquals(folderViewModel.activeFolder.value!!.name, "test 1")
    folderViewModel.renameFolder("test 2")
    assertEquals(folderViewModel.activeFolder.value!!.name, "test 2")
  }

  @Test
  fun createFileInFolderTest() {
    folderViewModel.createFileInFolder("fileId", "name", folder2)

    assertEquals(folder2.files.size, 1)
    assertEquals(folder2.files[0].fileId, "fileId")
    assertEquals(folder2.files[0].name, "name")
  }

  @Test
  fun getArchivedUserFoldersTest() {
    folderViewModel.getArchivedUserFolders()
    assertEquals(folderViewModel.folders.value.size, 1)
    assertEquals(folderViewModel.folders.value[0], archivedFolder)
  }

  @Test
  fun archiveFolderTest() {
    folderViewModel.archiveFolder(folder)
    assertEquals(folderViewModel.folders.value.size, 0)
    assertEquals(folder.archived, true)
  }

  @Test
  fun unarchiveFolderTest() {
    folderViewModel.getArchivedUserFolders()
    folderViewModel.unarchiveFolder(archivedFolder)
    assertEquals(folderViewModel.folders.value.size, 0)
    assertEquals(archivedFolder.archived, false)
  }

  @Test
  fun testCreateNewFolderFromName_onSuccess() {
    val newFolder =
        Folder("uid", emptyList<MyFile>().toMutableList(), "test", "id test", archived = false)
    folderViewModel.createNewFolderFromName("test", { assertEquals(newFolder, it) }, {})
  }
}

class MockFolderRepository(private val folder: Folder, private val archivedFolder: Folder) :
    FolderRepository {

  override fun getFolders(
      userId: String,
      archived: Boolean,
      onSuccess: (List<Folder>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    onSuccess(
        List(1) {
          return@List if (archived) archivedFolder else folder
        })
  }

  override fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    onSuccess()
  }

  override fun updateFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    onSuccess()
  }

  override suspend fun deleteFolders(
      folders: List<Folder>,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    onSuccess()
  }

  override fun getNewUid(): String {
    return "id test"
  }
}
