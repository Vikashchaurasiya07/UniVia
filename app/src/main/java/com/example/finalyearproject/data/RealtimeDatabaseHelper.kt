package com.example.finalyearproject.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

fun saveImageUrlToRealtimeDB(imageUrl: String, type: String) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val dbRef = FirebaseDatabase.getInstance().reference
        .child("users").child(userId).child("certificates").child(type)

    val certId = dbRef.push().key!!
    val certData = mapOf("imageUrl" to imageUrl, "timestamp" to System.currentTimeMillis())

    dbRef.child(certId).setValue(certData)
}
