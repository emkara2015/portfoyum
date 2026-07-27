package com.antigravity.networthtracker.domain.repository

import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.domain.model.AssetWithTransactions
import com.antigravity.networthtracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    fun getAssets(): Flow<List<Asset>>
    
    fun getAssetById(id: Long): Flow<Asset?>
    
    suspend fun getAssetByTypeAndSymbol(type: AssetType, symbol: String): Asset?
    
    suspend fun insertAsset(asset: Asset): Long
    
    suspend fun updateAsset(asset: Asset)
    
    suspend fun deleteAsset(asset: Asset)
    
    fun getTransactionsForAsset(assetId: Long): Flow<List<Transaction>>
    
    suspend fun insertTransaction(transaction: Transaction): Long
    
    suspend fun deleteTransaction(transaction: Transaction)
    
    fun getAssetsWithTransactions(): Flow<List<AssetWithTransactions>>
}
