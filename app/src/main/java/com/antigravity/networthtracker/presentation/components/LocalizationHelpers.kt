package com.antigravity.networthtracker.presentation.components

import com.antigravity.networthtracker.R
import com.antigravity.networthtracker.domain.model.AssetType

fun AssetType.getLocalizedNameRes(): Int {
    return when (this) {
        AssetType.STOCK -> R.string.cat_stock
        AssetType.CRYPTO -> R.string.cat_crypto
        AssetType.METAL -> R.string.cat_metal
        AssetType.FUND -> R.string.cat_fund
        AssetType.EUROBOND -> R.string.cat_eurobond
        AssetType.CASH -> R.string.cat_cash
        AssetType.REAL_ESTATE -> R.string.cat_real_estate
        AssetType.VEHICLE -> R.string.cat_vehicle
        AssetType.BES -> R.string.cat_bes
        AssetType.DEBT -> R.string.cat_debt
    }
}
