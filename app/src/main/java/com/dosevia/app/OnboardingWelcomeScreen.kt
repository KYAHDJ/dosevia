package com.dosevia.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWelcomeScreen(
    onContinueSkip: () -> Unit,
    onContinueSignIn: () -> Unit,
) {
    val ctx = LocalContext.current
    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    var accepted by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF0FB), Color(0xFFFFF7FD), Color(0xFFFFFFFF))
                )
            )
            .padding(horizontal = 18.dp)
            .padding(top = 22.dp, bottom = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Logo + title entrance
            var showHeader by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { showHeader = true }

            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 6 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 6 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Dosevia",
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(22.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Dosevia",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Your pill tracker — local first, with optional Google Drive sync.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Continue",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF111827)
                    )
                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = onContinueSignIn,
                        enabled = accepted,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827))
                    ) {
                        Text("Sign in with Google")
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onContinueSkip,
                        enabled = accepted,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Skip for now")
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF9FAFB))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Checkbox(
                            checked = accepted,
                            onCheckedChange = { accepted = it }
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "I agree to the Terms & Privacy Policy",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "Your data stays on your device or in your own Google Drive if you enable sync.",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.alpha(0.95f)
                            )
                        }
                        Text(
                            text = "View",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF609BC),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable { showLegal = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Tip: You can sign in later from Settings → Sync.",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            Spacer(Modifier.weight(1f))

            // Small footer
            Text(
                text = "By continuing, you confirm you’ve read and accepted our Terms & Privacy Policy.",
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }

    if (showLegal) {
        LegalDialog(onDismiss = { showLegal = false })
    }
}

@Composable
private fun LegalDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Terms & Privacy") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(LegalContent.termsTitle, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(LegalContent.termsText, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Text(LegalContent.privacyTitle, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(LegalContent.privacyText, fontSize = 12.sp)
            }
        }
    )
}
