package com.antigravity.networthtracker.domain.model

data class AssetWithTransactions(
    val asset: Asset,
    val transactions: List<Transaction>
)
