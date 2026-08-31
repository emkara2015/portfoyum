package com.antigravity.networthtracker.data.repository

import android.content.Context
import com.antigravity.networthtracker.data.remote.YahooFinanceApiService
import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.domain.model.SearchSuggestion
import com.antigravity.networthtracker.domain.model.PriceInfo
import com.antigravity.networthtracker.domain.repository.LivePriceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

import com.antigravity.networthtracker.data.remote.FintablesApiService
import com.antigravity.networthtracker.data.remote.TefasApiService
import com.antigravity.networthtracker.data.remote.dto.FintablesFundDto
import com.antigravity.networthtracker.data.remote.dto.TefasFundItemDto

class LivePriceRepositoryImpl @Inject constructor(
    private val apiService: YahooFinanceApiService,
    private val tefasApiService: TefasApiService,
    private val fintablesApiService: FintablesApiService,
    private val okHttpClient: okhttp3.OkHttpClient,
    @ApplicationContext private val context: Context
) : LivePriceRepository {

    private data class CachedPrice(
        val priceInfo: PriceInfo,
        val timestamp: Long
    )

    // Short-lived in-memory cache to deduplicate concurrent/rapid api requests
    private val priceCache = ConcurrentHashMap<String, CachedPrice>()
    private val CACHE_EXPIRATION_MS = 5000L // 5 seconds cache window

    // Persistent storage for offline availability
    private val sharedPrefs = context.getSharedPreferences("live_prices_cache", Context.MODE_PRIVATE)

    private fun getPersistedPrice(symbol: String): PriceInfo? {
        val price = sharedPrefs.getFloat("${symbol}_price", -1f)
        val change = sharedPrefs.getFloat("${symbol}_change", -1000f)
        if (price >= 0f && change > -1000f) {
            return PriceInfo(price.toDouble(), change.toDouble())
        }
        return null
    }

    private fun persistPrice(symbol: String, priceInfo: PriceInfo) {
        sharedPrefs.edit()
            .putFloat("${symbol}_price", priceInfo.price.toFloat())
            .putFloat("${symbol}_change", priceInfo.dailyChangePercent.toFloat())
            .apply()
    }

    override suspend fun getLivePrice(symbol: String, type: AssetType): Result<PriceInfo> {
        val uppercaseSymbol = symbol.uppercase().trim()
        val formatted = formatSymbol(uppercaseSymbol, type)
        val currentTime = System.currentTimeMillis()

        // 1. Check in-memory cache first
        priceCache[formatted]?.let { cached ->
            if (currentTime - cached.timestamp < CACHE_EXPIRATION_MS) {
                return Result.success(cached.priceInfo)
            }
        }
        
        // Special handling for Precious Metals
        if (type == AssetType.METAL || uppercaseSymbol.startsWith("XAU-") || uppercaseSymbol.startsWith("XAG-") || uppercaseSymbol.startsWith("XPT-") || uppercaseSymbol.startsWith("XPD-") || uppercaseSymbol.startsWith("COPPER-")) {
            val metalResult = getPreciousMetalLivePrice(uppercaseSymbol)
            if (metalResult.isSuccess) {
                return metalResult
            }
        }

        // Special handling for TEFAŞ Funds
        if (type == AssetType.FUND) {
            val tefasResult = getTefasFundLivePrice(uppercaseSymbol)
            if (tefasResult.isSuccess) {
                return tefasResult
            }
        }

        // 2. Try fetching from network
        try {
            val response = apiService.getChart(formatted)
            val meta = response.chart.result?.firstOrNull()?.meta
            val price = meta?.regularMarketPrice
            if (price != null) {
                val prevClose = meta.previousClose ?: meta.chartPreviousClose ?: price
                val changePercent = if (prevClose > 0.0) {
                    ((price - prevClose) / prevClose) * 100.0
                } else {
                    0.0
                }
                val priceInfo = PriceInfo(price, changePercent)
                
                // Cache in memory and persist in SharedPreferences
                priceCache[formatted] = CachedPrice(priceInfo, currentTime)
                persistPrice(formatted, priceInfo)
                
                return Result.success(priceInfo)
            }
        } catch (e: Exception) {
            // Network failed, try reading local persisted cache
            getPersistedPrice(formatted)?.let { persisted ->
                return Result.success(persisted)
            }
        }

        // 3. Fallback for stock symbol ".IS"
        if (type == AssetType.STOCK && !formatted.contains(".")) {
            val fallbackSymbol = "$formatted.IS"
            
            priceCache[fallbackSymbol]?.let { cached ->
                if (currentTime - cached.timestamp < CACHE_EXPIRATION_MS) {
                    return Result.success(cached.priceInfo)
                }
            }

            try {
                val response = apiService.getChart(fallbackSymbol)
                val meta = response.chart.result?.firstOrNull()?.meta
                val price = meta?.regularMarketPrice
                if (price != null) {
                    val prevClose = meta.previousClose ?: meta.chartPreviousClose ?: price
                    val changePercent = if (prevClose > 0.0) {
                        ((price - prevClose) / prevClose) * 100.0
                    } else {
                        0.0
                    }
                    val priceInfo = PriceInfo(price, changePercent)
                    
                    priceCache[fallbackSymbol] = CachedPrice(priceInfo, currentTime)
                    persistPrice(fallbackSymbol, priceInfo)
                    
                    return Result.success(priceInfo)
                }
            } catch (e: Exception) {
                getPersistedPrice(fallbackSymbol)?.let { persisted ->
                    return Result.success(persisted)
                }
            }
        }

        return Result.failure(Exception("Price not found for symbol: $symbol"))
    }

    override suspend fun getLivePrices(
        symbols: List<String>,
        types: List<AssetType>
    ): Result<Map<String, PriceInfo>> {
        if (symbols.isEmpty()) return Result.success(emptyMap())
        return try {
            val result = coroutineScope {
                symbols.zip(types).map { (symbol, type) ->
                    async {
                        val priceInfo = getLivePrice(symbol, type).getOrNull() ?: PriceInfo(0.0, 0.0)
                        symbol to priceInfo
                    }
                }.awaitAll()
            }.toMap()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchSymbols(query: String): Result<List<SearchSuggestion>> {
        return try {
            val response = apiService.searchSymbols(query)
            val suggestions = response.quotes.map { dto ->
                SearchSuggestion(
                    symbol = dto.symbol,
                    name = dto.shortname ?: dto.longname ?: dto.symbol,
                    exchange = dto.exchange
                )
            }
            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistoricalPrices(
        symbol: String,
        type: AssetType,
        range: String,
        interval: String
    ): Result<List<Pair<Long, Double>>> {
        val uppercaseSymbol = symbol.uppercase().trim()
        val formatted = formatSymbol(uppercaseSymbol, type)

        return try {
            val response = apiService.getChart(formatted, interval = interval, range = range)
            val result = response.chart.result?.firstOrNull()
            if (result != null) {
                val timestamps = result.timestamp ?: emptyList()
                val closes = result.indicators?.quote?.firstOrNull()?.close ?: emptyList()
                val dataPoints = timestamps.zip(closes)
                    .mapNotNull { (timestamp, close) ->
                        if (close != null) {
                            timestamp * 1000L to close
                        } else null
                    }
                Result.success(dataPoints)
            } else {
                // Try fallback for stock symbol ".IS"
                if (type == AssetType.STOCK && !formatted.contains(".")) {
                    val fallbackSymbol = "$formatted.IS"
                    val fallbackResponse = apiService.getChart(fallbackSymbol, interval = interval, range = range)
                    val fallbackResult = fallbackResponse.chart.result?.firstOrNull()
                    if (fallbackResult != null) {
                        val timestamps = fallbackResult.timestamp ?: emptyList()
                        val closes = fallbackResult.indicators?.quote?.firstOrNull()?.close ?: emptyList()
                        val dataPoints = timestamps.zip(closes)
                            .mapNotNull { (timestamp, close) ->
                                if (close != null) {
                                    timestamp * 1000L to close
                                } else null
                            }
                        return Result.success(dataPoints)
                    }
                }
                Result.failure(Exception("Historical data not found"))
            }
        } catch (e: Exception) {
            // Try fallback for stock symbol ".IS" in catch block too
            if (type == AssetType.STOCK && !formatted.contains(".")) {
                try {
                    val fallbackSymbol = "$formatted.IS"
                    val fallbackResponse = apiService.getChart(fallbackSymbol, interval = interval, range = range)
                    val fallbackResult = fallbackResponse.chart.result?.firstOrNull()
                    if (fallbackResult != null) {
                        val timestamps = fallbackResult.timestamp ?: emptyList()
                        val closes = fallbackResult.indicators?.quote?.firstOrNull()?.close ?: emptyList()
                        val dataPoints = timestamps.zip(closes)
                            .mapNotNull { (timestamp, close) ->
                                if (close != null) {
                                    timestamp * 1000L to close
                                } else null
                            }
                        return Result.success(dataPoints)
                    }
                } catch (fallbackEx: Exception) {
                    return Result.failure(fallbackEx)
                }
            }
            Result.failure(e)
        }
    }

    private suspend fun getPreciousMetalLivePrice(symbol: String): Result<PriceInfo> {
        val uppercaseSymbol = symbol.uppercase().trim()
        val currentTime = System.currentTimeMillis()

        priceCache[uppercaseSymbol]?.let { cached ->
            if (currentTime - cached.timestamp < CACHE_EXPIRATION_MS) {
                return Result.success(cached.priceInfo)
            }
        }

        return try {
            val isSilver = uppercaseSymbol.startsWith("XAG")
            val isPlatinum = uppercaseSymbol.startsWith("XPT")
            val isPalladium = uppercaseSymbol.startsWith("XPD")
            val isCopper = uppercaseSymbol.startsWith("COPPER")

            val metalTicker = when {
                isSilver -> "SI=F"
                isPlatinum -> "PL=F"
                isPalladium -> "PA=F"
                isCopper -> "HG=F"
                else -> "GC=F"
            }

            val metalChart = apiService.getChart(metalTicker)
            val usdTryChart = apiService.getChart("TRY=X")

            val metalMeta = metalChart.chart.result?.firstOrNull()?.meta
            val usdTryMeta = usdTryChart.chart.result?.firstOrNull()?.meta

            val metalPriceUsd = metalMeta?.regularMarketPrice ?: 0.0
            val usdTryRate = usdTryMeta?.regularMarketPrice ?: 33.0

            val prevMetalUsd = metalMeta?.previousClose ?: metalMeta?.chartPreviousClose ?: metalPriceUsd
            val prevUsdTry = usdTryMeta?.previousClose ?: usdTryMeta?.chartPreviousClose ?: usdTryRate

            val gramFactorUsdToTry = usdTryRate / 31.1034768
            val prevGramFactor = prevUsdTry / 31.1034768

            val hasGramPrice = metalPriceUsd * gramFactorUsdToTry
            val prevHasGramPrice = prevMetalUsd * prevGramFactor

            val unitPrice = when (uppercaseSymbol) {
                "XAU-HAS", "XAU-24K", "GOLD", "XAU" -> hasGramPrice
                "XAU-22K" -> hasGramPrice * 0.9166
                "XAU-18K" -> hasGramPrice * 0.7500
                "XAU-14K" -> hasGramPrice * 0.5833
                "XAU-CEYREK" -> 1.75 * (hasGramPrice * 0.9166) * 1.03
                "XAU-YARIM" -> 3.50 * (hasGramPrice * 0.9166) * 1.03
                "XAU-TAM" -> 7.00 * (hasGramPrice * 0.9166) * 1.03
                "XAU-ATA" -> 7.21 * (hasGramPrice * 0.9166) * 1.04
                "XAU-RESAT" -> 7.20 * (hasGramPrice * 0.9166) * 1.04
                "XAG-GRAM", "SILVER", "XAG" -> hasGramPrice
                "XPT-GRAM", "PLATINUM", "XPT" -> hasGramPrice
                "XPD-GRAM", "PALLADIUM", "XPD" -> hasGramPrice
                "COPPER-KG", "COPPER" -> (metalPriceUsd * usdTryRate) / 2.20462
                else -> hasGramPrice
            }

            val prevUnitPrice = when (uppercaseSymbol) {
                "XAU-HAS", "XAU-24K", "GOLD", "XAU" -> prevHasGramPrice
                "XAU-22K" -> prevHasGramPrice * 0.9166
                "XAU-18K" -> prevHasGramPrice * 0.7500
                "XAU-14K" -> prevHasGramPrice * 0.5833
                "XAU-CEYREK" -> 1.75 * (prevHasGramPrice * 0.9166) * 1.03
                "XAU-YARIM" -> 3.50 * (prevHasGramPrice * 0.9166) * 1.03
                "XAU-TAM" -> 7.00 * (prevHasGramPrice * 0.9166) * 1.03
                "XAU-ATA" -> 7.21 * (prevHasGramPrice * 0.9166) * 1.04
                "XAU-RESAT" -> 7.20 * (prevHasGramPrice * 0.9166) * 1.04
                "XAG-GRAM", "SILVER", "XAG" -> prevHasGramPrice
                "XPT-GRAM", "PLATINUM", "XPT" -> prevHasGramPrice
                "XPD-GRAM", "PALLADIUM", "XPD" -> prevHasGramPrice
                "COPPER-KG", "COPPER" -> (prevMetalUsd * prevUsdTry) / 2.20462
                else -> prevHasGramPrice
            }

            val changePercent = if (prevUnitPrice > 0.0) {
                ((unitPrice - prevUnitPrice) / prevUnitPrice) * 100.0
            } else 0.0

            val priceInfo = PriceInfo(unitPrice, changePercent)
            priceCache[uppercaseSymbol] = CachedPrice(priceInfo, currentTime)
            persistPrice(uppercaseSymbol, priceInfo)

            Result.success(priceInfo)
        } catch (e: Exception) {
            getPersistedPrice(uppercaseSymbol)?.let { persisted ->
                Result.success(persisted)
            } ?: Result.failure(e)
        }
    }

    private val BUILT_IN_TEFAS_FUNDS = listOf(
        // Tera Portföy Fonları
        SearchSuggestion("TP2", "TERA PORTFÖY PARA PİYASASI (TL) FONU", "TEFAŞ"),
        SearchSuggestion("TEJ", "TERA PORTFÖY HİSSE SENEDİ FONU (HİSSE SENEDİ YOĞUN FON)", "TEFAŞ"),
        SearchSuggestion("TPL", "TERA PORTFÖY DEĞİŞKEN FON", "TEFAŞ"),
        SearchSuggestion("TL2", "TERA PORTFÖY İKİNCİ PARA PİYASASI FONU", "TEFAŞ"),
        SearchSuggestion("TRE", "TERA PORTFÖY BİRİNCİ BORÇLANMA ARAÇLARI FONU", "TEFAŞ"),
        SearchSuggestion("TGO", "TERA PORTFÖY ALTIN FONU", "TEFAŞ"),
        SearchSuggestion("TBY", "TERA PORTFÖY BİRİNCİ DEĞİŞKEN FON", "TEFAŞ"),
        SearchSuggestion("TDG", "TERA PORTFÖY EUROBOND BORÇLANMA ARAÇLARI FONU", "TEFAŞ"),
        SearchSuggestion("TET", "TERA PORTFÖY TEKNOLOJİ DEĞİŞKEN FON", "TEFAŞ"),
        SearchSuggestion("TPS", "TERA PORTFÖY SERBEST FON", "TEFAŞ"),

        // Popüler TEFAŞ Fonları
        SearchSuggestion("TI2", "TEB PORTFÖY BORÇLANMA ARAÇLARI FONU", "TEFAŞ"),
        SearchSuggestion("TCD", "TACİRLER PORTFÖY DEĞİŞKEN FON", "TEFAŞ"),
        SearchSuggestion("NNF", "HEDEF PORTFÖY BİRİNCİ HİSSE SENEDİ FONU", "TEFAŞ"),
        SearchSuggestion("AFT", "AK PORTFÖY AMERİKA YABANCI HİSSE SENEDİ FONU", "TEFAŞ"),
        SearchSuggestion("MAC", "MARMARA CAPITAL PORTFÖY HİSSE SENEDİ FONU", "TEFAŞ"),
        SearchSuggestion("YAY", "YAPI KREDİ PORTFÖY YABANCI TEKNOLOJİ SEKTÖRÜ HİSSE SENEDİ FONU", "TEFAŞ"),
        SearchSuggestion("TI1", "İŞ PORTFÖY HİSSE SENEDİ FONU", "TEFAŞ"),
        SearchSuggestion("GAF", "GARANTİ PORTFÖY AMERİKA YABANCI HİSSE SENEDİ FONU", "TEFAŞ"),
        SearchSuggestion("AES", "AK PORTFÖY PETROL YABANCI BYF FON SEPETİ FONU", "TEFAŞ"),
        SearchSuggestion("AFV", "AK PORTFÖY AVROBOND BORÇLANMA ARAÇLARI FONU", "TEFAŞ"),
        SearchSuggestion("GBG", "GARANTİ PORTFÖY BİRİNCİ DEĞİŞKEN FON", "TEFAŞ"),
        SearchSuggestion("GTA", "GARANTİ PORTFÖY ALTIN FONU", "TEFAŞ"),
        SearchSuggestion("ZAA", "ZİRAAT PORTFÖY ALTIN FONU", "TEFAŞ"),
        SearchSuggestion("KDL", "KUVEYT TÜRK PORTFÖY KATILIM FONU", "TEFAŞ"),
        SearchSuggestion("MPS", "AZİMUT PORTFÖY BİRİNCİ HİSSE SENEDİ FONU", "TEFAŞ"),
        SearchSuggestion("HKH", "HEDEF PORTFÖY KATILIM HİSSE SENEDİ FONU", "TEFAŞ")
    )

    private var allFundsCache: List<FintablesFundDto>? = null
    private var allFundsCacheTime: Long = 0L

    private val SUPABASE_URL = "https://pxgbiedahlssklfjzwor.supabase.co/rest/v1/tefas_funds"
    private val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB4Z2JpZWRhaGxzc2tsZmp6d29yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ0NzM5OTQsImV4cCI6MjEwMDA0OTk5NH0.h5_IBFoAT1tFAw29lcWLsF1yC4GfoADSQ8XTSWCK6L0"

    override suspend fun searchTefasFunds(query: String): Result<List<SearchSuggestion>> {
        val trLocale = java.util.Locale.forLanguageTag("tr-TR")
        val uppercaseQuery = query.uppercase(trLocale).trim()
        if (uppercaseQuery.isBlank()) return Result.success(emptyList())

        // 1. Primary: Search in Supabase Database
        try {
            val url = "$SUPABASE_URL?select=symbol,name&or=(symbol.ilike.*${uppercaseQuery}*,name.ilike.*${uppercaseQuery}*)&limit=30"
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer $SUPABASE_KEY")
                .header("Cache-Control", "no-cache, no-store")
                .header("Pragma", "no-cache")
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                okHttpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                val jsonArray = org.json.JSONArray(jsonStr)
                if (jsonArray.length() > 0) {
                    val matches = mutableListOf<SearchSuggestion>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val sym = obj.optString("symbol", "")
                        val nm = obj.optString("name", "")
                        if (sym.isNotBlank()) {
                            matches.add(SearchSuggestion(sym, nm, "TEFAŞ"))
                        }
                    }
                    if (matches.isNotEmpty()) {
                        return Result.success(matches)
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Fallback: Search in Fintables cache / local built-in list
        val currentTime = System.currentTimeMillis()
        if (allFundsCache == null || currentTime - allFundsCacheTime > 86400_000L) {
            try {
                allFundsCache = fintablesApiService.getAllFunds()
                allFundsCacheTime = currentTime
            } catch (_: Exception) {
            }
        }

        val cachedFunds = allFundsCache
        if (!cachedFunds.isNullOrEmpty()) {
            val matches = cachedFunds.filter { item ->
                val codeMatch = item.code?.uppercase(trLocale)?.contains(uppercaseQuery) == true
                val titleMatch = item.title?.uppercase(trLocale)?.contains(uppercaseQuery) == true
                codeMatch || titleMatch
            }.take(30).map { item ->
                SearchSuggestion(
                    symbol = item.code?.uppercase() ?: "",
                    name = item.title ?: "",
                    exchange = "TEFAŞ"
                )
            }
            return Result.success(matches)
        }

        val localMatches = BUILT_IN_TEFAS_FUNDS.filter { item ->
            item.symbol.uppercase(trLocale).contains(uppercaseQuery) ||
            item.name.uppercase(trLocale).contains(uppercaseQuery)
        }

        return Result.success(localMatches)
    }

    private suspend fun getTefasFundLivePrice(symbol: String): Result<PriceInfo> {
        val uppercaseSymbol = symbol.uppercase().trim()
        val currentTime = System.currentTimeMillis()

        priceCache[uppercaseSymbol]?.let { cached ->
            if (currentTime - cached.timestamp < CACHE_EXPIRATION_MS) {
                return Result.success(cached.priceInfo)
            }
        }

        // 1. Primary: Fetch from Supabase Database
        try {
            val url = "$SUPABASE_URL?symbol=eq.$uppercaseSymbol&select=*"
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer $SUPABASE_KEY")
                .header("Cache-Control", "no-cache, no-store")
                .header("Pragma", "no-cache")
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                okHttpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                val jsonArray = org.json.JSONArray(jsonStr)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val price = obj.optDouble("price", 0.0)
                    if (price > 0.0) {
                        val taxPercent = if (obj.has("tax_percent") && !obj.isNull("tax_percent")) obj.getDouble("tax_percent") else null
                        val y1m = if (obj.has("yield_1m") && !obj.isNull("yield_1m")) obj.getDouble("yield_1m") else null
                        val y3m = if (obj.has("yield_3m") && !obj.isNull("yield_3m")) obj.getDouble("yield_3m") else null
                        val y6m = if (obj.has("yield_6m") && !obj.isNull("yield_6m")) obj.getDouble("yield_6m") else null
                        val y1y = if (obj.has("yield_1y") && !obj.isNull("yield_1y")) obj.getDouble("yield_1y") else null
                        val yYtd = if (obj.has("yield_ytd") && !obj.isNull("yield_ytd")) obj.getDouble("yield_ytd") else null

                        val fundDetails = com.antigravity.networthtracker.domain.model.TefasFundDetails(
                            taxPercent = taxPercent,
                            yield1m = y1m,
                            yield3m = y3m,
                            yield6m = y6m,
                            yield1y = y1y,
                            yieldYtd = yYtd
                        )

                        val priceInfo = PriceInfo(price, 0.0, tefasFundDetails = fundDetails)
                        priceCache[uppercaseSymbol] = CachedPrice(priceInfo, currentTime)
                        persistPrice(uppercaseSymbol, priceInfo)
                        return Result.success(priceInfo)
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Secondary Fallback: Direct Scraping
        try {
            val url = "https://fintables.com/fonlar/$uppercaseSymbol"
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                okHttpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                val priceRegex = Regex("""\\*"price\\*":\s*([0-9]+\.[0-9]+)""")
                val match = priceRegex.find(html)
                if (match != null) {
                    val price = match.groupValues[1].toDoubleOrNull()
                    if (price != null && price > 0.0) {
                        val taxRegex = Regex("""\\*"tax\\*":\s*([0-9\.]+)""")
                        val taxPercent = taxRegex.find(html)?.groupValues?.get(1)?.toDoubleOrNull()?.let { it * 100 }

                        val y1mRegex = Regex("""\\*"1m\\*":\s*\{[^}]*\\*"yield\\*":\s*([0-9\.-]+)""")
                        val y3mRegex = Regex("""\\*"3m\\*":\s*\{[^}]*\\*"yield\\*":\s*([0-9\.-]+)""")
                        val y6mRegex = Regex("""\\*"6m\\*":\s*\{[^}]*\\*"yield\\*":\s*([0-9\.-]+)""")
                        val y1yRegex = Regex("""\\*"1y\\*":\s*\{[^}]*\\*"yield\\*":\s*([0-9\.-]+)""")
                        val ytdRegex = Regex("""\\*"ytd\\*":\s*\{[^}]*\\*"yield\\*":\s*([0-9\.-]+)""")

                        val fundDetails = com.antigravity.networthtracker.domain.model.TefasFundDetails(
                            taxPercent = taxPercent,
                            yield1m = y1mRegex.find(html)?.groupValues?.get(1)?.toDoubleOrNull(),
                            yield3m = y3mRegex.find(html)?.groupValues?.get(1)?.toDoubleOrNull(),
                            yield6m = y6mRegex.find(html)?.groupValues?.get(1)?.toDoubleOrNull(),
                            yield1y = y1yRegex.find(html)?.groupValues?.get(1)?.toDoubleOrNull(),
                            yieldYtd = ytdRegex.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
                        )

                        val priceInfo = PriceInfo(price, 0.0, tefasFundDetails = fundDetails)
                        priceCache[uppercaseSymbol] = CachedPrice(priceInfo, currentTime)
                        persistPrice(uppercaseSymbol, priceInfo)
                        return Result.success(priceInfo)
                    }
                }
            }
        } catch (_: Exception) {
        }

        getPersistedPrice(uppercaseSymbol)?.let { persisted ->
            return Result.success(persisted)
        }

        val defaultPriceInfo = PriceInfo(0.0, 0.0)
        return Result.success(defaultPriceInfo)
    }

    private fun formatSymbol(symbol: String, type: AssetType): String {
        val uppercaseSymbol = symbol.uppercase().trim()
        return when (type) {
            AssetType.CRYPTO -> {
                if (!uppercaseSymbol.contains("-")) "${uppercaseSymbol}-USD" else uppercaseSymbol
            }
            AssetType.METAL -> {
                when {
                    uppercaseSymbol.startsWith("XAG") -> "SI=F"
                    uppercaseSymbol.startsWith("XPT") -> "PL=F"
                    uppercaseSymbol.startsWith("XPD") -> "PA=F"
                    uppercaseSymbol.startsWith("COPPER") -> "HG=F"
                    else -> "GC=F"
                }
            }
            AssetType.STOCK -> {
                uppercaseSymbol
            }
            else -> uppercaseSymbol
        }
    }
}
