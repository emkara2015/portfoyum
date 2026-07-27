package com.antigravity.networthtracker.domain.model

data class PriceInfo(
    val price: Double,
    val dailyChangePercent: Double,
    val tefasFundDetails: TefasFundDetails? = null
)
