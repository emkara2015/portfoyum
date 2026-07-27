package com.antigravity.networthtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.antigravity.networthtracker.domain.model.AssetType

@Entity(tableName = "assets_table")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val assetId: Long = 0,
    val assetType: AssetType,
    val name: String,
    val symbol: String?,
    val currency: String,
    val isLiability: Boolean,
    val isAutoUpdate: Boolean,
    val initialPrice: Double = 0.0
)
