package com.antigravity.networthtracker.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.networthtracker.R
import com.antigravity.networthtracker.data.local.entity.DailyNetWorthEntity
import com.antigravity.networthtracker.domain.model.NetWorthResult
import com.antigravity.networthtracker.presentation.theme.TextGraySecondary
import java.util.Locale

@Composable
fun BalanceCard(
    result: NetWorthResult,
    isValuesHidden: Boolean,
    onToggleValuesHidden: () -> Unit,
    onChartClick: () -> Unit,
    history: List<DailyNetWorthEntity>,
    modifier: Modifier = Modifier
) {
    // Calculate historical profits
    val firstNetWorthTry = history.firstOrNull()?.netWorthTry ?: result.netWorthTry
    val profitTry = result.netWorthTry - firstNetWorthTry
    val profitPct = if (firstNetWorthTry != 0.0) (profitTry / firstNetWorthTry) * 100.0 else 0.0

    val isPositive = profitTry >= 0
    val profitColor = if (isPositive) Color(0xFF089981) else Color(0xFFF23645)
    val profitSign = if (isPositive) "+" else ""
    val arrowText = if (isPositive) "↗" else "↘"

    // Net worth history values for sparkline
    val sparklineValues = history.map { it.netWorthTry }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Net Worth Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.total_net_worth),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGraySecondary,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isValuesHidden) "***,**" else formatCurrency(result.netWorthTry, "TRY"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Profit display: e.g. +₺1.250.230 (5.30%) ↗
                    Text(
                        text = if (isValuesHidden) "***,**" else String.format(
                            Locale.US,
                            "%s%s (%s%.2f%%) %s",
                            profitSign,
                            formatCurrency(profitTry, "TRY"),
                            profitSign,
                            profitPct,
                            arrowText
                        ),
                        color = profitColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // USD conversion and visibility toggle icon right next to it
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isValuesHidden) "***,**" else formatCurrency(result.netWorthUsd, "USD"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGraySecondary
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onToggleValuesHidden,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = if (isValuesHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Show/Hide values",
                                tint = TextGraySecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Right Column: Mini Sparkline Graph & Trend Badge
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    // Percentage change badge: ▲ 5.30%
                    val badgeSign = if (isPositive) "▲" else "▼"
                    Surface(
                        color = profitColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%s %.2f%%", badgeSign, kotlin.math.abs(profitPct)),
                            color = profitColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Mini-sparkline graph
                    if (sparklineValues.size >= 2) {
                        Sparkline(
                            values = sparklineValues,
                            color = profitColor,
                            modifier = Modifier
                                .width(120.dp)
                                .height(35.dp)
                        )
                    } else {
                        // Display a show chart navigation button if history is insufficient
                        IconButton(
                            onClick = onChartClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "Show net worth history",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom assets vs liabilities grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Assets Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.assets),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF089981),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isValuesHidden) "***,**" else formatCurrency(result.totalAssetsTry, "TRY"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isValuesHidden) "***,**" else formatCurrency(result.totalAssetsUsd, "USD"),
                        fontSize = 11.sp,
                        color = TextGraySecondary
                    )
                }

                // Vertical divider
                Box(
                    modifier = Modifier
                        .height(35.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Liabilities Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.liabilities),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF23645),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isValuesHidden) "***,**" else formatCurrency(result.totalLiabilitiesTry, "TRY"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isValuesHidden) "***,**" else formatCurrency(result.totalLiabilitiesUsd, "USD"),
                        fontSize = 11.sp,
                        color = TextGraySecondary
                    )
                }
            }
        }
    }
}

@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (values.size < 2) return@Canvas

        val maxVal = values.maxOrNull() ?: 1.0
        val minVal = values.minOrNull() ?: 0.0
        val valueRange = if (maxVal == minVal) 1.0 else maxVal - minVal

        val points = values.mapIndexed { index, value ->
            val x = index.toFloat() / (values.size - 1) * width
            val y = (1f - ((value - minVal) / valueRange).toFloat()) * height
            Offset(x, y)
        }

        // Draw area gradient
        val fillPath = Path().apply {
            moveTo(points.first().x, height)
            for (point in points) {
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw line
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

fun formatUnitPrice(amount: Double, currency: String): String {
    val locale = java.util.Locale.forLanguageTag("tr-TR")
    val nf = java.text.NumberFormat.getNumberInstance(locale) as java.text.DecimalFormat
    if (amount % 1.0 == 0.0) {
        nf.applyPattern("#,##0")
    } else {
        nf.applyPattern("#,##0.00###")
    }
    val absFormatted = nf.format(kotlin.math.abs(amount))
    val sign = if (amount < 0) "-" else ""
    return when (currency.uppercase()) {
        "TRY", "TL" -> "${sign}₺$absFormatted"
        "USD" -> "${sign}\$$absFormatted"
        "EUR" -> "${sign}€$absFormatted"
        else -> "$sign$absFormatted $currency"
    }
}

fun formatCurrency(amount: Double, currency: String): String {
    val locale = java.util.Locale.forLanguageTag("tr-TR")
    val nf = java.text.NumberFormat.getNumberInstance(locale) as java.text.DecimalFormat
    nf.applyPattern("#,##0")
    val absFormatted = nf.format(kotlin.math.round(kotlin.math.abs(amount)))
    val sign = if (amount < 0) "-" else ""
    
    return when (currency.uppercase()) {
        "TRY", "TL" -> "${sign}₺$absFormatted"
        "USD" -> "${sign}\$$absFormatted"
        "EUR" -> "${sign}€$absFormatted"
        else -> "$sign$absFormatted $currency"
    }
}
