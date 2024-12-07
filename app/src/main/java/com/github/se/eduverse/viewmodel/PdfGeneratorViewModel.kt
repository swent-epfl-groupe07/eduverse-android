package com.github.se.eduverse.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.repository.ConvertApiRepository
import com.github.se.eduverse.repository.OpenAiRepository
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.repository.PdfRepositoryImpl
import com.github.se.eduverse.showToast
import com.github.se.eduverse.ui.pdfGenerator.PdfGeneratorOption
import java.io.File
import java.util.concurrent.TimeUnit
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
    private val convertApiRepository: ConvertApiRepository
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

    data class Error(val message: String = "Failed to generate PDF file") : PdfGenerationState()
  }

  private val _newFileName = MutableStateFlow<String>("")
  val newFileName: StateFlow<String> = _newFileName.asStateFlow()

  private val _pdfGenerationState = MutableStateFlow<PdfGenerationState>(PdfGenerationState.Ready)
  val pdfGenerationState: StateFlow<PdfGenerationState> = _pdfGenerationState.asStateFlow()

  var currentFile: File? = null

  var pdfGenerationJob: Job? = null

  // create viewmodel factory
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // Use a custom OkHttpClient for the conversion process to prevent okhttp socket timeout
            // errors that prevent the conversion of large files to succeed even though the
            // conversion process is still running on the server (also the latter already has a
            // timeout of 1200 seconds so the client read timeout is set a little bit higher to
            // account for overhead)
            val conversionOkHttpClient =
                OkHttpClient.Builder()
                    .readTimeout(
                        1250,
                        TimeUnit
                            .SECONDS) // makes sure the socket doesn't timeout too early for large
                                      // files conversion
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
            return PdfGeneratorViewModel(
                PdfRepositoryImpl(),
                OpenAiRepository(OkHttpClient()),
                ConvertApiRepository(conversionOkHttpClient))
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
                currentFile =
                    pdfRepository.writePdfDocumentToTempFile(pdfFile, newFileName.value, context)
              }
              PdfGeneratorOption.TEXT_TO_PDF -> {
                val pdfFile = pdfRepository.convertTextToPdf(uri, context)
                currentFile =
                    pdfRepository.writePdfDocumentToTempFile(pdfFile, newFileName.value, context)
              }
              PdfGeneratorOption.DOCUMENT_TO_PDF -> {
                val file = pdfRepository.getTempFileFromUri(uri, context)
                val pdfFile =
                    withContext(Dispatchers.IO) {
                      convertApiRepository.convertToPdf(file, newFileName.value, context)
                    }
                currentFile = pdfFile
              }
              PdfGeneratorOption.SUMMARIZE_FILE -> {
                val text = pdfRepository.readTextFromPdfFile(uri, context, MAX_SUMMARY_INPUT_SIZE)
                getSummary(text, context)
              }
              PdfGeneratorOption.EXTRACT_TEXT -> {
                extractTextToPdf(uri, context)
              }
              PdfGeneratorOption.NONE ->
                  throw Exception("No converter option selected") // Should never happen
            }

            delay(3000) // Simulate a delay to show the progress indicator(for testing purposes)

            currentFile?.let { file ->
              _pdfGenerationState.value = PdfGenerationState.Success(file)
            } ?: { _pdfGenerationState.value = PdfGenerationState.Error() }
          } catch (e: Exception) {
            handleException(e)
          }
        }
  }

  /** Set the PDF generation state to ready and reset the current file */
  fun setPdfGenerationStateToReady() {
    _pdfGenerationState.value = PdfGenerationState.Ready
    if (currentFile != null) {
      pdfRepository.deleteTempPdfFile(currentFile!!)
      currentFile = null
    }
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
        { context.showToast("${it.name} saved to ${DEFAULT_DESTINATION_DIRECTORY.name}") },
        {
          Log.e("savePdfToDevice", "Failed to save pdf to device", it)
          context.showToast("Failed to save PDF to ${DEFAULT_DESTINATION_DIRECTORY.name}")
        })
  }

  /** Abort the PDF generation process if it's in progress */
  fun abortPdfGeneration() {
    if (pdfGenerationJob != null) {
      if (pdfGenerationJob!!.isActive) {
        pdfGenerationJob!!.cancel()
      }
      if (currentFile != null) {
        pdfRepository.deleteTempPdfFile(currentFile!!)
        currentFile = null
      }
      _pdfGenerationState.value = PdfGenerationState.Aborted
    }
  }

  /**
   * Helper function to handle the summarization process
   *
   * @param text The text to summarize
   */
  private fun getSummary(text: String, context: Context) {
    openAiRepository.summarizeText(
        text,
        onSuccess = { summary ->
          if (summary != null) {
            val pdfFile = pdfRepository.writeTextToPdf(summary, context)
            currentFile =
                pdfRepository.writePdfDocumentToTempFile(pdfFile, newFileName.value, context)
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
          val pdfFile = pdfRepository.writeTextToPdf(extractedText, context)
          currentFile =
              pdfRepository.writePdfDocumentToTempFile(pdfFile, newFileName.value, context)
        },
        onFailure = {
          Log.e("extractTextFromImage", "Failed to extract text from image", it)
          throw it
        })
  }

  private fun handleException(e: Exception) {
    Log.e("generatePdf", "Failed to generate pdf", e)
    _pdfGenerationState.value = PdfGenerationState.Error(e.message ?: "Failed to generate PDF")
  }
}
