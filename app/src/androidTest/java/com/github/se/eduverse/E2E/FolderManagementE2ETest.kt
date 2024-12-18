package com.github.se.eduverse.E2E

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.se.eduverse.model.CommonWidgetType
import com.github.se.eduverse.model.FilterTypes
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.ui.dashboard.DashboardScreen
import com.github.se.eduverse.ui.folder.CreateFileScreen
import com.github.se.eduverse.ui.folder.CreateFolderScreen
import com.github.se.eduverse.ui.folder.FolderScreen
import com.github.se.eduverse.ui.folder.ListFoldersScreen
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopLevelDestination
import com.github.se.eduverse.viewmodel.FileViewModel
import com.github.se.eduverse.viewmodel.FolderViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.StorageReference
import io.mockk.mockk
import io.mockk.unmockkAll
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FolderManagementE2ETest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var dashboardViewModel: FakeDashboardViewModel
    private lateinit var folderViewModel: FakeFolderViewModel
    private lateinit var fileViewModel: FakeFileViewModel
    private lateinit var navigationActions: FakeFolderNavigationActions

    @Before
    fun setup() {
        MockFirebaseAuth.setup()
        dashboardViewModel = FakeDashboardViewModel()
        val fakeFileRepository = FakeFileRepository()
        val fakeFolderRepository = FakeFolderRepository()
        fileViewModel = FakeFileViewModel(fakeFileRepository)
        folderViewModel =
            MockFirebaseAuth.mockAuth?.let { FakeFolderViewModel(fakeFolderRepository, it) }!!
        navigationActions = FakeFolderNavigationActions()

        composeTestRule.setContent {
            TestFolderNavigation(
                dashboardViewModel = dashboardViewModel,
                folderViewModel = folderViewModel,
                fileViewModel = fileViewModel,
                navigationActions = navigationActions
            )
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testFolderManagementFlow() {
        composeTestRule.apply {
            // 1. Start from Dashboard and navigate to Folders
            onNodeWithTag("empty_dashboard_message").assertIsDisplayed()
            onNodeWithTag("empty_state_add_button").assertIsDisplayed()

            // Add Folder Widget and wait for it to appear
            onNodeWithTag("empty_state_add_button").performClick()
            onNodeWithText(CommonWidgetType.FOLDERS.title).performClick()
            waitForIdle()

            // Navigate to Folders Screen
            onNodeWithText(CommonWidgetType.FOLDERS.title).performClick()
            navigationActions.navigateToListFolders()
            waitForIdle()

            // 2. Create a new folder
            onNodeWithTag("createFolder").performClick()
            navigationActions.navigateToCreateFolder()
            waitForIdle()

            // Fill folder details and save
            onNodeWithTag("courseNameField").performTextInput("Mathematics")
            waitForIdle()

            // Debug prints before save
            println("DEBUG: Current screen before save: ${navigationActions.currentRoute()}")
            println("DEBUG: Folders before save: ${folderViewModel.folders.value}")

            // Save the folder
            onNodeWithTag("folderSave").performClick()
            waitForIdle()

            // Add a small additional wait to ensure state propagation
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.mainClock.advanceTimeBy(100)
            composeTestRule.mainClock.autoAdvance = true

            // Debug prints after save
            println("DEBUG: Current screen after save: ${navigationActions.currentRoute()}")
            println("DEBUG: Folders after save: ${folderViewModel.folders.value}")
            println("DEBUG: Folder IDs: ${folderViewModel.folders.value.map { it.id }}")

            // Print all semantics for debugging
            onRoot().printToLog("FOLDER_TEST")

            // Get the actual folder ID from the ViewModel
            val folderId = folderViewModel.folders.value.firstOrNull()?.id
            println("DEBUG: Looking for folder with ID: $folderId")

            // After clicking folder and before going to file creation
            if (folderId != null) {
                onNodeWithTag("folderCard$folderId").assertExists()
                onNodeWithTag("folderCard$folderId").performClick()
                // Important: Select the folder in view model
                folderViewModel.selectFolder(folderViewModel.folders.value.first())
            } else {
                throw AssertionError("No folder was created")
            }

            navigationActions.navigateToFolder()
            waitForIdle()

            // 4. Add a file to the folder
            println("DEBUG: Before create file - Screen: ${navigationActions.currentRoute()}")
            println("DEBUG: Active folder: ${folderViewModel.activeFolder.value?.id}")
            onNodeWithTag("createFile").assertExists()
            onNodeWithTag("createFile").performClick()
            navigationActions.navigateToCreateFile()
            waitForIdle()

            // Add extra wait for navigation
            mainClock.autoAdvance = false
            mainClock.advanceTimeBy(100)
            mainClock.autoAdvance = true
            waitForIdle()

            // Fill file details and save
            onNodeWithTag("fileNameField").assertExists()
            onNodeWithTag("fileNameField").performTextInput("Calculus Notes")
            fileViewModel.createFakeFile("Calculus Notes")
            onNodeWithTag("fileSave").performClick()
            waitForIdle()

// Return to folder screen
            navigationActions.returnToFolderScreen()
            waitForIdle()

// Add wait for navigation and state updates
            mainClock.autoAdvance = false
            mainClock.advanceTimeBy(100)
            mainClock.autoAdvance = true
            waitForIdle()

// Debug UI tree to see actual tags
            println("DEBUG: Current screen: ${navigationActions.currentRoute()}")
            println("DEBUG: Active folder: ${folderViewModel.activeFolder.value?.id}")
            println("DEBUG: Active folder files: ${folderViewModel.activeFolder.value?.files}")
            onRoot().printToLog("BEFORE_SORT")

// Now handling sorting
            onNodeWithTag("sortingButton").assertExists()
            onNodeWithTag("sortingButton").performClick()
            waitForIdle()

// After sorting
            onNodeWithText("Alphabetic").performClick()
            waitForIdle()

// Debug UI tree after sort
            onRoot().printToLog("AFTER_SORT")

// Print out any nodes with "file" in their tag
            onAllNodesWithTag("file", useUnmergedTree = true).printToLog("FILE_NODES")

// Try to find the file card with different tag combinations

            // Debug current state
            println("DEBUG: Looking for file nodes...")
            onAllNodesWithText("Calculus Notes").printToLog("CALCULUS_NOTES_NODES")

            // Try to find the specific file card
            onNodeWithTag("editButton").assertExists()  // Using correct tag name
            onNodeWithTag("editButton").performClick()

// Click delete in the menu
            onNodeWithTag("delete").assertExists().performClick()

// Rest of deletion flow
            onNodeWithTag("confirm").performClick()
            waitForIdle()

            // 7. Go back to folders list
            onNodeWithTag("goBackButton").performClick()
            waitForIdle()

            // 8. Delete the folder
            onNodeWithTag("folderCard$folderId").performTouchInput { longClick() }
            onNodeWithTag("delete").performClick()
            onNodeWithTag("confirm").performClick()
            waitForIdle()

            // 9. Return to dashboard
            onNodeWithTag("goBackButton").performClick()
            waitForIdle()
        }
    }

}

class FakeFileRepository : FileRepository {
    private val files = mutableMapOf<String, Uri>()
    private var lastFileId = 0

    override fun getNewUid(): String {
        lastFileId++
        return "file_$lastFileId"
    }

    override fun savePdfFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        files[fileId] = file
        onSuccess()
    }

    override fun modifiyFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        files[fileId] = file
        onSuccess()
    }

    override fun deleteFile(fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        files.remove(fileId)
        onSuccess()
    }

    override fun accessFile(
        fileId: String,
        onSuccess: (StorageReference, String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun savePathToFirestore(path: String, suffix: String, fileId: String, onSuccess: () -> Unit) {
        onSuccess()
    }
}

class FakeFolderRepository : FolderRepository {
    private val folders = mutableMapOf<String, Folder>()
    private var lastFolderId = 0

    override fun getFolders(userId: String, archived: Boolean, onSuccess: (List<Folder>) -> Unit, onFailure: (Exception) -> Unit) {
        onSuccess(folders.values.filter { it.ownerID == userId && it.archived == archived }.toList())
    }

    override fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        folders[folder.id] = folder
        onSuccess()
    }

    override fun updateFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        folders[folder.id] = folder
        onSuccess()
    }

    override suspend fun deleteFolders(folders: List<Folder>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        folders.forEach { this.folders.remove(it.id) }
        onSuccess()
    }

    override fun getNewUid(): String {
        lastFolderId++
        return "folder_$lastFolderId"
    }
}

