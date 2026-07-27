package com.antigravity.networthtracker.data.repository

import com.antigravity.networthtracker.domain.model.NewsItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewsRssParser {
    fun parse(xmlContent: String): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentTitle = ""
            var currentLink = ""
            var currentPubDate = ""
            var currentSource = ""
            var insideItem = false
            var text = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            insideItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentPubDate = ""
                            currentSource = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        text = parser.text ?: ""
                    }
                    XmlPullParser.END_TAG -> {
                        if (insideItem) {
                            when {
                                tagName.equals("item", ignoreCase = true) -> {
                                    // Remove source suffix from title if Google News format "Title - Source"
                                    var cleanTitle = currentTitle
                                    if (currentSource.isNotEmpty()) {
                                        val suffix = " - $currentSource"
                                        if (cleanTitle.endsWith(suffix, ignoreCase = true)) {
                                            cleanTitle = cleanTitle.substring(0, cleanTitle.length - suffix.length).trim()
                                        }
                                    }
                                    items.add(
                                        NewsItem(
                                            title = cleanTitle.trim(),
                                            link = currentLink.trim(),
                                            pubDate = formatPubDate(currentPubDate),
                                            source = if (currentSource.isNotEmpty()) currentSource else "Google News"
                                        )
                                    )
                                    insideItem = false
                                }
                                tagName.equals("title", ignoreCase = true) -> {
                                    currentTitle = text
                                }
                                tagName.equals("link", ignoreCase = true) -> {
                                    currentLink = text
                                }
                                tagName.equals("pubDate", ignoreCase = true) -> {
                                    currentPubDate = text
                                }
                                tagName.equals("source", ignoreCase = true) -> {
                                    currentSource = text
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    private fun formatPubDate(rawDate: String): String {
        try {
            val formats = listOf(
                "EEE, dd MMM yyyy HH:mm:ss z",
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            var date: Date? = null
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    date = sdf.parse(rawDate)
                    if (date != null) break
                } catch (e: Exception) {
                    // try next format
                }
            }
            if (date != null) {
                val diffMs = System.currentTimeMillis() - date.time
                val diffMins = diffMs / (60 * 1000)
                val diffHours = diffMins / 60
                val diffDays = diffHours / 24

                val lang = Locale.getDefault().language
                return when (lang) {
                    "tr" -> {
                        when {
                            diffMins < 1 -> "Az önce"
                            diffMins < 60 -> "$diffMins dakika önce"
                            diffHours < 24 -> "$diffHours saat önce"
                            else -> "$diffDays gün önce"
                        }
                    }
                    "es" -> {
                        when {
                            diffMins < 1 -> "Hace un momento"
                            diffMins < 60 -> "Hace $diffMins minutos"
                            diffHours < 24 -> "Hace $diffHours horas"
                            else -> "Hace $diffDays días"
                        }
                    }
                    else -> {
                        when {
                            diffMins < 1 -> "Just now"
                            diffMins < 60 -> "$diffMins mins ago"
                            diffHours < 24 -> "$diffHours hours ago"
                            else -> "$diffDays days ago"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback to original date if parsing fails
        }
        return rawDate
    }
}
