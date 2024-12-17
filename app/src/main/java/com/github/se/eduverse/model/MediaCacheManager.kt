package com.github.se.eduverse.model

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL

// Interface pour le téléchargement
interface Downloader {
  @Throws(IOException::class) fun openStream(url: String): InputStream
}

class DefaultDownloader : Downloader {
  override fun openStream(url: String): InputStream {
    return URL(url).openStream()
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
  }

  fun saveFileToCache(url: String, fileName: String): File? {
    return try {
      val file = File(context.cacheDir, fileName)
      if (!file.exists()) {
        val inputStream: InputStream = downloader.openStream(url)
        val outputStream = FileOutputStream(file)
        inputStream.use { it.copyTo(outputStream) }
        outputStream.close()
      }
      file
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun <T> savePublicationToCache(
      publication: T,
      mediaUrl: String,
      mediaFileName: String,
      metadataFileName: String
  ): Boolean {
    val mediaFile = saveFileToCache(mediaUrl, mediaFileName)
    if (mediaFile == null) {
      return false
    }
    return try {
      val json = serializer.serialize(publication)
      val metadataFile = File(context.cacheDir, metadataFileName)
      metadataFile.writeText(json)
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  fun getFileFromCache(fileName: String): File? {
    val file = File(context.cacheDir, fileName)
    return if (file.exists()) file else null
  }

  fun <T> getPublicationFromCache(metadataFileName: String, clazz: Class<T>): T? {
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

  fun cleanCache() {
    val currentTime = System.currentTimeMillis()
    val cacheDir = context.cacheDir
    cacheDir.listFiles()?.forEach { file ->
      if (currentTime - file.lastModified() > CACHE_LIFETIME_MS) {
        file.delete()
      }
    }
  }

  fun hasCachedFiles(): Boolean {
    val cacheDir = context.cacheDir
    return cacheDir.listFiles()?.isNotEmpty() ?: false
  }
}
