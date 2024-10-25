package com.github.se.eduverse.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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
  override fun saveFile(
      file: Uri,
      fileId: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val storageReference = storage.reference
    val pdfRef = storageReference.child("pdfs/${file.lastPathSegment}")

    pdfRef
        .putFile(file)
        .addOnSuccessListener { taskSnapshot ->
          pdfRef.downloadUrl.addOnSuccessListener { downloadUri ->
            savePDFUrlToFirestore(downloadUri.toString(), fileId, onSuccess)
          }
        }
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

  /** Does nothing for now */
  override fun deleteFile(
      file: Uri,
      fileId: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    TODO("Not yet implemented")
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
      onSuccess: (Uri) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    db.collection(collectionPath)
        .document(fileId)
        .get()
        .addOnSuccessListener {
            onSuccess(Uri.parse(it.getString("url")))
        }
        .addOnFailureListener(onFailure)
  }

  /**
   * Save the path to a file on firebase. Isn't private for testing purpose, but shouldn't be
   * called.
   *
   * @param path the path to save
   * @param fileId the id of the document in which the path should be stored
   * @param onSuccess to execute in case of success
   */
  fun savePDFUrlToFirestore(path: String, fileId: String, onSuccess: () -> Unit) {
    val pdfData = hashMapOf("url" to path)
    db.collection(collectionPath)
        .document(fileId)
        .set(pdfData)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { Log.e("File Upload", "Can't store reference of file $path: $it") }
  }
}
