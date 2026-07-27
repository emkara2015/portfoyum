package com.antigravity.networthtracker.domain.repository

import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.domain.model.SearchSuggestion
import com.antigravity.networthtracker.domain.model.PriceInfo

interface LivePriceRepository {
    suspend fun getLivePrice(symbol: String, type: AssetType): Result<PriceInfo>
    
    suspend fun getLivePrices(symbols: List<String>, types: List<AssetType>): Result<Map<String, PriceInfo>>

    suspend fun searchSymbols(query: String): Result<List<SearchSuggestion>>

    suspend fun searchTefasFunds(query: String): Result<List<SearchSuggestion>>

    suspend fun getHistoricalPrices(
        symbol: String,
        type: AssetType,
        range: String = "1mo",
        interval: String = "1d"
    ): Result<List<Pair<Long, Double>>>
}
