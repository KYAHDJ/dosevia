package com.dosevia.app

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.URLEncoder

class DriveAppDataService(private val authManager: GoogleAuthManager) {

    class DriveAuthException : Exception("Google Drive authorization is invalid")

    companion object {
        const val BACKUP_FILE_NAME = "dosevia_backup_v1.json"
        private const val BASE_DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val BASE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val gson = Gson()
    private val client = OkHttpClient()

    suspend fun findBackupFileId(): String? = withContext(Dispatchers.IO) {
        val query = "name='${BACKUP_FILE_NAME}' and 'appDataFolder' in parents and trashed=false"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$BASE_DRIVE_FILES_URL?q=$encodedQuery&spaces=appDataFolder&fields=files(id,name,modifiedTime)"
        val response = executeWithAuthRetry { token ->
            Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer $token")
                .build()
        } ?: return@withContext null

        response.use {
            if (!it.isSuccessful) return@withContext null
            val body = it.body?.string().orEmpty()
            val root = gson.fromJson(body, JsonObject::class.java)
            val files = root.getAsJsonArray("files") ?: JsonArray()
            if (files.size() == 0) null else files[0].asJsonObject.get("id")?.asString
        }
    }

    suspend fun downloadBackup(fileId: String): String = withContext(Dispatchers.IO) {
        val url = "$BASE_DRIVE_FILES_URL/$fileId?alt=media"
        val response = executeWithAuthRetry { token ->
            Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer $token")
                .build()
        } ?: error("No signed-in account")

        response.use {
            if (!it.isSuccessful) error("Download failed: HTTP ${it.code}")
            it.body?.string().orEmpty()
        }
    }

    suspend fun createBackup(json: String): Int = withContext(Dispatchers.IO) {
        val boundary = "dosevia-boundary-${System.currentTimeMillis()}"
        val metadata = """
            {"name":"$BACKUP_FILE_NAME","parents":["appDataFolder"],"mimeType":"application/json"}
        """.trimIndent()
        val multipart = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json\r\n\r\n")
            append(json)
            append("\r\n--$boundary--")
        }

        val response = executeWithAuthRetry { token ->
            Request.Builder()
                .url("$BASE_UPLOAD_URL?uploadType=multipart")
                .post(multipart.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()
        } ?: error("No signed-in account")

        response.use {
            if (!it.isSuccessful) error("Create failed: HTTP ${it.code}")
            it.code
        }
    }

    suspend fun updateBackup(fileId: String, json: String): Int = withContext(Dispatchers.IO) {
        val response = executeWithAuthRetry { token ->
            Request.Builder()
                .url("$BASE_UPLOAD_URL/$fileId?uploadType=media")
                .patch(json.toRequestBody(JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } ?: error("No signed-in account")

        response.use {
            if (!it.isSuccessful) error("Update failed: HTTP ${it.code}")
            it.code
        }
    }

    private suspend fun executeWithAuthRetry(buildRequest: (token: String) -> Request): Response? {
        var token = authManager.getAccessToken(forceRefresh = false) ?: return null
        var response = client.newCall(buildRequest(token)).execute()
        if (response.code != 401 && response.code != 403) return response

        response.close()
        authManager.clearToken(token)
        token = authManager.getAccessToken(forceRefresh = true) ?: return null
        val refreshedResponse = client.newCall(buildRequest(token)).execute()
        if (refreshedResponse.code == 401 || refreshedResponse.code == 403) {
            refreshedResponse.close()
            throw DriveAuthException()
        }
        return refreshedResponse
    }
}
