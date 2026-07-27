package com.antigravity.networthtracker.data.repository

import com.antigravity.networthtracker.domain.model.NewsCategory
import com.antigravity.networthtracker.domain.model.NewsItem
import com.antigravity.networthtracker.domain.repository.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) : NewsRepository {

    override suspend fun getNews(category: NewsCategory): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        try {
            val lang = Locale.getDefault().language
            val url = getFeedUrl(category, lang)
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error: ${response.code}"))
            }

            val xmlContent = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
            val parser = NewsRssParser()
            val news = parser.parse(xmlContent)
            Result.success(news)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFeedUrl(category: NewsCategory, language: String): String {
        return when (language) {
            "tr" -> {
                when (category) {
                    NewsCategory.ECONOMY -> "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=tr&gl=TR&ceid=TR:tr"
                    NewsCategory.STOCKS -> "https://news.google.com/rss/search?q=borsa+OR+hisse+OR+hisse+senedi&hl=tr&gl=TR&ceid=TR:tr"
                    NewsCategory.CRYPTO -> "https://news.google.com/rss/search?q=kripto+OR+bitcoin+OR+ethereum&hl=tr&gl=TR&ceid=TR:tr"
                }
            }
            "es" -> {
                when (category) {
                    NewsCategory.ECONOMY -> "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=es&gl=ES&ceid=ES:es"
                    NewsCategory.STOCKS -> "https://news.google.com/rss/search?q=acciones+OR+bolsa+OR+finanzas&hl=es&gl=ES&ceid=ES:es"
                    NewsCategory.CRYPTO -> "https://news.google.com/rss/search?q=cripto+OR+bitcoin+OR+ethereum&hl=es&gl=ES&ceid=ES:es"
                }
            }
            else -> {
                when (category) {
                    NewsCategory.ECONOMY -> "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=en&gl=US&ceid=US:en"
                    NewsCategory.STOCKS -> "https://news.google.com/rss/search?q=stocks+OR+shares+OR+market&hl=en&gl=US&ceid=US:en"
                    NewsCategory.CRYPTO -> "https://news.google.com/rss/search?q=crypto+OR+bitcoin+OR+ethereum&hl=en&gl=US&ceid=US:en"
                }
            }
        }
    }
}
