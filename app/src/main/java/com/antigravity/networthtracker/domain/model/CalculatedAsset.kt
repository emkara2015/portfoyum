package com.antigravity.networthtracker.domain.model

data class CalculatedAsset(
    val asset: Asset,
    val totalQuantity: Double,
    val currentPrice: Double,
    val currentValue: Double,
    val totalCost: Double,
    val profitLoss: Double,
    val profitLossPercentage: Double,
    val dailyChangePercentage: Double = 0.0,
    val tefasFundDetails: TefasFundDetails? = null
) {
    val taxPercent: Double?
        get() = tefasFundDetails?.taxPercent

    val stopajAmount: Double
        get() {
            val tax = taxPercent ?: return 0.0
            if (asset.type != AssetType.FUND || tax <= 0.0 || profitLoss <= 0.0) return 0.0
            return profitLoss * (tax / 100.0)
        }

    val netCurrentValue: Double
        get() = currentValue - stopajAmount

    val netProfitLoss: Double
        get() = profitLoss - stopajAmount

    val netProfitLossPercentage: Double
        get() = if (totalCost > 0.0) (netProfitLoss / totalCost) * 100.0 else 0.0
}
