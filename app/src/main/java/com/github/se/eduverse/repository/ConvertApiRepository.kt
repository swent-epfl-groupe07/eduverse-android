package com.github.se.eduverse.repository

import android.content.Context
import com.github.se.eduverse.BuildConfig
import com.github.se.eduverse.api.ConvertApiResponse
import com.github.se.eduverse.api.FileConversionException
import com.github.se.eduverse.api.FileDownloadException
import com.github.se.eduverse.api.SUPPORTED_CONVERSION_TYPES
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

open class ConvertApiRepository(private val client: OkHttpClient) {
  private val apiKey = BuildConfig.CONVERT_API_KEY
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val responseAdapter = moshi.adapter(ConvertApiResponse::class.java)
  private val CONVERSION_TIMEOUT = "1200" // Maximum conversion timeout value accepted by ConvertAPI

  /**
   * Converts the given file to a PDF file using the ConvertAPI service
   *
   * @param file The file to convert
   * @param pdfName The name that the result PDF file should have
   * @return The converted PDF file if successful
   * @throws FileConversionException If the conversion of the file fails
   * @throws FileDownloadException If the download of the converted PDF file fails
   * @throws IllegalArgumentException If the file type is not supported for conversion
   */
  open fun convertToPdf(file: File, pdfName: String, context: Context): File? {
    val fileType = file.extension

    try {
      // Check if the file type is supported for conversion
      if (fileType !in SUPPORTED_CONVERSION_TYPES) {
        throw IllegalArgumentException(
            "Selected file type: $fileType, is not supported for conversion")
      }

      val url = "https://v2.convertapi.com/convert/$fileType/to/pdf"

      val requestBody =
          MultipartBody.Builder()
              .setType(MultipartBody.FORM)
              .addFormDataPart(
                  "File", file.name, file.asRequestBody()) // Upload the file to convert
              .addFormDataPart(
                  "StoreFile",
                  "true") // We store the file on the server because otherwise the result file data
              // is entirely provided in the response body which can lead to out of
              // memory errors if the file is too large, and with the url we download
              // the file in a streamed manner avoiding memory issues
              .addFormDataPart("Timeout", CONVERSION_TIMEOUT) // Set the conversion timeout
              .build()

      val request =
          Request.Builder()
              .url(url)
              .addHeader("Authorization", "Bearer $apiKey")
              .addHeader("accept", "application/json")
              .post(requestBody)
              .build()

      // We use synchronous api calls for the conversion and the download because the latter depends
      // on the former's result and making the calls synchronous avoids callback hell and makes the
      // code easier to read. Also the blocking nature of the calls is dealt with in the viewmodel
      // by running the conversion in a background thread which makes it as though the calls were
      // asynchronous
      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        throw Exception("Unsuccessful convert api respsonse: $response")
      }

      response.body?.let { body ->
        val convertApiResponse = responseAdapter.fromJson(body.string())
        val fileUrl =
            convertApiResponse?.files?.firstOrNull()?.url // Get the URL of the converted PDF file
        if (fileUrl != null) {
          file.delete() // Delete the original file (which is a temp copy file and not needed
          // anymore once converted)
          return downloadPdfFile(
              fileUrl,
              pdfName,
              context) // Download the converted PDF file and return it if successful
        } else {
          throw Exception("File URL not found in convert response")
        }
      } ?: throw Exception("Empty convert response body")
    } catch (e: Exception) {
      file.delete()
      if (e is FileDownloadException || e is IllegalArgumentException) {
        throw e
      } else {
        throw FileConversionException("Failed to convert file to PDF: ${e.message}", e)
      }
    }
  }

  /**
   * Helper function to download the converted PDF file when the conversion is successful
   *
   * @param fileUrl The URL of the converted PDF file
   * @param pdfName The name that the result PDF file should have
   * @return The downloaded PDF file
   * @throws FileDownloadException If the download fails
   */
  private fun downloadPdfFile(fileUrl: String, pdfName: String, context: Context): File? {
    val pdfFile = File.createTempFile(pdfName, ".pdf", context.externalCacheDir)
    try {
      val request = Request.Builder().url(fileUrl).build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) throw Exception("Unsuccessful download response: $response")

      val inputStream =
          response.body?.byteStream() ?: throw Exception("Empty download response body")
      val outputStream = FileOutputStream(pdfFile)

      inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
      return pdfFile
    } catch (e: Exception) {
      pdfFile.delete()
      throw FileDownloadException("Failed to download PDF file", e)
    }
  }
}
