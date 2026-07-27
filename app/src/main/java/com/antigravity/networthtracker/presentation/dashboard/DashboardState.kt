package com.antigravity.networthtracker.presentation.dashboard

import com.antigravity.networthtracker.domain.model.CalculatedAsset
import com.antigravity.networthtracker.domain.model.NetWorthResult

data class DashboardState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isInitialLoadComplete: Boolean = false,
    val netWorthResult: NetWorthResult? = null,
    val assets: List<CalculatedAsset> = emptyList(),
    val errorMessage: String? = null
)
