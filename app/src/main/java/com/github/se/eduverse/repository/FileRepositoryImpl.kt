package com.github.se.eduverse.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class FileRepositoryImpl(private val db: FirebaseFirestore, private val storage: FirebaseStorage) :
    FileRepository {
    private val collectionPath = "files"

    override fun getNewUid(): String {
        return db.collection(collectionPath).document().id
    }

    override fun saveFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val storageReference = storage.reference
        val pdfRef = storageReference.child("pdfs/${file.lastPathSegment}")

        pdfRef.putFile(file)
            .addOnSuccessListener { taskSnapshot ->
                pdfRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    savePDFUrlToFirestore(downloadUri.toString(), fileId, onSuccess)
                }
            }
            .addOnFailureListener (onFailure)
    }

    private fun savePDFUrlToFirestore(path: String, fileId: String, onSuccess: () -> Unit) {
        val pdfData = hashMapOf("url" to path)
        db.collection(collectionPath).document(fileId).set(pdfData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("File Upload", "Can't store reference of file $path: $it")
            }
    }

    override fun modifiyFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun deleteFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun accessFile(file: Uri, fileId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        TODO("Not yet implemented")
    }


}