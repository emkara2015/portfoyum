package com.antigravity.networthtracker.domain.model

data class NetWorthResult(
    val totalAssetsTry: Double,
    val totalLiabilitiesTry: Double,
    val netWorthTry: Double,
    val totalAssetsUsd: Double,
    val totalLiabilitiesUsd: Double,
    val netWorthUsd: Double,
    val usdTryRate: Double = 33.0,
    val eurTryRate: Double = 36.0
)
