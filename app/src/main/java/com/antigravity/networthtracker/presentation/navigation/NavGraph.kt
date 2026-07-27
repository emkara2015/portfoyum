package com.antigravity.networthtracker.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.presentation.add_asset.AddAssetScreen
import com.antigravity.networthtracker.presentation.add_asset.AddAssetViewModel
import com.antigravity.networthtracker.presentation.asset_detail.AssetDetailScreen
import com.antigravity.networthtracker.presentation.asset_detail.AssetDetailViewModel
import com.antigravity.networthtracker.presentation.dashboard.DashboardScreen
import com.antigravity.networthtracker.presentation.dashboard.DashboardViewModel
import com.antigravity.networthtracker.presentation.markets.MarketsScreen
import com.antigravity.networthtracker.presentation.markets.MarketsViewModel
import com.antigravity.networthtracker.presentation.profile.ProfileScreen
import com.antigravity.networthtracker.presentation.profile.ProfileViewModel
import com.antigravity.networthtracker.presentation.summary.NetWorthSummaryScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(route = Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onAddAssetClick = { categoryType ->
                    navController.navigate(Screen.AddAsset.createRoute(categoryType?.name))
                },
                onAssetClick = { asset ->
                    navController.navigate(Screen.AssetDetail.createRoute(asset.id))
                },
                onChartClick = {
                    navController.navigate(Screen.NetWorthSummary.route)
                }
            )
        }
        
        composable(
            route = Screen.AddAsset.route,
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val categoryStr = backStackEntry.arguments?.getString("category")
            val viewModel: AddAssetViewModel = hiltViewModel()

            // Pre-select category if provided in the route
            LaunchedEffect(categoryStr) {
                categoryStr?.let {
                    try {
                        val type = AssetType.valueOf(it)
                        viewModel.selectAssetType(type)
                    } catch (e: Exception) {
                        // ignore invalid categories
                    }
                }
            }

            AddAssetScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.AssetDetail.route,
            arguments = listOf(
                navArgument("assetId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val assetId = backStackEntry.arguments?.getLong("assetId") ?: -1L
            val viewModel: AssetDetailViewModel = hiltViewModel()
            AssetDetailScreen(
                assetId = assetId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.NetWorthSummary.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            NetWorthSummaryScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.SearchAsset.route) {
            // Placeholder for SearchAssetScreen (Step 10)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Search Asset Screen Placeholder")
            }
        }

        composable(route = Screen.Markets.route) {
            val viewModel: MarketsViewModel = hiltViewModel()
            MarketsScreen(viewModel = viewModel)
        }

        composable(route = Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(viewModel = viewModel)
        }
    }
}
