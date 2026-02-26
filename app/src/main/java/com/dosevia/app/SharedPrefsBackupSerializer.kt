package com.dosevia.app

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.File

object SharedPrefsBackupSerializer {
    private val gson = Gson()

    fun findAllPrefsNames(context: Context): List<String> {
        val appContext = context.applicationContext
        val prefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")
        if (!prefsDir.exists() || !prefsDir.isDirectory) return emptyList()

        return prefsDir.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "xml" }
            .map { it.name.removeSuffix(".xml") }
            .sorted()
    }

    fun exportAllPrefs(context: Context): BackupPayload {
        val appContext = context.applicationContext
        val allPrefs = linkedMapOf<String, Map<String, Any?>>()

        findAllPrefsNames(appContext).forEach { prefName ->
            val prefs = appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val prefEntries = linkedMapOf<String, Any?>()
            prefs.all.forEach { (key, value) ->
                prefEntries[key] = when (value) {
                    is Set<*> -> value.mapNotNull { it?.toString() }
                    else -> value
                }
            }
            allPrefs[prefName] = prefEntries
        }

        return BackupPayload(
            version = 1,
            lastModifiedEpochMs = System.currentTimeMillis(),
            deviceId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown_device",
            prefs = allPrefs
        )
    }

    fun toJson(payload: BackupPayload): String = gson.toJson(payload)

    fun fromJson(json: String): BackupPayload {
        val root = gson.fromJson(json, JsonObject::class.java)
        val version = root.get("version")?.asInt ?: 1
        val lastModifiedEpochMs = root.get("lastModifiedEpochMs")?.asLong ?: 0L
        val deviceId = root.get("deviceId")?.asString ?: "unknown_device"
        val prefsRoot = root.getAsJsonObject("prefs") ?: JsonObject()

        val prefs = linkedMapOf<String, Map<String, Any?>>()
        prefsRoot.entrySet().forEach { (prefName, prefValue) ->
            if (!prefValue.isJsonObject) return@forEach
            val entries = linkedMapOf<String, Any?>()
            prefValue.asJsonObject.entrySet().forEach { (key, value) ->
                entries[key] = parseJsonValue(value)
            }
            prefs[prefName] = entries
        }

        return BackupPayload(
            version = version,
            lastModifiedEpochMs = lastModifiedEpochMs,
            deviceId = deviceId,
            prefs = prefs
        )
    }

    fun restoreAllPrefs(context: Context, payload: BackupPayload) {
        val appContext = context.applicationContext
        payload.prefs.forEach { (prefName, entries) ->
            val prefs = appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val editor = prefs.edit().clear()
            entries.forEach { (key, value) ->
                when (value) {
                    null -> editor.remove(key)
                    is Boolean -> editor.putBoolean(key, value)
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is List<*> -> editor.putStringSet(key, value.mapNotNull { it?.toString() }.toSet())
                    else -> editor.putString(key, value.toString())
                }
            }
            editor.apply()
        }
    }

    private fun parseJsonValue(element: JsonElement): Any? {
        if (element.isJsonNull) return null
        if (element.isJsonArray) {
            return element.asJsonArray.mapNotNull { if (it.isJsonNull) null else it.asString }
        }
        if (!element.isJsonPrimitive) return element.toString()

        val primitive = element.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isString -> primitive.asString
            primitive.isNumber -> {
                val raw = primitive.asString
                when {
                    raw.contains('.') -> raw.toDouble()
                    else -> raw.toLong().let { longValue ->
                        if (longValue in Int.MIN_VALUE..Int.MAX_VALUE) longValue.toInt() else longValue
                    }
                }
            }
            else -> primitive.asString
        }
    }
}
