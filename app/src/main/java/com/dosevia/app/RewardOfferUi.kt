package com.dosevia.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

object RewardOfferManager {
    private const val PREFS = "reward_offer_prefs"
    private const val KEY_LAUNCH_COUNT = "launch_count"
    private const val KEY_TAKEN_COUNT = "taken_count"
    private const val KEY_LAST_OFFER_TIME = "last_offer_time"

    private const val LAUNCH_THRESHOLD = 3
    private const val TAKEN_THRESHOLD = 2
    private const val OFFER_COOLDOWN_MS = 2 * 60 * 60 * 1000L

    fun onAppOpened(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_LAUNCH_COUNT, next).apply()
    }

    fun onTakenCompleted(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_TAKEN_COUNT, 0) + 1
        prefs.edit().putInt(KEY_TAKEN_COUNT, next).apply()
    }

    fun markShown(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_OFFER_TIME, System.currentTimeMillis())
            .putInt(KEY_LAUNCH_COUNT, 0)
            .putInt(KEY_TAKEN_COUNT, 0)
            .apply()
    }

    fun shouldShow(
        context: Context,
        isFreeUser: Boolean,
        adFreeRewardActive: Boolean,
        isSignedInPremium: Boolean = false
    ): Boolean {
        if (!isFreeUser) return false
        if (adFreeRewardActive) return false
        if (isSignedInPremium) return false

        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastOffer = prefs.getLong(KEY_LAST_OFFER_TIME, 0L)
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val takenCount = prefs.getInt(KEY_TAKEN_COUNT, 0)

        val cooldownPassed = System.currentTimeMillis() - lastOffer >= OFFER_COOLDOWN_MS
        if (!cooldownPassed) return false

        return launchCount >= LAUNCH_THRESHOLD || takenCount >= TAKEN_THRESHOLD
    }

    fun randomMessage(): String {
        val messages = listOf(
            "Enjoy Dosevia without ads for the next 24 hours.",
            "Take a little break from ads today.",
            "Watch one quick ad and enjoy a smoother day.",
            "Unlock a softer, cleaner Dosevia experience for 24 hours.",
            "Take a small pause from ads and enjoy your routine in peace.",
            "One short ad can give you a full day without interruptions.",
            "Make today feel lighter with 24 hours of ad-free Dosevia.",
            "Treat yourself to a calmer app experience for the next 24 hours."
        )
        return messages[Random.nextInt(messages.size)]
    }
}

@Composable
fun RewardOfferDialog(
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit
) {
    val message = remember { RewardOfferManager.randomMessage() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = null,
        text = {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFF609BC), Color(0xFFFAB86D))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "No Ads for 24 Hours",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = message,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Skip",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onWatchAd,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF609BC)
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 0.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = "Watch Ad",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "You can still keep using Dosevia normally",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

fun formatAdFreeRemaining(untilMs: Long): String {
    val remaining = (untilMs - System.currentTimeMillis()).coerceAtLeast(0L)
    val totalMinutes = remaining / 1000L / 60L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return "${hours}h ${minutes}m"
}