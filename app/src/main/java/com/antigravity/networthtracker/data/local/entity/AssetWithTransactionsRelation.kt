package com.antigravity.networthtracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AssetWithTransactionsRelation(
    @Embedded val asset: AssetEntity,
    
    @Relation(
        parentColumn = "assetId",
        entityColumn = "assetId"
    )
    val transactions: List<TransactionEntity>
)
