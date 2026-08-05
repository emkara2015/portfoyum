package com.antigravity.networthtracker.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.networthtracker.domain.model.CalculatedAsset
import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.presentation.theme.TextGraySecondary
import com.antigravity.networthtracker.presentation.theme.TradingViewGreen
import com.antigravity.networthtracker.presentation.theme.TradingViewRed
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.antigravity.networthtracker.R

import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import com.antigravity.networthtracker.domain.model.getLocalizedDisplayName

import com.antigravity.networthtracker.presentation.theme.AccordionItemTextPrimary
import com.antigravity.networthtracker.presentation.theme.AccordionItemTextSecondary

@Composable
fun AssetItemRow(
    calculatedAsset: CalculatedAsset,
    onClick: () -> Unit,
    isValuesHidden: Boolean = false,
    usdRate: Double = 0.0,
    eurRate: Double = 0.0,
    isLightBg: Boolean = false,
    modifier: Modifier = Modifier
) {
    val asset = calculatedAsset.asset
    val primaryTextColor = if (isLightBg) AccordionItemTextPrimary else Color.White
    val secondaryTextColor = if (isLightBg) AccordionItemTextSecondary else TextGraySecondary
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetTypeIcon(
            type = asset.type,
            boxSize = 20.dp,
            iconSize = 10.dp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = calculatedAsset.getLocalizedDisplayName(LocalContext.current),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )
            if (asset.type == AssetType.FUND) {
                val hasNote = asset.name.isNotBlank() && asset.name != asset.symbol
                if (hasNote) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = asset.name,
                        fontSize = 10.sp,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            } else if (asset.isAutoUpdate) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val unitPriceText = if (isValuesHidden) "***,**" else formatUnitPrice(calculatedAsset.currentPrice, asset.currency)
                    Text(
                        text = unitPriceText,
                        fontSize = 10.sp,
                        color = secondaryTextColor
                    )
                    
                    val dailyChange = calculatedAsset.dailyChangePercentage
                    if (dailyChange != 0.0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val sign = if (dailyChange > 0) "+" else ""
                        val isGood = if (asset.isLiability) dailyChange < 0 else dailyChange > 0
                        val color = if (isGood) TradingViewGreen else TradingViewRed
                        Text(
                            text = String.format(Locale.US, "%s%.2f%%", sign, dailyChange),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            } else if (calculatedAsset.profitLoss != 0.0 && !asset.isLiability) {
                Spacer(modifier = Modifier.height(2.dp))
                val absProfit = kotlin.math.abs(calculatedAsset.profitLoss)
                val profitText = if (isValuesHidden) "***,**" else formatCurrency(absProfit, asset.currency)
                val isProfit = calculatedAsset.profitLoss > 0
                val sign = if (isProfit && !isValuesHidden) "+" else if (!isValuesHidden) "-" else ""
                val color = if (isProfit) TradingViewGreen else TradingViewRed
                Text(
                    text = "${stringResource(id = R.string.label_profit)}: $sign$profitText",
                    fontSize = 10.sp,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            val displayValue = if (asset.type == AssetType.FUND) calculatedAsset.netCurrentValue else calculatedAsset.currentValue
            Text(
                text = if (isValuesHidden) "***,**" else formatCurrency(displayValue, asset.currency),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )
            if (asset.currency.uppercase() !in listOf("TRY", "TL")) {
                val rate = when (asset.currency.uppercase()) {
                    "USD" -> usdRate
                    "EUR" -> eurRate
                    else -> 0.0
                }
                if (rate > 0.0) {
                    val tryEquivalent = displayValue * rate
                    Text(
                        text = if (isValuesHidden) "***,**" else formatCurrency(tryEquivalent, "TRY"),
                        fontSize = 10.sp,
                        color = secondaryTextColor
                    )
                }
            }
            if (asset.type == AssetType.FUND) {
                val grossProfit = calculatedAsset.profitLoss
                val absGrossProfit = kotlin.math.abs(grossProfit)
                val grossSign = if (grossProfit > 0) "+" else if (grossProfit < 0) "-" else ""
                val grossText = if (isValuesHidden) "***,**" else formatCurrency(absGrossProfit, asset.currency)
                Text(
                    text = "${stringResource(id = R.string.label_gross_return)}: $grossSign$grossText",
                    fontSize = 9.5.sp,
                    color = secondaryTextColor
                )
            }
        }
        
        val plPercent = if (asset.type == AssetType.FUND) calculatedAsset.netProfitLossPercentage else calculatedAsset.profitLossPercentage
        val badgeColor = when {
            plPercent > 0.0 -> TradingViewGreen
            plPercent < 0.0 -> TradingViewRed
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        val badgeText = when {
            plPercent > 0.0 -> String.format("+%.2f%%", plPercent)
            plPercent < 0.0 -> String.format("%.2f%%", plPercent)
            else -> "0.00%"
        }
        
        Box(
            modifier = Modifier
                .width(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun formatNumber(number: Double): String {
    val locale = java.util.Locale.forLanguageTag("tr-TR")
    return if (number == number.toLong().toDouble()) {
        String.format(locale, "%d", number.toLong())
    } else {
        String.format(locale, "%,.4f", number)
    }
}
