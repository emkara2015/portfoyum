package com.antigravity.networthtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.antigravity.networthtracker.data.local.entity.AssetEntity
import com.antigravity.networthtracker.data.local.entity.TransactionEntity
import com.antigravity.networthtracker.data.local.entity.DailyNetWorthEntity

@Database(
    entities = [AssetEntity::class, TransactionEntity::class, DailyNetWorthEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun dailyNetWorthDao(): DailyNetWorthDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions_table ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
