package com.antigravity.networthtracker.domain.usecase

import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.domain.model.NetWorthResult
import com.antigravity.networthtracker.domain.model.PriceInfo
import com.antigravity.networthtracker.domain.repository.AssetRepository
import com.antigravity.networthtracker.domain.repository.LivePriceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CalculateNetWorthUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
    private val livePriceRepository: LivePriceRepository
) {
    operator fun invoke(): Flow<Result<NetWorthResult>> = flow {
        assetRepository.getAssetsWithTransactions().collect { assetsWithTx ->
            try {
                val autoUpdateAssets = assetsWithTx.filter { it.asset.isAutoUpdate && !it.asset.symbol.isNullOrBlank() }
                val distinctSymbolsAndTypes = autoUpdateAssets.map { it.asset.symbol!! to it.asset.type }.distinct()
                val symbols = distinctSymbolsAndTypes.map { it.first }
                val types = distinctSymbolsAndTypes.map { it.second }

                val allQuerySymbols = symbols.toMutableList()
                val queryTypes = types.toMutableList()
                
                if (!allQuerySymbols.contains("USDTRY=X")) {
                    allQuerySymbols.add("USDTRY=X")
                    queryTypes.add(AssetType.CASH)
                }
                if (!allQuerySymbols.contains("EURTRY=X")) {
                    allQuerySymbols.add("EURTRY=X")
                    queryTypes.add(AssetType.CASH)
                }

                val livePricesResult = livePriceRepository.getLivePrices(allQuerySymbols, queryTypes)
                if (livePricesResult.isFailure) {
                    emit(Result.failure(livePricesResult.exceptionOrNull() ?: Exception("Failed to fetch live prices")))
                    return@collect
                }
                val livePrices = livePricesResult.getOrThrow()

                val usdTryRate = livePrices["USDTRY=X"]?.price ?: 33.0
                val exchangeRate = if (usdTryRate > 0) usdTryRate else 33.0
                val eurTryRate = livePrices["EURTRY=X"]?.price ?: 36.0

                var totalAssetsTry = 0.0
                var totalLiabilitiesTry = 0.0

                for (assetWithTx in assetsWithTx) {
                    val asset = assetWithTx.asset
                    val transactions = assetWithTx.transactions

                    if (transactions.isEmpty()) continue

                    val rawValue = if (asset.isAutoUpdate) {
                        val totalQty = transactions.sumOf { it.quantity }
                        val livePrice = livePrices[asset.symbol]?.price ?: transactions.lastOrNull()?.price ?: 0.0
                        totalQty * livePrice
                    } else {
                        transactions.sumOf { it.quantity * it.price }
                    }

                    val originalCurrencyValue = if (asset.type == AssetType.FUND) {
                        val taxPercent = livePrices[asset.symbol]?.tefasFundDetails?.taxPercent
                        if (taxPercent != null && taxPercent > 0.0) {
                            val totalCost = transactions.filter { it.quantity > 0 }.sumOf { it.quantity * it.price }
                            val profitLoss = rawValue - totalCost
                            if (profitLoss > 0.0) {
                                val stopaj = profitLoss * (taxPercent / 100.0)
                                rawValue - stopaj
                            } else {
                                rawValue
                            }
                        } else {
                            rawValue
                        }
                    } else {
                        rawValue
                    }

                    val valueInTry = when (asset.currency.uppercase()) {
                        "USD" -> originalCurrencyValue * exchangeRate
                        "EUR" -> originalCurrencyValue * eurTryRate
                        else -> originalCurrencyValue
                    }

                    if (asset.isLiability) {
                        totalLiabilitiesTry += valueInTry
                    } else {
                        totalAssetsTry += valueInTry
                    }
                }

                val netWorthTry = totalAssetsTry - totalLiabilitiesTry
                val totalAssetsUsd = totalAssetsTry / exchangeRate
                val totalLiabilitiesUsd = totalLiabilitiesTry / exchangeRate
                val netWorthUsd = netWorthTry / exchangeRate

                emit(
                    Result.success(
                        NetWorthResult(
                            totalAssetsTry = totalAssetsTry,
                            totalLiabilitiesTry = totalLiabilitiesTry,
                            netWorthTry = netWorthTry,
                            totalAssetsUsd = totalAssetsUsd,
                            totalLiabilitiesUsd = totalLiabilitiesUsd,
                            netWorthUsd = netWorthUsd,
                            usdTryRate = exchangeRate,
                            eurTryRate = eurTryRate
                        )
                    )
                )
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }
    }
}
