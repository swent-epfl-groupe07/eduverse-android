package com.github.se.eduverse.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.eduverse.showToast
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class PdfConverterViewModel : ViewModel() {

  private val newFileName_ = MutableStateFlow<String>("")
  val newFileName: StateFlow<String> = newFileName_.asStateFlow()

  /**
   * Set the new file name for the PDF to be created
   *
   * @param pdfFileName The new file name for the PDF
   */
  fun setNewFileName(pdfFileName: String) {
    // Sanitize the file name to prevent malicious file names
    val sanitizedFileName = pdfFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    newFileName_.value = sanitizedFileName
  }

  /**
   * Convert the image corresponding to the given URI to a PDF
   *
   * @param imageUri The URI of the image to convert
   * @param context The context of the application
   */
  fun convertImageToPdf(imageUri: Uri?, context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        // Read the image bitmap from the URI and display a toast if the inputStream fails to open
        val imageBitmap =
            context.contentResolver.openInputStream(imageUri!!)?.use { inputStream ->
              BitmapFactory.decodeStream(inputStream)
            }
                ?: run {
                  withContext(Dispatchers.Main) {
                    Log.e("convertImageToPdf", "Failed to decode image")
                    context.showToast("Failed to open image")
                  }
                  return@launch
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

        // Create the PDF file, write the document to it, and generate a toast if the file creation
        // is successful
        val file =
            createUniqueFile(
                context.filesDir,
                "${newFileName_.value}.pdf") // For now, save the PDF in the app's files directory,
        // later will add option to give user choice of
        // location
        FileOutputStream(file).use { outputStream -> pdfDocument.writeTo(outputStream) }
        pdfDocument.close()
        withContext(Dispatchers.Main) { context.showToast("PDF created successfully") }
      } catch (e: Exception) {
        // If an exception occurs during the conversion process, log the error and display a toast
        Log.e("convertImageToPdf", "Image conversion failed", e)
        withContext(Dispatchers.Main) { context.showToast("Failed to create PDF") }
      }
    }
  }

  /**
   * Convert the document corresponding to the given URI to a PDF (for now only handles .docx and
   * .txt files, will add later other file types)
   *
   * @param fileUri The URI of the document to convert
   * @param context The context of the application
   */
  fun convertDocumentToPdf(fileUri: Uri?, context: Context) {
    val mimeType = context.contentResolver.getType(fileUri!!)
    when (mimeType) {
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
        convertWordToPdf(fileUri, context)
      }
      "text/plain" -> {
        convertTextToPdf(fileUri, context)
      }
      // Not needed because the file picker only allows the selection of text and word files, but
      // kept to be safe
      else -> {
        context.showToast("Unsupported file type")
      }
    }
  }

  /**
   * Convert the text file corresponding to the given URI to a PDF document
   *
   * @param fileUri The URI of the text file to convert
   * @param context The context of the application
   */
  fun convertTextToPdf(fileUri: Uri, context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        // Open the text file input stream and create a buffered reader on it, display a toast if
        // the file fails to open
        val inputStream = context.contentResolver.openInputStream(fileUri)
        val bufferedReader =
            inputStream?.bufferedReader()
                ?: run {
                  withContext(Dispatchers.Main) {
                    Log.e("convertTextToPdf", "Failed to open text file")
                    context.showToast("Failed to open text file")
                  }
                  return@launch
                }

        // Create a new PDF document
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 page size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint =
            Paint()
                .apply { // Set the paint properties for the text to be written on the pdf document
                  isAntiAlias = true
                  textSize = 12f
                  color = Color.BLACK
                }
        var yPosition = 40f // Start writing the text from 40px from the top of the page

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
                canvas.drawText(currentLine, 20f, yPosition, paint)
                yPosition += paint.descent() - paint.ascent()
                currentLine = word
              }

              // If the current page is full, create a new page and add it to the pdf document
              if (yPosition >
                  pageInfo.pageHeight - 80) { // 40px padding on top and bottom of the page
                pdfDocument.finishPage(page)
                yPosition = 40f
                val newPageInfo =
                    PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
              }
            }

            // Draw the last word of the read line if it didn't fit in the page's previous line
            if (currentLine.isNotEmpty()) {
              canvas.drawText(currentLine, 10f, yPosition, paint)
              yPosition += paint.descent() - paint.ascent()
            }

            // If the last word of the read line was written on the last line of the page, create a
            // new page
            if (yPosition > pageInfo.pageHeight - 40) {
              pdfDocument.finishPage(page)
              yPosition = 40f
              val newPageInfo =
                  PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
              page = pdfDocument.startPage(newPageInfo)
              canvas = page.canvas
            }
          }
        }

        // Finish the last page and write the pdf document to the destination file, display a toast
        // if document creation is successful
        pdfDocument.finishPage(page)
        val file = createUniqueFile(context.filesDir, "${newFileName_.value}.pdf")
        FileOutputStream(file).use { outputStream -> pdfDocument.writeTo(outputStream) }
        pdfDocument.close()
        withContext(Dispatchers.Main) { context.showToast("PDF created successfully") }
      } catch (e: Exception) {
        // If an exception occurs during the conversion process, log the error and display a toast
        Log.e("convertTextToPdf", "Text conversion failed", e)
        withContext(Dispatchers.Main) { context.showToast("Failed to create PDF") }
      }
    }
  }

  // Not implemented yet
  fun convertWordToPdf(fileUri: Uri, context: Context) {
    viewModelScope.launch { context.showToast("Not implemented yet") }
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
