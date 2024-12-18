package com.github.se.eduverse.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.github.se.eduverse.repository.ConvertApiRepository
import com.github.se.eduverse.repository.FileRepository
import com.github.se.eduverse.repository.FileRepositoryImpl
import com.github.se.eduverse.repository.FolderRepository
import com.github.se.eduverse.repository.FolderRepositoryImpl
import com.github.se.eduverse.repository.OpenAiRepository
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.repository.PdfRepositoryImpl
import com.github.se.eduverse.showToast
import com.github.se.eduverse.ui.pdfGenerator.PdfGeneratorOption
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class PdfGeneratorViewModel(
    private val pdfRepository: PdfRepository,
    private val openAiRepository: OpenAiRepository,
    private val convertApiRepository: ConvertApiRepository,
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

  val DEFAULT_DESTINATION_DIRECTORY =
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
  val MAX_SUMMARY_INPUT_SIZE =
      16000 // Limit set so that the max number of input tokens sent to openAI Api is not exceeded

  /** Sealed class representing the different states of the PDF generation process */
  sealed class PdfGenerationState {
    data object Ready : PdfGenerationState()

    data object InProgress : PdfGenerationState()

    data object Aborted : PdfGenerationState()

    data class Success(val pdfFile: File) : PdfGenerationState()

    data object Error : PdfGenerationState()
  }

  private val _newFileName = MutableStateFlow<String>("")
  val newFileName: StateFlow<String> = _newFileName.asStateFlow()

  private val _pdfGenerationState = MutableStateFlow<PdfGenerationState>(PdfGenerationState.Ready)
  val pdfGenerationState: StateFlow<PdfGenerationState> = _pdfGenerationState.asStateFlow()

  private val _transcriptionFile = MutableStateFlow<File?>(null)
  val transcriptionFile: StateFlow<File?> = _transcriptionFile.asStateFlow()

  var currentFile: File? = null

  var pdfGenerationJob: Job? = null

  // create viewmodel factory
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PdfGeneratorViewModel(
                PdfRepositoryImpl(),
                OpenAiRepository(OkHttpClient()),
                ConvertApiRepository(OkHttpClient()),
                FileRepositoryImpl(Firebase.firestore, Firebase.storage),
                FolderRepositoryImpl(Firebase.firestore))
                as T
          }
        }
  }

  /**
   * Set the new file name for the PDF to be created
   *
   * @param pdfFileName The new file name for the PDF
   */
  fun setNewFileName(pdfFileName: String) {
    // Sanitize the file name to prevent malicious file names
    val sanitizedFileName = pdfFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    _newFileName.value = sanitizedFileName
  }

  /**
   * Handle the generation of a PDF file from the given URI using the specified converter option
   *
   * @param uri The URI of the file to convert to PDF
   * @param context The context of the application
   * @param converterOption The option to use to convert the file to PDF
   */
  fun generatePdf(uri: Uri?, context: Context, converterOption: PdfGeneratorOption) {
    _pdfGenerationState.value = PdfGenerationState.InProgress
    pdfGenerationJob =
        viewModelScope.launch {
          try {
            when (converterOption) {
              PdfGeneratorOption.IMAGE_TO_PDF -> {
                val pdfFile = pdfRepository.convertImageToPdf(uri, context)
                currentFile = pdfRepository.writePdfDocumentToTempFile(pdfFile, newFileName.value)
                delay(3000) // Simulate a delay to show the progress indicator(for testing purposes)
              }
              PdfGeneratorOption.TEXT_TO_PDF,
              PdfGeneratorOption.TRANSCRIBE_SPEECH -> {
                val pdfFile = pdfRepository.convertTextToPdf(uri, context)
                currentFile = pdfRepository.writePdfDocumentToTempFile(pdfFile, newFileName.value)
              }
              PdfGeneratorOption.DOCUMENT_TO_PDF -> {
                val file = pdfRepository.getTempFileFromUri(uri, context)
                val pdfFile =
                    withContext(Dispatchers.IO) {
                      convertApiRepository.convertToPdf(file, newFileName.value)
                    }
                currentFile = pdfFile
              }
              PdfGeneratorOption.SUMMARIZE_FILE -> {
                val text = pdfRepository.readTextFromPdfFile(uri, context, MAX_SUMMARY_INPUT_SIZE)
                getSummary(text)
              }
              PdfGeneratorOption.EXTRACT_TEXT -> {
                extractTextToPdf(uri, context)
              }
              PdfGeneratorOption.NONE ->
                  throw Exception("No converter option selected") // Should never happen
            }
            currentFile?.let { file ->
              _pdfGenerationState.value = PdfGenerationState.Success(file)
            } ?: { _pdfGenerationState.value = PdfGenerationState.Error }
          } catch (e: Exception) {
            Log.e("generatePdf", "Failed to generate pdf", e)
            if (e is IllegalArgumentException) {
              context.showToast(e.message.toString())
            }
            _pdfGenerationState.value = PdfGenerationState.Error
          }
        }
  }

  /** Set the PDF generation state to ready and reset the current file */
  fun setPdfGenerationStateToReady() {
    resetTranscriptionFile()
    _pdfGenerationState.value = PdfGenerationState.Ready
  }

  /** Save the PDF file to the device */
  fun savePdfToDevice(
      pdfFile: File,
      context: Context,
      directory: File = DEFAULT_DESTINATION_DIRECTORY
  ) {
    pdfRepository.savePdfToDevice(
        pdfFile,
        newFileName.value,
        directory,
        {
          context.showToast(
              "${it.name} saved to device folder: ${DEFAULT_DESTINATION_DIRECTORY.name}")
        },
        {
          Log.e(
              "savePdfToDevice",
              "Failed to save pdf to device folder: ${DEFAULT_DESTINATION_DIRECTORY.name}",
              it)
          context.showToast(
              "Failed to save PDF to device folder: ${DEFAULT_DESTINATION_DIRECTORY.name}")
        })
  }

  /** Abort the PDF generation process if it's in progress */
  fun abortPdfGeneration() {
    if (pdfGenerationJob != null) {
      if (pdfGenerationJob!!.isActive) {
        pdfGenerationJob!!.cancel()
      }
      deleteGeneratedPdf()
      _pdfGenerationState.value = PdfGenerationState.Aborted
    }
  }

  /** Delete the generated PDF file */
  fun deleteGeneratedPdf() {
    if (currentFile != null) {
      pdfRepository.deleteTempPdfFile(currentFile!!)
      currentFile = null
    }
  }

  /**
   * Helper function to handle the summarization process
   *
   * @param text The text to summarize
   */
  private fun getSummary(text: String) {
    openAiRepository.summarizeText(
        text,
        onSuccess = { summary ->
          if (summary != null) {
            val pdfFile = pdfRepository.writeTextToPdf(summary)
            currentFile = pdfRepository.writePdfDocumentToTempFile(pdfFile, newFileName.value)
          } else throw Exception("Failed to generate summary")
        },
        onFailure = {
          Log.e("getSummary", "Failed to get summary from openAi api", it)
          throw it
        })
  }

  /**
   * Helper function to handle the text extraction process
   *
   * @param uri The URI of the image to extract text from
   * @param context The context of the application
   */
  private fun extractTextToPdf(uri: Uri?, context: Context) {
    pdfRepository.extractTextFromImage(
        uri,
        context,
        onSuccess = { extractedText ->
          val pdfFile = pdfRepository.writeTextToPdf(extractedText)
          currentFile = pdfRepository.writePdfDocumentToTempFile(pdfFile, newFileName.value)
        },
        onFailure = {
          Log.e("extractTextFromImage", "Failed to extract text from image", it)
          throw it
        })
  }

  /**
   * Save the PDF file to the selected app folder
   *
   * @param folder The folder to save the PDF file to
   * @param uri The uri of the PDF file to save
   * @param context The context of the application
   */
  fun savePdfToFolder(
      folder: Folder,
      uri: Uri,
      context: Context,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) {
    val fileId = fileRepository.getNewUid()
    fileRepository.savePdfFile(
        uri,
        fileId,
        {
          // Make sure that each file has a unique name in the folder
          var newName = "${newFileName.value}.pdf"
          var i = 1
          while (folder.files.any { it.name == newName }) {
            newName = "${newFileName.value}($i).pdf"
            i++
          }

          val newFile =
              MyFile(
                  id = "",
                  fileId = fileId,
                  name = newName,
                  creationTime = Calendar.getInstance(),
                  lastAccess = Calendar.getInstance(),
                  numberAccess = 0)

          folderRepository.updateFolder(
              folder.copy(files = (folder.files + newFile).toMutableList()),
              {
                context.showToast("$newName saved to app folder: ${folder.name}")
                onSuccess()
              },
              {
                context.showToast("Failed to add PDF to app folder: ${folder.name}")
                onFailure()
                Log.e("updateFolder", "Failed to to update folder in firestore", it)
                // Rollback the file upload if the folder update fails
                fileRepository.deleteFile(
                    fileId,
                    {},
                    { e ->
                      Log.e("savePdfToFolder", "Failed to delete pdf from firebase storage", e)
                    })
              })
        },
        {
          onFailure()
          context.showToast("Failed to upload PDF to cloud storage")
          Log.e("savePdfToFolder", "Failed to upload pdf to firebase storage", it)
        })
  }

  /**
   * Create a temporary file to store the transcribed text
   *
   * @param onFailure The callback to handle the case where the file creation fails
   */
  fun createTranscriptionFile(onSuccess: (File) -> Unit, onFailure: (String) -> Unit) {
    try {
      _transcriptionFile.value = File.createTempFile("transcription", ".txt")
      onSuccess(transcriptionFile.value!!)
    } catch (e: Exception) {
      Log.e("createTranscriptionFile", "Failed to create transcription file", e)
      onFailure(e.message.toString())
    }
  }

  /** Deletes the temporary transcription file */
  fun resetTranscriptionFile() {
    transcriptionFile.value?.delete()
    _transcriptionFile.value = null
  }
}
