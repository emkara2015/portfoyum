package com.antigravity.networthtracker.domain.repository

import com.antigravity.networthtracker.domain.model.NewsCategory
import com.antigravity.networthtracker.domain.model.NewsItem

interface NewsRepository {
    suspend fun getNews(category: NewsCategory): Result<List<NewsItem>>
}
