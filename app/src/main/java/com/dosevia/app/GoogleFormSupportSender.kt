package com.dosevia.app

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Minimal Google Form sender (no WebView). */
object GoogleFormSupportSender {

    fun send(
        topic: String,
        email: String,
        message: String,
    ): Result<Unit> {
        // If not configured, fail fast with a clear message.
        if (SupportFormConfig.isPlaceholder()) {
            return Result.failure(IllegalStateException("Support form is not configured."))
        }

        return runCatching {
            val url = URL(SupportFormConfig.FORM_RESPONSE_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent", "Dosevia")
                connectTimeout = 12_000
                readTimeout = 12_000
            }

            val data = buildFormBody(
                mapOf(
                    SupportFormConfig.ENTRY_TOPIC to topic,
                    SupportFormConfig.ENTRY_EMAIL to email,
                    SupportFormConfig.ENTRY_MESSAGE to message,
                )
            )

            BufferedWriter(OutputStreamWriter(conn.outputStream, Charsets.UTF_8)).use { out ->
                out.write(data)
                out.flush()
            }

            // Google Forms often responds with 200 or 302.
            val code = conn.responseCode
            conn.disconnect()
            if (code !in 200..399) {
                throw IllegalStateException("Server returned $code")
            }
        }
    }

    private fun buildFormBody(fields: Map<String, String>): String {
        return fields.entries
            .joinToString("&") { (k, v) ->
                val key = URLEncoder.encode(k, "UTF-8")
                val value = URLEncoder.encode(v, "UTF-8")
                "$key=$value"
            }
    }
}
