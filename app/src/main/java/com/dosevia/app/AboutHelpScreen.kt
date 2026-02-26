package com.dosevia.app

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutHelpScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var isSending by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val emailTrim = email.trim()
    val isEmailValid = remember(emailTrim) {
        emailTrim.isEmpty() || isValidEmail(emailTrim)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("About & Help", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF0FB))
                .padding(pad)
                .verticalScroll(scroll)
                .padding(16.dp)
        ) {
            SectionHeader("ABOUT")
            InfoCard(
                icon = Icons.Default.Medication,
                iconTint = gradient,
                title = "Dosevia",
                body = "A simple pill tracker with alarms, history, notes, and widgets to help you stay consistent." )

            Spacer(Modifier.height(16.dp))

            SectionHeader("HELP")
            InfoCard(
                icon = Icons.Default.TipsAndUpdates,
                iconTint = Brush.linearGradient(listOf(Color(0xFF6200A0), Color(0xFFF609BC))),
                title = "Quick tips",
                body = "If widgets or alarms look out of sync, open the app once so it can refresh. Make sure battery optimisation is disabled for Dosevia." )

            Spacer(Modifier.height(16.dp))

            Text( "Contact Support",
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

                    // Email (optional)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (optional)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = !isEmailValid,
                        supportingText = {
                            if (!isEmailValid) {
                                Text("Use a real email like name@gmail.com / name@yahoo.com / name@outlook.com")
                            }
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    // Subject (required) — 50 words max
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

                    // Message (required) — 200 words max
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
                                    topic = subj, // mapped to your form's Subject field
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

                    Spacer(Modifier.height(8.dp))
                    Text( "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (toast != null) {
        LaunchedEffect(toast) {
            val msg = toast ?: return@LaunchedEffect
            toast = null
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

private fun isValidEmail(value: String): Boolean {
    // Basic email format check.
    if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) return false

    // Enforce common email domains to prevent random text.
    // (User asked: must end with @gmail.com or similar email providers.)
    val at = value.lastIndexOf('@')
    if (at <= 0 || at >= value.length - 3) return false
    val domain = value.substring(at + 1).lowercase()

    val allowedDomains = setOf( "gmail.com", "googlemail.com", "yahoo.com", "outlook.com", "hotmail.com", "live.com", "icloud.com", "me.com", "proton.me", "protonmail.com" )

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