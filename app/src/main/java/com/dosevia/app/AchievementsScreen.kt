package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AchievementsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val userTier by viewModel.userTier.collectAsState()
    val achievements = AchievementsManager.getAchievements(context.applicationContext)
    val stats = AchievementsManager.getStats(context.applicationContext)
    val unlocked = achievements.count { it.unlocked }
    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF0FB))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { UiSoundPlayer.playBack(); onBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    text = "Achievements",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("Your progress", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111827))
                        Spacer(Modifier.height(10.dp))
                        Text("Unlocked $unlocked / 30 achievements", color = Color(0xFF6B7280))
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { unlocked / 30f },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatChip("Current streak", "${stats.currentStreak}d", Modifier.weight(1f))
                            StatChip("Best streak", "${stats.bestStreak}d", Modifier.weight(1f))
                            StatChip("Taken total", "${stats.totalTaken}", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatChip("Notes", "${stats.totalNotes}", Modifier.weight(1f))
                            StatChip("Syncs", "${stats.totalSyncs}", Modifier.weight(1f))
                            StatChip("Plan", userTier.name, Modifier.weight(1f))
                        }
                    }
                }
            }
            items(achievements) { item ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.unlocked) Color.White else Color(0xFFF9FAFB)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(
                                if (item.unlocked) Color(item.accent) else Color(0xFFE5E7EB)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontSize = 15.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(item.description, color = Color(0xFF6B7280), fontSize = 12.sp)
                        }
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(if (item.unlocked) "Unlocked" else "Locked") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.Start) {
            Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(label, color = Color(0xFF6B7280), fontSize = 11.sp)
        }
    }
}
