package com.github.se.eduverse.model.folder

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.io.InvalidClassException
import java.util.Calendar

class FolderRepositoryImpl (private val db: FirebaseFirestore): FolderRepository {

    private val collectionPath = "folders"

    override fun getFolders(
        userId: String,
        onSuccess: (List<Folder>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(collectionPath)
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { folders ->
                onSuccess(folders.documents.map { document -> convertFolder(document, onFailure)})
            }
            .addOnFailureListener(onFailure)
    }

    override fun addFolder(folder: Folder, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val mappedFolders =
            hashMapOf(
                "name" to folder.name,
                "ownerId" to folder.ownerID,
                "filterType" to filterToString(folder.filterType))

        folder.files.forEach {
            addFileInFolder(it, folder, onFailure)
        }

        db.collection(collectionPath)
            .document(folder.id)
            .set(mappedFolders)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    override fun updateFolder(
        folder: Folder,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val mappedFolders =
            mapOf(
                "name" to folder.name,
                "ownerId" to folder.ownerID,
                "filterType" to filterToString(folder.filterType))

        folder.files.forEach {
            updateFileInFolder(it, folder, onFailure)
        }

        db.collection(collectionPath)
            .document(folder.id)
            .update(mappedFolders)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    override fun deleteFolder(
        folder: Folder,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(collectionPath)
            .document(folder.id)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    override fun getNewUid(): String {
        return db.collection(collectionPath).document().id
    }

    private fun addFileInFolder(file: MyFile, folder: Folder, onFailure: (Exception) -> Unit) {
        val mappedFiles =
            hashMapOf(
                "name" to file.name,
                "fileId" to file.fileId,
                "creationTime" to file.creationTime.timeInMillis,
                "lastAccess" to file.lastAccess.timeInMillis,
                "numberAccess" to file.numberAccess.toLong())

        db.collection(collectionPath)
            .document(folder.id)
            .collection("files")
            .document(file.id)
            .set(mappedFiles)
            .addOnFailureListener(onFailure)
    }

    private fun updateFileInFolder(file: MyFile, folder: Folder, onFailure: (Exception) -> Unit) {
        val mappedFiles =
            mapOf(
                "name" to file.name,
                "fileId" to file.fileId,
                "creationTime" to file.creationTime.timeInMillis,
                "lastAccess" to file.lastAccess.timeInMillis,
                "numberAccess" to file.numberAccess.toLong())

        db.collection(collectionPath)
            .document(folder.id)
            .collection("files")
            .document(file.id)
            .update(mappedFiles)
            .addOnFailureListener(onFailure)
    }

    private fun convertFolder(document: DocumentSnapshot, onFailure: (Exception) -> Unit): Folder {
        var files: List<MyFile> = emptyList()
        getFiles(
            document.id,
            { files = it },
            onFailure
        )
        return Folder(
            ownerID = document.getString("ownerId")!!,
            files = files.toMutableList(),
            name = document.getString("name")!!,
            id = document.id,
            filterType = stringToFilter(document.getString("filterType")!!))
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

    private fun getFiles(folderId: String, onSuccess: (List<MyFile>) -> Unit, onFailure: (Exception) -> Unit) {
         db.collection(collectionPath)
            .document(folderId)
            .collection("files")
            .get()
            .addOnSuccessListener { files ->
                onSuccess(files.documents.map { document -> convertFile(document) })
            }
            .addOnFailureListener(onFailure)
    }

    private fun convertFile(document: DocumentSnapshot): MyFile {
        val file = MyFile(
            id = document.id,
            fileId = document.getString("fileId")!!,
            name = document.getString("name")!!,
            creationTime = Calendar.getInstance(),
            lastAccess = Calendar.getInstance(),
            numberAccess = document.getLong("numberAccess")!!.toInt())
        file.creationTime.timeInMillis = document.getLong("creationTime")!!
        file.lastAccess.timeInMillis = document.getLong("lastAccess")!!
        return file
    }
}