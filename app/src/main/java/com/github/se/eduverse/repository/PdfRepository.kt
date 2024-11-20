package com.github.se.eduverse.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import com.github.se.eduverse.showToast
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import java.io.BufferedReader
import java.io.File

interface PdfRepository {
  fun savePdfToDevice(
      pdfFile: File,
      pdfFileName: String,
      destinationDirectory: File,
      onSuccess: (File) -> Unit,
      onFailure: (e: Exception) -> Unit
  )

  fun convertImageToPdf(imageUri: Uri?, context: Context): PdfDocument

  fun convertTextToPdf(fileUri: Uri?, context: Context): PdfDocument

  fun writePdfDocumentToTempFile(pdf: PdfDocument, pdfFileName: String): File

  fun deleteTempPdfFile(pdfFile: File)

  fun createUniqueFile(directory: File, fileName: String): File

  fun readTextFromPdfFile(pdfUri: Uri?, context: Context, limit: Int = 0): String

  fun writeTextToPdf(text: String): PdfDocument
}

class PdfRepositoryImpl : PdfRepository {

  val TEXT_PAGE_TOP_PADDING = 40f
  val TEXT_PAGE_BOTTOM_PADDING = 40f
  val TEXT_PAGE_LEFT_PADDING = 20f
  val TEXT_PAGE_RIGHT_PADDING = 20f
  val TEXT_FONT_SIZE = 12f
  val A4_PAGE_WIDTH = 595
  val A4_PAGE_HEIGHT = 842

