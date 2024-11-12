package com.github.se.eduverse.repository

import com.github.se.eduverse.model.FilterTypes
import com.github.se.eduverse.model.Folder
import com.github.se.eduverse.model.MyFile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.io.InvalidClassException
import java.util.Calendar
import java.util.HashMap

class FolderRepositoryImpl(private val db: FirebaseFirestore) : FolderRepository {
    private val folderNameText = "name"
    private val ownerIdText = "ownerId"
    private val filesText = "files"
    private val filterTypeText = "filterType"

    private val fileNameText = "name"
    private val fileIdText = "fileId"
    private val creationTimeText = "creationTime"
    private val lastAccessText = "lastAccess"
    private val numberAccessText = "numberAccess"

  private val collectionPath = "folders"

  /**
   * Access the list of folders associated to a user.
   *
   * @param userId the id of the active user
   * @param onSuccess code executed if the folders are successfully accessed
   * @param onFailure code executed if the folders can't be accessed
   */
  override fun getFolders(
      userId: String,
      onSuccess: (List<Folder>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    db.collection(collectionPath)
        .whereEqualTo("ownerId", userId)
        .get()
        .addOnSuccessListener { folders ->
          onSuccess(folders.documents.map { document -> convertFolder(document) })
        }
        .addOnFailureListener(onFailure)
  }

  /**
   * Add a folder to the database.
   *
   * @param folder the folder to add
   * @param onSuccess code executed if the folder is successfully added
   * @param onFailure code executed if the folder can't be added
   */
  override fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    val mappedFolders =
        hashMapOf(
            folderNameText to folder.name,
            ownerIdText to folder.ownerID,
            filesText to folder.files.map { fileToMap(it) },
            filterTypeText to filterToString(folder.filterType))

    db.collection(collectionPath)
        .document(folder.id)
        .set(mappedFolders)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  /**
   * Update the values of a folder inside the database.
   *
   * @param folder the folder to update
   * @param onSuccess code executed if the folder is successfully updated
   * @param onFailure code executed if the folder can't be updated
   */
  override fun updateFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    val mappedFolders =
        hashMapOf(
            folderNameText to folder.name,
            ownerIdText to folder.ownerID,
            filesText to folder.files.map { fileToMap(it) },
            filterTypeText to filterToString(folder.filterType))

    db.collection(collectionPath)
        .document(folder.id)
        .update(mappedFolders)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  /**
   * Remove a folder from the database.
   *
   * @param folder the folder to delete
   * @param onSuccess code executed if the folder is successfully deleted
   * @param onFailure code executed if the folder can't be deleted
   */
  override fun deleteFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    db.collection(collectionPath)
        .document(folder.id)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onFailure)
  }

  /**
   * Create a new folder document in the database.
   *
   * @return the id of the new document
   */
  override fun getNewUid(): String {
    return db.collection(collectionPath).document().id
  }

  private fun fileToMap(file: MyFile): HashMap<String, String> {
    return hashMapOf(
        fileNameText to file.name,
        fileIdText to file.fileId,
        creationTimeText to file.creationTime.timeInMillis.toString(),
        lastAccessText to file.lastAccess.timeInMillis.toString(),
        numberAccessText to file.numberAccess.toString())
  }

  private fun mapToFile(map: Map<String, String>): MyFile {
    return MyFile(
        "",
        map[fileIdText]!!,
        map[fileNameText]!!,
        Calendar.getInstance().apply { timeInMillis = (map[creationTimeText]!!.toLong()) },
        Calendar.getInstance().apply { timeInMillis = (map[lastAccessText]!!.toLong()) },
        map[numberAccessText]!!.toInt())
  }

  fun convertFolder(document: DocumentSnapshot): Folder {
    val rawFiles = document.get(filesText) as? List<Map<String, String>>
    val files: List<MyFile> = rawFiles?.map { mapToFile(it) } ?: emptyList()

    return Folder(
        ownerID = document.getString(ownerIdText)!!,
        files = files.toMutableList(),
        name = document.getString(folderNameText)!!,
        id = document.id,
        filterType = stringToFilter(document.getString(filterTypeText)!!))
  }

  private fun stringToFilter(string: String): FilterTypes {
    return when (string) {
      "NAME" -> FilterTypes.NAME
      "CREATION_UP" -> FilterTypes.CREATION_UP
      "CREATION_DOWN" -> FilterTypes.CREATION_DOWN
      "ACCESS_RECENT" -> FilterTypes.ACCESS_RECENT
      "ACCESS_OLD" -> FilterTypes.ACCESS_OLD
      "ACCESS_MOST" -> FilterTypes.ACCESS_MOST
      "ACCESS_LEAST" -> FilterTypes.ACCESS_LEAST
      else -> throw InvalidClassException("Status string value $string is unknown")
    }
  }

  private fun filterToString(filter: FilterTypes): String {
    return when (filter) {
      FilterTypes.NAME -> "NAME"
      FilterTypes.CREATION_UP -> "CREATION_UP"
      FilterTypes.CREATION_DOWN -> "CREATION_DOWN"
      FilterTypes.ACCESS_RECENT -> "ACCESS_RECENT"
      FilterTypes.ACCESS_OLD -> "ACCESS_OLD"
      FilterTypes.ACCESS_MOST -> "ACCESS_MOST"
      FilterTypes.ACCESS_LEAST -> "ACCESS_LEAST"
    }
  }
}
