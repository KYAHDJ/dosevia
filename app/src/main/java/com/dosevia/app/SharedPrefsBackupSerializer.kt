package com.dosevia.app

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject

private const val PREF_KEY_SEPARATOR = "::"

object SharedPrefsBackupSerializer {
    val backupPrefFiles = listOf("dosevia_prefs", "dosevia_status")
    private val gson = Gson()

    fun exportAllPrefs(context: Context): BackupPayload {
        val appContext = context.applicationContext
        val serializedPrefs = linkedMapOf<String, Any?>()

        backupPrefFiles.forEach { prefName ->
            val prefs = appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            prefs.all.forEach { (key, value) ->
                serializedPrefs["$prefName$PREF_KEY_SEPARATOR$key"] = serializeValue(value)
            }
        }

        return BackupPayload(
            lastModifiedEpochMs = System.currentTimeMillis(),
            deviceId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown_device",
            prefs = serializedPrefs
        )
    }

    fun toJson(payload: BackupPayload): String {
        val root = JsonObject().apply {
            addProperty("version", payload.version)
            addProperty("lastModifiedEpochMs", payload.lastModifiedEpochMs)
            addProperty("deviceId", payload.deviceId)
            add("prefs", JsonObject())
        }

        val prefsJson = root.getAsJsonObject("prefs")
        payload.prefs.forEach { (key, value) ->
            val typed = value as? Map<*, *> ?: return@forEach
            val node = JsonObject()
            node.addProperty("type", typed["type"] as? String ?: "string")
            val rawValue = typed["value"]
            when (rawValue) {
                null -> node.add("value", JsonNull.INSTANCE)
                is Boolean -> node.addProperty("value", rawValue)
                is Number -> node.addProperty("value", rawValue)
                is String -> node.addProperty("value", rawValue)
                is List<*> -> {
                    val arr = JsonArray()
                    rawValue.forEach { arr.add(it?.toString()) }
                    node.add("value", arr)
                }
                else -> node.addProperty("value", rawValue.toString())
            }
            prefsJson.add(key, node)
        }

        return gson.toJson(root)
    }

    fun fromJson(json: String): BackupPayload {
        val root = gson.fromJson(json, JsonObject::class.java)
        val prefsJson = root.getAsJsonObject("prefs")
        val prefsMap = linkedMapOf<String, Any?>()

        prefsJson.entrySet().forEach { (key, valueNode) ->
            val obj = valueNode.asJsonObject
            val type = obj.get("type")?.asString ?: "string"
            val value = obj.get("value")
            val typedValue: Any? = when (type) {
                "boolean" -> if (value == null || value.isJsonNull) null else value.asBoolean
                "int" -> if (value == null || value.isJsonNull) null else value.asInt
                "long" -> if (value == null || value.isJsonNull) null else value.asLong
                "float" -> if (value == null || value.isJsonNull) null else value.asFloat
                "string_set" -> if (value == null || value.isJsonNull) null else value.asJsonArray.mapNotNull { it.asString }
                else -> if (value == null || value.isJsonNull) null else value.asString
            }
            prefsMap[key] = mapOf("type" to type, "value" to typedValue)
        }

        return BackupPayload(
            version = root.get("version")?.asInt ?: 1,
            lastModifiedEpochMs = root.get("lastModifiedEpochMs")?.asLong ?: 0L,
            deviceId = root.get("deviceId")?.asString ?: "unknown_device",
            prefs = prefsMap
        )
    }

    fun restoreAllPrefs(context: Context, payload: BackupPayload) {
        val appContext = context.applicationContext
        val grouped = mutableMapOf<String, MutableMap<String, Any?>>()

        payload.prefs.forEach { (compositeKey, value) ->
            val split = compositeKey.split(PREF_KEY_SEPARATOR, limit = 2)
            if (split.size != 2) return@forEach
            grouped.getOrPut(split[0]) { linkedMapOf() }[split[1]] = value
        }

        backupPrefFiles.forEach { prefName ->
            val prefs = appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val editor = prefs.edit().clear()
            grouped[prefName]?.forEach { (key, rawTyped) ->
                applyTypedValue(editor, key, rawTyped)
            }
            editor.commit()
        }
    }

    private fun serializeValue(value: Any?): Map<String, Any?> {
        return when (value) {
            is String -> mapOf("type" to "string", "value" to value)
            is Boolean -> mapOf("type" to "boolean", "value" to value)
            is Int -> mapOf("type" to "int", "value" to value)
            is Long -> mapOf("type" to "long", "value" to value)
            is Float -> mapOf("type" to "float", "value" to value)
            is Set<*> -> mapOf("type" to "string_set", "value" to value.map { it.toString() })
            null -> mapOf("type" to "string", "value" to null)
            else -> mapOf("type" to "string", "value" to value.toString())
        }
    }

    private fun applyTypedValue(editor: SharedPreferences.Editor, key: String, rawTyped: Any?) {
        val typed = rawTyped as? Map<*, *> ?: return
        val type = typed["type"] as? String ?: "string"
        val value = typed["value"]
        when (type) {
            "boolean" -> if (value is Boolean) editor.putBoolean(key, value)
            "int" -> if (value is Number) editor.putInt(key, value.toInt())
            "long" -> if (value is Number) editor.putLong(key, value.toLong())
            "float" -> if (value is Number) editor.putFloat(key, value.toFloat())
            "string_set" -> {
                val set = when (value) {
                    is List<*> -> value.mapNotNull { it?.toString() }.toSet()
                    is Set<*> -> value.mapNotNull { it?.toString() }.toSet()
                    else -> emptySet()
                }
                editor.putStringSet(key, set)
            }
            else -> editor.putString(key, value?.toString())
        }
    }
}
