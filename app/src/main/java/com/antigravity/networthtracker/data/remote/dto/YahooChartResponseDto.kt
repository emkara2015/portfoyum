package com.antigravity.networthtracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class YahooChartResponseDto(
    val chart: ChartDataDto
)

@Serializable
data class ChartDataDto(
    val result: List<ChartResultDto>? = null,
    val error: ChartErrorDto? = null
)

@Serializable
data class ChartResultDto(
    val meta: ChartMetaDto,
    val timestamp: List<Long>? = null,
    val indicators: ChartIndicatorsDto? = null
)

@Serializable
data class ChartIndicatorsDto(
    val quote: List<ChartQuoteDto>? = null
)

@Serializable
data class ChartQuoteDto(
    val close: List<Double?>? = null
)

@Serializable
data class ChartMetaDto(
    val currency: String? = null,
    val symbol: String,
    val regularMarketPrice: Double? = null,
    val chartPreviousClose: Double? = null,
    val previousClose: Double? = null,
    val shortName: String? = null,
    val longName: String? = null
)

@Serializable
data class ChartErrorDto(
    val code: String? = null,
    val description: String? = null
)
