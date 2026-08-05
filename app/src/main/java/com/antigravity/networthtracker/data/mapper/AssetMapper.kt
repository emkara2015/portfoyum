package com.antigravity.networthtracker.data.mapper

import com.antigravity.networthtracker.data.local.entity.AssetEntity
import com.antigravity.networthtracker.data.local.entity.AssetWithTransactionsRelation
import com.antigravity.networthtracker.data.local.entity.TransactionEntity
import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.model.AssetWithTransactions
import com.antigravity.networthtracker.domain.model.Transaction

fun AssetEntity.toDomainAsset(): Asset {
    return Asset(
        id = assetId,
        type = assetType,
        name = name,
        symbol = symbol,
        currency = currency,
        isLiability = isLiability,
        isAutoUpdate = isAutoUpdate,
        initialPrice = initialPrice
    )
}

fun Asset.toAssetEntity(): AssetEntity {
    return AssetEntity(
        assetId = id,
        assetType = type,
        name = name,
        symbol = symbol,
        currency = currency,
        isLiability = isLiability,
        isAutoUpdate = isAutoUpdate,
        initialPrice = initialPrice
    )
}

fun TransactionEntity.toDomainTransaction(): Transaction {
    return Transaction(
        id = txId,
        assetId = assetId,
        quantity = quantity,
        price = price,
        date = date,
        note = note
    )
}

fun Transaction.toTransactionEntity(): TransactionEntity {
    return TransactionEntity(
        txId = id,
        assetId = assetId,
        quantity = quantity,
        price = price,
        date = date,
        note = note
    )
}

fun AssetWithTransactionsRelation.toDomainAssetWithTransactions(): AssetWithTransactions {
    return AssetWithTransactions(
        asset = asset.toDomainAsset(),
        transactions = transactions.map { it.toDomainTransaction() }
    )
}
