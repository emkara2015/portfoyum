package com.antigravity.networthtracker.presentation.asset_detail

import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.model.CalculatedAsset
import com.antigravity.networthtracker.domain.model.Transaction

enum class ChartRange(val label: String, val rangeStr: String, val intervalStr: String) {
    ONE_DAY("1G", "1d", "5m"),
    ONE_WEEK("1H", "5d", "15m"),
    ONE_MONTH("1A", "1mo", "1d"),
    ONE_YEAR("1Y", "1y", "1d"),
    FIVE_YEARS("5Y", "5y", "1wk")
}

data class AssetDetailState(
    val isLoading: Boolean = false,
    val asset: Asset? = null,
    val calculatedAsset: CalculatedAsset? = null,
    val transactions: List<Transaction> = emptyList(),
    val errorMessage: String? = null,
    val isDeleted: Boolean = false,
    val chartData: List<Pair<Long, Double>> = emptyList(),
    val selectedRange: ChartRange = ChartRange.ONE_MONTH,
    val isChartLoading: Boolean = false,
    val tefasFundDetails: com.antigravity.networthtracker.domain.model.TefasFundDetails? = null
)
