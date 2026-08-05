package com.antigravity.networthtracker.presentation.asset_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.model.CalculatedAsset
import com.antigravity.networthtracker.domain.model.Transaction
import com.antigravity.networthtracker.domain.repository.AssetRepository
import com.antigravity.networthtracker.domain.repository.LivePriceRepository
import com.antigravity.networthtracker.domain.usecase.DeleteAssetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetDetailViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val deleteAssetUseCase: DeleteAssetUseCase,
    private val livePriceRepository: LivePriceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AssetDetailState())
    val state: StateFlow<AssetDetailState> = _state.asStateFlow()

    private var currentAssetId: Long = -1

    fun loadAssetDetails(assetId: Long) {
        currentAssetId = assetId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, selectedRange = ChartRange.ONE_MONTH) }
            
            combine(
                assetRepository.getAssetById(assetId),
                assetRepository.getTransactionsForAsset(assetId)
            ) { asset, transactions ->
                if (asset == null) {
                    _state.update { it.copy(isLoading = false, errorMessage = "Varlık bulunamadı.") }
                    return@combine
                }
                
                var currentPrice = transactions.lastOrNull()?.price ?: 0.0
                var dailyChangePercent = 0.0
                var chartPoints = emptyList<Pair<Long, Double>>()
                
                var fetchedFundDetails: com.antigravity.networthtracker.domain.model.TefasFundDetails? = null
                
                val symbol = asset.symbol
                if (asset.isAutoUpdate && !symbol.isNullOrBlank()) {
                    livePriceRepository.getLivePrice(symbol, asset.type)
                        .onSuccess { priceInfo ->
                            if (priceInfo.price > 0.0) {
                                currentPrice = priceInfo.price
                            }
                            dailyChangePercent = priceInfo.dailyChangePercent
                            fetchedFundDetails = priceInfo.tefasFundDetails
                        }
                        .onFailure {
                            // fallback
                        }
                    
                    if (asset.type != com.antigravity.networthtracker.domain.model.AssetType.FUND) {
                        val currentRange = _state.value.selectedRange
                        livePriceRepository.getHistoricalPrices(symbol, asset.type, range = currentRange.rangeStr, interval = currentRange.intervalStr)
                            .onSuccess { points ->
                                chartPoints = points
                            }
                    }
                }
                
                val totalQuantity = transactions.sumOf { it.quantity }
                val currentValue = if (asset.isAutoUpdate) {
                    totalQuantity * currentPrice
                } else {
                    transactions.sumOf { it.quantity * it.price }
                }
                val totalCost = if (asset.isAutoUpdate) {
                    transactions.filter { it.quantity > 0 }.sumOf { it.quantity * it.price }
                } else {
                    if (asset.initialPrice > 0.0) asset.initialPrice else transactions.filter { it.quantity > 0 }.sumOf { it.quantity * it.price }
                }
                val profitLoss = if (asset.isLiability) {
                    totalCost - currentValue
                } else {
                    currentValue - totalCost
                }
                val profitLossPercentage = if (totalCost > 0.0) {
                    (profitLoss / totalCost) * 100.0
                } else {
                    0.0
                }
                
                val calculatedAsset = CalculatedAsset(
                    asset = asset,
                    totalQuantity = totalQuantity,
                    currentPrice = currentPrice,
                    currentValue = currentValue,
                    totalCost = totalCost,
                    profitLoss = profitLoss,
                    profitLossPercentage = profitLossPercentage,
                    dailyChangePercentage = dailyChangePercent,
                    tefasFundDetails = fetchedFundDetails
                )
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        asset = asset,
                        calculatedAsset = calculatedAsset,
                        transactions = transactions,
                        chartData = chartPoints,
                        tefasFundDetails = fetchedFundDetails,
                        errorMessage = null
                    )
                }
            }.catch { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Beklenmeyen hata."
                    )
                }
            }.collect()
        }
    }

    fun deleteAsset() {
        val asset = _state.value.asset ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            deleteAssetUseCase(asset)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isDeleted = true) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Silinirken hata: ${error.localizedMessage}"
                        )
                    }
                }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                if (_state.value.transactions.size <= 1) {
                    deleteAsset()
                } else {
                    assetRepository.deleteTransaction(transaction)
                    loadAssetDetails(currentAssetId)
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "İşlem silinirken hata: ${e.localizedMessage}") }
            }
        }
    }

    fun updateTransaction(transaction: Transaction, newQuantity: Double, newPrice: Double, newDate: Long, newNote: String = transaction.note) {
        viewModelScope.launch {
            try {
                val updatedTx = transaction.copy(
                    quantity = newQuantity,
                    price = newPrice,
                    date = newDate,
                    note = newNote
                )
                assetRepository.insertTransaction(updatedTx)
                loadAssetDetails(currentAssetId)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "İşlem güncellenirken hata: ${e.localizedMessage}") }
            }
        }
    }

    fun updateAssetNote(newNote: String) {
        val currentAsset = _state.value.asset ?: return
        val updatedAsset = currentAsset.copy(
            name = if (newNote.isNotBlank()) newNote.trim() else currentAsset.symbol ?: currentAsset.name
        )
        viewModelScope.launch {
            try {
                assetRepository.updateAsset(updatedAsset)
                loadAssetDetails(currentAssetId)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Not güncellenirken hata: ${e.localizedMessage}") }
            }
        }
    }

    fun selectChartRange(range: ChartRange) {
        _state.update { it.copy(selectedRange = range) }
        val asset = _state.value.asset ?: return
        val symbol = asset.symbol
        if (asset.isAutoUpdate && !symbol.isNullOrBlank()) {
            viewModelScope.launch {
                _state.update { it.copy(isChartLoading = true) }
                livePriceRepository.getHistoricalPrices(symbol, asset.type, range.rangeStr, range.intervalStr)
                    .onSuccess { points ->
                        _state.update { it.copy(chartData = points, isChartLoading = false) }
                    }
                    .onFailure {
                        _state.update { it.copy(isChartLoading = false) }
                    }
            }
        }
    }
}
