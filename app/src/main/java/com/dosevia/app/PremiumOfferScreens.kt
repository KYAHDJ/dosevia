package com.dosevia.app

import com.android.billingclient.api.ProductDetails
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val premiumGradient = Brush.linearGradient(
    listOf(Color(0xFFF609BC), Color(0xFFFAB86D))
)
private val premiumBg = Color(0xFFFFF0FB)
private val premiumText = Color(0xFF111827)
private val premiumSubtle = Color(0xFF6B7280)

@Composable
fun PremiumHubScreen(
    currentTier: UserTier,
    proDetails: ProductDetails?,
    lifetimeDetails: ProductDetails?,
    onBack: () -> Unit,
    onOpenPro: () -> Unit,
    onOpenLifetime: () -> Unit,
) {
    PremiumScaffold(title = "Unlock Premium", onBack = onBack) { isTablet, padH ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = padH, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Choose the plan that fits how you use Dosevia.",
                color = premiumSubtle,
                fontSize = if (isTablet) 14.sp else 13.sp,
                lineHeight = if (isTablet) 20.sp else 18.sp,
            )

            PlanPreviewCard(
                title = "Dosevia Pro",
                subtitle = "Best for stronger reminders and medium widget customization.",
                price = proDetails.subscriptionDisplayPrice(),
                badge = if (currentTier == UserTier.PRO) "ACTIVE" else null,
                imageRes = R.drawable.widget_preview_medium,
                points = listOf(
                    "Medium widget customization",
                    "Alarm screen icon",
                    "Notification sound"
                ),
                buttonLabel = if (currentTier == UserTier.PRO) {
                    "Viewing current plan"
                } else {
                    "See Pro details"
                },
                enabled = currentTier != UserTier.PRO,
                onClick = onOpenPro,
            )

            PlanPreviewCard(
                title = "Dosevia Lifetime",
                subtitle = "One payment for permanent premium access.",
                price = lifetimeDetails.oneTimeDisplayPrice(),
                badge = if (currentTier == UserTier.LIFETIME) "OWNED" else null,
                imageRes = R.drawable.widget_preview_calendar,
                points = listOf(
                    "Calendar widget access",
                    "Mark Taken or Not Taken from the widget",
                    "Everything in Pro included"
                ),
                buttonLabel = if (currentTier == UserTier.LIFETIME) {
                    "Already unlocked"
                } else {
                    "See Lifetime details"
                },
                enabled = currentTier != UserTier.LIFETIME,
                onClick = onOpenLifetime,
            )
        }
    }
}

@Composable
fun ProOfferScreen(
    currentTier: UserTier,
    productDetails: ProductDetails?,
    onBack: () -> Unit,
    onSelectPro: () -> Unit,
) {
    PremiumScaffold(title = "Dosevia Pro", onBack = onBack) { isTablet, padH ->
        val isAlready = currentTier == UserTier.PRO || currentTier == UserTier.LIFETIME

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = padH, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PremiumHeroCard(
                title = "Personalize every reminder",
                subtitle = "Unlock the Pro tools that make your reminders feel more personal and easier to notice every day.",
                price = "",
                tag = if (currentTier == UserTier.LIFETIME) {
                    "Lifetime already includes this"
                } else {
                    null
                },
                preview = {
                    Image(
                        painter = painterResource(R.drawable.widget_preview_medium),
                        contentDescription = "Dosevia Pro preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isTablet) 260.dp else 220.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            )

            MediumWidgetCustomizationShowcase()

            FeatureIconGrid(
                title = "What Pro includes",
                items = listOf(
                    PremiumFeatureItem("Alarm screen icon", Icons.Default.Alarm),
                    PremiumFeatureItem("Notification sound", Icons.Default.NotificationsActive),
                )
            )

            PurchaseActionCard(
                note = "Google Play handles subscription billing and renewals securely. Cancel anytime from the Play Store.",
                buttonLabel = when {
                    currentTier == UserTier.LIFETIME -> "Lifetime already unlocked"
                    isAlready -> "Already on Pro"
                    else -> productDetails.subscriptionDisplayPrice()
                        .takeIf { it.isNotBlank() }
                        ?: "Monthly plan"
                },
                enabled = !isAlready,
                onClick = onSelectPro,
            )
        }
    }
}

@Composable
fun LifetimeOfferScreen(
    currentTier: UserTier,
    productDetails: ProductDetails?,
    onBack: () -> Unit,
    onSelectLifetime: () -> Unit,
) {
    PremiumScaffold(title = "Dosevia Lifetime", onBack = onBack) { isTablet, padH ->
        val isAlready = currentTier == UserTier.LIFETIME

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = padH, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PremiumHeroCard(
                title = "Own your premium access once",
                subtitle = "Everything in Pro plus the calendar widget where you can mark Taken or Not Taken right from your home screen.",
                price = "",
                tag = if (isAlready) "Already unlocked" else null,
                preview = {
                    Image(
                        painter = painterResource(R.drawable.widget_preview_calendar),
                        contentDescription = "Dosevia Lifetime preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isTablet) 260.dp else 220.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            )

            FeatureIconGrid(
                title = "What Lifetime includes",
                items = listOf(
                    PremiumFeatureItem("Everything in Pro", Icons.Default.WorkspacePremium),
                    PremiumFeatureItem("Calendar widget", Icons.Default.CalendarMonth),
                    PremiumFeatureItem("Future premium updates", Icons.Default.AutoAwesome),
                    PremiumFeatureItem("One payment only", Icons.Default.CheckCircle),
                )
            )

            PurchaseActionCard(
                note = "Google Play handles the purchase securely. Restore it anytime with the same Play account.",
                buttonLabel = if (isAlready) {
                    "Already on Lifetime"
                } else {
                    productDetails.oneTimeBottomLabel()
                },
                enabled = !isAlready,
                onClick = onSelectLifetime,
            )
        }
    }
}

@Composable
private fun PremiumScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (isTablet: Boolean, padH: Dp) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(premiumBg)
    ) {
        val isTablet = maxWidth >= 480.dp
        val padH = if (isTablet) 32.dp else 16.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(premiumGradient)
                    .padding(top = 8.dp)
                    .padding(horizontal = padH, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                    Text(
                        text = title,
                        fontSize = if (isTablet) 22.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            content(isTablet, padH)
        }
    }
}

@Composable
private fun PlanPreviewCard(
    title: String,
    subtitle: String,
    price: String,
    badge: String?,
    imageRes: Int,
    points: List<String>,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.78f)) {
                    Text(
                        text = title,
                        color = premiumText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = subtitle,
                        color = premiumSubtle,
                        fontSize = 12.sp
                    )
                }

                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF111827))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Image(
                painter = painterResource(imageRes),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                contentScale = ContentScale.Fit
            )

            PricePill(price)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                points.forEach { FeatureRow(it) }
            }

            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827))
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun PremiumHeroCard(
    title: String,
    subtitle: String,
    price: String,
    tag: String?,
    preview: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (tag != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF111827))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tag,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = title,
                color = premiumText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
            Text(
                text = subtitle,
                color = premiumSubtle,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            preview()

            if (price.isNotBlank()) PricePill(price)
        }
    }
}

