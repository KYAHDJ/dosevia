package com.dosevia.app

/**
 * Google Form configuration for in-app Contact Support.
 *
 * This app submits directly to your Google Form's `formResponse` endpoint (no WebView).
 */
object SupportFormConfig {

    // Your Google Form endpoint (must end with /formResponse)
    const val FORM_RESPONSE_URL: String =
        "https://docs.google.com/forms/d/e/1FAIpQLSfGgP1v95pcjX6RF_ZcPJ9JdjKqBHl1SGf_hvsDJZuRo_h_cw/formResponse"

    // Field entry IDs from your prefilled URL
    // entry.678600213   -> Email (optional)
    // entry.726926682   -> Subject / Topic (required)
    // entry.1736286681  -> Message (required)
    const val ENTRY_EMAIL: String = "entry.678600213"
    const val ENTRY_TOPIC: String = "entry.726926682"
    const val ENTRY_MESSAGE: String = "entry.1736286681"

    fun isPlaceholder(): Boolean {
        return FORM_RESPONSE_URL.isBlank() ||
            ENTRY_TOPIC.isBlank() ||
            ENTRY_EMAIL.isBlank() ||
            ENTRY_MESSAGE.isBlank()
    }
}
