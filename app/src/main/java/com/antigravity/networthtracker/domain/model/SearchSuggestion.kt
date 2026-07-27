package com.antigravity.networthtracker.domain.model

data class SearchSuggestion(
    val symbol: String,
    val name: String,
    val exchange: String?
)
