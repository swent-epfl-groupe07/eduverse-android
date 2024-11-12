package com.github.se.eduverse.viewmodel

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.repository.PdfRepository
import com.github.se.eduverse.repository.PdfRepositoryImpl
import com.github.se.eduverse.showToast
import com.github.se.eduverse.ui.converter.PdfConverterOption
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PdfConverterViewModel(private val pdfRepository: PdfRepository) : ViewModel() {

  val DEFAULT_DESTINATION_DIRECTORY =
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

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

  var currentFile: File? = null

  var pdfGenerationJob: Job? = null

  // create viewmodel factory
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PdfConverterViewModel(PdfRepositoryImpl()) as T
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
  fun generatePdf(uri: Uri?, context: Context, converterOption: PdfConverterOption) {
    _pdfGenerationState.value = PdfGenerationState.InProgress
    pdfGenerationJob =
        viewModelScope.launch {
          try {
            val pdfFile: PdfDocument =
                when (converterOption) {
                  PdfConverterOption.IMAGE_TO_PDF -> pdfRepository.convertImageToPdf(uri, context)
                  PdfConverterOption.TEXT_TO_PDF -> pdfRepository.convertTextToPdf(uri, context)
                  PdfConverterOption.DOCUMENT_TO_PDF -> TODO()
                  PdfConverterOption.SUMMARIZE_FILE -> TODO()
                  PdfConverterOption.EXTRACT_TEXT -> TODO()
                  PdfConverterOption.NONE ->
                      throw Exception("No converter option selected") // Should never happen
                }
            // Simulate a delay to show the progress indicator(for testing purposes)
            delay(3000)
            currentFile = pdfRepository.writePdfDocumentToTempFile(pdfFile, newFileName.value)
            _pdfGenerationState.value = PdfGenerationState.Success(currentFile!!)
          } catch (e: Exception) {
            Log.e("generatePdf", "Failed to generate pdf", e)
            _pdfGenerationState.value = PdfGenerationState.Error
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
}
