package com.antigravity.networthtracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class YahooFinanceQuoteDto(
    val symbol: String,
    val regularMarketPrice: Double? = null,
    val currency: String? = null,
    val quoteType: String? = null,
    val longName: String? = null
)

@Serializable
data class YahooFinanceQuoteResponseDto(
    val result: List<YahooFinanceQuoteDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class YahooFinanceResponseDto(
    val quoteResponse: YahooFinanceQuoteResponseDto
)
