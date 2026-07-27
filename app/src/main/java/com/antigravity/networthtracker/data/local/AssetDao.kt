package com.antigravity.networthtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.antigravity.networthtracker.data.local.entity.AssetEntity
import com.antigravity.networthtracker.data.local.entity.AssetWithTransactionsRelation
import com.antigravity.networthtracker.data.local.entity.TransactionEntity
import com.antigravity.networthtracker.domain.model.AssetType
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets_table")
    fun getAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets_table WHERE assetId = :id")
    fun getAssetById(id: Long): Flow<AssetEntity?>

    @Query("SELECT * FROM assets_table WHERE assetType = :type AND symbol = :symbol LIMIT 1")
    suspend fun getAssetByTypeAndSymbol(type: AssetType, symbol: String): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Query("SELECT * FROM transactions_table WHERE assetId = :assetId ORDER BY date DESC")
    fun getTransactionsForAsset(assetId: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Transaction
    @Query("SELECT * FROM assets_table")
    fun getAssetsWithTransactions(): Flow<List<AssetWithTransactionsRelation>>
}
