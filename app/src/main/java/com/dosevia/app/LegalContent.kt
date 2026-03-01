package com.dosevia.app

/**
 * Lightweight, in-app legal text.
 *
 * Note: Keep wording accurate. Dosevia uses Google Sign-In and Google Drive (user's own storage)
 * for optional cloud sync, and AdMob for ads.
 */
object LegalContent {

    const val termsTitle = "Terms & Conditions"
    const val privacyTitle = "Privacy Policy"

    const val effectiveDate = "Effective Date: March 1, 2026"

    val termsText: String = """
$effectiveDate

Welcome to Dosevia.

By using this app, you agree to these Terms & Conditions.

1) Purpose
Dosevia is a personal medication tracking app that helps you set reminders, record your history, and keep notes.

2) Your Data & Ownership
• Your medication schedule, history, and notes belong to you.
• Dosevia does not sell your data.

3) Local Storage
Your data is stored on your device so the app can function offline.

4) Optional Cloud Sync (Google Drive)
If you choose to sign in with Google and enable sync:
• Dosevia saves and restores your data using your own Google Drive storage.
• The app stores your backup in your Google account (not on private developer servers).
• You control access to your Google account and Google Drive.

5) Ads
Dosevia may show ads via Google AdMob. Ad providers may collect device identifiers and related information to deliver and measure ads, according to their policies.

6) No Medical Advice
Dosevia does not provide medical advice. Always consult a licensed healthcare professional for medical decisions.

7) Limitation of Liability
You are responsible for verifying your schedule and reminders. The developer is not liable for missed doses, incorrect entries, or any outcomes from using the app.

8) Changes
We may update these terms. Continued use means you accept the updated terms.
""".trimIndent()

    val privacyText: String = """
$effectiveDate

Dosevia is designed to keep your information under your control.

What we collect
• Dosevia itself does not ask you to enter personal identity data to use the core features.
• If you sign in with Google, Google provides basic account information required for sign-in.

How your medication data is stored
• On-device: your schedules, history, and notes are saved locally on your device.
• Optional cloud backup: if you enable sync, Dosevia stores a backup in your own Google Drive.

How we use your data
• Your local and Google Drive backup data is used only to provide app features like saving, restoring, and syncing.
• We do not sell your medication data.

Ads (AdMob)
• When ads are enabled, Google AdMob may collect device identifiers and other information for ad delivery, measurement, and fraud prevention.

Data sharing
• Dosevia does not share your medication data with third parties.
• Ads are provided by Google; their SDK may process device information as described in Google’s policies.

Security
• Your cloud backup is protected by your Google account.
• Keep your device and Google account secure (screen lock, strong password, etc.).

Contact
You can contact support from the About & Help screen in the app.
""".trimIndent()
}
