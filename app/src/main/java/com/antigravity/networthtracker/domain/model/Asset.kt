package com.antigravity.networthtracker.domain.model

data class Asset(
    val id: Long = 0,
    val type: AssetType,
    val name: String,
    val symbol: String?,
    val currency: String,
    val isLiability: Boolean,
    val isAutoUpdate: Boolean,
    val initialPrice: Double = 0.0
)
