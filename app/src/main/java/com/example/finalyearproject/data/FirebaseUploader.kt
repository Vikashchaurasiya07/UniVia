package com.example.finalyearproject.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

fun uploadImageToFirebase(context: Context, imageUri: Uri, type: String) {
    val storageRef = FirebaseStorage.getInstance().reference
    val fileName = UUID.randomUUID().toString() + ".jpg"
    val imageRef = storageRef.child("certificates/$type/$fileName")

    imageRef.putFile(imageUri)
        .addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                saveImageUrlToRealtimeDB(downloadUrl.toString(), type)
                Toast.makeText(context, "Upload successful 💖", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Upload failed 😔", Toast.LENGTH_SHORT).show()
        }
}
