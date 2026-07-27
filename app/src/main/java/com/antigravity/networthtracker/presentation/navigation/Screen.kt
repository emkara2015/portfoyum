package com.antigravity.networthtracker.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddAsset : Screen("add_asset?category={category}") {
        fun createRoute(category: String?) = if (category != null) "add_asset?category=$category" else "add_asset"
    }
    object SearchAsset : Screen("search_asset")
    object AssetDetail : Screen("asset_detail/{assetId}") {
        fun createRoute(assetId: Long) = "asset_detail/$assetId"
    }
    object NetWorthSummary : Screen("net_worth_summary")
    object Markets : Screen("markets")
    object Profile : Screen("profile")
}
