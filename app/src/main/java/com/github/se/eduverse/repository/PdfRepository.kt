package com.github.se.eduverse.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

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

  fun writePdfDocumentToTempFile(pdf: PdfDocument, pdfFileName: String, context: Context): File?

  fun deleteTempPdfFile(pdfFile: File)

  fun createUniqueFile(directory: File, fileName: String): File

  fun readTextFromPdfFile(pdfUri: Uri?, context: Context, limit: Int = 0): String

  fun writeTextToPdf(text: String, context: Context): PdfDocument

  fun getTempFileFromUri(uri: Uri?, context: Context): File

  fun extractTextFromImage(
      imageUri: Uri?,
      context: Context,
      onSuccess: (String) -> Unit,
      onFailure: (Exception) -> Unit
  )
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
    // Read the image bitmap from the URI, throw an exception if getImageBitmap fails
    val imageBitmap = getImageBitmap(imageUri, context)

    // Create a new PDF document and add the image to it
    val pdfDocument = PdfDocument()

    try {
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
      // If an exception occurs during the conversion process, close the PdfDocument
      try {
        pdfDocument.close()
      } catch (closeException: Exception) {
        Log.e("writeTextFileToPdf", "Failed to close PdfDocument", closeException)
      }
      // If an exception occurs during the conversion process, log the error and display a toast
      Log.e("convertImageToPdf", "Image conversion failed for uri: $imageUri", e)
      throw Exception("Image to PDF conversion failed, please try again.")
    } finally {
      // Recycle the image bitmap to free up memory
      imageBitmap.recycle()
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
      // Open the text file input stream and create a buffered reader on it. Make sure the input
      // stream is correctly closed.
      val pdf =
          context.contentResolver.openInputStream(fileUri!!).use { inputStream ->
            if (inputStream != null) {
              val bufferedReader = inputStream.bufferedReader()
              writeTextFileToPdf(bufferedReader)
            } else {
              throw FileNotFoundException("Failed to open text file, opened input stream is null")
            }
          }
      return pdf
    } catch (e: Exception) {
      // If an exception occurs during the conversion process, log the error and throw the exception
      Log.e("convertTextToPdf", "Text conversion failed for uri: $fileUri", e)
      if (e is FileNotFoundException) {
        throw Exception("Failed to open the selected file, please try with another file.")
      } else {
        throw Exception("Text to PDF conversion failed, please try again.")
      }
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
      val file = createUniqueFile(destinationDirectory, "$pdfFileName.pdf")
      pdfFile.copyTo(file)
      deleteTempPdfFile(pdfFile)
      onSuccess(file)
    } catch (e: Exception) {
      deleteTempPdfFile(pdfFile)
      Log.e("savePdf", "Failed to save pdf to folder: ${destinationDirectory.name}", e)
      onFailure(e)
    }
  }

  /**
   * Write the given PdfDocument to a temporary file with the given file name
   *
   * @param pdf The PdfDocument to write to the file
   * @param pdfFileName The name of the file to create
   * @param context The context of the application
   * @return The created temporary file
   * @throws Exception If an error occurs during the writing process
   */
  override fun writePdfDocumentToTempFile(
      pdf: PdfDocument,
      pdfFileName: String,
      context: Context
  ): File? {
    var tempFile: File? = null

    try {
      // Create a temporary file with the given file name, in the external cache directory of the
      // app since it has more space (and can support large files) than the internal cache directory
      // (used by default by createTempFile in the previous implementation)
      tempFile = File.createTempFile(pdfFileName, ".pdf", context.externalCacheDir)

      // Write the PDF document to the file and ensure outputStream is always closed
      tempFile.outputStream().use { outputStream -> pdf.writeTo(outputStream) }

      return tempFile
    } catch (e: Exception) {
      // If an error occurs, delete the temp file if it was created
      tempFile?.delete()

      Log.e(
          "writePdfDocumentToTempFile",
          "Failed to write PdfDocument with name $pdfFileName to temp file",
          e)

      return null // Return null to indicate that the temp file creation failed
    } finally {
      // Ensure the PdfDocument is always closed and make sure that if an exception occurs while
      // closing the pdf document doesn't result in throwing an exception
      try {
        pdf.close()
      } catch (closeException: Exception) {
        Log.e("writePdfDocumentToTempFile", "Failed to close PdfDocument", closeException)
      }
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
      // Make sure the input stream is correctly
      context.contentResolver.openInputStream(pdfUri!!)?.use { inputStream ->
        // Make sure the pdf reader is correctly closed
        PdfReader(inputStream).use { pdfReader ->
          val pdfTextExtractor = PdfTextExtractor(pdfReader)
          var text = ""
          for (i in 1..pdfReader.numberOfPages) {
            val extractedText = pdfTextExtractor.getTextFromPage(i)
            if (extractedText.isNotEmpty()) {
              text += extractedText
            }
            if (limit > 0 && text.length > limit) {
              throw Exception("Limit exceeded")
            }
          }
          return text // Return the extracted text after successful reading
        }
      }
          ?: throw Exception(
              "Failed to open InputStream for the URI (openInputStream returned null).")
    } catch (e: Exception) {
      Log.e("readTextFromPdfFile", "Failed to read text from pdf file with uri: $pdfUri", e)
      if (e.message == "Limit exceeded") {
        throw Exception("The PDF file contains too much text (supported limit: $limit characters).")
      } else {
        throw Exception("Failed to read text from PDF file, please try with another file.")
      }
    }
  }

  /**
   * Write the given text to a PDF document (the purpose of this function is to create a PDF file
   * directly from a string given as input, rather than reading the text from an existing file)
   *
   * @param text The text to write to the PDF document
   * @param context the context of the app
   * @return The created PDF document
   * @throws Exception If an error occurs during the process
   */
  override fun writeTextToPdf(text: String, context: Context): PdfDocument {
    var tempFile: File? = null
    try {
      tempFile = createTempTextFile(text, context)
      val pdfDocument = writeTextFileToPdf(tempFile.bufferedReader())
      return pdfDocument
    } catch (e: Exception) {
      tempFile?.delete()
      Log.e("writeTextToPdf", "Failed to write text to pdf", e)
      throw Exception("Failed to write result to PDF, please try again.")
    }
  }

  /**
   * Get a temporary file from the given URI
   *
   * @param uri The URI of the file to get
   * @param context The context of the application
   * @return The temporary file created from the URI
   * @throws Exception If an error occurs during the process
   */
  override fun getTempFileFromUri(uri: Uri?, context: Context): File {
    var tempFile: File? = null
    try {
      // Get file extension from uri, through the uri path if not null, otherwise by querying the
      // content resolver
      val documentType =
          if (uri!!.scheme == "file") uri.path?.substringAfterLast(".")
          else getFileExtensionFromUri(context, uri!!)
      tempFile = File.createTempFile("tempDocument", ".$documentType", context.externalCacheDir)

      // Make sure the input stream is correctly closed
      context.contentResolver.openInputStream(uri).use { inputStream ->
        tempFile.outputStream().use { outputStream -> inputStream?.copyTo(outputStream) }
      }
          ?: throw Exception(
              "Failed to open InputStream for the URI (openInputStream returned null).")
      return tempFile
    } catch (e: Exception) {
      tempFile?.delete()
      Log.e("getTempFileFromUri", "Failed to get temp file from uri: $uri", e)
      throw Exception("Failed to open the selected file, please try with another file.")
    }
  }

  /**
   * Extract text from the image corresponding to the given URI using the ML Kit Text Recognition
   * library
   *
   * @param imageUri The URI of the image to extract text from
   * @param context The context of the application
   * @param onSuccess The callback to be called when the text is successfully extracted
   * @param onFailure The callback to be called when an error occurs
   */
  override fun extractTextFromImage(
      imageUri: Uri?,
      context: Context,
      onSuccess: (String) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    // Read the image bitmap from the URI, throws an exception if getImageBitmap fails
    val imageBitmap = getImageBitmap(imageUri, context)

    var recognizer: TextRecognizer? = null

    try {
      recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
      val image = InputImage.fromBitmap(imageBitmap, 0)
      recognizer
          .process(image)
          .addOnSuccessListener { visionText ->
            val extractedText = visionText.text
            if (extractedText.isNotEmpty()) {
              onSuccess(extractedText)
            } else {
              onFailure(Exception("No text found on the selected image, please try with another."))
            }
          }
          .addOnFailureListener { throw it }
    } catch (e: Exception) {
      Log.e("extractTextFromImage", "Failed to extract text from image with uri: $imageUri", e)
      onFailure(Exception("Failed to extract text from image, please try again."))
    } finally {
      recognizer?.close() // Close the recognizer client when no longer needed
      imageBitmap.recycle() // Recycle the image bitmap to free up memory
    }
  }

  /**
   * Helper function to get the image bitmap from the given URI
   *
   * @param imageUri The URI of the image for which to get the bitmap
   * @param context The context of the application
   * @return The bitmap of the image
   * @throws Exception If an error occurs during the process
   */
  private fun getImageBitmap(imageUri: Uri?, context: Context): Bitmap {
    try {
      val imageBitmap =
          context.contentResolver.openInputStream(imageUri!!).use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
          } ?: throw FileNotFoundException("Failed to open image file, opened input stream is null")
      return imageBitmap
    } catch (e: Exception) {
      // If an exception occurs while getting the image bitmap, log the error to be able to easily
      // trace back the origin of the exception when debugging
      Log.e(
          "getImageBitmap", "An exception occurred while getting image bitmap of uri: $imageUri", e)
      if (e is IOException) {
        // If an IOException occurs while reading the image file the user should be notified to
        // retry
        throw Exception("Failed to read the image file, please try again.")
      } else {
        // If the image cannot be decoded, the user should be notified to try with another image
        throw Exception("The selected image cannot be decoded, please try with another image.")
      }
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
    try {
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
    } catch (e: Exception) {
      // If an exception occurs during the writing process, close the PdfDocument
      try {
        pdfDocument.close()
      } catch (closeException: Exception) {
        Log.e("writeTextFileToPdf", "Failed to close PdfDocument", closeException)
      }
      Log.e("writeTextFileToPdf", "Failed to write text file to pdf", e)
      throw e
    }
  }

  /**
   * Heleper function to create a temporary text file with the given text written to it
   *
   * @param text The text to write to the temp file
   * @param context the context of the app
   * @return The created temporary text file
   */
  private fun createTempTextFile(text: String, context: Context): File {
    var tempFile: File? = null
    try {
      tempFile = File.createTempFile("temp_text", ".txt", context.externalCacheDir)
      tempFile.writeText(text)
      return tempFile
    } catch (e: Exception) {
      tempFile?.delete()
      Log.e("createTempTextFile", "Failed to create temp text file", e)
      throw e
    }
  }

  /**
   * Helper function to get the file extension from the given URI
   *
   * @param context The context of the application
   * @param uri The URI of the file to get the extension from
   * @return The extension of the file
   * @throws Exception If an error occurs during the process
   */
  private fun getFileExtensionFromUri(context: Context, uri: Uri): String {
    try {
      val cursor = context.contentResolver.query(uri, null, null, null, null)
      var extension = ""

      // Different exceptions are thrown to be able to easily trace back the cause of the failure
      cursor?.use {
        if (it.moveToFirst()) {
          val fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
          extension = fileName.substringAfterLast('.', "")
        } else {
          throw Exception("Queried cursor is empty")
        }
      } ?: throw Exception("Cursor is null")
      if (extension.isEmpty()) {
        throw Exception("No extension found in file name")
      } else {
        return extension
      }
    } catch (e: Exception) {
      // Throw the exception so that the caller doesn't proceed with the rest of execution if the
      // extension is not valid (i.e empty), since it may lead to errors and make it harder to debug
      // and find the cause of the problem (for example when getTempFileFromUri calls
      // File.createTempFile with an empty extension, the latter will fail to create the file and
      // throw an exception, when the cause of its failure is actually the empty extension and it's
      // easier to trace back the cause of the failure if the exception is thrown here and the the
      // log is made as soon as the problem is detected)
      Log.e("getFileExtensionFromUri", "Failed to get file extension from uri: $uri", e)
      throw e
    }
  }
}