@Composable
private fun MediumWidgetCustomizationShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Medium widget customization preview",
                color = premiumText,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                text = "This preview mirrors the actual customization layout. It is display-only here so users can clearly see what Pro unlocks.",
                color = premiumSubtle,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6EAF6)),
                border = BorderStroke(1.dp, Color(0xFFEAD7E8))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MockMediumWidgetPreviewCard()
                    MockChangeRow("Background color", Color(0xFF4A86E8))
                    MockChangeRow("Accent / bar color", Color(0xFF16F25F))
                    MockChangeRow("Accent 2", Color(0xFFFF1B1B))
                    MockChangeRow("Primary text color", Color(0xFF0A1538))
                    MockChangeRow("Secondary text color", Color(0xFFF4A300))

                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Reset to Default")
                    }
                }
            }
        }
    }
}

@Composable
private fun MockMediumWidgetPreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A86E8))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Pill Statistics",
                color = Color(0xFF0B1020),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )

            MockStatisticBar(
                label = "Total Pills",
                value = "28",
                dotColor = Color(0xFFC8C8D4),
                rowColor = Color(0xFFF8F8FA),
                labelColor = Color(0xFFE1A531),
                valueColor = Color(0xFF0B1020)
            )
            MockStatisticBar(
                label = "Taken",
                value = "0",
                dotColor = Color(0xFF16F25F),
                rowColor = Color(0xFFE6F6EA),
                labelColor = Color(0xFFE1A531),
                valueColor = Color(0xFF16F25F)
            )
            MockStatisticBar(
                label = "Missed",
                value = "2",
                dotColor = Color(0xFFFF1B1B),
                rowColor = Color(0xFFF9ECEC),
                labelColor = Color(0xFFE1A531),
                valueColor = Color(0xFFFF1B1B)
            )
        }
    }
}

@Composable
private fun MockStatisticBar(
    label: String,
    value: String,
    dotColor: Color,
    rowColor: Color,
    labelColor: Color,
    valueColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(rowColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Text(
                text = label,
                color = labelColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private fun responsiveSettingLabel(label: String): String {
    val words = label.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.size <= 1 -> label
        else -> words.dropLast(1).joinToString(" ") + "\n" + words.last()
    }
}

@Composable
private fun MockChangeRow(
    label: String,
    color: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = responsiveSettingLabel(label),
                    color = premiumText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.width(104.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7054B8),
                    disabledContainerColor = Color(0xFF7054B8)
                )
            ) {
                Text("Change", color = Color.White, maxLines = 1)
            }
        }
    }
}

@Composable
private fun FeatureVisualCard(
    title: String,
    body: String,
    icon: ImageVector,
    imageRes: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(premiumGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        color = premiumText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = body,
                        color = premiumSubtle,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Image(
                painter = painterResource(imageRes),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private data class PremiumFeatureItem(
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun FeatureIconGrid(
    title: String,
    items: List<PremiumFeatureItem>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = premiumText,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            Card(
                                modifier = Modifier.width(160.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = Color(0xFF111827),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = item.label,
                                        color = premiumText,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (rowItems.size == 1) {
                            Spacer(Modifier.width(160.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseActionCard(
    note: String,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = note,
                color = premiumSubtle,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827))
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun PricePill(price: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(premiumGradient)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = price,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF16A34A),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = premiumText
        )
    }
}

private fun ProductDetails?.subscriptionDisplayPrice(): String {
    val pricingPhase = this
        ?.subscriptionOfferDetails
        ?.firstOrNull {
            it.offerToken.isNotBlank() && it.pricingPhases.pricingPhaseList.isNotEmpty()
        }
        ?.pricingPhases
        ?.pricingPhaseList
        ?.lastOrNull()

    return pricingPhase?.formattedPrice?.let { "$it / month" } ?: "Monthly plan"
}

private fun ProductDetails?.oneTimeDisplayPrice(): String {
    return this?.oneTimePurchaseOfferDetails?.formattedPrice ?: "One-time payment"
}

private fun ProductDetails?.oneTimeBottomLabel(): String {
    val price = this?.oneTimePurchaseOfferDetails?.formattedPrice
    return if (price.isNullOrBlank()) {
        "One-time payment"
    } else {
        "One-time payment • $price"
    }
}
