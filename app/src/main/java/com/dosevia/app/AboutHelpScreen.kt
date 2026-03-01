package com.dosevia.app

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosevia.app.ui.theme.OrangeAccent
import com.dosevia.app.ui.theme.PinkLight
import com.dosevia.app.ui.theme.PinkPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutHelpScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val headerGradient = Brush.linearGradient(listOf(PinkPrimary, OrangeAccent))

    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var isSending by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val emailTrim = email.trim()
    val isEmailValid = remember(emailTrim) {
        emailTrim.isEmpty() || isValidEmail(emailTrim)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PinkLight)
    ) {
        val isTablet = maxWidth >= 480.dp
        val padH = if (isTablet) 32.dp else 16.dp
        val titleSp = if (isTablet) 22.sp else 18.sp

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(top = 8.dp)
                    .padding(horizontal = padH, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(if (isTablet) 28.dp else 22.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "About & Help",
                        color = Color.White,
                        fontSize = titleSp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = padH, vertical = 16.dp)
            ) {
                SectionHeader("ABOUT")
                InfoCard(
                    icon = Icons.Default.Medication,
                    iconTint = headerGradient,
                    title = "Dosevia",
                    body = "A simple pill tracker with alarms, history, notes, and widgets to help you stay consistent."
                )

                Spacer(Modifier.height(16.dp))

                SectionHeader("LEGAL")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        LegalRow(
                            icon = Icons.Default.Gavel,
                            title = "Terms & Conditions",
                            subtitle = "How Dosevia works and your responsibilities",
                            onClick = { showTerms = true }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        LegalRow(
                            icon = Icons.Default.PrivacyTip,
                            title = "Privacy Policy",
                            subtitle = "How your data is stored (device + optional Google Drive)",
                            onClick = { showPrivacy = true }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionHeader("HELP")
                InfoCard(
                    icon = Icons.Default.TipsAndUpdates,
                    iconTint = Brush.linearGradient(listOf(Color(0xFF7C3AED), PinkPrimary)),
                    title = "Quick tips",
                    body = "If widgets or alarms look out of sync, open the app once so it can refresh. Make sure battery optimisation is disabled for Dosevia."
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Contact Support",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email (optional)") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            isError = !isEmailValid,
                            supportingText = {
                                if (!isEmailValid) {
                                    Text("Use a real email like name@gmail.com / name@yahoo.com / name@outlook.com")
                                }
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = subject,
                            onValueChange = { new ->
                                if (wordCount(new) <= 50) subject = new
                            },
                            label = { Text("Subject (required)") },
                            leadingIcon = { Icon(Icons.Default.Subject, contentDescription = null) },
                            singleLine = true,
                            supportingText = {
                                Text("${wordCount(subject)}/50 words")
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = message,
                            onValueChange = { new ->
                                if (wordCount(new) <= 200) message = new
                            },
                            label = { Text("Message (required)") },
                            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) },
                            minLines = 5,
                            supportingText = {
                                Text("${wordCount(message)}/200 words")
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (isSending) return@Button

                                val subj = subject.trim()
                                val msg = message.trim()

                                if (!isEmailValid) {
                                    toast = "Please fix your email format."
                                    return@Button
                                }
                                if (subj.isEmpty()) {
                                    toast = "Subject is required."
                                    return@Button
                                }
                                if (msg.isEmpty()) {
                                    toast = "Message is required."
                                    return@Button
                                }
                                if (wordCount(subj) > 50) {
                                    toast = "Subject must be 50 words or less."
                                    return@Button
                                }
                                if (wordCount(msg) > 200) {
                                    toast = "Message must be 200 words or less."
                                    return@Button
                                }

                                isSending = true
                                scope.launch(Dispatchers.IO) {
                                    val result = GoogleFormSupportSender.send(
                                        topic = subj,
                                        email = emailTrim,
                                        message = msg
                                    )
                                    launch(Dispatchers.Main) {
                                        isSending = false
                                        if (result.isSuccess) {
                                            subject = ""
                                            message = ""
                                            toast = "Sent! Thanks — we’ll review it."
                                        } else {
                                            toast = result.exceptionOrNull()?.message ?: "Failed to send."
                                        }
                                    }
                                }
                            },
                            enabled = !isSending,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Sending…")
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Send")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (toast != null) {
        LaunchedEffect(toast) {
            val msg = toast ?: return@LaunchedEffect
            toast = null
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (showTerms) {
        LegalTextDialog(
            title = LegalContent.termsTitle,
            body = LegalContent.termsText,
            onDismiss = { showTerms = false }
        )
    }
    if (showPrivacy) {
        LegalTextDialog(
            title = LegalContent.privacyTitle,
            body = LegalContent.privacyText,
            onDismiss = { showPrivacy = false }
        )
    }
}

@Composable
private fun LegalRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF0FB)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFF609BC))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun LegalTextDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(body, fontSize = 12.sp)
            }
        }
    )
}

private fun isValidEmail(value: String): Boolean {
    if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) return false

    val at = value.lastIndexOf('@')
    if (at <= 0 || at >= value.length - 3) return false
    val domain = value.substring(at + 1).lowercase()

    val allowedDomains = setOf(
        "gmail.com", "googlemail.com", "yahoo.com", "outlook.com", "hotmail.com",
        "live.com", "icloud.com", "me.com", "proton.me", "protonmail.com"
    )

    return domain in allowedDomains
}

private fun wordCount(text: String): Int {
    val t = text.trim()
    if (t.isEmpty()) return 0
    return t.split(Regex("\\s+")).count { it.isNotBlank() }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Brush,
    title: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
