package com.antigravity.networthtracker.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.networthtracker.R
import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.domain.model.CalculatedAsset
import com.antigravity.networthtracker.presentation.theme.TextGraySecondary
import java.util.Locale

@Composable
fun CategoryHeader(
    type: AssetType,
    assets: List<CalculatedAsset>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    usdRate: Double,
    eurRate: Double,
    totalPortfolioAssetsTry: Double,
    isValuesHidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalTry = assets.sumOf { calculatedAsset ->
        val asset = calculatedAsset.asset
        val rate = when (asset.currency.uppercase()) {
            "USD" -> usdRate
            "EUR" -> eurRate
            else -> 1.0
        }
        val currentValue = if (asset.type == AssetType.FUND) calculatedAsset.netCurrentValue else calculatedAsset.currentValue
        currentValue * rate
    }
    val totalUsd = totalTry / usdRate

    val (_, color) = when (type) {
        AssetType.STOCK -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF089981)
        AssetType.CRYPTO -> Icons.Default.Refresh to Color(0xFFF2A900)
        AssetType.METAL -> Icons.Default.Star to Color(0xFFD4AF37)
        AssetType.FUND -> Icons.AutoMirrored.Filled.ShowChart to Color(0xFF9C27B0)
        AssetType.EUROBOND -> Icons.Default.Lock to Color(0xFF3F51B5)
        AssetType.CASH -> Icons.Default.AccountBalance to Color(0xFF4CAF50)
        AssetType.REAL_ESTATE -> Icons.Default.Home to Color(0xFFFF5722)
        AssetType.VEHICLE -> Icons.Default.DirectionsCar to Color(0xFF607D8B)
        AssetType.BES -> Icons.Default.Info to Color(0xFF00BCD4)
        AssetType.DEBT -> Icons.Default.Warning to Color(0xFFF23645)
    }

    val percentage = if (totalPortfolioAssetsTry > 0.0) {
        (totalTry / totalPortfolioAssetsTry) * 100.0
    } else {
        0.0
    }

    val weightedChangePercent = remember(assets, usdRate, eurRate, type) {
        val autoUpdateAssets = assets.filter { it.asset.isAutoUpdate }
        if (autoUpdateAssets.isNotEmpty()) {
            var totalWeightValue = 0.0
            var weightedSum = 0.0
            for (calculatedAsset in autoUpdateAssets) {
                val asset = calculatedAsset.asset
                val valueTry = when (asset.currency.uppercase()) {
                    "USD" -> calculatedAsset.currentValue * usdRate
                    "EUR" -> calculatedAsset.currentValue * eurRate
                    else -> calculatedAsset.currentValue
                }
                
                if (valueTry > 0.0) {
                    totalWeightValue += valueTry
                    val dailyChange = if (asset.isLiability) -calculatedAsset.dailyChangePercentage else calculatedAsset.dailyChangePercentage
                    weightedSum += valueTry * dailyChange
                }
            }
            if (totalWeightValue > 0.0) {
                weightedSum / totalWeightValue
            } else {
                0.0
            }
        } else {
            var totalCurrentValueTry = 0.0
            var totalCostTry = 0.0
            var isLiabilityGroup = (type == AssetType.DEBT)
            for (calculatedAsset in assets) {
                val asset = calculatedAsset.asset
                if (asset.isLiability) isLiabilityGroup = true
                val valueTry = when (asset.currency.uppercase()) {
                    "USD" -> calculatedAsset.currentValue * usdRate
                    "EUR" -> calculatedAsset.currentValue * eurRate
                    else -> calculatedAsset.currentValue
                }
                val costTry = when (asset.currency.uppercase()) {
                    "USD" -> calculatedAsset.totalCost * usdRate
                    "EUR" -> calculatedAsset.totalCost * eurRate
                    else -> calculatedAsset.totalCost
                }
                totalCurrentValueTry += valueTry
                totalCostTry += costTry
            }
            if (totalCostTry > 0.0) {
                if (isLiabilityGroup) {
                    ((totalCostTry - totalCurrentValueTry) / totalCostTry) * 100.0
                } else {
                    ((totalCurrentValueTry - totalCostTry) / totalCostTry) * 100.0
                }
            } else {
                0.0
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Circle Category Icon
        AssetTypeIcon(
            type = type,
            boxSize = 40.dp,
            iconSize = 20.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Middle: Category Name & Weight Progress Bar
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = type.getLocalizedNameRes()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (weightedChangePercent != 0.0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    val sign = if (weightedChangePercent > 0) "+" else ""
                    val text = String.format(Locale.US, "(%s%.1f%%)", sign, weightedChangePercent)
                    val changeColor = if (weightedChangePercent > 0) Color(0xFF089981) else Color(0xFFF23645)
                    Text(
                        text = text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = changeColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Progress Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (kotlin.math.abs(percentage) / 100.0).coerceIn(0.0, 1.0).toFloat())
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val pctText = if (type == AssetType.DEBT) {
                    String.format(Locale.US, "-%.1f%%", kotlin.math.abs(percentage))
                } else {
                    String.format(Locale.US, "%.1f%%", percentage)
                }

                Text(
                    text = pctText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGraySecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right: Value texts
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = if (isValuesHidden) "***,**" else formatCurrency(totalTry, "TRY"),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (type == AssetType.DEBT) Color(0xFFF23645) else Color.White
            )
            Text(
                text = if (isValuesHidden) "***,**" else formatCurrency(totalUsd, "USD"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextGraySecondary
            )
        }

        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            label = "rotation"
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = TextGraySecondary,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotationAngle)
        )
    }
}
