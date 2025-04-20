package com.example.finalyearproject.data

import android.content.Context
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import java.io.InputStream
import android.net.Uri
import android.provider.OpenableColumns

object GoogleDriveServiceHelper {
    fun getDriveService(context: Context): Drive {
        val jsonFactory = GsonFactory.getDefaultInstance()
        val transport = NetHttpTransport()

        val inputStream: InputStream = context.resources.openRawResource(
            context.resources.getIdentifier("drive_credentials", "raw", context.packageName)
        )

        val credential = GoogleCredential.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/drive"))

        return Drive.Builder(transport, jsonFactory, credential)
            .setApplicationName("UniVia")
            .build()
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) {
            it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        } else null
    }
}