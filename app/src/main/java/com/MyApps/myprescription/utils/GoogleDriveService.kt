package com.MyApps.myprescription.utils

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Collections

class GoogleDriveService(context: Context) {

    private val drive: Drive

    init {
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context)

        if (lastSignedInAccount != null) {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
            ).setSelectedAccount(lastSignedInAccount.account)

            drive = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("MyPrescription").build()
        } else {
            // This will be caught by the worker, preventing a crash.
            throw IllegalStateException("Cannot initialize Google Drive service: No user is signed in.")
        }
    }

    /**
     * Creates a backup file in the user's appDataFolder on Google Drive.
     * @param backupFile The local file to be uploaded.
     * @return The ID of the created file on Google Drive, or null on failure.
     */
    fun createBackup(backupFile: File): String? {
        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name = "myprescription_backup.zip"
            parents = listOf("appDataFolder")
        }
        val mediaContent = com.google.api.client.http.FileContent("application/zip", backupFile)
        val file = drive.files().create(fileMetadata, mediaContent).setFields("id").execute()
        return file.id
    }

    /**
     * Searches for a backup file in the user's appDataFolder.
     * @return The ID of the backup file if found, otherwise null.
     */
    fun findBackup(): String? {
        val files = drive.files().list()
            .setSpaces("appDataFolder")
            .setFields("nextPageToken, files(id, name)")
            .execute()
        for (file in files.files) {
            if (file.name == "myprescription_backup.zip") {
                return file.id
            }
        }
        return null
    }

    /**
     * Downloads a file from Google Drive and writes it to the provided OutputStream.
     * @param fileId The ID of the file to download.
     * @param outputStream The stream to write the downloaded file content to.
     */
    fun restoreBackup(fileId: String, outputStream: OutputStream) {
        drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
    }
}