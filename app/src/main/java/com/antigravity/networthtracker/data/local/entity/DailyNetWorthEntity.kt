package com.antigravity.networthtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_net_worth_table")
data class DailyNetWorthEntity(
    @PrimaryKey
    val date: String, // format "yyyy-MM-dd"
    val netWorthTry: Double,
    val netWorthUsd: Double
)