class FakeFileViewModel(fileRepository: FileRepository) : FileViewModel(fileRepository) {
    private val _validNewFile = MutableStateFlow(false)
    override val validNewFile: StateFlow<Boolean> = _validNewFile
    private var newFile: MyFile? = null
    private var lastCreatedFile: MyFile? = null  // Add this to store the last created file

    fun createFakeFile(name: String) {
        newFile = MyFile(
            id = "file_1",
            fileId = "file_1",
            name = name,
            creationTime = Calendar.getInstance(),
            lastAccess = Calendar.getInstance(),
            numberAccess = 0
        )
        lastCreatedFile = newFile  // Store the file
        _validNewFile.value = true
    }

    override fun getNewFile(): MyFile? {
        val file = newFile
        newFile = null
        _validNewFile.value = false
        return file
    }

    // Add this helper method
    fun getLastCreatedFile(): MyFile? {
        return lastCreatedFile
    }
}

class FakeFolderViewModel(private val folderRepository: FolderRepository, auth: FirebaseAuth)
    : FolderViewModel(folderRepository, auth) {

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    override val folders: StateFlow<List<Folder>> = _folders

    private val _activeFolder = MutableStateFlow<Folder?>(null)
    override val activeFolder: StateFlow<Folder?> = _activeFolder

    // Add this to track folders in memory
    private val foldersList = mutableListOf<Folder>()

    override fun getUserFolders() {
        println("DEBUG: Getting user folders")
        // Instead of querying repository, return our in-memory list
        _folders.value = foldersList.toList()
        println("DEBUG: Retrieved folders: ${_folders.value.map { it.id }}")
    }

    override fun addFolder(folder: Folder) {
        println("DEBUG: Adding folder directly: ${folder.id}")
        folderRepository.addFolder(
            folder,
            {
                println("DEBUG: Successfully added folder to repository")
                foldersList.add(folder) // Add to our in-memory list
                _folders.value = foldersList.toList()
                println("DEBUG: Updated folders state: ${_folders.value.map { it.id }}")
            },
            { error ->
                println("DEBUG: Failed to add folder: $error")
            }
        )
    }

    override fun createNewFolderFromName(
        folderName: String,
        onSuccess: (Folder) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val folderId = folderRepository.getNewUid()
        println("DEBUG: Creating folder with ID: $folderId")

        val newFolder = Folder(
            ownerID = "test_user",
            files = mutableListOf(),
            name = folderName,
            id = folderId,
            archived = false
        )

        foldersList.add(newFolder) // Add to in-memory list first
        _folders.value = foldersList.toList()
        println("DEBUG: Updated folders state immediately: ${_folders.value.map { it.id }}")

        folderRepository.addFolder(
            newFolder,
            {
                println("DEBUG: Added folder to repository")
                onSuccess(newFolder)
            },
            { error ->
                println("DEBUG: Failed to add folder: $error")
                foldersList.remove(newFolder) // Remove on failure
                _folders.value = foldersList.toList()
                onFailure(error.toString())
            }
        )
    }
    override fun selectFolder(folder: Folder?) {
        println("DEBUG: Selecting folder: ${folder?.id}")
        _activeFolder.value = folder
    }


    override fun updateFolder(folder: Folder) {
        println("DEBUG: Updating folder ${folder.id} with ${folder.files.size} files")
        folderRepository.updateFolder(
            folder,
            {
                val updatedFolders = foldersList.map {
                    if (it.id == folder.id) folder else it
                }
                foldersList.clear()
                foldersList.addAll(updatedFolders)
                _folders.value = foldersList.toList()
                // Ensure active folder is updated too
                if (_activeFolder.value?.id == folder.id) {
                    _activeFolder.value = folder
                }
                println("DEBUG: Folder update successful")
            },
            { error -> println("DEBUG: Failed to update folder: $error") }
        )
    }


}

