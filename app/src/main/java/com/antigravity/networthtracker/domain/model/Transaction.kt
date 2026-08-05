package com.antigravity.networthtracker.domain.model

data class Transaction(
    val id: Long = 0,
    val assetId: Long,
    val quantity: Double,
    val price: Double,
    val date: Long,
    val note: String = ""
)
