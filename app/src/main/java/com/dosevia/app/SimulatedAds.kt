package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SimulatedBannerAd(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFE5E7EB)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Banner Ad (simulation)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF111827)
        )
    }
}

/**
 * Full-screen 'App Open' ad simulation.
 * - Shows for [minSeconds] before the close button becomes active.
 */
@Composable
fun SimulatedAppOpenAd(
    minSeconds: Int = 5,
    onClose: () -> Unit
) {
    var remaining by remember { mutableIntStateOf(minSeconds) }
    val canClose = remaining <= 0

    LaunchedEffect(Unit) {
        remaining = minSeconds
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(if (canClose) 1f else 0.35f)
                        .clickable(enabled = canClose) { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Advertisement",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (canClose) "Tap ✕ to close" else "You can close in $remaining…",
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color(0xFF374151), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "App Open Ad Creative\n(simulation)",
                        color = Color.White
                    )
                }
            }

            // Leave space for the persistent banner (if you show it under the ad overlay)
            Spacer(Modifier.height(60.dp))
        }
    }
}
