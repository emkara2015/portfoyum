package com.antigravity.networthtracker.data.repository

import com.antigravity.networthtracker.data.local.AssetDao
import com.antigravity.networthtracker.data.mapper.toAssetEntity
import com.antigravity.networthtracker.data.mapper.toDomainAsset
import com.antigravity.networthtracker.data.mapper.toDomainAssetWithTransactions
import com.antigravity.networthtracker.data.mapper.toDomainTransaction
import com.antigravity.networthtracker.data.mapper.toTransactionEntity
import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.domain.model.AssetWithTransactions
import com.antigravity.networthtracker.domain.model.Transaction
import com.antigravity.networthtracker.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao
) : AssetRepository {

    override fun getAssets(): Flow<List<Asset>> {
        return assetDao.getAssets().map { entities ->
            entities.map { it.toDomainAsset() }
        }
    }

    override fun getAssetById(id: Long): Flow<Asset?> {
        return assetDao.getAssetById(id).map { entity ->
            entity?.toDomainAsset()
        }
    }

    override suspend fun getAssetByTypeAndSymbol(type: AssetType, symbol: String): Asset? {
        return assetDao.getAssetByTypeAndSymbol(type, symbol)?.toDomainAsset()
    }

    override suspend fun insertAsset(asset: Asset): Long {
        return assetDao.insertAsset(asset.toAssetEntity())
    }

    override suspend fun updateAsset(asset: Asset) {
        assetDao.updateAsset(asset.toAssetEntity())
    }

    override suspend fun deleteAsset(asset: Asset) {
        assetDao.deleteAsset(asset.toAssetEntity())
    }

    override fun getTransactionsForAsset(assetId: Long): Flow<List<Transaction>> {
        return assetDao.getTransactionsForAsset(assetId).map { entities ->
            entities.map { it.toDomainTransaction() }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return assetDao.insertTransaction(transaction.toTransactionEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        assetDao.deleteTransaction(transaction.toTransactionEntity())
    }

    override fun getAssetsWithTransactions(): Flow<List<AssetWithTransactions>> {
        return assetDao.getAssetsWithTransactions().map { relations ->
            relations.map { it.toDomainAssetWithTransactions() }
        }
    }
}
