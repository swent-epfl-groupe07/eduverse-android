package com.github.se.eduverse.api

import com.squareup.moshi.Json

// Data class to encapsulate the response from the ConvertAPI service
data class ConvertApiResponse(@Json(name = "Files") val files: List<ConvertedFile>)

// Data class to encapsulate the converted file from the ConvertAPI service (only keeps the Url
// field because that's all we need when processing the response)
data class ConvertedFile(@Json(name = "Url") val url: String)

// List of supported file types for conversion with the ConvertAPI service
val SUPPORTED_CONVERSION_TYPES =
    listOf("docx", "doc", "pptx", "ppt", "xls", "xlsx", "pages", "numbers", "key", "csv")

// These exceptions are defined so that it's clearer when debugging which api call resulted in the
// failure of the whole conversion process
class FileConversionException(message: String, cause: Throwable) : Exception(message, cause)

class FileDownloadException(message: String, cause: Throwable) : Exception(message, cause)