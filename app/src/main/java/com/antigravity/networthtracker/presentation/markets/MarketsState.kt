package com.antigravity.networthtracker.presentation.markets

import com.antigravity.networthtracker.domain.model.NewsCategory
import com.antigravity.networthtracker.domain.model.NewsItem

data class MarketsState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedCategory: NewsCategory = NewsCategory.ECONOMY,
    val newsItems: List<NewsItem> = emptyList(),
    val errorMessage: String? = null
)
