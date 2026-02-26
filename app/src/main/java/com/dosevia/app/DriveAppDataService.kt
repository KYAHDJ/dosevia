package com.dosevia.app

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DriveAppDataService(private val authManager: GoogleAuthManager) {

    companion object {
        const val BACKUP_FILE_NAME = "dosevia_backup_v1.json"
        const val BACKUP_MIME_TYPE = "application/json"
    }

    private suspend fun drive(): Drive? = withContext(Dispatchers.IO) {
        val credential = authManager.createDriveCredential() ?: return@withContext null
        Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Dosevia").build()
    }

    suspend fun findBackupFileId(): String? = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext null
        val files = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='${BACKUP_FILE_NAME.replace("'", "\\'")}' and trashed=false")
            .setFields("files(id,name,modifiedTime)")
            .execute()
            .files
        files.firstOrNull()?.id
    }

    suspend fun downloadBackup(fileId: String): String = withContext(Dispatchers.IO) {
        val drive = drive() ?: error("No signed-in account")
        drive.files().get(fileId).executeMediaAsInputStream().bufferedReader().use { it.readText() }
    }

    suspend fun createBackup(json: String): String = withContext(Dispatchers.IO) {
        val drive = drive() ?: error("No signed-in account")
        val metadata = File().apply {
            name = BACKUP_FILE_NAME
            mimeType = BACKUP_MIME_TYPE
            parents = listOf("appDataFolder")
        }
        val content = ByteArrayContent(BACKUP_MIME_TYPE, json.toByteArray())
        drive.files().create(metadata, content).setFields("id").execute().id
    }

    suspend fun updateBackup(fileId: String, json: String) = withContext(Dispatchers.IO) {
        val drive = drive() ?: error("No signed-in account")
        val content = ByteArrayContent(BACKUP_MIME_TYPE, json.toByteArray())
        drive.files().update(fileId, null, content).execute()
    }

    suspend fun getBackupModifiedEpochMs(fileId: String): Long? = withContext(Dispatchers.IO) {
        val drive = drive() ?: return@withContext null
        val file = drive.files().get(fileId).setFields("modifiedTime").execute()
        file.modifiedTime?.value
    }
}
