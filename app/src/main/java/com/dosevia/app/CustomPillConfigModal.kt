package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dosevia.app.ui.theme.*

@Composable
fun CustomPillConfigModal(
    onClose: () -> Unit,
    onSave: (active: Int, placebo: Int, lowDose: Int) -> Unit
) {
    var activePills by remember { mutableStateOf(21) }
    var placeboPills by remember { mutableStateOf(7) }
    var lowDosePills by remember { mutableStateOf(0) }
    val total = activePills + placeboPills + lowDosePills

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFEF3F9), Color.White, Color(0xFFFEF9ED)))
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PinkPrimary, OrangeAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Medication, null, tint = Color.White,
                                modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Custom Pill Configuration",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PinkPrimary
                            )
                            Text(
                                "Configure your pill pack exactly how you need it",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    // Active Pills row
                    PillCountRow(
                        label = "Active Pills",
                        subtitle = "Hormone-containing pills",
                        value = activePills,
                        valueColor = Brush.linearGradient(listOf(PinkPrimary, OrangeAccent)),
                        onDecrement = { if (activePills > 0) activePills-- },
                        onIncrement = { if (activePills < 365) activePills++ }
                    )

                    // Placebo Pills row
                    PillCountRow(
                        label = "Placebo Pills",
                        subtitle = "No hormones (period week)",
                        value = placeboPills,
                        valueColor = Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFF6B7280))),
                        onDecrement = { if (placeboPills > 0) placeboPills-- },
                        onIncrement = { if (placeboPills < 28) placeboPills++ }
                    )

                    // Low-Dose Pills row
                    PillCountRow(
                        label = "Low-Dose Pills",
                        subtitle = "Low hormone (instead of placebo)",
                        value = lowDosePills,
                        valueColor = Brush.linearGradient(listOf(LowDoseAmber, Color(0xFFF59E0B))),
                        onDecrement = { if (lowDosePills > 0) lowDosePills-- },
                        onIncrement = { if (lowDosePills < 28) lowDosePills++ }
                    )

                    // Total summary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFF0F9), Color(0xFFFFF6EC)))
                            )
                            .border(2.dp, PinkPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Total Pills:", fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp, color = Color(0xFF111827))
                                Text("$activePills active + $placeboPills placebo + $lowDosePills low-dose",
                                    fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                            Text(
                                "$total",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = PinkPrimary
                            )
                        }
                    }

                    // Info box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "💡  Set this to match your exact prescription. Most packs are 21-28 pills total. Extended cycles can be up to 365 pills.",
                            fontSize = 12.sp,
                            color = Color(0xFF1E40AF)
                        )
                    }

                    // Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (activePills < 1) return@Button
                                onSave(activePills, placeboPills, lowDosePills)
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(PinkPrimary, OrangeAccent)))
                        ) {
                            Text("Save Configuration", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillCountRow(
    label: String,
    subtitle: String,
    value: Int,
    valueColor: Brush,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFFFBCFE8), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = Color(0xFF111827))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrement
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB))
                ) {
                    Icon(Icons.Default.Remove, null,
                        tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                }

                // Value display
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(valueColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$value",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Increment
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB))
                ) {
                    Icon(Icons.Default.Add, null,
                        tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
