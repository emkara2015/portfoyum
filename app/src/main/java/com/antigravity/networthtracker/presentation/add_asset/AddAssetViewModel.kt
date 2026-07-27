package com.antigravity.networthtracker.presentation.add_asset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.domain.repository.LivePriceRepository
import com.antigravity.networthtracker.domain.usecase.AddAssetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.antigravity.networthtracker.domain.model.SearchSuggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.antigravity.networthtracker.R
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class AddAssetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addAssetUseCase: AddAssetUseCase,
    private val livePriceRepository: LivePriceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddAssetState())
    val state: StateFlow<AddAssetState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun selectAssetType(type: AssetType) {
        _state.update { 
            it.copy(
                selectedType = type,
                currency = if (type == AssetType.CRYPTO) "USD" else "TRY"
            ) 
        }
    }

    fun clearAssetType() {
        _state.update { AddAssetState() } // reset form
    }

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name.uppercase(java.util.Locale.getDefault()), errorMessageResId = null, errorMessageText = null) }
    }

    fun onFundCategoryChange(category: com.antigravity.networthtracker.presentation.add_asset.FundCategory) {
        _state.update {
            it.copy(
                fundCategory = category,
                symbol = "",
                name = "",
                searchSuggestions = emptyList(),
                isSymbolSelected = false,
                errorMessageResId = null,
                errorMessageText = null
            )
        }
    }

    fun onSymbolChange(symbol: String) {
        _state.update { it.copy(symbol = symbol.uppercase(), errorMessageResId = null, errorMessageText = null, isSymbolSelected = false) }
        
        searchJob?.cancel()
        if (symbol.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300)
                _state.update { it.copy(isSearchLoading = true) }
                val searchResult = if (_state.value.selectedType == AssetType.FUND && _state.value.fundCategory == com.antigravity.networthtracker.presentation.add_asset.FundCategory.TEFAS) {
                    livePriceRepository.searchTefasFunds(symbol)
                } else {
                    livePriceRepository.searchSymbols(symbol)
                }

                searchResult
                    .onSuccess { suggestions ->
                        _state.update { 
                            it.copy(
                                searchSuggestions = suggestions,
                                isSearchLoading = false
                            ) 
                        }
                    }
                    .onFailure {
                        _state.update { 
                            it.copy(
                                searchSuggestions = emptyList(),
                                isSearchLoading = false
                            ) 
                        }
                    }
            }
        } else {
            _state.update { it.copy(searchSuggestions = emptyList()) }
        }
    }

    fun selectSuggestion(suggestion: SearchSuggestion) {
        _state.update { 
            it.copy(
                symbol = suggestion.symbol,
                name = suggestion.name,
                searchSuggestions = emptyList(),
                isSymbolSelected = true
            ) 
        }
        fetchLivePrice()
    }

    fun onQuantityChange(quantity: String) {
        _state.update { it.copy(quantity = quantity, errorMessageResId = null, errorMessageText = null) }
    }

    fun onPriceChange(price: String) {
        _state.update { it.copy(price = price, errorMessageResId = null, errorMessageText = null) }
    }

    fun onCurrencyChange(currency: String) {
        _state.update { it.copy(currency = currency) }
    }

    fun onDateChange(timestamp: Long) {
        _state.update { it.copy(purchaseDate = timestamp) }
    }

    fun fetchLivePrice() {
        val currentState = _state.value
        val symbol = currentState.symbol.trim()
        val type = currentState.selectedType
 
        if (symbol.isBlank() || type == null) {
            _state.update { it.copy(errorMessageResId = R.string.error_enter_symbol_first, errorMessageText = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLivePriceLoading = true, errorMessageResId = null, errorMessageText = null) }
            livePriceRepository.getLivePrice(symbol, type)
                .onSuccess { priceInfo ->
                    _state.update { 
                        it.copy(
                            price = if (priceInfo.price > 0.0) String.format(java.util.Locale.US, "%.5f", priceInfo.price) else it.price,
                            isLivePriceLoading = false
                        ) 
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            errorMessageResId = if (type == AssetType.FUND) null else R.string.error_fetch_price_failed,
                            errorMessageText = if (type == AssetType.FUND) null else error.localizedMessage,
                            isLivePriceLoading = false
                        ) 
                    }
                }
        }
    }

    fun saveAsset() {
        val currentState = _state.value
        val type = currentState.selectedType ?: return
        
        // Specialized logic for Precious Metals
        if (type == AssetType.METAL) {
            val metalCategory = currentState.metalCategory ?: return
            val (name, symbol, quantity) = when (metalCategory) {
                com.antigravity.networthtracker.domain.model.MetalCategory.GOLD -> {
                    val goldMode = currentState.goldInputMode ?: return
                    if (goldMode == com.antigravity.networthtracker.domain.model.GoldInputMode.PIECE) {
                        val pieceType = currentState.selectedGoldPieceType ?: return
                        val count = currentState.metalPieceCount.replace(',', '.').toDoubleOrNull()
                        if (count == null || count <= 0) {
                            _state.update { it.copy(errorMessageResId = R.string.error_invalid_quantity, errorMessageText = null) }
                            return
                        }
                        val pieceLabel = context.getString(pieceType.labelRes)
                        val formattedName = context.getString(R.string.metal_name_piece_format, pieceLabel, currentState.metalPieceCount)
                        Triple(formattedName, pieceType.symbol, count)
                    } else {
                        val karat = currentState.selectedGoldKarat ?: return
                        val grams = currentState.metalGramWeight.replace(',', '.').toDoubleOrNull()
                        if (grams == null || grams <= 0) {
                            _state.update { it.copy(errorMessageResId = R.string.error_invalid_quantity, errorMessageText = null) }
                            return
                        }
                        val karatLabel = context.getString(karat.labelRes)
                        val formattedName = context.getString(R.string.metal_name_gram_format, karatLabel, currentState.metalGramWeight)
                        Triple(formattedName, karat.symbol, grams)
                    }
                }
                else -> {
                    val grams = currentState.metalGramWeight.replace(',', '.').toDoubleOrNull()
                    if (grams == null || grams <= 0) {
                        _state.update { it.copy(errorMessageResId = R.string.error_invalid_quantity, errorMessageText = null) }
                        return
                    }
                    val catName = context.getString(metalCategory.nameRes)
                    val formattedName = context.getString(R.string.metal_name_gram_format, catName, currentState.metalGramWeight)
                    Triple(formattedName, metalCategory.defaultSymbol, grams)
                }
            }

            val priceInput = currentState.price.replace(',', '.').toDoubleOrNull()

            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, errorMessageResId = null, errorMessageText = null) }
                
                val livePrice = livePriceRepository.getLivePrice(symbol, AssetType.METAL).getOrNull()?.price ?: 0.0
                val unitPrice = if (priceInput != null && priceInput > 0) priceInput else livePrice
                
                val asset = Asset(
                    type = AssetType.METAL,
                    name = name,
                    symbol = symbol,
                    currency = "TRY",
                    isLiability = false,
                    isAutoUpdate = true,
                    initialPrice = unitPrice
                )

                addAssetUseCase(asset, quantity, unitPrice, purchaseDate = currentState.purchaseDate)
                    .onSuccess {
                        _state.update { it.copy(isLoading = false, isSuccess = true) }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessageResId = R.string.error_save_failed,
                                errorMessageText = error.localizedMessage
                            )
                        }
                    }
            }
            return
        }

        val isAutoUpdate = type == AssetType.STOCK || type == AssetType.CRYPTO || (type == AssetType.FUND && currentState.fundCategory == com.antigravity.networthtracker.presentation.add_asset.FundCategory.TEFAS)
        val isLiability = type == AssetType.DEBT

        val symbol = if (isAutoUpdate) currentState.symbol.trim() else null
        if (isAutoUpdate && symbol.isNullOrBlank()) {
            _state.update { it.copy(errorMessageResId = R.string.error_invalid_symbol, errorMessageText = null) }
            return
        }

        val name = if (isAutoUpdate) {
            if (currentState.name.isNotBlank()) currentState.name.trim().uppercase(java.util.Locale.getDefault()) else symbol!!.uppercase()
        } else currentState.name.trim().uppercase(java.util.Locale.getDefault())
        if (name.isBlank()) {
            _state.update { it.copy(errorMessageResId = R.string.error_invalid_name, errorMessageText = null) }
            return
        }

        val quantity = currentState.quantity.replace(',', '.').toDoubleOrNull() ?: if (!isAutoUpdate) 1.0 else null
        if (quantity == null || quantity <= 0) {
            _state.update { it.copy(errorMessageResId = R.string.error_invalid_quantity, errorMessageText = null) }
            return
        }

        val price = currentState.price.replace(',', '.').toDoubleOrNull()
        if (price == null || price < 0) {
            _state.update { it.copy(errorMessageResId = R.string.error_invalid_price, errorMessageText = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessageResId = null, errorMessageText = null) }
            
            val asset = Asset(
                type = type,
                name = name,
                symbol = if (isAutoUpdate) symbol!!.uppercase() else null,
                currency = currentState.currency,
                isLiability = isLiability,
                isAutoUpdate = isAutoUpdate,
                initialPrice = price
            )

            if (isAutoUpdate) {
                livePriceRepository.getLivePrice(symbol!!, type)
                    .onSuccess {
                        addAssetUseCase(asset, quantity, price, purchaseDate = currentState.purchaseDate)
                            .onSuccess {
                                _state.update { it.copy(isLoading = false, isSuccess = true) }
                            }
                            .onFailure { error ->
                                _state.update { 
                                    it.copy(
                                        isLoading = false,
                                        errorMessageResId = R.string.error_save_failed,
                                        errorMessageText = error.localizedMessage
                                    ) 
                                }
                            }
                    }
                    .onFailure { _ ->
                        // Fallback to saving asset with user-provided price if live fetch is unavailable
                        addAssetUseCase(asset, quantity, price, purchaseDate = currentState.purchaseDate)
                            .onSuccess {
                                _state.update { it.copy(isLoading = false, isSuccess = true) }
                            }
                            .onFailure { error ->
                                _state.update { 
                                    it.copy(
                                        isLoading = false,
                                        errorMessageResId = R.string.error_save_failed,
                                        errorMessageText = error.localizedMessage
                                    ) 
                                }
                            }
                    }
            } else {
                addAssetUseCase(asset, quantity, price, purchaseDate = currentState.purchaseDate)
                    .onSuccess {
                        _state.update { it.copy(isLoading = false, isSuccess = true) }
                    }
                    .onFailure { error ->
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                errorMessageResId = R.string.error_save_failed,
                                errorMessageText = error.localizedMessage
                            ) 
                        }
                    }
            }
        }
    }

    fun selectMetalCategory(category: com.antigravity.networthtracker.domain.model.MetalCategory) {
        _state.update {
            it.copy(
                metalCategory = category,
                goldInputMode = null,
                selectedGoldPieceType = null,
                selectedGoldKarat = null
            )
        }
    }

    fun selectGoldInputMode(mode: com.antigravity.networthtracker.domain.model.GoldInputMode) {
        _state.update {
            it.copy(
                goldInputMode = mode,
                selectedGoldPieceType = null,
                selectedGoldKarat = null
            )
        }
    }

    fun selectGoldPieceType(type: com.antigravity.networthtracker.domain.model.GoldPieceType) {
        _state.update { it.copy(selectedGoldPieceType = type) }
    }

    fun selectGoldKarat(karat: com.antigravity.networthtracker.domain.model.GoldKarat) {
        _state.update { it.copy(selectedGoldKarat = karat) }
    }

    fun onMetalPieceCountChange(count: String) {
        _state.update { it.copy(metalPieceCount = count) }
    }

    fun onMetalGramWeightChange(weight: String) {
        _state.update { it.copy(metalGramWeight = weight) }
    }
}
