package com.github.se.eduverse.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.showToast
import com.github.se.eduverse.ui.converter.PdfConverterOption
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PdfConverterViewModel : ViewModel() {

  val TEXT_PAGE_TOP_PADDING = 40f
  val TEXT_PAGE_LEFT_PADDING = 20f
  val A4_PAGE_WIDTH = 595
  val A4_PAGE_HEIGHT = 842
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

  private var currentFile: File? = null

  private var pdfGenerationJob: Job? = null

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
        viewModelScope.launch(Dispatchers.IO) {
          try {
            val pdfFile: PdfDocument =
                when (converterOption) {
                  PdfConverterOption.IMAGE_TO_PDF -> convertImageToPdf(uri, context)
                  PdfConverterOption.TEXT_TO_PDF -> convertTextToPdf(uri, context)
                  PdfConverterOption.DOCUMENT_TO_PDF -> TODO()
                  PdfConverterOption.SUMMARIZE_FILE -> TODO()
                  PdfConverterOption.EXTRACT_TEXT -> TODO()
                  PdfConverterOption.NONE ->
                      throw Exception("No converter option selected") // Should never happen
                }
            _pdfGenerationState.value = PdfGenerationState.Success(savePdf(pdfFile))
          } catch (e: Exception) {
            Log.e("generatePdf", "Failed to generate pdf", e)
            _pdfGenerationState.value = PdfGenerationState.Error
          }
        }
  }

  /**
   * Convert the image corresponding to the given URI to a PDF
   *
   * @param imageUri The URI of the image to convert
   * @param context The context of the application
   * @return The PDF document created from the image
   * @throws Exception If an error occurs during the conversion process
   */
  private fun convertImageToPdf(imageUri: Uri?, context: Context): PdfDocument {
    try {
      // Read the image bitmap from the URI
      val imageBitmap =
          context.contentResolver.openInputStream(imageUri!!).use { inputStream ->
            if (inputStream == null) {
              throw Exception("Failed to open image file")
            }
            BitmapFactory.decodeStream(inputStream)
          }

      // Create a new PDF document and add the image to it
      val pdfDocument = PdfDocument()
      val pageInfo =
          PdfDocument.PageInfo.Builder(imageBitmap.width, imageBitmap.height, 1)
              .create() // The pdf page will have the same size as the input image
      val page = pdfDocument.startPage(pageInfo)
      val canvas = page.canvas
      canvas.drawBitmap(
          imageBitmap,
          0f,
          0f,
          null) // Draw the image on the canvas starting from the top-left corner
      pdfDocument.finishPage(page)
      return pdfDocument
    } catch (e: Exception) {
      // If an exception occurs during the conversion process, log the error and display a toast
      Log.e("convertImageToPdf", "Image conversion failed", e)
      throw e
    }
  }

  /**
   * Convert the text file corresponding to the given URI to a PDF document
   *
   * @param fileUri The URI of the text file to convert
   * @param context The context of the application
   * @return The PDF document created from the text file
   * @throws Exception If an error occurs during the conversion process
   */
  private fun convertTextToPdf(fileUri: Uri?, context: Context): PdfDocument {
    try {
      // Open the text file input stream and create a buffered reader on it
      val inputStream =
          context.contentResolver.openInputStream(fileUri!!)
              ?: run { throw Exception("Failed to open text file") }
      val bufferedReader = inputStream.bufferedReader()

      // Create a new PDF document
      val pdfDocument = PdfDocument()
      val pageInfo =
          PdfDocument.PageInfo.Builder(A4_PAGE_WIDTH, A4_PAGE_HEIGHT, 1).create() // A4 page size
      var page = pdfDocument.startPage(pageInfo)
      var canvas = page.canvas
      val paint =
          Paint().apply { // Set the paint properties for the text to be written on the pdf document
            isAntiAlias = true
            textSize = 12f
            color = Color.BLACK
          }
      var yPosition =
          TEXT_PAGE_TOP_PADDING // Start writing the text from 40px from the top of the page

      // Read the text from the file in buffered way to avoid memory issues
      bufferedReader.use { reader ->
        var line: String?
        while (reader.readLine().also { line = it } != null) {
          // Split the line into words to avoid overflowing the page width in case of long lines
          val words = line!!.split(" ")

          // Draw the read line word by word into the pdf document
          var currentLine = ""
          for (word in words) {
            // Check if the line with the next word fits in the pdf page width
            val tempLine =
                if (currentLine.isEmpty()) word
                else "$currentLine $word" // Add the next word to the current line after a space,
            // for the first line of the doc no space needed before
            // the word
            val textWidth = paint.measureText(tempLine)
            if (textWidth <= pageInfo.pageWidth - 40) { // 20px padding on each side of the page
              currentLine = tempLine
            } else {
              // Draw the current line and start a new line with the current word
              canvas.drawText(currentLine, TEXT_PAGE_LEFT_PADDING, yPosition, paint)
              yPosition += paint.descent() - paint.ascent()
              currentLine = word
            }

            // If the current page is full, create a new page and add it to the pdf document
            if (yPosition >
                pageInfo.pageHeight - 80) { // 40px padding on top and bottom of the page
              pdfDocument.finishPage(page)
              yPosition = TEXT_PAGE_TOP_PADDING
              val newPageInfo =
                  PdfDocument.PageInfo.Builder(
                          A4_PAGE_WIDTH, A4_PAGE_HEIGHT, pdfDocument.pages.size + 1)
                      .create()
              page = pdfDocument.startPage(newPageInfo)
              canvas = page.canvas
            }
          }

          // Draw the last word of the read line if it didn't fit in the page's previous line
          if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, TEXT_PAGE_LEFT_PADDING, yPosition, paint)
            yPosition += paint.descent() - paint.ascent()
          }

          // If the last word of the read line was written on the last line of the page, create a
          // new page
          if (yPosition > pageInfo.pageHeight - 40) {
            pdfDocument.finishPage(page)
            yPosition = TEXT_PAGE_TOP_PADDING
            val newPageInfo =
                PdfDocument.PageInfo.Builder(
                        A4_PAGE_WIDTH, A4_PAGE_HEIGHT, pdfDocument.pages.size + 1)
                    .create()
            page = pdfDocument.startPage(newPageInfo)
            canvas = page.canvas
          }
        }
      }

      // Finish the last page and write the pdf document to the destination file, display a toast
      // if document creation is successful
      pdfDocument.finishPage(page)

      return pdfDocument
    } catch (e: Exception) {
      // If an exception occurs during the conversion process, log the error and throw the exception
      Log.e("convertTextToPdf", "Text conversion failed", e)
      throw e
    }
  }

  // Not implemented yet
  private fun convertWordToPdf(fileUri: Uri, context: Context) {
    viewModelScope.launch { context.showToast("Not implemented yet") }
  }

  /**
   * Save the given PDF document to the documents directory of the device
   *
   * @param pdfDocument The PDF document to save
   * @return The file where the PDF document was saved
   * @throws Exception If an error occurs during the saving process
   */
  private fun savePdf(pdfDocument: PdfDocument): File {
    try {
      val file = createUniqueFile(DEFAULT_DESTINATION_DIRECTORY, "${newFileName.value}.pdf")
      FileOutputStream(file).use { outputStream -> pdfDocument.writeTo(outputStream) }
      pdfDocument.close()
      currentFile = file
      return file
    } catch (e: Exception) {
      pdfDocument
          .close() // Close the pdf document in case of an exception when writing to the output file
      Log.e("savePdf", "Failed to save pdf to documents directory", e)
      throw e
    }
  }

  /** Set the PDF generation state to ready and reset the current file */
  fun setPdfGenerationStateToReady() {
    _pdfGenerationState.value = PdfGenerationState.Ready
    currentFile = null
  }

  /** Abort the PDF generation process if it's in progress */
  fun abortPdfGeneration() {
    if (pdfGenerationJob != null) {
      if (pdfGenerationJob!!.isActive) {
        pdfGenerationJob!!.cancel()
        // In case the job is cancelled before the job is completed but after the file has been
        // saved to the device set the generation state to success
        if (currentFile != null) {
          _pdfGenerationState.value = PdfGenerationState.Success(currentFile!!)
        } else {
          _pdfGenerationState.value = PdfGenerationState.Aborted
        }
      }
    }
  }

  /**
   * Helper function to create a unique file in the specified directory with the given file name,if
   * the file already exists it will append a number to the file name to make it unique (like it's
   * done by the mac os file system)
   *
   * @param directory The directory where to create the file
   * @param fileName The name of the file to create
   * @return The created file
   */
  private fun createUniqueFile(directory: File, fileName: String): File {
    var file = File(directory, fileName)
    var fileIndex = 1
    val baseName = fileName.substringBeforeLast(".")
    val extension = fileName.substringAfterLast(".", "")

    while (file.exists()) {
      val newFileName = "$baseName($fileIndex).$extension"
      file = File(directory, newFileName)
      fileIndex++
    }
    return file
  }
}
