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

@Composable
fun AssetItemRow(
    calculatedAsset: CalculatedAsset,
    onClick: () -> Unit,
    isValuesHidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    val asset = calculatedAsset.asset
    
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
                color = Color.White
            )
            if (asset.isAutoUpdate) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val unitPriceText = if (isValuesHidden) "***,**" else formatUnitPrice(calculatedAsset.currentPrice, asset.currency)
                    Text(
                        text = unitPriceText,
                        fontSize = 10.sp,
                        color = TextGraySecondary
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
            } else if (calculatedAsset.profitLoss != 0.0) {
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
                color = Color.White
            )
            if (asset.type == AssetType.FUND && calculatedAsset.stopajAmount > 0) {
                val taxVal = calculatedAsset.taxPercent ?: 0.0
                val formattedTax = if (taxVal % 1.0 == 0.0) {
                    String.format(Locale.US, "%.0f", taxVal)
                } else {
                    String.format(Locale.US, "%.1f", taxVal)
                }
                Text(
                    text = stringResource(id = R.string.label_net_badge_item, formattedTax),
                    fontSize = 9.sp,
                    color = TextGraySecondary
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
    return if (number == number.toLong().toDouble()) {
        String.format(Locale.US, "%d", number.toLong())
    } else {
        String.format(Locale.US, "%,.4f", number)
    }
}
