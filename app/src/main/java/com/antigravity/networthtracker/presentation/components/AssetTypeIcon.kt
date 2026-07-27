package com.antigravity.networthtracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.antigravity.networthtracker.R
import com.antigravity.networthtracker.domain.model.AssetType

@Composable
fun AssetTypeIcon(
    type: AssetType,
    modifier: Modifier = Modifier,
    boxSize: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    if (type == AssetType.STOCK) {
        Box(
            modifier = modifier
                .size(boxSize)
                .clip(CircleShape)
                .background(Color(0xFF089981).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_stock),
                contentDescription = type.name,
                tint = Color.Unspecified,
                modifier = Modifier.size(iconSize)
            )
        }
    } else if (type == AssetType.CRYPTO) {
        Box(
            modifier = modifier
                .size(boxSize)
                .clip(CircleShape)
                .background(Color(0xFFF2A900).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_crypto),
                contentDescription = type.name,
                tint = Color.Unspecified,
                modifier = Modifier.size(iconSize)
            )
        }
    } else {
        val (icon, color) = when (type) {
            AssetType.STOCK -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF089981) // won't be reached
            AssetType.CRYPTO -> Icons.Default.Refresh to Color(0xFFF2A900) // won't be reached
            AssetType.METAL -> Icons.Default.Star to Color(0xFFD4AF37)
            AssetType.FUND -> Icons.AutoMirrored.Filled.ShowChart to Color(0xFF9C27B0)
            AssetType.EUROBOND -> Icons.Default.Lock to Color(0xFF3F51B5)
            AssetType.CASH -> Icons.Default.AccountBalance to Color(0xFF4CAF50)
            AssetType.REAL_ESTATE -> Icons.Default.Home to Color(0xFFFF5722)
            AssetType.VEHICLE -> Icons.Default.DirectionsCar to Color(0xFF607D8B)
            AssetType.BES -> Icons.Default.Info to Color(0xFF00BCD4)
            AssetType.DEBT -> Icons.Default.Warning to Color(0xFFF23645)
        }

        Box(
            modifier = modifier
                .size(boxSize)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.name,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