class FakeFolderNavigationActions : NavigationActions(mockk(relaxed = true)) {
    private var _currentRoute = mutableStateOf("DASHBOARD")

    fun navigateToListFolders() {
        _currentRoute.value = "LIST_FOLDERS"
    }

    fun navigateToCreateFolder() {
        _currentRoute.value = "CREATE_FOLDER"
    }

    fun navigateToFolder() {
        _currentRoute.value = "FOLDER"
    }



    override fun navigateTo(destination: TopLevelDestination) {
        _currentRoute.value = destination.route
    }


    override fun currentRoute(): String = _currentRoute.value

    fun navigateToCreateFile() {
        println("DEBUG: Navigating to CREATE_FILE screen")
        _currentRoute.value = "CREATE_FILE"
    }

    override fun navigateTo(route: String) {
        println("DEBUG: Navigating to route: $route")
        _currentRoute.value = route
    }

    fun returnToFolderScreen() {
        println("DEBUG: Returning to FOLDER screen from file creation")
        _currentRoute.value = "FOLDER"
    }

    override fun goBack() {
        println("DEBUG: Going back from: ${_currentRoute.value}")
        val newRoute = when (_currentRoute.value) {
            "CREATE_FILE" -> "FOLDER"  // When going back from file creation, return to folder view
            "FOLDER" -> "LIST_FOLDERS" // Only go to list when explicitly navigating back from folder view
            "CREATE_FOLDER" -> "LIST_FOLDERS"
            "LIST_FOLDERS" -> "DASHBOARD"
            else -> "LIST_FOLDERS"
        }
        println("DEBUG: Navigating back to: $newRoute")
        _currentRoute.value = newRoute
    }
}

@Composable
fun TestFolderNavigation(
    dashboardViewModel: FakeDashboardViewModel,
    folderViewModel: FakeFolderViewModel,
    fileViewModel: FakeFileViewModel,
    navigationActions: FakeFolderNavigationActions
) {
    var currentScreen by remember { mutableStateOf("DASHBOARD") }

    LaunchedEffect(navigationActions.currentRoute()) {
        currentScreen = navigationActions.currentRoute()
    }

    when (currentScreen) {
        "LIST_FOLDERS" -> ListFoldersScreen(
            navigationActions = navigationActions,
            folderViewModel = folderViewModel
        )
        "CREATE_FOLDER" -> CreateFolderScreen(
            navigationActions = navigationActions,
            folderViewModel = folderViewModel,
            fileViewModel = fileViewModel
        )
        "FOLDER" -> FolderScreen(
            navigationActions = navigationActions,
            folderViewModel = folderViewModel,
            fileViewModel = fileViewModel
        )
        "CREATE_FILE" -> CreateFileScreen(
            navigationActions = navigationActions,
            fileViewModel = fileViewModel
        )
        else -> DashboardScreen(
            viewModel = dashboardViewModel,
            navigationActions = navigationActions
        )
    }
}