  /**
   * Convert the image corresponding to the given URI to a PDF
   *
   * @param imageUri The URI of the image to convert
   * @param context The context of the application
   * @return The PDF document created from the image
   * @throws Exception If an error occurs during the conversion process
   */
  override fun convertImageToPdf(imageUri: Uri?, context: Context): PdfDocument {
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
  override fun convertTextToPdf(fileUri: Uri?, context: Context): PdfDocument {
    try {
      // Open the text file input stream and create a buffered reader on it
      val inputStream =
          context.contentResolver.openInputStream(fileUri!!)
              ?: run { throw Exception("Failed to open text file") }
      val bufferedReader = inputStream.bufferedReader()
      return writeTextFileToPdf(bufferedReader)
    } catch (e: Exception) {
      // If an exception occurs during the conversion process, log the error and throw the exception
      Log.e("convertTextToPdf", "Text conversion failed", e)
      throw e
    }
  }

  /**
   * Save the given PDF document to the given directory of the device
   *
   * @param pdfFile The PDF file to save
   * @param destinationDirectory The directory where to save the PDF file
   * @param onSuccess The code to execute if the PDF file is saved successfully
   * @param onFailure The code to execute if the PDF file can't be saved
   */
  override fun savePdfToDevice(
      pdfFile: File,
      pdfFileName: String,
      destinationDirectory: File,
      onSuccess: (File) -> Unit,
      onFailure: (e: Exception) -> Unit
  ) {
    try {
      val file = createUniqueFile(destinationDirectory, pdfFileName + ".pdf")
      pdfFile.copyTo(file)
      deleteTempPdfFile(pdfFile)
      onSuccess(file)
    } catch (e: Exception) {
      deleteTempPdfFile(pdfFile)
      Log.e("savePdf", "Failed to save pdf to {${destinationDirectory.name}}", e)
      onFailure(e)
    }
  }

  /**
   * Write the given PdfDocument to a temporary file with the given file name
   *
   * @param pdf The PdfDocument to write to the file
   * @param pdfFileName The name of the file to create
   * @return The created temporary file
   * @throws Exception If an error occurs during the writing process
   */
  override fun writePdfDocumentToTempFile(pdf: PdfDocument, pdfFileName: String): File {
    val tempFile = File.createTempFile(pdfFileName, ".pdf")
    val outputStream = tempFile.outputStream()
    try {
      pdf.writeTo(outputStream)
      pdf.close()
      outputStream.close()
      return tempFile
    } catch (e: Exception) {
      pdf.close()
      outputStream.close()
      Log.e("writePdfDocumentToTempFile", "Failed to write PdfDocument to temp file", e)
      throw e
    }
  }

  /**
   * Delete the given temporary PDF file
   *
   * @param pdfFile The PDF file to delete
   */
  override fun deleteTempPdfFile(pdfFile: File) {
    try {
      pdfFile.delete()
    } catch (e: Exception) {
      Log.e("deleteTempPdfFile", "Failed to delete temp pdf file", e)
    }
  }

  /**
   * Function to create a unique file in the specified directory with the given file name,if the
   * file already exists it will append a number to the file name to make it unique (like it's done
   * by the mac os file system)
   *
   * @param directory The directory where to create the file
   * @param fileName The name of the file to create
   * @return The created file
   */
  override fun createUniqueFile(directory: File, fileName: String): File {
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

  /**
   * Read the text from the PDF file corresponding to the given URI
   *
   * @param pdfUri The URI of the PDF file to read
   * @param context The context of the application
   * @param limit The maximum number of characters to read from the PDF file
   * @return The text read from the PDF file
   * @throws Exception If an error occurs during the reading process
   */
  override fun readTextFromPdfFile(pdfUri: Uri?, context: Context, limit: Int): String {
    try {
      val inputStream =
          context.contentResolver.openInputStream(pdfUri!!)
              ?: run { throw Exception("Failed to open pdf file") }
      val pdfReader = PdfReader(inputStream)
      val pdfTextExtractor = PdfTextExtractor(pdfReader)
      var text = ""
      for (i in 1..pdfReader.numberOfPages) {
        val extractedText = pdfTextExtractor.getTextFromPage(i)
        if (extractedText.isNotEmpty()) {
          text += extractedText
        }
        if (limit > 0 && text.length > limit) {
          val error = "Pdf file contains too much text and exceeds tool's supported limit"
          context.showToast(error)
          throw Exception(error)
        }
      }
      return text
    } catch (e: Exception) {
      Log.e("readTextFromPdfFile", "Failed to read text from pdf file", e)
      throw e
    }
  }

  /**
   * Write the given text to a PDF document (the purpose of this function is to create a PDF file
   * directly from a string given as input, rather than reading the text from an existing file)
   *
   * @param text The text to write to the PDF document
   * @return The created PDF document
   * @throws Exception If an error occurs during the process
   */
  override fun writeTextToPdf(text: String): PdfDocument {
    try {
      val tempFile = createTempTextFile(text)
      val bufferedReader = tempFile.bufferedReader()
      val pdfDocument = writeTextFileToPdf(bufferedReader)
      tempFile.delete()
      return pdfDocument
    } catch (e: Exception) {
      Log.e("writeTextToPdf", "Failed to write text to pdf", e)
      throw e
    }
  }

  /**
   * Helper function to write the text read from the given buffered reader to a PDF document
   *
   * @param bufferedReader The buffered reader to read the text from
   * @return The created PDF document
   */
  private fun writeTextFileToPdf(bufferedReader: BufferedReader): PdfDocument {
    // Create a new PDF document
    val pdfDocument = PdfDocument()
    val pageInfo =
        PdfDocument.PageInfo.Builder(A4_PAGE_WIDTH, A4_PAGE_HEIGHT, 1).create() // A4 page size
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    val paint =
        Paint().apply { // Set the paint properties for the text to be written on the pdf document
          isAntiAlias = true
          textSize = TEXT_FONT_SIZE
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
          if (textWidth <=
              pageInfo.pageWidth -
                  (TEXT_PAGE_LEFT_PADDING +
                      TEXT_PAGE_RIGHT_PADDING)) { // 20px padding on each side of the page
            currentLine = tempLine
          } else {
            // Draw the current line and start a new line with the current word
            canvas.drawText(currentLine, TEXT_PAGE_LEFT_PADDING, yPosition, paint)
            yPosition += paint.descent() - paint.ascent()
            currentLine = word
          }

          // If the current page is full, create a new page and add it to the pdf document
          if (yPosition > pageInfo.pageHeight - TEXT_PAGE_BOTTOM_PADDING) {
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
        if (yPosition > pageInfo.pageHeight - TEXT_PAGE_BOTTOM_PADDING) {
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
  }

  /**
   * Heleper function to create a temporary text file with the given text written to it
   *
   * @param text The text to write to the temp file
   * @return The created temporary text file
   */
  private fun createTempTextFile(text: String): File {
    val tempFile = File.createTempFile("temp_text", ".txt")
    tempFile.writeText(text)
    return tempFile
  }
}
