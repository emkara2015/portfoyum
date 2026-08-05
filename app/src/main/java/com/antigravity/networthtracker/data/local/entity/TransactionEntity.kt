package com.antigravity.networthtracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions_table",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["assetId"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["assetId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val txId: Long = 0,
    val assetId: Long,
    val quantity: Double,
    val price: Double,
    val date: Long,
    @ColumnInfo(name = "note", defaultValue = "")
    val note: String = ""
)
