package com.dosevia.app

// ─────────────────────────────────────────────────────────────────────────────
//  SoundManager.kt  — v15
//
//  Manages user-supplied alarm sounds stored in the app's private folder:
//      context.filesDir/alarm_sounds/
//
//  Public API
//  ──────────────────────────────────────────────────────────────────────────
//  copyAudioFileToApp(context, sourceUri)  → Result<File>
//      Copies the picked audio file into alarm_sounds/, using the original
//      display name (sanitised).  Returns the saved File on success.
//
//  listSavedSounds(context)  → List<SavedSound>
//      Returns all files currently in alarm_sounds/.
//
//  deleteSound(file)  → Boolean
//      Deletes a saved sound file. Returns true if successful.
//
//  soundDisplayName(path)  → String
//      Extracts the human-readable filename (without extension) from a path.
// ─────────────────────────────────────────────────────────────────────────────

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

// ── Data class returned by listSavedSounds() ──────────────────────────────────
data class SavedSound(
    val file: File,
    val displayName: String   // filename without extension
)

// ── Directory name inside context.filesDir ────────────────────────────────────
private const val SOUNDS_DIR = "alarm_sounds"

/** Returns (creating if needed) the alarm_sounds/ directory. */
fun getAlarmSoundsDir(context: Context): File =
    File(context.filesDir, SOUNDS_DIR).also { it.mkdirs() }

// ─────────────────────────────────────────────────────────────────────────────
//  Copy a user-picked audio URI into alarm_sounds/
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Copies the audio file at [sourceUri] (e.g. from the system file picker) into
 * the app's private alarm_sounds/ folder.
 *
 * Accepted audio formats: mp3, m4a, aac, ogg, wav, flac, opus, wma, 3gp, amr
 *
 * Returns [Result.success] with the saved [File], or [Result.failure] with a
 * descriptive [Exception] on any error (bad MIME type, IO failure, etc.).
 */
fun copyAudioFileToApp(context: Context, sourceUri: Uri): Result<File> {
    return try {
        // ── 1. Resolve display name ───────────────────────────────────────────
        var rawName = resolveDisplayName(context, sourceUri) ?: "alarm_sound"

        // ── 2. Validate extension ─────────────────────────────────────────────
        val ext = rawName.substringAfterLast('.', "").lowercase()
        val allowed = setOf("mp3", "m4a", "aac", "ogg", "wav", "flac", "opus", "wma", "3gp", "amr")
        if (ext !in allowed) {
            return Result.failure(
                IllegalArgumentException(
                    "\".$ext\" is not a supported audio format. " +
                    "Please pick an MP3, M4A, WAV, OGG, FLAC, AAC, OPUS, WMA, AMR, or 3GP file."
                )
            )
        }

        // ── 3. Sanitise filename (no slashes, spaces → underscores) ───────────
        val safeName = rawName
            .replace(Regex("[/\\\\]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(128)   // cap length

        // ── 4. Avoid collisions — append (2), (3) … if file already exists ───
        val dir  = getAlarmSoundsDir(context)
        val base = safeName.substringBeforeLast(".$ext", safeName)
        var dest = File(dir, "$base.$ext")
        var counter = 2
        while (dest.exists()) {
            dest = File(dir, "${base}_($counter).$ext")
            counter++
        }

        // ── 5. Stream-copy ────────────────────────────────────────────────────
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output, bufferSize = 64 * 1024)
            }
        } ?: return Result.failure(IOException("Could not open the selected file for reading."))

        Result.success(dest)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  List & delete saved sounds
// ─────────────────────────────────────────────────────────────────────────────

/** Returns all audio files stored in alarm_sounds/, sorted by name. */
fun listSavedSounds(context: Context): List<SavedSound> {
    val dir = getAlarmSoundsDir(context)
    return dir.listFiles()
        ?.filter { it.isFile }
        ?.sortedBy { it.name.lowercase() }
        ?.map { SavedSound(it, it.nameWithoutExtension) }
        ?: emptyList()
}

/** Deletes [file] from alarm_sounds/. Returns true if deletion succeeded. */
fun deleteSound(file: File): Boolean = try { file.delete() } catch (_: Exception) { false }

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Extracts a human-readable filename from an absolute path (without extension). */
fun soundDisplayName(path: String): String =
    File(path).nameWithoutExtension.replace('_', ' ')

/** Queries the ContentResolver for the file's display name; falls back to URI last segment. */
private fun resolveDisplayName(context: Context, uri: Uri): String? {
    // Try ContentResolver columns first (works for content:// URIs)
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(col)
                if (!name.isNullOrBlank()) return name
            }
        }
    } catch (_: Exception) {}

    // Fallback — last path segment of the URI
    return uri.lastPathSegment?.substringAfterLast('/')
}

// Alias so call-sites inside this file compile without explicit import
private typealias IOException = java.io.IOException
