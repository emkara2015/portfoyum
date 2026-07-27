package com.antigravity.networthtracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class YahooSearchResponseDto(
    val quotes: List<SearchQuoteDto> = emptyList()
)

@Serializable
data class SearchQuoteDto(
    val symbol: String,
    val shortname: String? = null,
    val longname: String? = null,
    val exchange: String? = null,
    val quoteType: String? = null
)
