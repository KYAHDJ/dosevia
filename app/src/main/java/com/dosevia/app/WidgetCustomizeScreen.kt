package com.dosevia.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WidgetCustomizeScreen(
    onBack: () -> Unit,
    onSelectWidget: (WidgetKind) -> Unit,
    userTier: UserTier
) {
    // Use explicit state objects to avoid requiring Kotlin property delegate imports (getValue/setValue).
    val showProDialog = remember { mutableStateOf(false) }
    val showLifetimeDialog = remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF0FB))) {
        val isTablet = maxWidth >= 480.dp
        val padH = if (isTablet) 32.dp else 16.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(top = 8.dp)
                    .padding(horizontal = padH, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Text(
                        text = "Customize Widgets",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet) 22.sp else 18.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = padH, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WidgetCardButton(
                    label = "Small Widget",
                    imageRes = R.drawable.widget_preview_image,
                    badge = null,
                    alpha = 1f
                ) {
                    onSelectWidget(WidgetKind.SMALL)
                }
                WidgetCardButton(
                    label = "Medium Widget",
                    imageRes = R.drawable.widget_preview_medium,
                    badge = if (userTier == UserTier.FREE) "PRO" else null,
                    alpha = if (userTier == UserTier.FREE) 0.45f else 1f
                ) {
                    if (userTier == UserTier.FREE) showProDialog.value = true
                    else onSelectWidget(WidgetKind.MEDIUM)
                }
                WidgetCardButton(
                    label = "Calendar Widget",
                    imageRes = R.drawable.widget_preview_calendar,
                    badge = if (userTier != UserTier.LIFETIME) "LIFETIME" else null,
                    alpha = if (userTier != UserTier.LIFETIME) 0.45f else 1f
                ) {
                    if (userTier != UserTier.LIFETIME) showLifetimeDialog.value = true
                    else onSelectWidget(WidgetKind.CALENDAR)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showProDialog.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showProDialog.value = false },
            title = { Text("PRO required") },
            text = { Text("Widget customization is a PRO feature.\n\nTip: Go to Settings → Premium to preview PRO/LIFETIME while Billing is not added yet.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showProDialog.value = false }) { Text("OK") }
            }
        )
    }

    if (showLifetimeDialog.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLifetimeDialog.value = false },
            title = { Text("LIFETIME required") },
            text = { Text("The Calendar widget is LIFETIME-only.\n\nTip: Go to Settings → Premium to preview LIFETIME while Billing is not added yet.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showLifetimeDialog.value = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun WidgetCardButton(
    label: String,
    imageRes: Int,
    badge: String? = null,
    alpha: Float = 1f,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .alpha(alpha)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF111827))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = label,
                modifier = Modifier.fillMaxWidth().height(190.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF111827),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
