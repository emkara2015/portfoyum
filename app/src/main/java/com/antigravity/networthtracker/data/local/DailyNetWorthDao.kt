package com.antigravity.networthtracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigravity.networthtracker.data.local.entity.DailyNetWorthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyNetWorthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyNetWorth(dailyNetWorth: DailyNetWorthEntity)

    @Query("SELECT * FROM daily_net_worth_table ORDER BY date ASC")
    fun getAllDailyNetWorth(): Flow<List<DailyNetWorthEntity>>
}
