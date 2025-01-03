package com.github.se.eduverse.model

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

interface Downloader {
  @Throws(IOException::class) fun openStream(url: String): InputStream
}

class DefaultDownloader : Downloader {
  private val client =
      OkHttpClient.Builder()
          .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
          .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
          .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
          .build()

  @OptIn(UnstableApi::class)
  override fun openStream(url: String): InputStream {
    Log.d("DOWNLOAD_DEBUG", "Starting download for URL: $url")
    try {
      val request = Request.Builder().url(url).build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        Log.e(
            "DOWNLOAD_ERROR",
            "Request failed with code: ${response.code} and message: ${response.message}")
        throw IOException("Failed to download file: ${response.code}")
      }

      val contentLength = response.body?.contentLength() ?: 0L
      Log.d("DOWNLOAD_DEBUG", "Response successful. Content-Length: $contentLength bytes")

      return response.body?.byteStream() ?: throw IOException("Empty response body for URL: $url")
    } catch (e: Exception) {
      Log.e("DOWNLOAD_ERROR", "Exception while downloading file: ${e.message}", e)
      throw e
    }
  }
}

interface MetadataSerializer {
  fun <T> serialize(data: T): String

  fun <T> deserialize(json: String, clazz: Class<T>): T?
}

class GsonMetadataSerializer : MetadataSerializer {
  private val gson = Gson()

  override fun <T> serialize(data: T): String {
    return gson.toJson(data)
  }

  override fun <T> deserialize(json: String, clazz: Class<T>): T? {
    return try {
      gson.fromJson(json, clazz)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}

class MediaCacheManager(
    private val context: Context,
    private val downloader: Downloader = DefaultDownloader(),
    private val serializer: MetadataSerializer = GsonMetadataSerializer()
) {
  companion object {
    private const val CACHE_LIFETIME_MS = 2 * 24 * 60 * 60 * 1000L // 2 jours
    // private const val CACHE_LIFETIME_MS = 0L
  }

  @OptIn(UnstableApi::class)
  suspend fun saveFileToCache(url: String, fileName: String): File? =
      withContext(Dispatchers.IO) {
        Log.d("CACHE_DEBUG", "Attempting to download URL: $url")
        try {
          if (url.isEmpty()) throw IOException("Empty URL")

          val timestamp = System.currentTimeMillis()
          val timestampedFileName = "${timestamp}_$fileName"

          val file = File(context.cacheDir, timestampedFileName)
          val inputStream: InputStream = downloader.openStream(url)
          val outputStream = FileOutputStream(file)

          inputStream.use { it.copyTo(outputStream) }
          outputStream.close()
          Log.d("CACHE_DEBUG", "File saved: $timestampedFileName at ${file.absolutePath}")
          file
        } catch (e: Exception) {
          Log.e("CACHE_ERROR", "Error saving file: ${e.message}")
          null
        }
      }

  @OptIn(UnstableApi::class)
  suspend fun <T> savePublicationToCache(
      publication: T,
      mediaUrl: String,
      mediaFileName: String,
      metadataFileName: String
  ): Boolean {
    try {
      val mediaFile = saveFileToCache(mediaUrl, mediaFileName)
      if (mediaFile == null) {
        Log.e("CACHE_ERROR", "Failed to save media file: $mediaFileName")
        return false
      }
      val json = serializer.serialize(publication)
      val metadataFile = File(context.cacheDir, metadataFileName)
      metadataFile.writeText(json)
      return true
    } catch (e: Exception) {
      Log.e("CACHE_ERROR", "Exception during savePublicationToCache: ${e.message}")
      return false
    }
  }

  fun getFileFromCache(fileName: String): File? {
    val file = File(context.cacheDir, fileName)
    return if (file.exists()) file else null
  }

  private fun <T> getPublicationFromCache(metadataFileName: String, clazz: Class<T>): T? {
    val metadataFile = File(context.cacheDir, metadataFileName)
    if (!metadataFile.exists()) {
      return null
    }
    return try {
      val json = metadataFile.readText()
      serializer.deserialize(json, clazz)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun deleteFileFromCache(fileName: String): Boolean {
    val file = File(context.cacheDir, fileName)
    return file.delete()
  }

  @OptIn(UnstableApi::class)
  fun cleanCache() {
    val currentTime = System.currentTimeMillis()
    val cacheDir = context.cacheDir

    cacheDir.listFiles()?.forEach { file ->
      val timestamp = file.name.split("_").firstOrNull()?.toLongOrNull()
      if (timestamp != null) {
        val age = currentTime - timestamp
        Log.d("CACHE_CLEAN", "File: ${file.name}, Age: $age ms")

        if (age > CACHE_LIFETIME_MS) {
          if (file.delete()) {
            Log.d("CACHE_CLEAN", "Deleted expired file: ${file.name}")
          } else {
            Log.e("CACHE_CLEAN", "Failed to delete file: ${file.name}")
          }
        }
      } else {
        Log.e("CACHE_CLEAN", "Invalid file name, cannot extract timestamp: ${file.name}")
      }
    }
  }

  @OptIn(UnstableApi::class)
  fun hasCachedFiles(): Boolean {
    val currentTime = System.currentTimeMillis()
    val cacheDir = context.cacheDir
    val hasRecentFiles =
        cacheDir.listFiles()?.any { file ->
          val timestamp = file.name.split("_").firstOrNull()?.toLongOrNull()
          val isRecent = timestamp != null && currentTime - timestamp <= CACHE_LIFETIME_MS
          Log.d("CACHE_CHECK", "File: ${file.name}, Is recent: $isRecent")
          isRecent
        } ?: false

    Log.d("CACHE_CHECK", "Has cached files: $hasRecentFiles")
    return hasRecentFiles
  }
}
