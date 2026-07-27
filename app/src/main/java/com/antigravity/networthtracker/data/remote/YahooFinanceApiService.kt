package com.antigravity.networthtracker.data.remote

import com.antigravity.networthtracker.data.remote.dto.YahooChartResponseDto
import com.antigravity.networthtracker.data.remote.dto.YahooFinanceResponseDto
import com.antigravity.networthtracker.data.remote.dto.YahooSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YahooFinanceApiService {
    
    @GET("v7/finance/quote")
    suspend fun getQuotes(
        @Query("symbols") symbols: String
    ): YahooFinanceResponseDto

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1d"
    ): YahooChartResponseDto

    @GET("v1/finance/search")
    suspend fun searchSymbols(
        @Query("q") query: String
    ): YahooSearchResponseDto

    companion object {
        const val BASE_URL = "https://query1.finance.yahoo.com/"
    }
}
