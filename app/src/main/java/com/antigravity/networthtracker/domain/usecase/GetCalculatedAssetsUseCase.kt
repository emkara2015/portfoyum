package com.antigravity.networthtracker.domain.usecase

import com.antigravity.networthtracker.domain.model.CalculatedAsset
import com.antigravity.networthtracker.domain.repository.AssetRepository
import com.antigravity.networthtracker.domain.repository.LivePriceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetCalculatedAssetsUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
    private val livePriceRepository: LivePriceRepository
) {
    operator fun invoke(): Flow<Result<List<CalculatedAsset>>> = flow {
        assetRepository.getAssetsWithTransactions().collect { assetsWithTx ->
            try {
                val autoUpdateAssets = assetsWithTx.filter { it.asset.isAutoUpdate && !it.asset.symbol.isNullOrBlank() }
                val distinctSymbolsAndTypes = autoUpdateAssets.map { it.asset.symbol!! to it.asset.type }.distinct()
                val symbols = distinctSymbolsAndTypes.map { it.first }
                val types = distinctSymbolsAndTypes.map { it.second }

                val livePricesResult = livePriceRepository.getLivePrices(symbols, types)
                val livePrices = livePricesResult.getOrDefault(emptyMap())

                val calculatedAssets = assetsWithTx.map { assetWithTx ->
                    val asset = assetWithTx.asset
                    val transactions = assetWithTx.transactions

                    val totalQuantity = transactions.sumOf { it.quantity }
                    
                    val currentPrice = if (asset.isAutoUpdate) {
                        val fetchedPrice = livePrices[asset.symbol]?.price
                        if (fetchedPrice != null && fetchedPrice > 0.0) {
                            fetchedPrice
                        } else {
                            transactions.lastOrNull()?.price ?: asset.initialPrice
                        }
                    } else {
                        transactions.lastOrNull()?.price ?: asset.initialPrice
                    }

                    val dailyChangePercent = if (asset.isAutoUpdate) {
                        livePrices[asset.symbol]?.dailyChangePercent ?: 0.0
                    } else {
                        0.0
                    }

                    val currentValue = if (asset.isAutoUpdate) {
                        totalQuantity * currentPrice
                    } else {
                        transactions.sumOf { it.quantity * it.price }
                    }

                    val totalCost = if (asset.isAutoUpdate) {
                        transactions.filter { it.quantity > 0 }.sumOf { it.quantity * it.price }
                    } else {
                        if (asset.initialPrice > 0.0) asset.initialPrice else transactions.filter { it.quantity > 0 }.sumOf { it.quantity * it.price }
                    }

                    val profitLoss = if (asset.isLiability) {
                        totalCost - currentValue
                    } else {
                        currentValue - totalCost
                    }
                    val profitLossPercentage = if (totalCost > 0.0) {
                        (profitLoss / totalCost) * 100.0
                    } else {
                        0.0
                    }

                    CalculatedAsset(
                        asset = asset,
                        totalQuantity = totalQuantity,
                        currentPrice = currentPrice,
                        currentValue = currentValue,
                        totalCost = totalCost,
                        profitLoss = profitLoss,
                        profitLossPercentage = profitLossPercentage,
                        dailyChangePercentage = dailyChangePercent,
                        tefasFundDetails = livePrices[asset.symbol]?.tefasFundDetails
                    )
                }

                emit(Result.success(calculatedAssets))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }
    }
}
