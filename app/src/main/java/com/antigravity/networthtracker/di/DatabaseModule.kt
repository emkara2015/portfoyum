package com.antigravity.networthtracker.di

import android.content.Context
import androidx.room.Room
import com.antigravity.networthtracker.data.local.AppDatabase
import com.antigravity.networthtracker.data.local.AssetDao
import com.antigravity.networthtracker.data.local.DailyNetWorthDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "networth_tracker.db"
        )
        .addMigrations(AppDatabase.MIGRATION_3_4)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideAssetDao(db: AppDatabase): AssetDao {
        return db.assetDao()
    }

    @Provides
    @Singleton
    fun provideDailyNetWorthDao(db: AppDatabase): DailyNetWorthDao {
        return db.dailyNetWorthDao()
    }
}
