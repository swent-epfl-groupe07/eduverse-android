package com.github.se.eduverse.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class FileRepositoryImpl(private val db: FirebaseFirestore, private val storage: FirebaseStorage) :
    FileRepository {
  private val collectionPath = "files"

  /**
   * Create a new file document in the firebase firestore
   *
   * @return the id of the document
   */
  override fun getNewUid(): String {
    return db.collection(collectionPath).document().id
  }

  /**
   * Save a file in the firebase storage, and save the path to it in a file on firestore
   *
   * @param file the URI of the file to add
   * @param fileId the id of the firestore document in which the path should be stored
   * @param onSuccess to execute if the task is done successfully
   * @param onFailure error management method
   */
  override fun savePdfFile(
      file: Uri,
      fileId: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val storageReference = storage.reference
    val path = "pdfs/${file.lastPathSegment}"
    val pdfRef = storageReference.child(path)

    pdfRef
        .putFile(file)
        .addOnSuccessListener { savePathToFirestore(path, ".pdf", fileId, onSuccess) }
        .addOnFailureListener(onFailure)
  }

  /** Does nothing for now */
  override fun modifiyFile(
      file: Uri,
      fileId: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    TODO("Not yet implemented")
  }

  /**
   * Delete a file on firebase, both in storage and in firestore
   *
   * @param fileId the id of the file to delete
   * @param onSuccess the code to execute if the deletion is successful
   * @param onFailure error management method
   */
  override fun deleteFile(fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    db.collection(collectionPath)
        .document(fileId)
        .get()
        .addOnSuccessListener {
          storage.reference
              .child(it.getString("url")!!)
              .delete()
              .addOnSuccessListener {
                db.collection(collectionPath)
                    .document(fileId)
                    .delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener(onFailure)
              }
              .addOnFailureListener(onFailure)
        }
        .addOnFailureListener(onFailure)
  }

  /**
   * Access a file in the firebase storage and execute some given code with it
   *
   * @param fileId the id of the file as stored in the database (!= storage)
   * @param onSuccess the code to execute with the accessed code
   * @param onFailure error management method
   */
  override fun accessFile(
      fileId: String,
      onSuccess: (StorageReference, String) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    db.collection(collectionPath)
        .document(fileId)
        .get()
        .addOnSuccessListener {
          onSuccess(
              storage.reference.child(it.getString("url")!!),
              it.getString("suffix") ?: ".pdf" // Default .pdf for backward compatibility
              )
        }
        .addOnFailureListener(onFailure)
  }

  /**
   * Save the path to a file on firebase. Isn't private for testing purpose, but shouldn't be
   * called.
   *
   * @param path the path to save
   * @param suffix the suffix of the fyle type, e.g. .pdf, .jpg
   * @param fileId the id of the document in which the path should be stored
   * @param onSuccess to execute in case of success
   */
  override fun savePathToFirestore(
      path: String,
      suffix: String,
      fileId: String,
      onSuccess: () -> Unit
  ) {
    val pdfData = hashMapOf("url" to path, "suffix" to suffix)
    db.collection(collectionPath)
        .document(fileId)
        .set(pdfData)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { Log.e("File Upload", "Can't store reference of file $path: $it") }
  }
}
