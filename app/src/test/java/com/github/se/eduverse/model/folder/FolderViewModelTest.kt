package com.github.se.eduverse.model.folder

import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class FolderViewModelTest {
  private lateinit var folderRepository: FolderRepository
  private lateinit var currentUser: FirebaseUser
  private lateinit var folderViewModel: FolderViewModel

  lateinit var folder: Folder
  lateinit var folder2: Folder

  val file1 = MyFile("name 1", Calendar.getInstance(), Calendar.getInstance(), 0)
  val file2 =
      MyFile("name 2", java.util.Calendar.getInstance(), java.util.Calendar.getInstance(), 0)
  val file3 =
      MyFile("name 3", java.util.Calendar.getInstance(), java.util.Calendar.getInstance(), 0)

  @Before
  fun setUp() {
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
            TimeTable())
    folder2 = Folder("uid", emptyList<MyFile>().toMutableList(), "folder2", "2", TimeTable())

    folderRepository = MockFolderRepository(folder)
    currentUser = mock(FirebaseUser::class.java)
    `when`(currentUser.uid).thenReturn("uid")
    folderViewModel = FolderViewModel(folderRepository, currentUser)
  }

  @Test
  fun addFolderTest() {
    folderViewModel.addFolder(folder2)
    assertEquals(folderViewModel.existingFolders.value.size, 2)
    assertSame(folderViewModel.existingFolders.value[0], folder)
    assertSame(folderViewModel.existingFolders.value[1], folder2)
  }

  @Test
  fun removeFolderTest() {
    folderViewModel.addFolder(folder2)
    folderViewModel.selectFolder(folder)

    folderViewModel.deleteFolder(folder)
    assertEquals(folderViewModel.existingFolders.value.size, 1)
    assertSame(folderViewModel.existingFolders.value[0], folder2)
    assertNull(folderViewModel.activeFolder.value)
  }

  @Test
  fun updateFolderTest() {
    folderViewModel.selectFolder(folder)
    val folder3 = Folder("uid", emptyList<MyFile>().toMutableList(), "folder3", "1", TimeTable())
    folderViewModel.updateFolder(folder3)
    assertSame(folderViewModel.activeFolder.value, folder3)
    assertEquals(folderViewModel.existingFolders.value.size, 1)
    assertSame(folderViewModel.existingFolders.value[0], folder3)

    val folder4 =
        Folder(
            "uid",
            emptyList<MyFile>().toMutableList(),
            "folder4",
            folderViewModel.getNewUid(),
            TimeTable())
    assertEquals(folder4.id, "id test")
    folderViewModel.updateFolder(folder4)
    assertEquals(folderViewModel.existingFolders.value.size, 2)
    assertSame(folderViewModel.existingFolders.value[1], folder4)
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
    val file4 = MyFile("name 4", Calendar.getInstance(), Calendar.getInstance(), 0)
    folderViewModel.selectFolder(folder)
    folderViewModel.addFile(file4)
    assertEquals(folder.files.count { it == file4 }, 1)
  }
}

class MockFolderRepository(private val folder: Folder) : FolderRepository {

  override fun getFolders(onSuccess: (List<Folder>) -> Unit, onFailure: (Exception) -> Unit) {
    onSuccess(
        List(1) {
          return@List folder
        })
  }

  override fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {}

  override fun updateFolder(
      folder: Folder,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {}

  override fun deleteFolder(
      folder: Folder,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {}

  override fun getNewUid(): String {
    return "id test"
  }
}
