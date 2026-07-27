package com.antigravity.networthtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.antigravity.networthtracker.data.local.entity.AssetEntity
import com.antigravity.networthtracker.data.local.entity.TransactionEntity
import com.antigravity.networthtracker.data.local.entity.DailyNetWorthEntity

@Database(
    entities = [AssetEntity::class, TransactionEntity::class, DailyNetWorthEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun dailyNetWorthDao(): DailyNetWorthDao
}
