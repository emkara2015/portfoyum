package com.antigravity.networthtracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.antigravity.networthtracker.R
import com.antigravity.networthtracker.presentation.navigation.Screen
import com.antigravity.networthtracker.presentation.theme.TextGraySecondary

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val primaryColor = Color(0xFF3F8CFF) // AssetDash signature blue

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Tab 1: Ana Sayfa
        BottomNavItem(
            route = Screen.Dashboard.route,
            icon = Icons.Default.Home,
            labelRes = R.string.nav_home,
            isSelected = currentRoute == Screen.Dashboard.route,
            onClick = {
                if (currentRoute != Screen.Dashboard.route) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            }
        )

        // Tab 2: Portföy (using budget icon resource)
        BottomNavItem(
            route = Screen.NetWorthSummary.route,
            iconDrawableRes = R.drawable.ic_budget,
            labelRes = R.string.nav_portfolio,
            isSelected = currentRoute == Screen.NetWorthSummary.route,
            onClick = {
                if (currentRoute != Screen.NetWorthSummary.route) {
                    navController.navigate(Screen.NetWorthSummary.route) {
                        launchSingleTop = true
                    }
                }
            }
        )

        // Tab 3: Plus button in the center
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(primaryColor)
                .clickable {
                    navController.navigate(Screen.AddAsset.route)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(id = R.string.desc_add_asset),
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Tab 4: Piyasalar
        BottomNavItem(
            route = Screen.Markets.route,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            labelRes = R.string.nav_markets,
            isSelected = currentRoute == Screen.Markets.route,
            onClick = {
                if (currentRoute != Screen.Markets.route) {
                    navController.navigate(Screen.Markets.route) {
                        launchSingleTop = true
                    }
                }
            }
        )

        // Tab 5: Profil
        BottomNavItem(
            route = Screen.Profile.route,
            icon = Icons.Default.Person,
            labelRes = R.string.nav_profile,
            isSelected = currentRoute == Screen.Profile.route,
            onClick = {
                if (currentRoute != Screen.Profile.route) {
                    navController.navigate(Screen.Profile.route) {
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    route: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconDrawableRes: Int? = null,
    labelRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = Color(0xFF3F8CFF) // Selected tab color blue
    val unselectedColor = TextGraySecondary

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconDrawableRes != null) {
            Icon(
                painter = painterResource(id = iconDrawableRes),
                contentDescription = stringResource(id = labelRes),
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(22.dp)
                    .alpha(if (isSelected) 1f else 0.5f)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = labelRes),
                tint = if (isSelected) selectedColor else unselectedColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(id = labelRes),
            color = if (isSelected) selectedColor else unselectedColor,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
