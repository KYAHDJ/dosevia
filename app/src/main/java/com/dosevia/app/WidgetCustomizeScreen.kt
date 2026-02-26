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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onSelectWidget: (WidgetKind) -> Unit
) {
    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF0FB))) {
        val isTablet = maxWidth >= 480.dp
        val padH = if (isTablet) 32.dp else 16.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .statusBarsPadding()
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
                WidgetCardButton("Small Widget", R.drawable.widget_preview_image) {
                    onSelectWidget(WidgetKind.SMALL)
                }
                WidgetCardButton("Medium Widget", R.drawable.widget_preview_medium) {
                    onSelectWidget(WidgetKind.MEDIUM)
                }
                WidgetCardButton("Calendar Widget", R.drawable.widget_preview_calendar) {
                    onSelectWidget(WidgetKind.CALENDAR)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun WidgetCardButton(label: String, imageRes: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
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
