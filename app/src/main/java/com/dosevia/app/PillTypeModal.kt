package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dosevia.app.ui.theme.*

@Composable
fun PillTypeModal(
    currentType: PillType,
    onClose: () -> Unit,
    onSelect: (PillType) -> Unit,
    onCustomSelect: () -> Unit
) {
    val categories = pillTypeOptions.map { it.category }.distinct()
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xF0FEF3F9), Color.White, Color(0xF0FEF9ED))
                            )
                        )
                        .fillMaxSize()
                ) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFEF3F9), Color(0xFFFEF9ED)))
                            )
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(PinkPrimary, OrangeAccent))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Medication, null, tint = Color.White,
                                    modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Select Your Pill Pack Type",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PinkPrimary
                                )
                                Text(
                                    "Choose the regimen that matches your prescription",
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, null, tint = Color(0xFF9CA3AF))
                            }
                        }
                    }

                    HorizontalDivider(color = PinkPrimary.copy(alpha = 0.2f))

                    // Scrollable list
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        categories.forEach { category ->
                            val options = pillTypeOptions.filter { it.category == category }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(Icons.Default.Info, null,
                                        tint = Color(0xFF4B5563),
                                        modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        category,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF374151)
                                    )
                                }

                                options.forEach { option ->
                                    val isSelected = currentType == option.value
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected)
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFFFFF0F9), Color(0xFFFFF6EC))
                                                    )
                                                else
                                                    Brush.horizontalGradient(
                                                        listOf(Color.White, Color.White)
                                                    )
                                            )
                                            .border(
                                                2.dp,
                                                if (isSelected) PinkPrimary.copy(alpha = 0.5f)
                                                else PinkPrimary.copy(alpha = 0.2f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                if (option.value == PillType.CUSTOM) {
                                                    onClose()
                                                    onCustomSelect()
                                                } else {
                                                    onSelect(option.value)
                                                    onClose()
                                                }
                                            }
                                            .padding(16.dp)
                                    ) {
                                        Row {
                                            // Selection indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected)
                                                            Brush.linearGradient(listOf(PinkPrimary, OrangeAccent))
                                                        else
                                                            Brush.linearGradient(listOf(Color.White, Color.White))
                                                    )
                                                    .border(
                                                        2.dp,
                                                        if (isSelected) PinkPrimary else Color(0xFFD1D5DB),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Spacer(Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    option.label,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSelected) PinkDark else Color(0xFF111827)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    option.description,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF6B7280),
                                                    lineHeight = 17.sp
                                                )
                                                option.brands?.let { brands ->
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        "Examples: $brands",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = PinkPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Info boxes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF))
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "💡  Medical Accuracy: All pill types are based on actual birth control regimens prescribed worldwide.",
                                fontSize = 12.sp,
                                color = Color(0xFF1E40AF)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFFBEB))
                                .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "⚠️  Important: Always follow your healthcare provider's instructions. If unsure which type you have, check your pill pack or consult your doctor.",
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }
    }
}
