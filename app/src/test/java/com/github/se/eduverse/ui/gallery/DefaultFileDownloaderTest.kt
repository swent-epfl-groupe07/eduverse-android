import android.content.Context
import com.github.se.eduverse.ui.gallery.DefaultFileDownloader
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFileDownloaderTest {

  private lateinit var context: Context
  private lateinit var firebaseStorage: FirebaseStorage
  private lateinit var storageReference: StorageReference
  private lateinit var fileDownloader: DefaultFileDownloader

  @Rule @JvmField val tempFolder = TemporaryFolder()

  @Before
  fun setup() {
    // Mock du contexte et du dossier de cache
    context = mock()
    whenever(context.cacheDir).thenReturn(tempFolder.root)

    // Mock de FirebaseStorage et StorageReference
    firebaseStorage = mock()
    storageReference = mock()

    // Injectez l'instance mockée de FirebaseStorage dans DefaultFileDownloader
    fileDownloader = DefaultFileDownloader(firebaseStorage)
  }

  /*@Test
  fun `ensureLocalFile downloads remote mp4`() = runTest {
      val remoteUrl = "http://example.com/video.mp4"

      // Mock de firebaseStorage.getReferenceFromUrl(remoteUrl) pour retourner storageReference
      whenever(firebaseStorage.getReferenceFromUrl(remoteUrl)).thenReturn(storageReference)

      // Mock de storageReference.getFile(any<File>()) pour retourner mockTask
      val mockTask = mock<FileDownloadTask>()

      whenever(storageReference.getFile(any<File>())).thenReturn(mockTask)

      // Mock d'un TaskSnapshot factice
      val mockSnapshot = mock<FileDownloadTask.TaskSnapshot>()

      // Configuration du mockTask pour simuler un téléchargement réussi
      whenever(mockTask.isComplete).thenReturn(true)
      whenever(mockTask.isSuccessful).thenReturn(true)
      whenever(mockTask.result).thenReturn(mockSnapshot)
      whenever(mockTask.exception).thenReturn(null)

      // Simulez l'appel de onSuccessListener
      doAnswer { invocation ->
          val listener = invocation.getArgument<OnSuccessListener<FileDownloadTask.TaskSnapshot>>(0)
          listener.onSuccess(mockSnapshot)
          mockTask
      }.whenever(mockTask).addOnSuccessListener(any())

      // Simulez l'appel de onFailureListener (non utilisé dans ce test)
      whenever(mockTask.addOnFailureListener(any())).thenReturn(mockTask)

      // Appel de la méthode à tester
      val result = fileDownloader.ensureLocalFile(context, remoteUrl)

      // Optionnel : Affichez le résultat pour déboguer
      println("Result is: $result")

      // Assertions
      assertNotNull("Le résultat ne doit pas être nul", result)
      assertTrue("Le chemin ne se termine pas par .mp4", result!!.endsWith(".mp4"))
  }*/

  /*@Test
  fun `ensureLocalFile downloads remote jpg and applies rotation`() = runTest {
      val remoteUrl = "http://example.com/file.jpg"

      // Mock de firebaseStorage.getReferenceFromUrl(remoteUrl) pour retourner storageReference
      whenever(firebaseStorage.getReferenceFromUrl(remoteUrl)).thenReturn(storageReference)

      // Mock de storageReference.getFile(any<File>()) pour retourner mockTask
      val mockTask = mock<FileDownloadTask>()

      whenever(storageReference.getFile(any<File>())).thenReturn(mockTask)

      // Mock d'un TaskSnapshot factice
      val mockSnapshot = mock<FileDownloadTask.TaskSnapshot>()

      // Configuration du mockTask pour simuler un téléchargement réussi
      whenever(mockTask.isComplete).thenReturn(true)
      whenever(mockTask.isSuccessful).thenReturn(true)
      whenever(mockTask.result).thenReturn(mockSnapshot)
      whenever(mockTask.exception).thenReturn(null)

      // Simulez l'appel de onSuccessListener
      doAnswer { invocation ->
          val listener = invocation.getArgument<OnSuccessListener<FileDownloadTask.TaskSnapshot>>(0)
          listener.onSuccess(mockSnapshot)
          mockTask
      }.whenever(mockTask).addOnSuccessListener(any())

      // Simulez l'appel de onFailureListener (non utilisé dans ce test)
      whenever(mockTask.addOnFailureListener(any())).thenReturn(mockTask)

      // Mock BitmapFactory.decodeFile
      val originalBitmap = mock<Bitmap>()
      whenever(BitmapFactory.decodeFile(any<String>())).thenReturn(originalBitmap)

      // Mock Bitmap.createBitmap
      val rotatedBitmap = mock<Bitmap>()
      whenever(Bitmap.createBitmap(
          any<Bitmap>(),
          anyInt(),
          anyInt(),
          anyInt(),
          anyInt(),
          any(),
          anyBoolean()
      )).thenReturn(rotatedBitmap)

      // Mock Bitmap.compress
      whenever(rotatedBitmap.compress(eq(Bitmap.CompressFormat.JPEG), eq(100), any())).thenReturn(true)

      // Appel de la méthode à tester
      val result = fileDownloader.ensureLocalFile(context, remoteUrl)

      // Optionnel : Affichez le résultat pour déboguer
      println("Result is: $result")

      // Assertions
      assertNotNull("Le résultat ne doit pas être nul", result)
      assertTrue("Le chemin ne se termine pas par .jpg", result!!.endsWith(".jpg"))
  }*/

  @Test
  fun `ensureLocalFile handles download failure`() = runTest {
    val remoteUrl = "http://example.com/video.mp4"

    // Mock de firebaseStorage.getReferenceFromUrl(remoteUrl) pour retourner storageReference
    whenever(firebaseStorage.getReferenceFromUrl(remoteUrl)).thenReturn(storageReference)

    // Mock de storageReference.getFile(any<File>()) pour lancer une exception
    whenever(storageReference.getFile(any<File>())).thenThrow(RuntimeException("Download failed"))

    // Appel de la méthode à tester
    val result = fileDownloader.ensureLocalFile(context, remoteUrl)

    // Assertions
    assertNull("Le résultat doit être nul en cas d'échec", result)
  }

  @Test
  fun `ensureLocalFile returns null when path is null or empty`() = runTest {
    val result = fileDownloader.ensureLocalFile(context, null)
    assertNull(result)

    val emptyResult = fileDownloader.ensureLocalFile(context, "")
    assertNull(emptyResult)
  }

  @Test
  fun `ensureLocalFile returns local file when path is local and file exists`() = runTest {
    val localPath = "/path/to/local/file.jpg"

    // Créez un vrai fichier dans le dossier temporaire
    val file = File(tempFolder.root, "file.jpg").apply { createNewFile() }
    val result = fileDownloader.ensureLocalFile(context, file.absolutePath)
    assertNotNull(result)
    assertEquals(file.absolutePath, result?.absolutePath)
  }

  @Test
  fun `ensureLocalFile returns null when path is local but file does not exist`() = runTest {
    val localPath = "/path/to/nonexistent/file.jpg"
    val result = fileDownloader.ensureLocalFile(context, localPath)
    assertNull(result)
  }
}
